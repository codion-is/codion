/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.Configuration;
import org.jminor.common.Util;
import org.jminor.common.db.Database;
import org.jminor.common.db.DatabaseConnection;
import org.jminor.common.db.valuemap.ValueProvider;
import org.jminor.common.value.PropertyValue;
import org.jminor.framework.domain.property.ColumnProperty;
import org.jminor.framework.domain.property.DerivedProperty;
import org.jminor.framework.domain.property.ForeignKeyProperty;
import org.jminor.framework.domain.property.MirrorProperty;
import org.jminor.framework.domain.property.Property;
import org.jminor.framework.domain.property.TransientProperty;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * A repository specifying the {@link Entity.Definition}s for a given domain.
 * Used to instantiate {@link Entity} and {@link Entity.Key} instances.
 */
public class Domain implements Entity.Definition.Provider, Serializable {

  private static final long serialVersionUID = 1;

  /**
   * Specifies whether or not to allow entities to be re-defined, that is,
   * allow a new definition to replace an old one.
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final PropertyValue<Boolean> ALLOW_REDEFINE_ENTITY = Configuration.booleanValue("jminor.domain.allowRedefineEntity", false);

  private static final String ENTITY_ID_PARAM = "entityId";

  private static final Map<String, Domain> REGISTERED_DOMAINS = new HashMap<>();

  private final String domainId;
  private final Map<String, Entity.Definition> entityDefinitions = new LinkedHashMap<>();
  private final transient Map<String, DatabaseConnection.Operation> databaseOperations = new HashMap<>();

  private Map<Class, Entity.Definition> beanEntities;
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
    this.entityDefinitions.putAll(domain.entityDefinitions);
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
    return new DefaultEntity(this, entityId);
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
   * Note that no validation is performed on the properties or map values is performed.
   * @param entityId the entity id
   * @param values the values
   * @param originalValues the original values
   * @return a new {@link Entity} instance
   */
  public final Entity entity(final String entityId, final Map<Property, Object> values, final Map<Property, Object> originalValues) {
    return new DefaultEntity(this, entityId, values, originalValues);
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
    final Entity.Definition entityDefinition = getDefinition(entityId);
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
   * @see Entity.Definition.Builder#setBeanClass(Class)
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
   * @see Entity.Definition.Builder#setBeanClass(Class)
   * @see Property.Builder#setBeanProperty(String)
   */
  public <V> V toBean(final Entity entity) {
    requireNonNull(entity, "entity");
    final Entity.Definition definition = getDefinition(entity.getEntityId());
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
   * @see Entity.Definition.Builder#setBeanClass(Class)
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
   * @see Entity.Definition.Builder#setBeanClass(Class)
   * @see Property.Builder#setBeanProperty(String)
   */
  public <V> Entity fromBean(final V bean) {
    requireNonNull(bean, "bean");
    final Class beanClass = bean.getClass();
    final Entity.Definition definition = getBeanEntity(beanClass);
    final Entity entity = entity(definition.getEntityId());
    try {
      final Map<String, BeanProperty> beanPropertyMap =
              getBeanProperties(definition.getEntityId());
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
    return new DefaultEntity.DefaultKey(getDefinition(entityId), null);
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
    return new DefaultEntity.DefaultKey(getDefinition(entityId), value);
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
    return new DefaultEntity.DefaultKey(getDefinition(entityId), value);
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
    Objects.requireNonNull(entities, "entities");

    return entities.stream().map(this::deepCopyEntity).collect(toList());
  }

  /**
   * Copies the given entity.
   * @param entity the entity to copy
   * @return copy of the given entity
   */
  public final Entity copyEntity(final Entity entity) {
    Objects.requireNonNull(entity, "entity");
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
    Objects.requireNonNull(entity, "entity");
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
    Objects.requireNonNull(key, "key");
    final Entity.Key copy = key(key.getEntityId());
    copy.setAs(key);

    return copy;
  }

  /**
   * Adds a new {@link Entity.Definition} to this domain model, using the {@code entityId} as table name.
   * Returns the {@link Entity.Definition} instance for further configuration.
   * @param entityId the id uniquely identifying the entity type
   * @param propertyBuilders the {@link Property.Builder} objects to base this entity on. In case a select query is specified
   * for this entity, the property order must match the select column order.
   * @return a {@link Entity.Definition.Builder}
   * @throws IllegalArgumentException in case the entityId has already been used to define an entity type or if
   * no primary key property is specified
   */
  public final Entity.Definition.Builder define(final String entityId, final Property.Builder... propertyBuilders) {
    return define(entityId, entityId, propertyBuilders);
  }

  /**
   * Adds a new {@link Entity.Definition} to this domain model.
   * Returns the {@link Entity.Definition} instance for further configuration.
   * @param entityId the id uniquely identifying the entity type
   * @param tableName the name of the underlying table
   * @param propertyBuilders the {@link Property.Builder} objects to base the entity on. In case a select query is specified
   * for this entity, the property order must match the select column order.
   * @return a {@link Entity.Definition.Builder}
   * @throws IllegalArgumentException in case the entityId has already been used to define an entity type or if
   * no primary key property is specified
   */
  public final Entity.Definition.Builder define(final String entityId, final String tableName,
                                                final Property.Builder... propertyBuilders) {
    requireNonNull(entityId, ENTITY_ID_PARAM);
    requireNonNull(tableName, "tableName");
    if (entityDefinitions.containsKey(entityId) && !ALLOW_REDEFINE_ENTITY.get()) {
      throw new IllegalArgumentException("Entity has already been defined: " + entityId + ", for table: " + tableName);
    }
    final Map<String, Property> propertyMap = initializePropertyMap(entityId, propertyBuilders);
    final List<ColumnProperty> columnProperties = unmodifiableList(getColumnProperties(propertyMap.values()));
    final List<ForeignKeyProperty> foreignKeyProperties = unmodifiableList(getForeignKeyProperties(propertyMap.values()));
    final List<TransientProperty> transientProperties = unmodifiableList(getTransientProperties(propertyMap.values()));

    final DefaultEntityDefinition entityDefinition = new DefaultEntityDefinition(domainId, entityId,
            tableName, propertyMap, columnProperties, foreignKeyProperties, transientProperties, new DefaultEntityValidator());
    entityDefinitions.put(entityId, entityDefinition);

    return entityDefinition.builder();
  }

  /**
   * @return all {@link Entity.Definition}s found in this domain model
   */
  public final Collection<Entity.Definition> getEntityDefinitions() {
    return Collections.unmodifiableCollection(entityDefinitions.values());
  }

  /**
   * @param entityId the entity id
   * @return true if the entity is defined
   */
  public final boolean isDefined(final String entityId) {
    return entityDefinitions.containsKey(entityId);
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
  public static final Entity.OrderBy orderBy() {
    return new DefaultOrderBy();
  }

  /**
   * Instantiates a primary key generator which fetches the current maximum primary key value and increments
   * it by one prior to insert.
   * Note that if the primary key value of the entity being inserted is already populated this key
   * generator does nothing, that is, it does not overwrite a manually set primary key value.
   * @param tableName the table name
   * @param columnName the primary key column name
   * @return a incrementing primary key generator
   */
  public final Entity.KeyGenerator incrementKeyGenerator(final String tableName, final String columnName) {
    return new IncrementKeyGenerator(tableName, columnName);
  }

  /**
   * Instantiates a primary key generator which fetches primary key values from a sequence prior to insert.
   * Note that if the primary key value of the entity being inserted is already populated this key
   * generator does nothing, that is, it does not overwrite a manually set primary key value.
   * @param sequenceName the sequence name
   * @return a sequence based primary key generator
   */
  public final Entity.KeyGenerator sequenceKeyGenerator(final String sequenceName) {
    return new SequenceKeyGenerator(sequenceName);
  }

  /**
   * Instantiates a primary key generator which fetches primary key values using the given query prior to insert.
   * Note that if the primary key value of the entity being inserted is already populated this key
   * generator does nothing, that is, it does not overwrite a manually set primary key value.
   * @param query a query for retrieving the primary key value
   * @return a query based primary key generator
   */
  public final Entity.KeyGenerator queriedKeyGenerator(final String query) {
    return new AbstractQueriedKeyGenerator() {
      @Override
      protected String getQuery(final Database database) {
        return query;
      }
    };
  }

  /**
   * Instantiates a primary key generator which fetches automatically incremented primary key values after insert.
   * @param valueSource the value source, whether a sequence or a table name
   * @return a auto-increment based primary key generator
   */
  public final Entity.KeyGenerator automaticKeyGenerator(final String valueSource) {
    return new AutomaticKeyGenerator(valueSource);
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
   * @param entityId the entity id
   * @return the definition of the given entity
   * @throws IllegalArgumentException in case no entity with the given id has been defined
   */
  public final Entity.Definition getDefinition(final String entityId) {
    final Entity.Definition definition = entityDefinitions.get(requireNonNull(entityId, ENTITY_ID_PARAM));
    if (definition == null) {
      throw new IllegalArgumentException("Undefined entity: " + entityId);
    }

    return definition;
  }

  private Map<String, Property> initializePropertyMap(final String entityId, final Property.Builder... propertyBuilders) {
    final Map<String, Property> propertyMap = new LinkedHashMap<>(propertyBuilders.length);
    for (final Property.Builder propertyBuilder : propertyBuilders) {
      validateAndAddProperty(propertyBuilder, entityId, propertyMap);
      if (propertyBuilder instanceof ForeignKeyProperty.Builder) {
        initializeForeignKeyProperty(entityId, propertyMap, (ForeignKeyProperty.Builder) propertyBuilder);
      }
    }
    checkIfPrimaryKeyIsSpecified(entityId, propertyMap);

    return unmodifiableMap(propertyMap);
  }

  private void initializeForeignKeyProperty(final String entityId, final Map<String, Property> propertyMap,
                                            final ForeignKeyProperty.Builder foreignKeyPropertyBuilder) {
    final ForeignKeyProperty foreignKeyProperty = foreignKeyPropertyBuilder.get();
    if (!entityId.equals(foreignKeyProperty.getForeignEntityId()) && Entity.Definition.STRICT_FOREIGN_KEYS.get()) {
      final Entity.Definition foreignEntity = entityDefinitions.get(foreignKeyProperty.getForeignEntityId());
      if (foreignEntity == null) {
        throw new IllegalArgumentException("Entity '" + foreignKeyProperty.getForeignEntityId()
                + "' referenced by entity '" + entityId + "' via foreign key property '"
                + foreignKeyProperty.getPropertyId() + "' has not been defined");
      }
      if (foreignKeyProperty.getProperties().size() != foreignEntity.getPrimaryKeyProperties().size()) {
        throw new IllegalArgumentException("Number of column properties in '" + entityId + "." + foreignKeyProperty.getPropertyId() +
                "' does not match the number of foreign properties in the referenced entity '" + foreignKeyProperty.getForeignEntityId() + "'");
      }
    }
    for (final ColumnProperty.Builder propertyBuilder : foreignKeyPropertyBuilder.getColmnPropertyBuilders()) {
      if (!(propertyBuilder.get() instanceof MirrorProperty)) {
        validateAndAddProperty(propertyBuilder, entityId, propertyMap);
      }
    }
  }

  /**
   * databaseOperations is transient and only null after deserialization.
   */
  private void checkIfDeserialized() {
    if (databaseOperations == null) {
      throw new IllegalStateException("Database operations are not available in a deserialized Domain model");
    }
  }

  private Entity.Definition getBeanEntity(final Class beanClass) {
    if (beanEntities == null) {
      beanEntities = new HashMap<>();
    }
    if (!beanEntities.containsKey(beanClass)) {
      final Optional<Entity.Definition> optionalDefinition = entityDefinitions.values().stream()
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
    final Entity.Definition entityDefinition = getDefinition(entityId);
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

  private static void validateAndAddProperty(final Property.Builder propertyBuilder, final String entityId,
                                             final Map<String, Property> propertyMap) {
    final Property property = propertyBuilder.get();
    checkIfUniquePropertyId(property, entityId, propertyMap);
    propertyBuilder.setEntityId(entityId);
    propertyMap.put(property.getPropertyId(), property);
  }

  private static void checkIfUniquePropertyId(final Property property, final String entityId, final Map<String, Property> propertyMap) {
    if (propertyMap.containsKey(property.getPropertyId())) {
      throw new IllegalArgumentException("Property with id " + property.getPropertyId()
              + (property.getCaption() != null ? " (caption: " + property.getCaption() + ")" : "")
              + " has already been defined as: " + propertyMap.get(property.getPropertyId()) + " in entity: " + entityId);
    }
  }

  private static void checkIfPrimaryKeyIsSpecified(final String entityId, final Map<String, Property> propertyMap) {
    final Collection<Integer> usedPrimaryKeyIndexes = new ArrayList<>();
    boolean primaryKeyPropertyFound = false;
    for (final Property property : propertyMap.values()) {
      if (property instanceof ColumnProperty && ((ColumnProperty) property).isPrimaryKeyProperty()) {
        final Integer index = ((ColumnProperty) property).getPrimaryKeyIndex();
        if (usedPrimaryKeyIndexes.contains(index)) {
          throw new IllegalArgumentException("Primary key index " + index + " in property " + property + " has already been used");
        }
        usedPrimaryKeyIndexes.add(index);
        primaryKeyPropertyFound = true;
      }
    }
    if (primaryKeyPropertyFound) {
      return;
    }

    throw new IllegalArgumentException("Entity is missing a primary key: " + entityId);
  }

  private static Domain registerDomain(final String domainId, final Domain domain) {
    REGISTERED_DOMAINS.put(domainId, domain);

    return domain;
  }

  private static List<ForeignKeyProperty> getForeignKeyProperties(final Collection<Property> properties) {
    return properties.stream().filter(property -> property instanceof ForeignKeyProperty)
            .map(property -> (ForeignKeyProperty) property).collect(toList());
  }

  private static List<ColumnProperty> getColumnProperties(final Collection<Property> properties) {
    return properties.stream().filter(property -> property instanceof ColumnProperty)
            .map(property -> (ColumnProperty) property).collect(toList());
  }

  private static List<TransientProperty> getTransientProperties(final Collection<Property> properties) {
    return properties.stream().filter(property -> property instanceof TransientProperty)
            .map(property -> (TransientProperty) property).collect(toList());
  }

  private abstract class AbstractQueriedKeyGenerator implements Entity.KeyGenerator {

    @Override
    public Type getType() {
      return Type.QUERY;
    }

    protected final ColumnProperty getPrimaryKeyProperty(final String entityId) {
      return getDefinition(entityId).getPrimaryKeyProperties().get(0);
    }

    protected final void queryAndSet(final Entity entity, final ColumnProperty keyProperty,
                                     final DatabaseConnection connection) throws SQLException {
      final Object value;
      switch (keyProperty.getColumnType()) {
        case Types.INTEGER:
          value = connection.queryInteger(getQuery(connection.getDatabase()));
          break;
        case Types.BIGINT:
          value = connection.queryLong(getQuery(connection.getDatabase()));
          break;
        default:
          throw new SQLException("Queried key generator only implemented for Types.INTEGER and Types.BIGINT datatypes", null, null);
      }
      entity.put(keyProperty, value);
    }

    protected abstract String getQuery(final Database database);
  }

  private final class IncrementKeyGenerator extends AbstractQueriedKeyGenerator {

    private final String query;

    private IncrementKeyGenerator(final String tableName, final String columnName) {
      this.query = "select max(" + columnName + ") + 1 from " + tableName;
    }

    @Override
    public Type getType() {
      return Type.INCREMENT;
    }

    @Override
    public void beforeInsert(final Entity entity, final DatabaseConnection connection) throws SQLException {
      final ColumnProperty primaryKeyProperty = getPrimaryKeyProperty(entity.getEntityId());
      if (entity.isNull(primaryKeyProperty)) {
        queryAndSet(entity, primaryKeyProperty, connection);
      }
    }

    @Override
    protected String getQuery(final Database database) {
      return query;
    }
  }

  private final class SequenceKeyGenerator extends AbstractQueriedKeyGenerator {

    private final String sequenceName;

    private SequenceKeyGenerator(final String sequenceName) {
      this.sequenceName = sequenceName;
    }

    @Override
    public Type getType() {
      return Type.SEQUENCE;
    }

    @Override
    public void beforeInsert(final Entity entity, final DatabaseConnection connection) throws SQLException {
      final ColumnProperty primaryKeyProperty = getPrimaryKeyProperty(entity.getEntityId());
      if (entity.isNull(primaryKeyProperty)) {
        queryAndSet(entity, primaryKeyProperty, connection);
      }
    }

    @Override
    protected String getQuery(final Database database) {
      return database.getSequenceQuery(sequenceName);
    }
  }

  private final class AutomaticKeyGenerator extends AbstractQueriedKeyGenerator {

    private final String valueSource;

    private AutomaticKeyGenerator(final String valueSource) {
      this.valueSource = valueSource;
    }

    @Override
    public Type getType() {
      return Type.AUTOMATIC;
    }

    @Override
    public void afterInsert(final Entity entity, final DatabaseConnection connection, final Statement insertStatement) throws SQLException {
      queryAndSet(entity, getPrimaryKeyProperty(entity.getEntityId()), connection);
    }

    @Override
    protected String getQuery(final Database database) {
      return database.getAutoIncrementQuery(valueSource);
    }
  }

  private static final class DefaultOrderBy implements Entity.OrderBy {

    private static final long serialVersionUID = 1;

    private final List<OrderByProperty> orderByProperties = new LinkedList<>();

    @Override
    public Entity.OrderBy ascending(final String... propertyIds) {
      add(false, propertyIds);
      return this;
    }

    @Override
    public Entity.OrderBy descending(final String... propertyIds) {
      add(true, propertyIds);
      return this;
    }

    @Override
    public List<OrderByProperty> getOrderByProperties() {
      return unmodifiableList(orderByProperties);
    }

    private void add(final boolean descending, final String... propertyIds) {
      requireNonNull(propertyIds, "propertyIds");
      for (final String propertyId : propertyIds) {
        final OrderByProperty property = new DefaultOrderByProperty(propertyId, descending);
        if (orderByProperties.contains(property)) {
          throw new IllegalArgumentException("Order by already contains property: " + propertyId);
        }
        orderByProperties.add(property);
      }
    }

    private static final class DefaultOrderByProperty implements OrderByProperty {

      private static final long serialVersionUID = 1;

      private final String propertyId;
      private final boolean descending;

      private DefaultOrderByProperty(final String propertyId, final boolean descending) {
        this.propertyId = requireNonNull(propertyId, "propertyId");
        this.descending = descending;
      }

      @Override
      public String getPropertyId() {
        return propertyId;
      }

      @Override
      public boolean isDescending() {
        return descending;
      }

      @Override
      public boolean equals(final Object object) {
        if (this == object) {
          return true;
        }
        if (object == null || getClass() != object.getClass()) {
          return false;
        }

        return propertyId.equals(((DefaultOrderByProperty) object).propertyId);
      }

      @Override
      public int hashCode() {
        return propertyId.hashCode();
      }
    }
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
}
