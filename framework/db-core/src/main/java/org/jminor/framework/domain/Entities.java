/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.Configuration;
import org.jminor.common.Serializer;
import org.jminor.common.Util;
import org.jminor.common.Value;
import org.jminor.common.db.Database;
import org.jminor.common.db.DatabaseConnection;
import org.jminor.common.db.ResultPacker;
import org.jminor.common.db.valuemap.DefaultValueMap;
import org.jminor.common.db.valuemap.ValueMap;
import org.jminor.common.db.valuemap.ValueProvider;
import org.jminor.common.db.valuemap.exception.LengthValidationException;
import org.jminor.common.db.valuemap.exception.NullValidationException;
import org.jminor.common.db.valuemap.exception.RangeValidationException;
import org.jminor.common.db.valuemap.exception.ValidationException;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.Collator;
import java.text.Format;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * A {@link Entity} repository specifying the {@link Entity.Definition}s for a given domain.
 */
public class Entities implements Serializable {

  private static final long serialVersionUID = 1;

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(Entities.class.getName(), Locale.getDefault());

  private static final String MSG_PROPERTY_VALUE_IS_REQUIRED = "property_value_is_required";

  /**
   * Specifies whether or not to allow entities to be re-defined, that is,
   * allow a new definition to replace an old one.
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final Value<Boolean> ALLOW_REDEFINE_ENTITY = Configuration.booleanValue("jminor.domain.allowRedefineEntity", false);

  /**
   * Specifies the class used for serializing and deserializing entity instances.<br>
   * Value type: String, the name of the class implementing org.jminor.common.Serializer&#60;Entity&#62;<br>
   * Default value: none
   */
  public static final Value<String> ENTITY_SERIALIZER_CLASS = Configuration.stringValue("jminor.domain.entitySerializerClass", null);

  private static final String ENTITY_PARAM = "entity";
  private static final String ENTITY_ID_PARAM = "entityId";
  private static final String ENTITIES_PARAM = "entities";
  private static final String PROPERTY_ID_PARAM = "propertyId";
  private static final String PROPERTY_PARAM = "property";

  private static final Map<String, Entities> REGISTERED_DOMAINS = new HashMap<>();

  private final String domainId;
  private final Map<String, Entity.Definition> entityDefinitions = new LinkedHashMap<>();

  private final transient Map<String, List<Property.ForeignKeyProperty>> foreignKeyReferenceMap = new HashMap<>();
  private final transient Map<String, DatabaseConnection.Operation> databaseOperations = new HashMap<>();

  /**
   * Instantiates a Entities instance
   */
  public Entities() {
    this.domainId = getClass().getSimpleName();
  }

  /**
   * @param domainId the domain identifier
   */
  public Entities(final String domainId) {
    this.domainId = Objects.requireNonNull(domainId, "domainId");
  }

  /**
   * A copy constructor
   * @param domain the domain to copy
   */
  public Entities(final Entities domain) {
    this.domainId = domain.domainId;
    this.entityDefinitions.putAll(domain.entityDefinitions);
  }

  /**
   * @return the domain Id
   */
  public final String getDomainId() {
    return domainId;
  }

  /**
   * Creates a new {@link Entity} instance with the given entityId
   * @param entityId the entity ID
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
   * @param entityId the entity ID
   * @param values the values
   * @param originalValues the original values
   * @return a new {@link Entity} instance
   */
  public final Entity entity(final String entityId, final Map<Property, Object> values, final Map<Property, Object> originalValues) {
    return new DefaultEntity(this, entityId, values, originalValues);
  }

  /**
   * Instantiates a new {@link Entity} of the given type using the values provided by {@code valueProvider}.
   * Values are fetched for {@link Property.ColumnProperty} and its descendants, {@link Property.ForeignKeyProperty}
   * and {@link Property.TransientProperty} (excluding its descendants).
   * If a {@link org.jminor.framework.domain.Property.ColumnProperty}s column has a default value the property is
   * skipped unless it has a default value, which then overrides the columns default value.
   * @param entityId the entity ID
   * @param valueProvider the value provider
   * @return the populated entity
   * @see org.jminor.framework.domain.Property.ColumnProperty#setColumnHasDefaultValue(boolean)
   * @see org.jminor.framework.domain.Property.ColumnProperty#setDefaultValue(Object)
   */
  public final Entity defaultEntity(final String entityId, final ValueProvider<Property, Object> valueProvider) {
    final Entity entity = entity(entityId);
    final Collection<Property.ColumnProperty> columnProperties = getColumnProperties(entityId);
    for (final Property.ColumnProperty property : columnProperties) {
      if (!property.isForeignKeyProperty() && !property.isDenormalized()//these are set via their respective parent properties
              && (!property.columnHasDefaultValue() || property.hasDefaultValue())) {
        entity.put(property, valueProvider.get(property));
      }
    }
    final Collection<Property.TransientProperty> transientProperties = getTransientProperties(entityId);
    for (final Property.TransientProperty transientProperty : transientProperties) {
      if (!(transientProperty instanceof Property.DerivedProperty)) {
        entity.put(transientProperty, valueProvider.get(transientProperty));
      }
    }
    final Collection<Property.ForeignKeyProperty> foreignKeyProperties = getForeignKeyProperties(entityId);
    for (final Property.ForeignKeyProperty foreignKeyProperty : foreignKeyProperties) {
      entity.put(foreignKeyProperty, valueProvider.get(foreignKeyProperty));
    }
    entity.saveAll();

    return entity;
  }

  /**
   * Creates a new {@link Entity.Key} instance with the given entityId
   * @param entityId the entity ID
   * @return a new {@link Entity.Key} instance
   */
  public final Entity.Key key(final String entityId) {
    return new DefaultEntity.DefaultKey(getDefinition(entityId), null);
  }

  /**
   * Defines a new entity, by default the {@code entityId} is used as the underlying table name
   * @param entityId the ID uniquely identifying the entity
   * @param properties the {@link Property} objects to base this entity on. In case a select query is specified
   * for this entity, the property order must match the select column order.
   * @return a new {@link Entity.Definition}
   * @throws IllegalArgumentException in case the entityId has already been used to define an entity type or if
   * no primary key property is specified
   */
  public final Entity.Definition define(final String entityId, final Property... properties) {
    return define(entityId, entityId, properties);
  }

  /**
   * Defines a new entity
   * @param entityId the ID uniquely identifying the entity
   * @param tableName the name of the underlying table
   * @param properties the {@link Property} objects to base the entity on. In case a select query is specified
   * for this entity, the property order must match the select column order.
   * @return a new {@link Entity.Definition}
   * @throws IllegalArgumentException in case the entityId has already been used to define an entity type or if
   * no primary key property is specified
   */
  public final Entity.Definition define(final String entityId, final String tableName, final Property... properties) {
    Objects.requireNonNull(entityId, ENTITY_ID_PARAM);
    Objects.requireNonNull(tableName, "tableName");
    if (entityDefinitions.containsKey(entityId) && !ALLOW_REDEFINE_ENTITY.get()) {
      throw new IllegalArgumentException("Entity has already been defined: " + entityId + ", for table: " + tableName);
    }
    final Map<String, Property> propertyMap = initializePropertyMap(domainId, entityId, properties);
    final List<Property.ColumnProperty> columnProperties = Collections.unmodifiableList(getColumnProperties(propertyMap.values()));
    final List<Property.ForeignKeyProperty> foreignKeyProperties = Collections.unmodifiableList(getForeignKeyProperties(propertyMap.values()));
    final List<Property.TransientProperty> transientProperties = Collections.unmodifiableList(getTransientProperties(propertyMap.values()));
    final EntityResultPacker resultPacker = new EntityResultPacker(this, entityId, columnProperties, transientProperties, propertyMap.size());

    final DefaultEntityDefinition entityDefinition = new DefaultEntityDefinition(domainId, entityId,
            tableName, resultPacker, propertyMap, columnProperties, foreignKeyProperties, transientProperties);
    entityDefinition.setValidator(new Validator());
    entityDefinitions.put(entityId, entityDefinition);

    return entityDefinition;
  }

