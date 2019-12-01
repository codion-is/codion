/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.Configuration;
import org.jminor.common.Util;
import org.jminor.common.db.DatabaseConnection;
import org.jminor.common.db.valuemap.ValueProvider;
import org.jminor.common.value.PropertyValue;
import org.jminor.framework.domain.property.ColumnProperty;
import org.jminor.framework.domain.property.DerivedProperty;
import org.jminor.framework.domain.property.ForeignKeyProperty;
import org.jminor.framework.domain.property.Property;
import org.jminor.framework.domain.property.TransientProperty;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * A repository specifying the {@link EntityDefinition}s for a given domain.
 * Factory for {@link Entity} and {@link Entity.Key} instances.
 */
public class Domain implements EntityDefinition.Provider, Serializable {

  private static final long serialVersionUID = 1;

  /**
   * Specifies whether or not to allow entities to be re-defined, that is,
   * allow a new definition to replace an old one.
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final PropertyValue<Boolean> ALLOW_REDEFINE_ENTITY = Configuration.booleanValue("jminor.domain.allowRedefineEntity", false);

  private static final String ENTITY_ID_PARAM = "entityId";
  private static final String ENTITY_PARAM = "entity";

  private static final Map<String, Domain> REGISTERED_DOMAINS = new HashMap<>();

  private final String domainId;
  private final DefaultEntityDefinitionProvider definitionProvider = new DefaultEntityDefinitionProvider();
  private final transient Map<String, DatabaseConnection.Operation> databaseOperations = new HashMap<>();

  private Map<Class, EntityDefinition> beanEntities;
  private Map<String, Map<String, BeanProperty>> beanProperties;

  /**
   * Instantiates a new Domain with the simple name of the class as domain id
   * @see Class#getSimpleName()
   */
  public Domain() {
    this.domainId = getClass().getSimpleName();
  }

  /**
   * Instantiates a new Domain
   * @param domainId the domain identifier
   */
  public Domain(final String domainId) {
    this.domainId = requireNonNull(domainId, "domainId");
  }

  /**
   * Instantiates a new domain and copies all the entity definitions
   * and database operations from {@code domain}
   * @param domain the domain to copy
   */
  public Domain(final Domain domain) {
    this.domainId = requireNonNull(domain).domainId;
    this.definitionProvider.entityDefinitions.putAll(domain.definitionProvider.entityDefinitions);
    this.beanEntities = domain.beanEntities;
    this.beanProperties = domain.beanProperties;
    if (domain.databaseOperations != null) {
      this.databaseOperations.putAll(domain.databaseOperations);
    }
  }

  /**
   * @return the domain Id
   */
  public final String getDomainId() {
    return domainId;
  }

  /**
   * Creates a new {@link Entity} instance with the given entityId
   * @param entityId the entity id
   * @return a new {@link Entity} instance
   */
  public final Entity entity(final String entityId) {
    return entity(getDefinition(entityId), null, null);
  }

  /**
   * Creates a new {@link Entity} instance with the given primary key
   * @param key the primary key
   * @return a new {@link Entity} instance
   */
  public final Entity entity(final Entity.Key key) {
    return new DefaultEntity(this, key);
  }

  /**
   * Instantiates a new {@link Entity} instance with the given values and original values.
   * @param entityId the entity id
   * @param values the values
   * @param originalValues the original values
   * @return a new {@link Entity} instance
   */
  public final Entity entity(final String entityId, final Map<Property, Object> values,
                             final Map<Property, Object> originalValues) {
    return entity(getDefinition(entityId), values, originalValues);
  }

  /**
   * Instantiates a new {@link Entity} instance with the given values and original values.
   * @param entityDefinition the entity definition
   * @param values the values
   * @param originalValues the original values
   * @return a new {@link Entity} instance
   */
  public final Entity entity(final EntityDefinition entityDefinition, final Map<Property, Object> values,
                             final Map<Property, Object> originalValues) {
    return new DefaultEntity(this, entityDefinition, values, originalValues);
  }

