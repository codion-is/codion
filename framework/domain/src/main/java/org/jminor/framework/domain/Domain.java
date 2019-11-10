/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.Configuration;
import org.jminor.common.PropertyValue;
import org.jminor.common.Serializer;
import org.jminor.common.Util;
import org.jminor.common.db.Database;
import org.jminor.common.db.DatabaseConnection;
import org.jminor.common.db.valuemap.DefaultValueMap;
import org.jminor.common.db.valuemap.ValueProvider;
import org.jminor.common.db.valuemap.exception.LengthValidationException;
import org.jminor.common.db.valuemap.exception.NullValidationException;
import org.jminor.common.db.valuemap.exception.RangeValidationException;
import org.jminor.common.db.valuemap.exception.ValidationException;
import org.jminor.framework.domain.property.ColumnProperty;
import org.jminor.framework.domain.property.ColumnPropertyBuilder;
import org.jminor.framework.domain.property.DerivedProperty;
import org.jminor.framework.domain.property.ForeignKeyProperty;
import org.jminor.framework.domain.property.ForeignKeyPropertyBuilder;
import org.jminor.framework.domain.property.MirrorProperty;
import org.jminor.framework.domain.property.Property;
import org.jminor.framework.domain.property.PropertyBuilder;
import org.jminor.framework.domain.property.TransientProperty;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.Format;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Function;

import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/**
 * A repository specifying the {@link Entity.Definition}s for a given domain.
 * Used to instantiate {@link Entity} and {@link Entity.Key} instances.
 */
public class Domain implements Serializable {

  private static final long serialVersionUID = 1;

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(Domain.class.getName(), Locale.getDefault());

  /**
   * Specifies whether or not to allow entities to be re-defined, that is,
   * allow a new definition to replace an old one.
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final PropertyValue<Boolean> ALLOW_REDEFINE_ENTITY = Configuration.booleanValue("jminor.domain.allowRedefineEntity", false);

  /**
   * Specifies the class used for serializing and deserializing entity instances.<br>
   * Value type: String, the name of the class implementing org.jminor.common.Serializer&#60;Entity&#62;<br>
   * Default value: none
   */
  public static final PropertyValue<String> ENTITY_SERIALIZER_CLASS = Configuration.stringValue("jminor.domain.entitySerializerClass", null);

  private static final String ENTITY_PARAM = "entity";
  private static final String ENTITY_ID_PARAM = "entityId";
  private static final String PROPERTY_ID_PARAM = "propertyId";
  private static final String PROPERTY_PARAM = "property";
  private static final String VALUE_REQUIRED_KEY = "property_value_is_required";

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
   * Instantiates a new {@link Entity} instance using the given maps for the values and original values respectively.
   * Note that the given map instances are used internally, modifying the contents of those maps outside the
   * {@link Entity} instance will definitely result in some unexpected and unpleasant behaviour.
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
   * @see ColumnPropertyBuilder#setColumnHasDefaultValue(boolean)
   * @see ColumnPropertyBuilder#setDefaultValue(Object)
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
   */
  public <V> V toBean(final Entity entity) {
    requireNonNull(entity, ENTITY_PARAM);
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

        propertyEntry.getValue().getSetter().invoke(bean, value);
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
   */
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
   * Creates a Entity from the given bean object.
   * @param bean the bean to convert to an Entity
   * @param <V> the bean type
   * @return a Entity based on the given bean
   * @see Entity.DefinitionBuilder#setBeanClass(Class)
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
        Object value = propertyEntry.getValue().getGetter().invoke(bean);
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
   * Adds a new {@link Entity.Definition} to this domain model, using the {@code entityId} as table name.
   * Returns the {@link Entity.Definition} instance for further configuration.
   * @param entityId the id uniquely identifying the entity type
   * @param propertyBuilders the {@link PropertyBuilder} objects to base this entity on. In case a select query is specified
   * for this entity, the property order must match the select column order.
   * @return a {@link Entity.DefinitionBuilder}
   * @throws IllegalArgumentException in case the entityId has already been used to define an entity type or if
   * no primary key property is specified
   */
  public final Entity.DefinitionBuilder define(final String entityId, final PropertyBuilder... propertyBuilders) {
    return define(entityId, entityId, propertyBuilders);
  }

  /**
   * Adds a new {@link Entity.Definition} to this domain model.
   * Returns the {@link Entity.Definition} instance for further configuration.
   * @param entityId the id uniquely identifying the entity type
   * @param tableName the name of the underlying table
   * @param propertyBuilders the {@link PropertyBuilder} objects to base the entity on. In case a select query is specified
   * for this entity, the property order must match the select column order.
   * @return a {@link Entity.DefinitionBuilder}
   * @throws IllegalArgumentException in case the entityId has already been used to define an entity type or if
   * no primary key property is specified
   */
  public final Entity.DefinitionBuilder define(final String entityId, final String tableName,
                                               final PropertyBuilder... propertyBuilders) {
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
            tableName, propertyMap, columnProperties, foreignKeyProperties, transientProperties, new Validator());
    entityDefinitions.put(entityId, entityDefinition);

    return new DefaultEntityDefinition.DefaultDefinitionBuilder(entityDefinition);
  }