  /**
   * @param entityId the entity ID
   * @return a String array containing the IDs of the properties used as default search properties
   * for entities identified by {@code entityId}
   * @see Entity.Definition#setSearchPropertyIds(String...)
   */
  public final Collection<String> getSearchPropertyIds(final String entityId) {
    return getDefinition(entityId).getSearchPropertyIds();
  }

  /**
   * Retrieves the properties used when searching for a entity of the given type,
   * if no search property IDs are defined all STRING based properties are returned.
   * @param entityId the entity ID
   * @return the search properties to use
   * @see Entity.Definition#setSearchPropertyIds(String...)
   */
  public final Collection<Property.ColumnProperty> getSearchProperties(final String entityId) {
    final Collection<String> searchPropertyIds = getSearchPropertyIds(entityId);
    return getSearchProperties(entityId, searchPropertyIds.toArray(new String[0]));
  }

  /**
   * Retrieves the properties used when searching for a entity of the given type,
   * @param entityId the entity ID
   * @param searchPropertyIds the IDs of the search properties to retrieve
   * @return the search properties to use
   * @see Entity.Definition#setSearchPropertyIds(String...)
   */
  public final Collection<Property.ColumnProperty> getSearchProperties(final String entityId, final String... searchPropertyIds) {
    if (searchPropertyIds != null && searchPropertyIds.length > 0) {
      return Arrays.stream(searchPropertyIds).map(propertyId -> getColumnProperty(entityId, propertyId)).collect(Collectors.toList());
    }

    return Collections.emptyList();
  }

  /**
   * @param entityId the entity ID
   * @return a list containing the primary key properties of the entity identified by {@code entityId}
   */
  public final List<Property.ColumnProperty> getPrimaryKeyProperties(final String entityId) {
    return getDefinition(entityId).getPrimaryKeyProperties();
  }

  /**
   * @param entityId the entity ID
   * @return true if the entity identified by {@code entityId} is read only
   * @throws IllegalArgumentException if the entity is undefined
   */
  public final boolean isReadOnly(final String entityId) {
    return getDefinition(entityId).isReadOnly();
  }

  /**
   * @param entityId the entity ID
   * @return true if the entity identified by {@code entityId} is based on a small dataset
   * @throws IllegalArgumentException if the entity is undefined
   */
  public final boolean isSmallDataset(final String entityId) {
    return getDefinition(entityId).isSmallDataset();
  }

  /**
   * @param entityId the entity ID
   * @return true if the entity identified by {@code entityId} is based on static data
   * @throws IllegalArgumentException if the entity is undefined
   */
  public final boolean isStaticData(final String entityId) {
    return getDefinition(entityId).isStaticData();
  }

  /**
   * @param entityId the entity ID
   * @return the default order by for this entity
   */
  public final Entity.OrderBy getOrderBy(final String entityId) {
    return getDefinition(entityId).getOrderBy();
  }

  /**
   * @param entityId the entity ID
   * @return a comma separated list of columns to use in the group by clause
   */
  public final String getGroupByClause(final String entityId) {
    return getDefinition(entityId).getGroupByClause();
  }

  /**
   * @param entityId the entity ID
   * @return the having clause associated with this entity
   */
  public final String getHavingClause(final String entityId) {
    return getDefinition(entityId).getHavingClause();
  }

  /**
   * @param entityId the entity ID
   * @return the name of the table used to select entities identified by {@code entityId}
   * @throws IllegalArgumentException if the entity is undefined
   */
  public final String getSelectTableName(final String entityId) {
    return getDefinition(entityId).getSelectTableName();
  }

  /**
   * @param entityId the entity ID
   * @return the name of the table on which entities identified by {@code entityId} are based
   * @throws IllegalArgumentException if the entity is undefined
   */
  public final String getTableName(final String entityId) {
    return getDefinition(entityId).getTableName();
  }

  /**
   * @param entityId the entity ID
   * @return the sql query used when selecting entities identified by {@code entityId}
   * @throws IllegalArgumentException if the entity is undefined
   */
  public final String getSelectQuery(final String entityId) {
    return getDefinition(entityId).getSelectQuery();
  }

  /**
   * @param entityId the entity ID
   * @return true if the select query for the given entity, if any, contains a where clause
   */
  public final boolean selectQueryContainsWhereClause(final String entityId) {
    return getDefinition(entityId).selectQueryContainsWhereClause();
  }

  /**
   * @param entityId the entity ID
   * @return the query string used to select entities identified by {@code entityId}
   * @throws IllegalArgumentException if the entity is undefined
   */
  public final String getSelectColumnsString(final String entityId) {
    return getDefinition(entityId).getSelectColumnsString();
  }

  /**
   * @param entityId the entity ID
   * @return the primary key generator for entities identified by {@code entityId}
   * @throws IllegalArgumentException if the entity is undefined
   */
  public final Entity.KeyGenerator getKeyGenerator(final String entityId) {
    return getDefinition(entityId).getKeyGenerator();
  }

  /**
   * @param entityId the entity ID
   * @return the type of primary key generator used by entities identified by {@code entityId}
   * @throws IllegalArgumentException if the entity is undefined
   */
  public final Entity.KeyGenerator.Type getKeyGeneratorType(final String entityId) {
    return getDefinition(entityId).getKeyGeneratorType();
  }

  /**
   * @param entityId the entity ID
   * @return the {@link Entity.ToString} instance used to provide string representations
   * of entities of the given type
   * @throws IllegalArgumentException if the entity is undefined
   */
  public final Entity.ToString getStringProvider(final String entityId) {
    return getDefinition(entityId).getStringProvider();
  }

  /**
   * @param entityId the entity ID
   * @return the default Comparator to use when sorting entities of the given type
   */
  public final Comparator<Entity> getComparator(final String entityId) {
    return getDefinition(entityId).getComparator();
  }

  /**
   * Returns true if the value for the primary key of this entity is automatically generated, either by the framework,
   * such as values queried from sequences or set by triggers. If not the primary key value must be set manually
   * before the entity is inserted.
   * @param entityId the entity ID
   * @return true if the value for the primary key is automatically generated
   */
  public final boolean isPrimaryKeyAutoGenerated(final String entityId) {
    return !getKeyGeneratorType(entityId).isManual();
  }

  /**
   * @param entityId the entity ID
   * @return true if the primary key of the given type of entity is comprised of a single integer value
   */
  public final boolean hasSingleIntegerPrimaryKey(final String entityId) {
    final List<Property.ColumnProperty> primaryKeyProperties = getDefinition(entityId).getPrimaryKeyProperties();
    return primaryKeyProperties.size() == 1 && primaryKeyProperties.get(0).isInteger();
  }