  /**
   * Instantiates a new {@link Entity} of the given type using the values provided by {@code valueProvider}.
   * Values are fetched for {@link ColumnProperty} and its descendants, {@link ForeignKeyProperty}
   * and {@link TransientProperty} (excluding its descendants).
   * If a {@link ColumnProperty}s underlying column has a default value the property is
   * skipped unless the property itself has a default value, which then overrides the columns default value.
   * @param entityId the entity id
   * @param valueProvider the value provider
   * @return the populated entity
   * @see ColumnProperty.Builder#setColumnHasDefaultValue(boolean)
   * @see ColumnProperty.Builder#setDefaultValue(Object)
   */
  public final Entity defaultEntity(final String entityId, final ValueProvider<Property, Object> valueProvider) {
    final Entity entity = entity(entityId);
    final EntityDefinition entityDefinition = getDefinition(entityId);
    final Collection<ColumnProperty> columnProperties = entityDefinition.getColumnProperties();
    for (final ColumnProperty property : columnProperties) {
      if (!property.isForeignKeyProperty() && !property.isDenormalized()//these are set via their respective parent properties
              && (!property.columnHasDefaultValue() || property.hasDefaultValue())) {
        entity.put(property, valueProvider.get(property));
      }
    }
    final Collection<TransientProperty> transientProperties = entityDefinition.getTransientProperties();
    for (final TransientProperty transientProperty : transientProperties) {
      if (!(transientProperty instanceof DerivedProperty)) {
        entity.put(transientProperty, valueProvider.get(transientProperty));
      }
    }
    final Collection<ForeignKeyProperty> foreignKeyProperties = entityDefinition.getForeignKeyProperties();
    for (final ForeignKeyProperty foreignKeyProperty : foreignKeyProperties) {
      entity.put(foreignKeyProperty, valueProvider.get(foreignKeyProperty));
    }
    entity.saveAll();

    return entity;
  }

  /**
   * Transforms the given entities into beans according to the information found in this Domain model
   * @param entities the entities to transform
   * @return a List containing the beans derived from the given entities, an empty List if {@code entities} is null or empty
   * @see EntityDefinition.Builder#setBeanClass(Class)
   * @see Property.Builder#setBeanProperty(String)
   */
  public final List<Object> toBeans(final List<Entity> entities) {
    if (Util.nullOrEmpty(entities)) {
      return emptyList();
    }
    final List<Object> beans = new ArrayList<>(entities.size());
    for (final Entity entity : entities) {
      beans.add(toBean(entity));
    }

    return beans;
  }