  /**
   * @return the entityIds of all defined entities
   */
  public final Collection<String> getDefinedEntities() {
    return new ArrayList<>(entityDefinitions.keySet());
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
   * @return a Serializer, if one is available on the classpath
   */
  @SuppressWarnings({"unchecked"})
  public final Serializer<Entity> getEntitySerializer() {
    if (!entitySerializerAvailable()) {
      throw new IllegalArgumentException("Required configuration property is missing: " + Domain.ENTITY_SERIALIZER_CLASS);
    }

    try {
      final String serializerClass = Domain.ENTITY_SERIALIZER_CLASS.get();

      return (Serializer<Entity>) Class.forName(serializerClass).getConstructor().newInstance();
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @return true if a entity serializer is specified and available on the classpath
   */
  public final boolean entitySerializerAvailable() {
    final String serializerClass = ENTITY_SERIALIZER_CLASS.get();
    return serializerClass != null && Util.onClasspath(serializerClass);
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

  private Map<String, Property> initializePropertyMap(final String entityId, final PropertyBuilder... properties) {
    final Map<String, Property> propertyMap = new LinkedHashMap<>(properties.length);
    for (final PropertyBuilder propertyBuilder : properties) {
      validateAndAddProperty(propertyBuilder, entityId, propertyMap);
      if (propertyBuilder instanceof ForeignKeyPropertyBuilder) {
        initializeForeignKeyProperty(entityId, propertyMap, (ForeignKeyPropertyBuilder) propertyBuilder);
      }
    }
    checkIfPrimaryKeyIsSpecified(entityId, propertyMap);

    return unmodifiableMap(propertyMap);
  }

  private void initializeForeignKeyProperty(final String entityId, final Map<String, Property> propertyMap,
                                            final ForeignKeyPropertyBuilder foreignKeyPropertyBuilder) {
    final List<ColumnPropertyBuilder> propertyBuilders = foreignKeyPropertyBuilder.getPropertyBuilders();
    final ForeignKeyProperty foreignKeyProperty = foreignKeyPropertyBuilder.get();
    final List<ColumnProperty> properties = propertyBuilders.stream().map(
            (Function<ColumnPropertyBuilder, ColumnProperty>) ColumnPropertyBuilder::get).collect(toList());
    if (!entityId.equals(foreignKeyProperty.getForeignEntityId()) && Entity.Definition.STRICT_FOREIGN_KEYS.get()) {
      final Entity.Definition foreignEntity = entityDefinitions.get(foreignKeyProperty.getForeignEntityId());
      if (foreignEntity == null) {
        throw new IllegalArgumentException("Entity '" + foreignKeyProperty.getForeignEntityId()
                + "' referenced by entity '" + entityId + "' via foreign key property '"
                + foreignKeyProperty.getPropertyId() + "' has not been defined");
      }
      if (properties.size() != foreignEntity.getPrimaryKeyProperties().size()) {
        throw new IllegalArgumentException("Number of column properties in '" + entityId + "." + foreignKeyProperty.getPropertyId() +
                "' does not match the number of foreign properties in the referenced entity '" + foreignKeyProperty.getForeignEntityId() + "'");
      }
    }
    for (final ColumnPropertyBuilder propertyBuilder : propertyBuilders) {
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

  private static void validateAndAddProperty(final PropertyBuilder propertyBuilder, final String entityId,
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
            .map(property -> (TransientProperty) property)
            .collect(toList());
  }

  /**
   * Provides String representations of {@link Entity} instances.<br>
   * Given a {@link Entity} instance named entity containing the following mappings:
   * <pre>
   * "key1" -&#62; value1
   * "key2" -&#62; value2
   * "key3" -&#62; value3
   * "key4" -&#62; {Entity instance with a single mapping "refKey" -&#62; refValue}
   * </pre>
   * {@code
   * Domain.StringProvider provider = new Domain.StringProvider();
   * provider.addText("key1=").addValue("key1").addText(", key3='").addValue("key3")
   *         .addText("' foreign key value=").addForeignKeyValue("key4", "refKey");
   * System.out.println(provider.toString(entity));
   * }
   * <br>
   * outputs the following String:<br><br>
   * {@code key1=value1, key3='value3' foreign key value=refValue}
   */
  public static final class StringProvider implements Entity.ToString {

    private static final long serialVersionUID = 1;

    /**
     * Holds the ValueProviders used when constructing the String representation
     */
    private final List<ValueProvider> valueProviders = new ArrayList<>();

    /**
     * Instantiates a new {@link StringProvider} instance
     */
    public StringProvider() {}

    /**
     * Instantiates a new {@link StringProvider} instance
     * @param propertyId the id of the property which value should be used for a string representation
     */
    public StringProvider(final String propertyId) {
      addValue(propertyId);
    }

    /** {@inheritDoc} */
    @Override
    public String toString(final Entity entity) {
      requireNonNull(entity, ENTITY_PARAM);

      return valueProviders.stream().map(valueProvider -> valueProvider.toString(entity)).collect(joining());
    }

    /**
     * Adds the value mapped to the given key to this {@link StringProvider}
     * @param propertyId the id of the property which value should be added to the string representation
     * @return this {@link StringProvider} instance
     */
    public StringProvider addValue(final String propertyId) {
      requireNonNull(propertyId, PROPERTY_ID_PARAM);
      valueProviders.add(new StringValueProvider(propertyId));
      return this;
    }

    /**
     * Adds the value mapped to the given key to this StringProvider
     * @param propertyId the id of the property which value should be added to the string representation
     * @param format the Format to use when appending the value
     * @return this {@link StringProvider} instance
     */
    public StringProvider addFormattedValue(final String propertyId, final Format format) {
      requireNonNull(propertyId, PROPERTY_ID_PARAM);
      requireNonNull(format, "format");
      valueProviders.add(new FormattedValueProvider(propertyId, format));
      return this;
    }

    /**
     * Adds the value mapped to the given property in the {@link Entity} instance mapped to the given foreignKeyProperty
     * to this {@link StringProvider}
     * @param foreignKeyProperty the foreign key property
     * @param propertyId the id of the property in the referenced entity to use
     * @return this {@link StringProvider} instance
     */
    public StringProvider addForeignKeyValue(final ForeignKeyProperty foreignKeyProperty,
                                             final String propertyId) {
      requireNonNull(foreignKeyProperty, "foreignKeyProperty");
      requireNonNull(propertyId, PROPERTY_ID_PARAM);
      valueProviders.add(new ForeignKeyValueProvider(foreignKeyProperty, propertyId));
      return this;
    }

    /**
     * Adds the given static text to this {@link StringProvider}
     * @param text the text to add
     * @return this {@link StringProvider} instance
     */
    public StringProvider addText(final String text) {
      valueProviders.add(new StaticTextProvider(text));
      return this;
    }

    private interface ValueProvider extends Serializable {
      /**
       * @param entity the entity
       * @return a String representation of a property value from the given entity
       */
      String toString(final Entity entity);
    }

    private static final class FormattedValueProvider implements StringProvider.ValueProvider {
      private static final long serialVersionUID = 1;
      private final String propertyId;
      private final Format format;

      private FormattedValueProvider(final String propertyId, final Format format) {
        this.propertyId = propertyId;
        this.format = format;
      }

      @Override
      public String toString(final Entity entity) {
        if (entity.isNull(propertyId)) {
          return "";
        }

        return format.format(entity.get(propertyId));
      }
    }

    private static final class ForeignKeyValueProvider implements StringProvider.ValueProvider {
      private static final long serialVersionUID = 1;
      private final ForeignKeyProperty foreignKeyProperty;
      private final String propertyId;

      private ForeignKeyValueProvider(final ForeignKeyProperty foreignKeyProperty,
                                      final String propertyId) {
        this.foreignKeyProperty = foreignKeyProperty;
        this.propertyId = propertyId;
      }

      @Override
      public String toString(final Entity entity) {
        if (entity.isNull(foreignKeyProperty)) {
          return "";
        }

        return entity.getForeignKey(foreignKeyProperty).getAsString(propertyId);
      }
    }

    private static final class StringValueProvider implements StringProvider.ValueProvider {
      private static final long serialVersionUID = 1;
      private final String propertyId;

      private StringValueProvider(final String propertyId) {
        this.propertyId = propertyId;
      }

      @Override
      public String toString(final Entity entity) {
        return entity.getAsString(propertyId);
      }
    }

    private static final class StaticTextProvider implements StringProvider.ValueProvider {
      private static final long serialVersionUID = 1;
      private final String text;

      private StaticTextProvider(final String text) {
        this.text = text;
      }

      @Override
      public String toString(final Entity entity) {
        return text;
      }
    }
  }

  /**
   * A default {@link Entity.Validator} implementation providing null validation for properties marked as not null,
   * range validation for numerical properties with max and/or min values specified and string length validation
   * based on the specified max length.
   * This Validator can be extended to provide further validation.
   * @see PropertyBuilder#setNullable(boolean)
   * @see PropertyBuilder#setMin(double)
   * @see PropertyBuilder#setMax(double)
   * @see PropertyBuilder#setMaxLength(int)
   */
  public static class Validator extends DefaultValueMap.DefaultValidator<Property, Entity> implements Entity.Validator {

    private static final long serialVersionUID = 1;

    private final boolean performNullValidation;

    /**
     * Instantiates a new {@link Entity.Validator}
     */
    public Validator() {
      this(true);
    }

    /**
     * Instantiates a new {@link Entity.Validator}
     * @param performNullValidation if true then automatic null validation is performed
     */
    public Validator(final boolean performNullValidation) {
      this.performNullValidation = performNullValidation;
    }

    /**
     * Returns true if the given property accepts a null value for the given entity,
     * by default this method simply returns {@code property.isNullable()}
     * @param entity the entity being validated
     * @param property the property
     * @return true if the property accepts a null value
     */
    @Override
    public boolean isNullable(final Entity entity, final Property property) {
      return property.isNullable();
    }

    /**
     * Validates all writable properties in the given entities
     * @param entities the entities to validate
     * @throws ValidationException in case validation fails
     */
    @Override
    public final void validate(final Collection<Entity> entities) throws ValidationException {
      for (final Entity entity : entities) {
        validate(entity);
      }
    }

    /**
     * Validates all writable properties in the given entity
     * @param entity the entity to validate
     * @throws ValidationException in case validation fails
     */
    @Override
    public void validate(final Entity entity) throws ValidationException {
      requireNonNull(entity, ENTITY_PARAM);
      for (final Property property : entity.getProperties()) {
        if (!property.isReadOnly()) {
          validate(entity, property);
        }
      }
    }

    /** {@inheritDoc} */
    @Override
    public void validate(final Entity entity, final Property property) throws ValidationException {
      requireNonNull(entity, ENTITY_PARAM);
      if (performNullValidation && !isForeignKeyProperty(property)) {
        performNullValidation(entity, property);
      }
      if (property.isNumerical()) {
        performRangeValidation(entity, property);
      }
      else if (property.isString()) {
        performLengthValidation(entity, property);
      }
    }

    /** {@inheritDoc} */
    @Override
    public final void performRangeValidation(final Entity entity, final Property property) throws RangeValidationException {
      requireNonNull(entity, ENTITY_PARAM);
      requireNonNull(property, PROPERTY_PARAM);
      if (entity.isNull(property)) {
        return;
      }

      final Number value = (Number) entity.get(property);
      if (value.doubleValue() < (property.getMin() == null ? Double.NEGATIVE_INFINITY : property.getMin())) {
        throw new RangeValidationException(property.getPropertyId(), value, "'" + property + "' " +
                MESSAGES.getString("property_value_too_small") + " " + property.getMin());
      }
      if (value.doubleValue() > (property.getMax() == null ? Double.POSITIVE_INFINITY : property.getMax())) {
        throw new RangeValidationException(property.getPropertyId(), value, "'" + property + "' " +
                MESSAGES.getString("property_value_too_large") + " " + property.getMax());
      }
    }

    /** {@inheritDoc} */
    @Override
    public final void performNullValidation(final Entity entity, final Property property) throws NullValidationException {
      requireNonNull(entity, ENTITY_PARAM);
      requireNonNull(property, PROPERTY_PARAM);
      if (!isNullable(entity, property) && entity.isNull(property)) {
        if ((entity.getKey().isNull() || entity.getOriginalKey().isNull()) && !(property instanceof ForeignKeyProperty)) {
          //a new entity being inserted, allow null for columns with default values and auto generated primary key values
          final boolean nonKeyColumnPropertyWithoutDefaultValue = isNonKeyColumnPropertyWithoutDefaultValue(property);
          final boolean primaryKeyPropertyWithoutAutoGenerate = isPrimaryKeyPropertyWithoutAutoGenerate(entity, property);
          if (nonKeyColumnPropertyWithoutDefaultValue || primaryKeyPropertyWithoutAutoGenerate) {
            throw new NullValidationException(property.getPropertyId(), MESSAGES.getString(VALUE_REQUIRED_KEY) + ": " + property);
          }
        }
        else {
          throw new NullValidationException(property.getPropertyId(), MESSAGES.getString(VALUE_REQUIRED_KEY) + ": " + property);
        }
      }
    }

    /** {@inheritDoc} */
    @Override
    public void performLengthValidation(final Entity entity, final Property property) throws LengthValidationException {
      requireNonNull(entity, ENTITY_PARAM);
      requireNonNull(property, PROPERTY_PARAM);
      if (entity.isNull(property)) {
        return;
      }

      final int maxLength = property.getMaxLength();
      final String value = (String) entity.get(property);
      if (maxLength != -1 && value.length() > maxLength) {
        throw new LengthValidationException(property.getPropertyId(), value, "'" + property + "' " +
                MESSAGES.getString("property_value_too_long") + " " + maxLength);
      }
    }

    private static boolean isPrimaryKeyPropertyWithoutAutoGenerate(final Entity entity, final Property property) {
      return (property instanceof ColumnProperty
              && ((ColumnProperty) property).isPrimaryKeyProperty()) && entity.getKeyGeneratorType().isManual();
    }

    /**
     * @param property the property
     * @return true if the property is a part of a foreign key
     */
    private static boolean isForeignKeyProperty(final Property property) {
      return property instanceof ColumnProperty && ((ColumnProperty) property).isForeignKeyProperty();
    }

    private static boolean isNonKeyColumnPropertyWithoutDefaultValue(final Property property) {
      return property instanceof ColumnProperty && !((ColumnProperty) property).isPrimaryKeyProperty()
              && !((ColumnProperty) property).columnHasDefaultValue();
    }
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
        this.propertyId = requireNonNull(propertyId, PROPERTY_ID_PARAM);
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

    public BeanProperty(final Method getter, final Method setter) {
      this.getter = getter;
      this.setter = setter;
    }

    public Method getGetter() {
      return getter;
    }

    public Method getSetter() {
      return setter;
    }
  }
}