  /**
   * Retrieves the column properties comprising the entity identified by {@code entityId}
   * @param entityId the entity ID
   * @param includePrimaryKeyProperties if true primary key properties are included
   * @param includeReadOnly if true then properties that are marked as 'read only' are included
   * @param includeNonUpdatable if true then non updatable properties are included
   * @return a list containing the database properties (properties that map to database columns) comprising
   * the entity identified by {@code entityId}
   */
  public final List<Property.ColumnProperty> getColumnProperties(final String entityId,
                                                                 final boolean includePrimaryKeyProperties,
                                                                 final boolean includeReadOnly,
                                                                 final boolean includeNonUpdatable) {
    final List<Property.ColumnProperty> properties = new ArrayList<>(getDefinition(entityId).getColumnProperties());
    properties.removeIf(property ->
            !includeReadOnly && property.isReadOnly()
                    || !includeNonUpdatable && !property.isUpdatable()
                    || !includePrimaryKeyProperties && property.isPrimaryKeyProperty());

    return properties;
  }

  /**
   * @param entityId the entity ID
   * @return a list containing the visible (non-hidden) properties
   * in the entity identified by {@code entityId}
   */
  public final List<Property> getVisibleProperties(final String entityId) {
    Objects.requireNonNull(entityId, ENTITY_ID_PARAM);
    return getDefinition(entityId).getVisibleProperties();
  }

  /**
   * @param entityId the entityId
   * @param propertyIds the IDs of the properties to retrieve
   * @return the {@link Property.ColumnProperty}s specified by the given property IDs
   * @throws IllegalArgumentException in case a given propertyId does not represent a {@link Property.ColumnProperty}
   */
  public final List<Property.ColumnProperty> getColumnProperties(final String entityId, final Collection<String> propertyIds) {
    if (propertyIds == null || propertyIds.isEmpty()) {
      return Collections.emptyList();
    }

    return propertyIds.stream().map(propertyId -> getColumnProperty(entityId, propertyId)).collect(Collectors.toList());
  }

  /**
   * @param entityId the entity ID
   * @param propertyId the property ID
   * @return the column property identified by property ID
   * @throws IllegalArgumentException in case the propertyId does not represent a {@link Property.ColumnProperty}
   */
  public final Property.ColumnProperty getColumnProperty(final String entityId, final String propertyId) {
    final Property property = getProperty(entityId, propertyId);
    if (!(property instanceof Property.ColumnProperty)) {
      throw new IllegalArgumentException(propertyId + ", " + property.getClass() + " does not implement Property.ColumnProperty");
    }

    return (Property.ColumnProperty) property;
  }

  /**
   * @param entityId the entity ID
   * @param propertyId the property ID
   * @return the property identified by {@code propertyId} in the entity identified by {@code entityId}
   * @throws IllegalArgumentException in case no such property exists
   */
  public final Property getProperty(final String entityId, final String propertyId) {
    Objects.requireNonNull(entityId, ENTITY_ID_PARAM);
    Objects.requireNonNull(propertyId, PROPERTY_ID_PARAM);
    final Property property = getDefinition(entityId).getPropertyMap().get(propertyId);
    if (property == null) {
      throw new IllegalArgumentException("Property '" + propertyId + "' not found in entity: " + entityId);
    }

    return property;
  }

  /**
   * @param entityId the entity ID
   * @param propertyIds the IDs of the properties to retrieve
   * @return a list containing the properties identified by {@code propertyIds}, found in
   * the entity identified by {@code entityId}
   */
  public final List<Property> getProperties(final String entityId, final Collection<String> propertyIds) {
    Objects.requireNonNull(propertyIds, PROPERTY_ID_PARAM);
    return getProperties(entityId, propertyIds.toArray(new String[0]));
  }

  /**
   * @param entityId the entity ID
   * @param propertyIds the IDs of the properties to retrieve
   * @return a list containing the properties identified by {@code propertyIds}, found in
   * the entity identified by {@code entityId}
   */
  public final List<Property> getProperties(final String entityId, final String... propertyIds) {
    Objects.requireNonNull(entityId, ENTITY_ID_PARAM);
    Objects.requireNonNull(propertyIds, PROPERTY_ID_PARAM);

    return Arrays.stream(propertyIds).map(propertyId -> getProperty(entityId, propertyId)).collect(Collectors.toList());
  }

  /**
   * @param entityId the entity ID
   * @param includeHidden true if hidden properties should be included in the result
   * @return a collection containing the properties found in the entity identified by {@code entityId}
   */
  public final Collection<Property> getProperties(final String entityId, final boolean includeHidden) {
    return includeHidden ? getProperties(entityId) : getVisibleProperties(entityId);
  }

  /**
   * @param entityId the entity ID
   * @return a list containing all database properties found in the entity identified by {@code entityId},
   * that is, properties that map to database columns
   */
  public final List<Property.ColumnProperty> getColumnProperties(final String entityId) {
    return getDefinition(entityId).getColumnProperties();
  }

  /**
   * @param entityId the entity ID
   * @return a list containing all transient database properties found in the entity identified by {@code entityId},
   * that is, properties that do not map to database columns
   */
  public final List<Property.TransientProperty> getTransientProperties(final String entityId) {
    return getDefinition(entityId).getTransientProperties();
  }

  /**
   * @param entityId the entity ID
   * @return a list containing all the foreign key properties found in the entity
   * identified by {@code entityId}
   */
  public final List<Property.ForeignKeyProperty> getForeignKeyProperties(final String entityId) {
    return getDefinition(entityId).getForeignKeyProperties();
  }

  /**
   * @param entityId the entity ID
   * @return true if the given entity contains denormalized properties
   */
  public final boolean hasDenormalizedProperties(final String entityId) {
    return getDefinition(entityId).hasDenormalizedProperties();
  }

  /**
   * @param entityId the entity ID
   * @param foreignKeyPropertyId the foreign key id
   * @return a list containing all denormalized properties of the entity identified by {@code entityId}
   * which source is the entity identified by {@code propertyOwnerEntityID}
   */
  public final List<Property.DenormalizedProperty> getDenormalizedProperties(final String entityId,
                                                                             final String foreignKeyPropertyId) {
    return getDefinition(entityId).getDenormalizedProperties(foreignKeyPropertyId);
  }

  /**
   * @param entityId the entity ID
   * @param foreignKeyPropertyId the foreign key id
   * @return true if the entity identified by {@code entityId} contains denormalized properties
   * which source is the entity identified by {@code propertyOwnerEntityID}
   */
  public final boolean hasDenormalizedProperties(final String entityId, final String foreignKeyPropertyId) {
    return getDefinition(entityId).hasDenormalizedProperties(foreignKeyPropertyId);
  }

  /**
   * Returns true if this entity contains properties which values are derived from the value of the given property
   * @param entityId the entityId
   * @param propertyId the ID of the property
   * @return true if any properties are derived from the given property
   */
  public final boolean hasDerivedProperties(final String entityId, final String propertyId) {
    return getDefinition(entityId).hasDerivedProperties(propertyId);
  }

  /**
   * Returns the properties which values are derived from the value of the given property,
   * an empty collection if no such derived properties exist
   * @param entityId the entityId
   * @param propertyId the ID of the property
   * @return a collection containing the properties which are derived from the given property
   */
  public final Collection<Property.DerivedProperty> getDerivedProperties(final String entityId, final String propertyId) {
    return getDefinition(entityId).getDerivedProperties(propertyId);
  }