  /**
   * Transforms the given entity into a bean according to the information found in this Domain model
   * @param <V> the bean type
   * @param entity the entity to transform
   * @return a bean derived from the given entity
   * @see EntityDefinition.Builder#setBeanClass(Class)
   * @see Property.Builder#setBeanProperty(String)
   */
  public <V> V toBean(final Entity entity) {
    requireNonNull(entity, ENTITY_PARAM);
    final EntityDefinition definition = getDefinition(entity.getEntityId());
    final Class<V> beanClass = definition.getBeanClass();
    if (beanClass == null) {
      throw new IllegalArgumentException("No bean class defined for entityId: " + definition.getEntityId());
    }
    final Map<String, BeanProperty> beanPropertyMap = getBeanProperties(definition.getEntityId());
    try {
      final V bean = beanClass.getConstructor().newInstance();
      for (final Map.Entry<String, BeanProperty> propertyEntry : beanPropertyMap.entrySet()) {
        final Property property = definition.getProperty(propertyEntry.getKey());
        Object value = entity.get(property);
        if (property instanceof ForeignKeyProperty && value != null) {
          value = toBean((Entity) value);
        }

        propertyEntry.getValue().setter.invoke(bean, value);
      }

      return bean;
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Transforms the given beans into a entities according to the information found in this Domain model
   * @param beans the beans to transform
   * @return a List containing the entities derived from the given beans, an empty List if {@code beans} is null or empty
   * @see EntityDefinition.Builder#setBeanClass(Class)
   * @see Property.Builder#setBeanProperty(String)
   * */
  public final List<Entity> fromBeans(final List beans) {
    if (Util.nullOrEmpty(beans)) {
      return emptyList();
    }
    final List<Entity> result = new ArrayList<>(beans.size());
    for (final Object bean : beans) {
      result.add(fromBean(bean));
    }

    return result;
  }

  /**
   * Creates an Entity from the given bean object.
   * @param bean the bean to convert to an Entity
   * @param <V> the bean type
   * @return a Entity based on the given bean
   * @see EntityDefinition.Builder#setBeanClass(Class)
   * @see Property.Builder#setBeanProperty(String)
   */
  public <V> Entity fromBean(final V bean) {
    requireNonNull(bean, "bean");
    final Class beanClass = bean.getClass();
    final EntityDefinition definition = getBeanEntity(beanClass);
    final Entity entity = entity(definition.getEntityId());
    try {
      final Map<String, BeanProperty> beanPropertyMap = getBeanProperties(definition.getEntityId());
      for (final Map.Entry<String, BeanProperty> propertyEntry : beanPropertyMap.entrySet()) {
        final Property property = definition.getProperty(propertyEntry.getKey());
        Object value = propertyEntry.getValue().getter.invoke(bean);
        if (property instanceof ForeignKeyProperty && value != null) {
          value = fromBean(value);
        }

        entity.put(property, value);
      }

      return entity;
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Creates a new {@link Entity.Key} instance with the given entityId
   * @param entityId the entity id
   * @return a new {@link Entity.Key} instance
   */
  public final Entity.Key key(final String entityId) {
    return new DefaultEntityKey(getDefinition(entityId), null);
  }

  /**
   * Creates a new {@link Entity.Key} instance with the given entityId, initialised with the given value
   * @param entityId the entity id
   * @param value the key value, assumes a single integer key
   * @return a new {@link Entity.Key} instance
   * @throws IllegalArgumentException in case the given primary key is a composite key
   * @throws NullPointerException in case entityId or value is null
   */
  public final Entity.Key key(final String entityId, final Integer value) {
    return new DefaultEntityKey(getDefinition(entityId), value);
  }

  /**
   * Creates a new {@link Entity.Key} instance with the given entityId, initialised with the given value
   * @param entityId the entity id
   * @param value the key value, assumes a single long key
   * @return a new {@link Entity.Key} instance
   * @throws IllegalArgumentException in case the given primary key is a composite key
   * @throws NullPointerException in case entityId or value is null
   */
  public final Entity.Key key(final String entityId, final Long value) {
    return new DefaultEntityKey(getDefinition(entityId), value);
  }

  /**
   * Creates new {@link Entity.Key} instances with the given entityId, initialised with the given values
   * @param entityId the entity id
   * @param values the key values, assumes a single integer key
   * @return new {@link Entity.Key} instances
   * @throws IllegalArgumentException in case the given primary key is a composite key
   * @throws NullPointerException in case entityId or values is null
   */
  public final List<Entity.Key> keys(final String entityId, final Integer... values) {
    requireNonNull(values, "values");
    return Arrays.stream(values).map(value -> key(entityId, value)).collect(toList());
  }

  /**
   * Creates new {@link Entity.Key} instances with the given entityId, initialised with the given values
   * @param entityId the entity id
   * @param values the key values, assumes a single integer key
   * @return new {@link Entity.Key} instances
   * @throws IllegalArgumentException in case the given primary key is a composite key
   * @throws NullPointerException in case entityId or values is null
   */
  public final List<Entity.Key> keys(final String entityId, final Long... values) {
    requireNonNull(values, "values");
    return Arrays.stream(values).map(value -> key(entityId, value)).collect(toList());
  }

  /**
   * Copies the given entities, with new copied instances of all foreign key value entities.
   * @param entities the entities to copy
   * @return deep copies of the entities, in the same order as they are received
   */
  public final List<Entity> deepCopyEntities(final List<Entity> entities) {
    requireNonNull(entities, "entities");

    return entities.stream().map(this::deepCopyEntity).collect(toList());
  }

  /**
   * Copies the given entity.
   * @param entity the entity to copy
   * @return copy of the given entity
   */
  public final Entity copyEntity(final Entity entity) {
    requireNonNull(entity, ENTITY_PARAM);
    final Entity copy = entity(entity.getEntityId());
    copy.setAs(entity);

    return copy;
  }

  /**
   * Copies the given entity, with new copied instances of all foreign key value entities.
   * @param entity the entity to copy
   * @return a deep copy of the given entity
   */
  public final Entity deepCopyEntity(final Entity entity) {
    requireNonNull(entity, ENTITY_PARAM);
    final Entity copy = entity(entity.getEntityId());
    copy.setAs(entity);
    for (final ForeignKeyProperty foreignKeyProperty : getDefinition(entity.getEntityId()).getForeignKeyProperties()) {
      final Entity foreignKeyValue = (Entity) entity.get(foreignKeyProperty);
      if (foreignKeyValue != null) {
        entity.put(foreignKeyProperty, deepCopyEntity(foreignKeyValue));
      }
    }

    return copy;
  }

  /**
   * Copies the given key.
   * @param key the key to copy
   * @return a copy of the given key
   */
  public final Entity.Key copyKey(final Entity.Key key) {
    requireNonNull(key, "key");
    final Entity.Key copy = key(key.getEntityId());
    copy.setAs(key);

    return copy;
  }

  /** {@inheritDoc} */
  @Override
  public final EntityDefinition getDefinition(final String entityId) {
    return definitionProvider.getDefinition(entityId);
  }

  /**
   * @return all {@link EntityDefinition}s found in this domain model
   */
  public final Collection<EntityDefinition> getEntityDefinitions() {
    return Collections.unmodifiableCollection(definitionProvider.entityDefinitions.values());
  }

  /**
   * Creates an empty Entity instance returning the given string on a call to toString(), all other
   * method calls are routed to an empty Entity instance.
   * @param entityId the entityId
   * @param toStringValue the string to return by a call to toString() on the resulting entity
   * @return an empty entity wrapping a string
   */
  public final Entity createToStringEntity(final String entityId, final String toStringValue) {
    final Entity entity = entity(entityId);
    return Util.initializeProxy(Entity.class, (proxy, method, args) -> {
      if ("toString".equals(method.getName())) {
        return toStringValue;
      }

      return method.invoke(entity, args);
    });
  }

  /**
   * Adds the given Operation to this domain
   * @param operation the operation to add
   * @throws IllegalArgumentException in case an operation with the same id has already been added
   */
  public final void addOperation(final DatabaseConnection.Operation operation) {
    checkIfDeserialized();
    if (databaseOperations.containsKey(operation.getId())) {
      throw new IllegalArgumentException("Operation already defined: " + databaseOperations.get(operation.getId()).getName());
    }

    databaseOperations.put(operation.getId(), operation);
  }

  /**
   * @param <C> the type of the database connection this procedure requires
   * @param procedureId the procedure id
   * @return the procedure
   * @throws IllegalArgumentException in case the procedure is not found
   */
  public final <C> DatabaseConnection.Procedure<C> getProcedure(final String procedureId) {
    requireNonNull(procedureId, "procedureId");
    checkIfDeserialized();
    final DatabaseConnection.Operation operation = databaseOperations.get(procedureId);
    if (operation == null) {
      throw new IllegalArgumentException("Procedure not found: " + procedureId);
    }

    return (DatabaseConnection.Procedure) operation;
  }

  /**
   * @param <C> the type of the database connection this function requires
   * @param functionId the function id
   * @return the function
   * @throws IllegalArgumentException in case the function is not found
   */
  public final <C> DatabaseConnection.Function<C> getFunction(final String functionId) {
    requireNonNull(functionId, "functionId");
    checkIfDeserialized();
    final DatabaseConnection.Operation operation = databaseOperations.get(functionId);
    if (operation == null) {
      throw new IllegalArgumentException("Function not found: " + functionId);
    }

    return (DatabaseConnection.Function) operation;
  }

  /**
   * Registers this instance for lookup via {@link Domain#getDomain(String)}, required for serialization
   * of domain objects, entities and related classes.
   * @return this Domain instance
   * @see #getDomainId()
   */
  public final Domain registerDomain() {
    return registerDomain(domainId, this);
  }

  /**
   * @return a new OrderBy instance
   */
  public static final OrderBy orderBy() {
    return new OrderBy();
  }

  /**
   * @param domainId the id of the domain for which to retrieve the entity definitions
   * @return the domain instance registered for the given domainId
   * @throws IllegalArgumentException in case the domain has not been registered
   * @see #registerDomain()
   */
  public static Domain getDomain(final String domainId) {
    final Domain domain = REGISTERED_DOMAINS.get(domainId);
    if (domain == null) {
      throw new IllegalArgumentException("Domain '" + domainId + "' has not been registered");
    }

    return domain;
  }

  /**
   * @return all domains that have been registered via {@link #registerDomain()}
   */
  public static Collection<Domain> getRegisteredDomains() {
    return Collections.unmodifiableCollection(REGISTERED_DOMAINS.values());
  }

  /**
   * Adds a new {@link EntityDefinition} to this domain model, using the {@code entityId} as table name.
   * Returns the {@link EntityDefinition} instance for further configuration.
   * @param entityId the id uniquely identifying the entity type
   * @param propertyBuilders the {@link Property.Builder} objects to base this entity on. In case a select query is specified
   * for this entity, the property order must match the select column order.
   * @return a {@link EntityDefinition.Builder}
   * @throws IllegalArgumentException in case the entityId has already been used to define an entity type or if
   * no primary key property is specified
   */
  protected final EntityDefinition.Builder define(final String entityId, final Property.Builder... propertyBuilders) {
    return define(entityId, entityId, propertyBuilders);
  }

  /**
   * Adds a new {@link EntityDefinition} to this domain model.
   * Returns the {@link EntityDefinition} instance for further configuration.
   * @param entityId the id uniquely identifying the entity type
   * @param tableName the name of the underlying table
   * @param propertyBuilders the {@link Property.Builder} objects to base the entity on. In case a select query is specified
   * for this entity, the property order must match the select column order.
   * @return a {@link EntityDefinition.Builder}
   * @throws IllegalArgumentException in case the entityId has already been used to define an entity type or if
   * no primary key property is specified
   */
  protected final EntityDefinition.Builder define(final String entityId, final String tableName,
                                                  final Property.Builder... propertyBuilders) {
    requireNonNull(entityId, ENTITY_ID_PARAM);
    requireNonNull(tableName, "tableName");
    final DefaultEntityDefinition entityDefinition = new DefaultEntityDefinition(this,
            domainId, entityId, tableName, new DefaultEntityValidator(), propertyBuilders);
    definitionProvider.addDefinition(entityDefinition);

    return entityDefinition.builder();
  }

  /**
   * databaseOperations is transient and only null after deserialization.
   */
  private void checkIfDeserialized() {
    if (databaseOperations == null) {
      throw new IllegalStateException("Database operations are not available in a deserialized Domain model");
    }
  }

  private EntityDefinition getBeanEntity(final Class beanClass) {
    if (beanEntities == null) {
      beanEntities = new HashMap<>();
    }
    if (!beanEntities.containsKey(beanClass)) {
      final Optional<EntityDefinition> optionalDefinition = getEntityDefinitions().stream()
              .filter(def -> Objects.equals(beanClass, def.getBeanClass())).findFirst();
      if (!optionalDefinition.isPresent()) {
        throw new IllegalArgumentException("No entity associated with bean class: " + beanClass);
      }
      beanEntities.put(beanClass, optionalDefinition.get());
    }

    return beanEntities.get(beanClass);
  }

  private Map<String, BeanProperty> getBeanProperties(final String entityId) {
    if (beanProperties == null) {
      beanProperties = new HashMap<>();
    }

    return beanProperties.computeIfAbsent(entityId, this::initializeBeanProperties);
  }

  private Map<String, BeanProperty> initializeBeanProperties(final String entityId) {
    final EntityDefinition entityDefinition = getDefinition(entityId);
    final Class beanClass = entityDefinition.getBeanClass();
    if (beanClass == null) {
      throw new IllegalArgumentException("No bean class specified for entity: " + entityId);
    }
    try {
      final Map<String, BeanProperty> map = new HashMap<>();
      for (final Property property : entityDefinition.getProperties()) {
        final String beanProperty = property.getBeanProperty();
        Class typeClass = property.getTypeClass();
        if (property instanceof ForeignKeyProperty) {
          typeClass = getDefinition(((ForeignKeyProperty) property)
                  .getForeignEntityId()).getBeanClass();
        }
        if (beanProperty != null && typeClass != null) {
          final Method getter = Util.getGetMethod(typeClass, beanProperty, beanClass);
          final Method setter = Util.getSetMethod(typeClass, beanProperty, beanClass);
          map.put(property.getPropertyId(), new BeanProperty(getter, setter));
        }
      }

      return map;
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static Domain registerDomain(final String domainId, final Domain domain) {
    REGISTERED_DOMAINS.put(domainId, domain);

    return domain;
  }

  private static final class BeanProperty implements Serializable {

    private static final long serialVersionUID = 1;

    private final Method getter;
    private final Method setter;

    private BeanProperty(final Method getter, final Method setter) {
      this.getter = requireNonNull(getter, "getter");
      this.setter = requireNonNull(setter, "setter");
    }
  }

  private static final class DefaultEntityDefinitionProvider implements EntityDefinition.Provider, Serializable {

    private static final long serialVersionUID = 1;

    private final Map<String, EntityDefinition> entityDefinitions = new LinkedHashMap<>();

    @Override
    public final EntityDefinition getDefinition(final String entityId) {
      final EntityDefinition definition = entityDefinitions.get(requireNonNull(entityId, ENTITY_ID_PARAM));
      if (definition == null) {
        throw new IllegalArgumentException("Undefined entity: " + entityId);
      }

      return definition;
    }

    private void addDefinition(final EntityDefinition definition) {
      if (entityDefinitions.containsKey(definition.getEntityId()) && !ALLOW_REDEFINE_ENTITY.get()) {
        throw new IllegalArgumentException("Entity has already been defined: " +
                definition.getEntityId() + ", for table: " + definition.getTableName());
      }

      entityDefinitions.put(definition.getEntityId(), definition);
    }
  }
}