  /**
   * Returns the foreign key properties referencing entities of the given type
   * @param entityId the ID of the entity from which to retrieve the foreign key properties
   * @param foreignEntityId the ID of the referenced entity
   * @return a List containing the properties, an empty list is returned in case no properties fit the condition
   */
  public final List<Property.ForeignKeyProperty> getForeignKeyProperties(final String entityId, final String foreignEntityId) {
    return getForeignKeyProperties(entityId).stream().filter(foreignKeyProperty ->
            foreignKeyProperty.getForeignEntityId().equals(foreignEntityId)).collect(Collectors.toList());
  }

  /**
   * @param entityId the entity ID
   * @param propertyId the property ID
   * @return the Property.ForeignKeyProperty with the given propertyId
   * @throws IllegalArgumentException in case no such property exists
   */
  public final Property.ForeignKeyProperty getForeignKeyProperty(final String entityId, final String propertyId) {
    final List<Property.ForeignKeyProperty> foreignKeyProperties = getForeignKeyProperties(entityId);
    for (int i = 0; i < foreignKeyProperties.size(); i++) {
      final Property.ForeignKeyProperty foreignKeyProperty = foreignKeyProperties.get(i);
      if (foreignKeyProperty.is(propertyId)) {
        return foreignKeyProperty;
      }
    }

    throw new IllegalArgumentException("Foreign key property with id: " + propertyId + " not found in entity of type: " + entityId);
  }

  /**
   * @param entityId the entityId
   * @return all foreign keys referencing entities of type {@code entityId}
   */
  public final Collection<Property.ForeignKeyProperty> getForeignKeyReferences(final String entityId) {
    List<Property.ForeignKeyProperty> foreignKeyReferences = foreignKeyReferenceMap.get(entityId);
    if (foreignKeyReferences == null) {
      foreignKeyReferences = new ArrayList<>();
      for (final String definedEntityId : entityDefinitions.keySet()) {
        for (final Property.ForeignKeyProperty foreignKeyProperty : getDefinition(definedEntityId).getForeignKeyProperties()) {
          if (foreignKeyProperty.getForeignEntityId().equals(entityId)) {
            foreignKeyReferences.add(foreignKeyProperty);
          }
        }
      }
      foreignKeyReferenceMap.put(entityId, foreignKeyReferences);
    }

    return foreignKeyReferences;
  }

  /**
   * @param entityId the entity ID
   * @return the properties comprising the given entity type
   */
  public final List<Property> getProperties(final String entityId) {
    return getDefinition(entityId).getProperties();
  }

  /**
   * @param entityId the entity ID
   * @return the caption associated with the given entity type
   */
  public final String getCaption(final String entityId) {
    return getDefinition(entityId).getCaption();
  }

  /**
   * @param entityId the entityId
   * @return the validator for the given entity type
   */
  public final Entity.Validator getValidator(final String entityId) {
    return getDefinition(entityId).getValidator();
  }

  /**
   * @param entityId the entityId
   * @return the ResultPacker responsible for packing this entity type
   */
  public final ResultPacker<Entity> getResultPacker(final String entityId) {
    return getDefinition(entityId).getResultPacker();
  }

  /**
   * @return the entityIds of all defined entities
   */
  public final Collection<String> getDefinedEntities() {
    return new ArrayList<>(entityDefinitions.keySet());
  }

  /**
   * @param entityId the entity ID
   * @return true if the entity is defined
   */
  public final boolean isDefined(final String entityId) {
    return entityDefinitions.containsKey(entityId);
  }

  /**
   * @param entityId the entity ID
   * @return a list containing all updatable properties associated with the given entity ID
   */
  public final List<Property> getUpdatableProperties(final String entityId) {
    final List<Property.ColumnProperty> columnProperties = getColumnProperties(entityId,
            getKeyGeneratorType(entityId).isManual(), false, false);
    columnProperties.removeIf(property -> property.isForeignKeyProperty() || property.isDenormalized());
    final List<Property> updatable = new ArrayList<>(columnProperties);
    final Collection<Property.ForeignKeyProperty> foreignKeyProperties = getForeignKeyProperties(entityId);
    for (final Property.ForeignKeyProperty foreignKeyProperty : foreignKeyProperties) {
      if (!foreignKeyProperty.isReadOnly() && foreignKeyProperty.isUpdatable()) {
        updatable.add(foreignKeyProperty);
      }
    }
    sort(updatable);

    return updatable;
  }

  /**
   * @param entities the entities to check
   * @return true if any of the given entities has a modified primary key
   */
  public final boolean isKeyModified(final Collection<Entity> entities) {
    if (Util.nullOrEmpty(entities)) {
      return false;
    }

    return entities.stream().anyMatch(entity ->
            getPrimaryKeyProperties(entity.getEntityId()).stream().anyMatch(entity::isModified));
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
      throw new IllegalArgumentException("Required configuration property is missing: " + Entities.ENTITY_SERIALIZER_CLASS);
    }

    try {
      final String serializerClass = Entities.ENTITY_SERIALIZER_CLASS.get();

      return (Serializer<Entity>) Class.forName(serializerClass).getConstructor().newInstance();
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @param entityId the entity ID
   * @param propertyIds the property IDs
   * @return the given properties sorted by caption, or if that is not available, property ID
   */
  public final List<Property> getSortedProperties(final String entityId, final Collection<String> propertyIds) {
    final List<Property> properties = new ArrayList<>(getProperties(entityId, propertyIds));
    sort(properties);

    return properties;
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
   * @throws IllegalArgumentException in case an operation with the same ID has already been added
   */
  public final void addOperation(final DatabaseConnection.Operation operation) {
    if (databaseOperations.containsKey(operation.getId())) {
      throw new IllegalArgumentException("Operation already defined: " + databaseOperations.get(operation.getId()).getName());
    }

    databaseOperations.put(operation.getId(), operation);
  }

  /**
   * @param <C> the type of the database connection this procedure requires
   * @param procedureId the procedure ID
   * @return the procedure
   * @throws IllegalArgumentException in case the procedure is not found
   */
  public final <C> DatabaseConnection.Procedure<C> getProcedure(final String procedureId) {
    final DatabaseConnection.Operation operation = databaseOperations.get(procedureId);
    if (operation == null) {
      throw new IllegalArgumentException("Procedure not found: " + procedureId);
    }

    return (DatabaseConnection.Procedure) operation;
  }

  /**
   * @param <C> the type of the database connection this function requires
   * @param functionId the function ID
   * @return the function
   * @throws IllegalArgumentException in case the function is not found
   */
  public final <C> DatabaseConnection.Function<C> getFunction(final String functionId) {
    final DatabaseConnection.Operation operation = databaseOperations.get(functionId);
    if (operation == null) {
      throw new IllegalArgumentException("Function not found: " + functionId);
    }

    return (DatabaseConnection.Function) operation;
  }

  /**
   * Registers this instance for lookup via {@link Entities#getDomain(String)}
   * @return this Entities instance
   * @see #getDomainId()
   */
  public final Entities registerDomain() {
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
   * @param tableName the table name
   * @param columnName the primary key column name
   * @return a incrementing primary key generator
   */
  public final Entity.KeyGenerator incrementKeyGenerator(final String tableName, final String columnName) {
    return new IncrementKeyGenerator(tableName, columnName);
  }

  /**
   * Instantiates a primary key generator which fetches primary key values from a sequence prior to insert
   * @param sequenceName the sequence name
   * @return a sequence based primary key generator
   */
  public final Entity.KeyGenerator sequenceKeyGenerator(final String sequenceName) {
    return new SequenceKeyGenerator(sequenceName);
  }

  /**
   * Instantiates a primary key generator which fetches primary key values using the given query prior to insert
   * @param query a query for retrieving the primary key value
   * @return a query based primary key generator
   */
  public final Entity.KeyGenerator queriedKeyGenerator(final String query) {
    return new QueriedKeyGenerator() {
      @Override
      protected String getQuery(final Database database) {
        return query;
      }
    };
  }

  /**
   * Instantiates a primary key generator which fetches automatically incremented primary key values after insert
   * @param valueSource the value source, whether a sequence or a table name
   * @return a auto-increment based primary key generator
   */
  public final Entity.KeyGenerator automaticKeyGenerator(final String valueSource) {
    return new AutomaticKeyGenerator(valueSource);
  }

  /**
   * Returns true if this entity has a null primary key or a null original primary key,
   * which is the best guess about an entity being new, as in, not existing in a database.
   * @param entity the entity
   * @return true if this entity has not been persisted
   */
  public static boolean isEntityNew(final Entity entity) {
    final Entity.Key key = entity.getKey();
    final Entity.Key originalKey = entity.getOriginalKey();

    return key.isNull() || originalKey.isNull();
  }

  /**
   * @param entities the entities
   * @return a List of entities that have been modified
   */
  public static List<Entity> getModifiedEntities(final Collection<Entity> entities) {
    Objects.requireNonNull(entities, ENTITIES_PARAM);

    return entities.stream().filter(ValueMap::isModified).collect(Collectors.toList());
  }

  /**
   * @param entity the entity instance to check
   * @param comparison the entity instance to compare with
   * @return all {@link Property.ColumnProperty}s which value is missing or the original value differs from the one in the comparison
   * entity, returns an empty Collection if all of {@code entity}s original values match the values found in {@code comparison}
   */
  public static final Collection<Property.ColumnProperty> getModifiedColumnProperties(final Entity entity, final Entity comparison) {
    //BLOB property values are not loaded, so we can't compare those
    return comparison.keySet().stream().filter(property ->
            property instanceof Property.ColumnProperty && !property.isType(Types.BLOB)
                    && isValueMissingOrModified(entity, comparison, property.getPropertyId()))
            .map(property -> (Property.ColumnProperty) property).collect(Collectors.toList());
  }

  /**
   * @param entities the entities
   * @return a List containing the primary keys of the given entities
   */
  public static List<Entity.Key> getKeys(final Collection<Entity> entities) {
    return getKeys(entities, false);
  }

  /**
   * @param entities the entities
   * @param originalValue if true then the original value of the primary key is used
   * @return a List containing the primary keys of the given entities
   */
  public static List<Entity.Key> getKeys(final Collection<Entity> entities, final boolean originalValue) {
    Objects.requireNonNull(entities, ENTITIES_PARAM);

    return entities.stream().map(entity -> originalValue ? entity.getOriginalKey() : entity.getKey()).collect(Collectors.toList());
  }

  /**
   * Retrieves the values of the given keys, assuming they are single column keys.
   * @param <T> the value type
   * @param keys the keys
   * @return the actual property values of the given keys
   */
  public static <T> List<T> getValues(final List<Entity.Key> keys) {
    Objects.requireNonNull(keys, "keys");
    final List<T> list = new ArrayList<>(keys.size());
    for (int i = 0; i < keys.size(); i++) {
      final Entity.Key key = keys.get(i);
      list.add((T) key.get(key.getFirstProperty()));
    }

    return list;
  }

  /**
   * @param <T> the value type
   * @param propertyId the ID of the property for which to retrieve the values
   * @param entities the entities from which to retrieve the property value
   * @return a Collection containing the values of the property with the given ID from the given entities,
   * null values are included
   */
  public static <T> Collection<T> getValues(final String propertyId, final Collection<Entity> entities) {
    return getValues(propertyId, entities, true);
  }

  /**
   * @param <T> the value type
   * @param propertyId the ID of the property for which to retrieve the values
   * @param entities the entities from which to retrieve the property value
   * @param includeNullValues if true then null values are included
   * @return a Collection containing the values of the property with the given ID from the given entities
   */
  public static <T> Collection<T> getValues(final String propertyId, final Collection<Entity> entities,
                                            final boolean includeNullValues) {
    return collectValues(new ArrayList<T>(entities == null ? 0 : entities.size()), propertyId, entities, includeNullValues);
  }

  /**
   * Returns a Collection containing the distinct values of {@code propertyId} from the given entities, excluding null values.
   * If the {@code entities} list is null an empty Collection is returned.
   * @param <T> the value type
   * @param propertyId the ID of the property for which to retrieve the values
   * @param entities the entities from which to retrieve the values
   * @return a Collection containing the distinct property values, excluding null values
   */
  public static <T> Collection<T> getDistinctValues(final String propertyId, final Collection<Entity> entities) {
    return getDistinctValues(propertyId, entities, false);
  }

  /**
   * Returns a Collection containing the distinct values of {@code propertyId} from the given entities.
   * If the {@code entities} list is null an empty Collection is returned.
   * @param <T> the value type
   * @param propertyId the ID of the property for which to retrieve the values
   * @param entities the entities from which to retrieve the values
   * @param includeNullValue if true then null is considered a value
   * @return a Collection containing the distinct property values
   */
  public static <T> Collection<T> getDistinctValues(final String propertyId, final Collection<Entity> entities,
                                                    final boolean includeNullValue) {
    return collectValues(new HashSet<T>(), propertyId, entities, includeNullValue);
  }

  /**
   * Sets the value of the property with ID {@code propertyId} to {@code value}
   * in the given entities
   * @param propertyId the ID of the property for which to set the value
   * @param value the value
   * @param entities the entities for which to set the value
   * @return the old property values mapped to their respective primary key
   */
  public static Map<Entity.Key, Object> put(final String propertyId, final Object value,
                                            final Collection<Entity> entities) {
    Objects.requireNonNull(entities, ENTITIES_PARAM);
    final Map<Entity.Key, Object> oldValues = new HashMap<>(entities.size());
    for (final Entity entity : entities) {
      oldValues.put(entity.getKey(), entity.put(propertyId, value));
    }

    return oldValues;
  }

  /**
   * Maps the given entities to their primary key
   * @param entities the entities to map
   * @return the mapped entities
   */
  public static Map<Entity.Key, Entity> mapToKey(final Collection<Entity> entities) {
    Objects.requireNonNull(entities, ENTITIES_PARAM);
    final Map<Entity.Key, Entity> entityMap = new HashMap<>();
    for (final Entity entity : entities) {
      entityMap.put(entity.getKey(), entity);
    }

    return entityMap;
  }

  /**
   * Returns a LinkedHashMap containing the given entities mapped to the value of the property with ID {@code propertyId},
   * respecting the iteration order of the given collection
   * @param <K> the key type
   * @param propertyId the ID of the property which value should be used for mapping
   * @param entities the entities to map by property value
   * @return a Map of entities mapped to property value
   */
  public static <K> LinkedHashMap<K, List<Entity>> mapToValue(final String propertyId, final Collection<Entity> entities) {
    return Util.map(entities, value -> (K) value.get(propertyId));
  }

  /**
   * Returns a LinkedHashMap containing the given entities mapped to their entityIds,
   * respecting the iteration order of the given collection
   * @param entities the entities to map by entityId
   * @return a Map of entities mapped to entityId
   */
  public static LinkedHashMap<String, List<Entity>> mapToEntityId(final Collection<Entity> entities) {
    return Util.map(entities, Entity::getEntityId);
  }

  /**
   * Returns a LinkedHashMap containing the given entity keys mapped to their entityIds,
   * respecting the iteration order of the given collection
   * @param keys the entity keys to map by entityId
   * @return a Map of entity keys mapped to entityId
   */
  public static LinkedHashMap<String, List<Entity.Key>> mapKeysToEntityID(final Collection<Entity.Key> keys) {
    return Util.map(keys, Entity.Key::getEntityId);
  }

  /**
   * Maps the given entities and their updated counterparts to their original primary keys,
   * assumes a single copy of each entity in the given lists.
   * @param entitiesBeforeUpdate the entities before update
   * @param entitiesAfterUpdate the entities after update
   * @return the updated entities mapped to their respective original primary keys
   */
  public static Map<Entity.Key, Entity> mapToOriginalPrimaryKey(final List<Entity> entitiesBeforeUpdate,
                                                                final List<Entity> entitiesAfterUpdate) {
    final List<Entity> entitiesAfterUpdateCopy = new ArrayList<>(entitiesAfterUpdate);
    final Map<Entity.Key, Entity> keyMap = new HashMap<>(entitiesBeforeUpdate.size());
    for (final Entity entity : entitiesBeforeUpdate) {
      keyMap.put(entity.getOriginalKey(), findAndRemove(entity.getKey(), entitiesAfterUpdateCopy.listIterator()));
    }

    return keyMap;
  }

  /**
   * Creates a two dimensional array containing the values of the given properties for the given entities in string format.
   * @param properties the properties
   * @param entities the entities
   * @return the values of the given properties from the given entities in a two dimensional array
   */
  public static String[][] getStringValueArray(final List<? extends Property> properties, final List<Entity> entities) {
    final String[][] data = new String[entities.size()][];
    for (int i = 0; i < data.length; i++) {
      final List<String> line = new ArrayList<>();
      for (final Property property : properties) {
        line.add(entities.get(i).getAsString(property));
      }

      data[i] = line.toArray(new String[0]);
    }

    return data;
  }

  /**
   * @param entities the entities to copy
   * @return deep copies of the entities, in the same order as they are received
   */
  public static List<Entity> copyEntities(final List<Entity> entities) {
    Objects.requireNonNull(entities, ENTITIES_PARAM);

    return entities.stream().map(entity -> (Entity) entity.getCopy()).collect(Collectors.toList());
  }

  /**
   * Sorts the given properties by caption, or if that is not available, property ID, ignoring case
   * @param properties the properties to sort
   */
  public static void sort(final List<? extends Property> properties) {
    Objects.requireNonNull(properties, "properties");
    final Collator collator = Collator.getInstance();
    properties.sort((o1, o2) -> collator.compare(o1.toString().toLowerCase(), o2.toString().toLowerCase()));
  }

  /**
   * @param domainId the ID of the domain for which to retrieve the entity definitions
   * @return the domain instance registered for the given domainId
   * @throws IllegalArgumentException in case the domain has not been registered
   * @see #registerDomain()
   */
  public static Entities getDomain(final String domainId) {
    final Entities domain = REGISTERED_DOMAINS.get(domainId);
    if (domain == null) {
      throw new IllegalArgumentException("Domain '" + domainId + "' has not been registered");
    }

    return domain;
  }

  /**
   * @return all domains that have been registered via {@link #registerDomain()}
   */
  public static Collection<Entities> getAllDomains() {
    return Collections.unmodifiableCollection(REGISTERED_DOMAINS.values());
  }

  /**
   * Finds entities according to the values of values
   * @param entities the entities to search
   * @param values the property values to use as condition mapped to their respective propertyIds
   * @return the entities having the exact same property values as in the given value map
   */
  public static List<Entity> getEntitiesByValue(final Collection<Entity> entities, final Map<String, Object> values) {
    final List<Entity> result = new ArrayList<>();
    for (final Entity entity : Objects.requireNonNull(entities, ENTITIES_PARAM)) {
      boolean equal = true;
      for (final Map.Entry<String, Object> entries : values.entrySet()) {
        final String propertyId = entries.getKey();
        if (!entity.get(propertyId).equals(entries.getValue())) {
          equal = false;
          break;
        }
      }
      if (equal) {
        result.add(entity);
      }
    }

    return result;
  }

  /**
   * @param entity the entity instance to check
   * @param comparison the entity instance to compare with
   * @param propertyId the property to check
   * @return true if the value is missing or the original value differs from the one in the comparison entity
   */
  static boolean isValueMissingOrModified(final Entity entity, final Entity comparison, final String propertyId) {
    return !entity.containsKey(propertyId) || !Objects.equals(comparison.get(propertyId), entity.getOriginal(propertyId));
  }

  Entity.Definition getDefinition(final String entityId) {
    final Entity.Definition definition = entityDefinitions.get(Objects.requireNonNull(entityId, ENTITY_ID_PARAM));
    if (definition == null) {
      throw new IllegalArgumentException("Undefined entity: " + entityId);
    }

    return definition;
  }

  private Map<String, Property> initializePropertyMap(final String domainId, final String entityId, final Property... properties) {
    final Map<String, Property> propertyMap = new LinkedHashMap<>(properties.length);
    for (final Property property : properties) {
      validateAndAddProperty(property, domainId, entityId, propertyMap);
      if (property instanceof Property.ForeignKeyProperty) {
        initializeForeignKeyProperty(domainId, entityId, propertyMap, (Property.ForeignKeyProperty) property);
      }
    }
    checkIfPrimaryKeyIsSpecified(entityId, propertyMap);

    return Collections.unmodifiableMap(propertyMap);
  }

  private void initializeForeignKeyProperty(final String domainId, final String entityId, final Map<String, Property> propertyMap,
                                            final Property.ForeignKeyProperty foreignKeyProperty) {
    final List<Property.ColumnProperty> properties = foreignKeyProperty.getProperties();
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
    for (final Property.ColumnProperty property : properties) {
      if (!(property instanceof Property.MirrorProperty)) {
        validateAndAddProperty(property, domainId, entityId, propertyMap);
      }
    }
  }

  private static Entity findAndRemove(final Entity.Key primaryKey, final ListIterator<Entity> iterator) {
    while (iterator.hasNext()) {
      final Entity current = iterator.next();
      if (current.getKey().equals(primaryKey)) {
        iterator.remove();

        return current;
      }
    }

    return null;
  }

  private static <T> Collection<T> collectValues(final Collection<T> collection, final String propertyId,
                                                 final Collection<Entity> entities, final boolean includeNullValues) {
    Objects.requireNonNull(collection);
    Objects.requireNonNull(propertyId);
    if (!Util.nullOrEmpty(entities)) {
      for (final Entity entity : entities) {
        final Object value = entity.get(propertyId);
        if (value != null || includeNullValues) {
          collection.add((T) value);
        }
      }
    }

    return collection;
  }

  private static void validateAndAddProperty(final Property property, final String domainId, final String entityId,
                                             final Map<String, Property> propertyMap) {
    checkIfUniquePropertyId(property, entityId, propertyMap);
    property.setDomainID(domainId);
    property.setEntityID(entityId);
    propertyMap.put(property.getPropertyId(), property);
  }

  private static void checkIfUniquePropertyId(final Property property, final String entityId, final Map<String, Property> propertyMap) {
    if (propertyMap.containsKey(property.getPropertyId())) {
      throw new IllegalArgumentException("Property with ID " + property.getPropertyId()
              + (property.getCaption() != null ? " (caption: " + property.getCaption() + ")" : "")
              + " has already been defined as: " + propertyMap.get(property.getPropertyId()) + " in entity: " + entityId);
    }
  }

  private static void checkIfPrimaryKeyIsSpecified(final String entityId, final Map<String, Property> propertyMap) {
    final Collection<Integer> usedPrimaryKeyIndexes = new ArrayList<>();
    boolean primaryKeyPropertyFound = false;
    for (final Property property : propertyMap.values()) {
      if (property instanceof Property.ColumnProperty && ((Property.ColumnProperty) property).isPrimaryKeyProperty()) {
        final Integer index = ((Property.ColumnProperty) property).getPrimaryKeyIndex();
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

  private Entities registerDomain(final String domainId, final Entities domain) {
    REGISTERED_DOMAINS.put(domainId, domain);

    return domain;
  }

  private static List<Property.ForeignKeyProperty> getForeignKeyProperties(final Collection<Property> properties) {
    return properties.stream().filter(property -> property instanceof Property.ForeignKeyProperty)
            .map(property -> (Property.ForeignKeyProperty) property).collect(Collectors.toList());
  }

  private static List<Property.ColumnProperty> getColumnProperties(final Collection<Property> properties) {
    final List<Property.ColumnProperty> columnProperties = properties.stream()
            .filter(property -> property instanceof Property.ColumnProperty)
            .map(property -> (Property.ColumnProperty) property).collect(Collectors.toList());

    for (int idx = 0; idx < columnProperties.size(); idx++) {
      columnProperties.get(idx).setSelectIndex(idx + 1);
    }

    return columnProperties;
  }

  private static List<Property.TransientProperty> getTransientProperties(final Collection<Property> properties) {
    return properties.stream().filter(property -> property instanceof Property.TransientProperty)
            .map(property -> (Property.TransientProperty) property)
            .collect(Collectors.toList());
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
   * Entities.StringProvider provider = new Entities.StringProvider();<br>
   * provider.addText("key1=").addValue("key1").addText(", key3='").addValue("key3")<br>
   *         .addText("' foreign key value=").addForeignKeyValue("key4", "refKey");<br>
   * System.out.println(provider.toString(entity));<br>
   * }
   * <br>
   * outputs the following String:<br><br>
   * {@code key1=value1, key3='value3' foreign key value=refValue}
   */
  public static final class StringProvider implements Entity.ToString {

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
     * @param propertyId the ID of the property which value should be used for a string representation
     */
    public StringProvider(final String propertyId) {
      addValue(propertyId);
    }

    /** {@inheritDoc} */
    @Override
    public String toString(final Entity entity) {
      Objects.requireNonNull(entity, ENTITY_PARAM);

      return valueProviders.stream().map(valueProvider -> valueProvider.toString(entity)).collect(Collectors.joining());
    }

    /**
     * Adds the value mapped to the given key to this {@link StringProvider}
     * @param propertyId the ID of the property which value should be added to the string representation
     * @return this {@link StringProvider} instance
     */
    public StringProvider addValue(final String propertyId) {
      Objects.requireNonNull(propertyId, PROPERTY_ID_PARAM);
      valueProviders.add(new StringValueProvider(propertyId));
      return this;
    }

    /**
     * Adds the value mapped to the given key to this StringProvider
     * @param propertyId the ID of the property which value should be added to the string representation
     * @param format the Format to use when appending the value
     * @return this {@link StringProvider} instance
     */
    public StringProvider addFormattedValue(final String propertyId, final Format format) {
      Objects.requireNonNull(propertyId, PROPERTY_ID_PARAM);
      Objects.requireNonNull(format, "format");
      valueProviders.add(new FormattedValueProvider(propertyId, format));
      return this;
    }

    /**
     * Adds the value mapped to the given property in the {@link Entity} instance mapped to the given foreignKeyProperty
     * to this {@link StringProvider}
     * @param foreignKeyProperty the foreign key property
     * @param propertyId the ID of the property in the referenced entity to use
     * @return this {@link StringProvider} instance
     */
    public StringProvider addForeignKeyValue(final Property.ForeignKeyProperty foreignKeyProperty,
                                             final String propertyId) {
      Objects.requireNonNull(foreignKeyProperty, "foreignKeyProperty");
      Objects.requireNonNull(propertyId, PROPERTY_ID_PARAM);
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
        if (entity.isValueNull(propertyId)) {
          return "";
        }

        return format.format(entity.get(propertyId));
      }
    }

    private static final class ForeignKeyValueProvider implements StringProvider.ValueProvider {
      private static final long serialVersionUID = 1;
      private final Property.ForeignKeyProperty foreignKeyProperty;
      private final String propertyId;

      private ForeignKeyValueProvider(final Property.ForeignKeyProperty foreignKeyProperty,
                                      final String propertyId) {
        this.foreignKeyProperty = foreignKeyProperty;
        this.propertyId = propertyId;
      }

      @Override
      public String toString(final Entity entity) {
        if (entity.isValueNull(foreignKeyProperty)) {
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
   * A default extensible {@link Entity.Validator} implementation.
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
      Objects.requireNonNull(entity, ENTITY_PARAM);
      for (final Property property : entity.getProperties()) {
        if (!property.isReadOnly()) {
          validate(entity, property);
        }
      }
    }

    /** {@inheritDoc} */
    @Override
    public void validate(final Entity entity, final Property property) throws ValidationException {
      Objects.requireNonNull(entity, ENTITY_PARAM);
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
      Objects.requireNonNull(entity, ENTITY_PARAM);
      Objects.requireNonNull(property, PROPERTY_PARAM);
      if (entity.isValueNull(property)) {
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
      Objects.requireNonNull(entity, ENTITY_PARAM);
      Objects.requireNonNull(property, PROPERTY_PARAM);
      if (!isNullable(entity, property) && entity.isValueNull(property)) {
        if ((entity.getKey().isNull() || entity.getOriginalKey().isNull()) && !(property instanceof Property.ForeignKeyProperty)) {
          //a new entity being inserted, allow null for columns with default values and auto generated primary key values
          final boolean nonKeyColumnPropertyWithoutDefaultValue = isNonKeyColumnPropertyWithoutDefaultValue(property);
          final boolean primaryKeyPropertyWithoutAutoGenerate = isPrimaryKeyPropertyWithoutAutoGenerate(entity, property);
          if (nonKeyColumnPropertyWithoutDefaultValue || primaryKeyPropertyWithoutAutoGenerate) {
            throw new NullValidationException(property.getPropertyId(), MESSAGES.getString(MSG_PROPERTY_VALUE_IS_REQUIRED) + ": " + property);
          }
        }
        else {
          throw new NullValidationException(property.getPropertyId(), MESSAGES.getString(MSG_PROPERTY_VALUE_IS_REQUIRED) + ": " + property);
        }
      }
    }

    /** {@inheritDoc} */
    @Override
    public void performLengthValidation(final Entity entity, final Property property) throws LengthValidationException {
      Objects.requireNonNull(entity, ENTITY_PARAM);
      Objects.requireNonNull(property, PROPERTY_PARAM);
      if (entity.isValueNull(property)) {
        return;
      }

      final int maxLength = property.getMaxLength();
      final String value = (String) entity.get(property);
      if (maxLength != -1 && value.length() > maxLength) {
        throw new LengthValidationException(property.getPropertyId(), value, "'" + property + "' " +
                MESSAGES.getString("property_value_too_long") + " " + maxLength);
      }
    }

    private boolean isPrimaryKeyPropertyWithoutAutoGenerate(final Entity entity, final Property property) {
      return (property instanceof Property.ColumnProperty
              && ((Property.ColumnProperty) property).isPrimaryKeyProperty()) && entity.getKeyGeneratorType().isManual();
    }

    /**
     * @param property the property
     * @return true if the property is a part of a foreign key
     */
    private static boolean isForeignKeyProperty(final Property property) {
      return property instanceof Property.ColumnProperty && ((Property.ColumnProperty) property).isForeignKeyProperty();
    }

    private static boolean isNonKeyColumnPropertyWithoutDefaultValue(final Property property) {
      return property instanceof Property.ColumnProperty && !((Property.ColumnProperty) property).isPrimaryKeyProperty()
              && !((Property.ColumnProperty) property).columnHasDefaultValue();
    }
  }

  /**
   * Handles packing Entity query results.
   * Loads all database property values except for foreign key properties (Property.ForeignKeyProperty).
   */
  private static final class EntityResultPacker implements ResultPacker<Entity> {

    private final Entities domain;
    private final String entityId;
    private final List<Property.ColumnProperty> properties;
    private final List<Property.TransientProperty> transientProperties;
    private final boolean hasTransientProperties;
    private final int propertyCount;

    /**
     * Instantiates a new EntityResultPacker.
     * @param entityId the ID of the entities this packer packs
     */
    private EntityResultPacker(final Entities domain, final String entityId, final List<Property.ColumnProperty> columnProperties,
                               final List<Property.TransientProperty> transientProperties, final int propertyCount) {
      this.domain = domain;
      this.entityId = entityId;
      this.properties = columnProperties;
      this.transientProperties = transientProperties;
      this.hasTransientProperties = !Util.nullOrEmpty(this.transientProperties);
      this.propertyCount = propertyCount;
    }

    @Override
    public Entity fetch(final ResultSet resultSet) throws SQLException {
      final Map<Property, Object> values = new HashMap<>(propertyCount);
      if (hasTransientProperties) {
        for (int i = 0; i < transientProperties.size(); i++) {
          final Property.TransientProperty transientProperty = transientProperties.get(i);
          if (!(transientProperty instanceof Property.DerivedProperty)) {
            values.put(transientProperty, null);
          }
        }
      }
      for (int i = 0; i < properties.size(); i++) {
        final Property.ColumnProperty property = properties.get(i);
        try {
          values.put(property, property.fetchValue(resultSet));
        }
        catch (final Exception e) {
          throw new SQLException("Exception fetching: " + property + ", entity: " + entityId + " [" + e.getMessage() + "]", e);
        }
      }

      return new DefaultEntity(domain, entityId, values);
    }
  }

  static final class DefaultStringProvider implements Entity.ToString {

    private static final long serialVersionUID = 1;

    @Override
    public String toString(final Entity entity) {
      return entity.getEntityId() + ": " + entity.getKey();
    }
  }

  static class DefaultKeyGenerator implements Entity.KeyGenerator {

    @Override
    public Type getType() {
      return Type.NONE;
    }

    @Override
    public void beforeInsert(final Entity entity, final DatabaseConnection connection) throws SQLException {/*Provided for subclasses*/}

    @Override
    public void afterInsert(final Entity entity, final DatabaseConnection connection, final Statement statement) throws SQLException {/*Provided for subclasses*/}
  }

  abstract class QueriedKeyGenerator extends DefaultKeyGenerator {

    @Override
    public Type getType() {
      return Type.QUERY;
    }

    protected final Property.ColumnProperty getPrimaryKeyProperty(final String entityId) {
      return getPrimaryKeyProperties(entityId).get(0);
    }

    protected final void queryAndSet(final Entity entity, final Property.ColumnProperty keyProperty,
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

  final class IncrementKeyGenerator extends QueriedKeyGenerator {

    private final String query;

    IncrementKeyGenerator(final String tableName, final String columnName) {
      this.query = "select max(" + columnName + ") + 1 from " + tableName;
    }

    @Override
    public Type getType() {
      return Type.INCREMENT;
    }

    @Override
    public void beforeInsert(final Entity entity, final DatabaseConnection connection) throws SQLException {
      final Property.ColumnProperty primaryKeyProperty = getPrimaryKeyProperty(entity.getEntityId());
      if (entity.isValueNull(primaryKeyProperty)) {
        queryAndSet(entity, primaryKeyProperty, connection);
      }
    }

    @Override
    protected String getQuery(final Database database) {
      return query;
    }
  }

  final class SequenceKeyGenerator extends QueriedKeyGenerator {

    private final String sequenceName;

    SequenceKeyGenerator(final String sequenceName) {
      this.sequenceName = sequenceName;
    }

    @Override
    public Type getType() {
      return Type.SEQUENCE;
    }

    @Override
    public void beforeInsert(final Entity entity, final DatabaseConnection connection) throws SQLException {
      final Property.ColumnProperty primaryKeyProperty = getPrimaryKeyProperty(entity.getEntityId());
      if (entity.isValueNull(primaryKeyProperty)) {
        queryAndSet(entity, primaryKeyProperty, connection);
      }
    }

    @Override
    protected String getQuery(final Database database) {
      return database.getSequenceQuery(sequenceName);
    }
  }

  final class AutomaticKeyGenerator extends QueriedKeyGenerator {

    private final String valueSource;

    AutomaticKeyGenerator(final String valueSource) {
      this.valueSource = valueSource;
    }

    @Override
    public Type getType() {
      return Type.AUTOMATIC;
    }

    @Override
    public void afterInsert(final Entity entity, final DatabaseConnection connection, final Statement statement) throws SQLException {
      queryAndSet(entity, getPrimaryKeyProperty(entity.getEntityId()), connection);
    }

    @Override
    protected String getQuery(final Database database) {
      return database.getAutoIncrementQuery(valueSource);
    }
  }

  private static final class DefaultOrderBy implements Entity.OrderBy {

    private static final long serialVersionUID = 1;

    private final HashMap<String, SortOrder> propertySortOrder = new LinkedHashMap<>();

    @Override
    public Entity.OrderBy ascending(final String... propertyIds) {
      put(SortOrder.ASCENDING, propertyIds);
      return this;
    }

    @Override
    public Entity.OrderBy descending(final String... propertyIds) {
      put(SortOrder.DESCENDING, propertyIds);
      return this;
    }

    @Override
    public Map<String, SortOrder> getSortOrder() {
      return propertySortOrder;
    }

    private void put(final SortOrder sortOrder, final String... propertyIds) {
      Objects.requireNonNull(propertyIds, "propertyIds");
      for (final String propertyId : propertyIds) {
        if (propertySortOrder.containsKey(propertyId)) {
          throw new IllegalArgumentException("Order by already contains property: " + propertyId);
        }
        propertySortOrder.put(propertyId, sortOrder);
      }
    }
  }
}
