/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.Configuration;
import org.jminor.common.Serializer;
import org.jminor.common.Util;
import org.jminor.common.Value;
import org.jminor.common.db.Database;
import org.jminor.common.db.DatabaseConnection;
import org.jminor.common.db.Databases;
import org.jminor.common.db.ResultPacker;
import org.jminor.common.db.exception.RecordModifiedException;
import org.jminor.common.db.valuemap.DefaultValueMap;
import org.jminor.common.db.valuemap.ValueProvider;
import org.jminor.common.db.valuemap.exception.NullValidationException;
import org.jminor.common.db.valuemap.exception.RangeValidationException;
import org.jminor.common.db.valuemap.exception.ValidationException;
import org.jminor.framework.i18n.FrameworkMessages;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.text.Collator;
import java.text.Format;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A {@link Entity} repository specifying the {@link Entity.Definition}s for a given domain.
 */
public class Entities {

  /**
   * Specifies whether or not to allow entities to be re-defined, that is,
   * allow a new definition to replace an old one.
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final Value<Boolean> ALLOW_REDEFINE_ENTITY = Configuration.booleanValue("jminor.domain.allowRedefineEntity", false);

  /**
   * Specifies whether the client layer should perform null validation on entities
   * before update/insert actions are performed<br>
   * Value type: Boolean<br>
   * Default value: true
   * @see Property#setNullable(boolean)
   */
  public static final Value<Boolean> PERFORM_NULL_VALIDATION = Configuration.booleanValue("jminor.client.performNullValidation", true);

  /**
   * Specifies the class used for serializing and deserializing entity instances.<br>
   * Value type: String, the name of the class implementing org.jminor.common.Serializer&#60;Entity&#62;<br>
   * Default value: none
   */
  public static final Value<String> ENTITY_SERIALIZER_CLASS = Configuration.stringValue("jminor.domain.entitySerializerClass", null);

  private static final String ENTITY_PARAM = "entity";
  private static final String ENTITY_ID_PARAM = "entityID";
  private static final String ENTITIES_PARAM = "entities";
  private static final String PROPERTY_ID_PARAM = "propertyID";

  private static final Map<String, Entities> DOMAIN_ENTITIES = new HashMap<>();

  private final Map<String, Entity.Definition> entityDefinitions = new LinkedHashMap<>();
  private final Map<String, List<Property.ForeignKeyProperty>> foreignKeyReferenceMap = new HashMap<>();
  private final Map<String, DatabaseConnection.Operation> databaseOperations = new HashMap<>();

  /**
   * Creates a new {@link Entity} instance with the given entityID
   * @param entityID the entity ID
   * @return a new {@link Entity} instance
   */
  public final Entity entity(final String entityID) {
    return new DefaultEntity(this, entityID);
  }

  /**
   * Creates a new {@link Entity} instance with the given primary key
   * @param key the primary key
   * @return a new {@link Entity} instance
   */
  public final Entity entity(final Entity.Key key) {
    return new DefaultEntity(this, key.getEntityID(), Objects.requireNonNull(key, "key"));
  }

  /**
   * Instantiates a new {@link Entity} instance using the given maps for the values and original values respectively.
   * Note that the given map instances are used internally, modifying the contents of those maps outside this
   * {@link Entity} instance will definitely result in some unexpected and unpleasant behaviour.
   * @param entityID the entity ID
   * @param values the values
   * @param originalValues the original values
   * @return a new {@link Entity} instance
   */
  public final Entity entity(final String entityID, final Map<Property, Object> values, final Map<Property, Object> originalValues) {
    return new DefaultEntity(this, entityID, values, originalValues);
  }

  /**
   * Creates a new {@link Entity.Key} instance with the given entityID
   * @param entityID the entity ID
   * @return a new {@link Entity.Key} instance
   */
  public final Entity.Key key(final String entityID) {
    return new DefaultEntity.DefaultKey(this, getDefinition(entityID), null);
  }

  /**
   * Defines a new entity, by default the {@code entityID} is used as the underlying table name
   * @param entityID the ID uniquely identifying the entity
   * @param properties the {@link Property} objects to base this entity on. In case a select query is specified
   * for this entity, the property order must match the select column order.
   * @return a new {@link Entity.Definition}
   * @throws IllegalArgumentException in case the entityID has already been used to define an entity type or if
   * no primary key property is specified
   */
  public final Entity.Definition define(final String entityID, final Property... properties) {
    return define(entityID, entityID, properties);
  }

  /**
   * Defines a new entity
   * @param entityID the ID uniquely identifying the entity
   * @param tableName the name of the underlying table
   * @param properties the {@link Property} objects to base the entity on. In case a select query is specified
   * for this entity, the property order must match the select column order.
   * @return a new {@link Entity.Definition}
   * @throws IllegalArgumentException in case the entityID has already been used to define an entity type or if
   * no primary key property is specified
   */
  public final Entity.Definition define(final String entityID, final String tableName, final Property... properties) {
    Objects.requireNonNull(entityID, ENTITY_ID_PARAM);
    Objects.requireNonNull(tableName, "tableName");
    if (entityDefinitions.containsKey(entityID) && !ALLOW_REDEFINE_ENTITY.get()) {
      throw new IllegalArgumentException("Entity has already been defined: " + entityID + ", for table: " + tableName);
    }
    final String domainID = getClass().getName();
    final Map<String, Property> propertyMap = initializeProperties(domainID, entityID, properties);
    final List<Property.ColumnProperty> columnProperties = Collections.unmodifiableList(getColumnProperties(propertyMap.values()));
    final List<Property.ColumnProperty> primaryKeyProperties = Collections.unmodifiableList(getPrimaryKeyProperties(propertyMap.values()));
    final List<Property> visibleProperties = Collections.unmodifiableList(getVisibleProperties(propertyMap.values()));
    final List<Property.TransientProperty> transientProperties = Collections.unmodifiableList(getTransientProperties(propertyMap.values()));
    final List<Property.ForeignKeyProperty> foreignKeyProperties = Collections.unmodifiableList(getForeignKeyProperties(propertyMap.values()));
    final Map<String, List<Property.DenormalizedProperty>> denormalizedProperties =
            Collections.unmodifiableMap(getDenormalizedProperties(propertyMap.values()));
    final Map<String, Set<Property.DerivedProperty>> derivedProperties = initializeDerivedProperties(propertyMap.values());
    final String selectColumnsString = initializeSelectColumnsString(columnProperties);

    final EntityResultPacker resultPacker = new EntityResultPacker(this, entityID,
            columnProperties, transientProperties, propertyMap.size());
    final String groupByClause = initializeGroupByClause(columnProperties);
    final DefaultEntityDefinition entityDefinition = new DefaultEntityDefinition(domainID, entityID,
            tableName, propertyMap, resultPacker,
            derivedProperties, primaryKeyProperties, foreignKeyProperties, transientProperties, visibleProperties,
            columnProperties, denormalizedProperties, selectColumnsString, groupByClause);
    entityDefinition.setValidator(new Validator(this, entityID));
    entityDefinitions.put(entityID, entityDefinition);

    return entityDefinition;
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
   * @param query the query
   * @return a query based primary key generator
   */
  public final Entity.KeyGenerator queriedKeyGenerator(final String query) {
    return queriedKeyGenerator(query);
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
   * @param entityID the entity ID
   * @return a String array containing the IDs of the properties used as default search properties
   * for entities identified by {@code entityID}
   * @see Entity.Definition#setSearchPropertyIDs(String...)
   */
  public final Collection<String> getSearchPropertyIDs(final String entityID) {
    return getDefinition(entityID).getSearchPropertyIDs();
  }

  /**
   * Retrieves the properties used when searching for a entity of the given type,
   * if no search property IDs are defined all STRING based properties are returned.
   * @param entityID the entity ID
   * @return the search properties to use
   * @see Entity.Definition#setSearchPropertyIDs(String...)
   */
  public final Collection<Property.ColumnProperty> getSearchProperties(final String entityID) {
    final Collection<String> searchPropertyIDs = getSearchPropertyIDs(entityID);
    return getSearchProperties(entityID, searchPropertyIDs.toArray(new String[searchPropertyIDs.size()]));
  }

  /**
   * Retrieves the properties used when searching for a entity of the given type,
   * @param entityID the entity ID
   * @param searchPropertyIds the IDs of the search properties to retrieve
   * @return the search properties to use
   * @see Entity.Definition#setSearchPropertyIDs(String...)
   */
  public final Collection<Property.ColumnProperty> getSearchProperties(final String entityID, final String... searchPropertyIds) {
    if (searchPropertyIds != null && searchPropertyIds.length > 0) {
      final List<Property.ColumnProperty> searchProperties = new ArrayList<>();
      for (final String propertyID : searchPropertyIds) {
        searchProperties.add(getColumnProperty(entityID, propertyID));
      }

      return searchProperties;
    }

    return Collections.emptyList();
  }

  /**
   * @param entityID the entity ID
   * @return a list containing the primary key properties of the entity identified by {@code entityID}
   */
  public final List<Property.ColumnProperty> getPrimaryKeyProperties(final String entityID) {
    return getDefinition(entityID).getPrimaryKeyProperties();
  }

  /**
   * @param entityID the entity ID
   * @return true if the entity identified by {@code entityID} is read only
   * @throws IllegalArgumentException if the entity is undefined
   */
  public final boolean isReadOnly(final String entityID) {
    return getDefinition(entityID).isReadOnly();
  }

  /**
   * @param entityID the entity ID
   * @return true if the entity identified by {@code entityID} is based on a small dataset
   * @throws IllegalArgumentException if the entity is undefined
   */
  public final boolean isSmallDataset(final String entityID) {
    return getDefinition(entityID).isSmallDataset();
  }

  /**
   * @param entityID the entity ID
   * @return true if the entity identified by {@code entityID} is based on static data
   * @throws IllegalArgumentException if the entity is undefined
   */
  public final boolean isStaticData(final String entityID) {
    return getDefinition(entityID).isStaticData();
  }

  /**
   * @param entityID the entity ID
   * @return a comma separated list of columns to use in the order by clause
   */
  public final String getOrderByClause(final String entityID) {
    return getDefinition(entityID).getOrderByClause();
  }

  /**
   * @param entityID the entity ID
   * @return a comma separated list of columns to use in the group by clause
   */
  public final String getGroupByClause(final String entityID) {
    return getDefinition(entityID).getGroupByClause();
  }

  /**
   * @param entityID the entity ID
   * @return the having clause associated with this entity
   */
  public final String getHavingClause(final String entityID) {
    return getDefinition(entityID).getHavingClause();
  }

  /**
   * @param entityID the entity ID
   * @return the name of the table used to select entities identified by {@code entityID}
   * @throws IllegalArgumentException if the entity is undefined
   */
  public final String getSelectTableName(final String entityID) {
    return getDefinition(entityID).getSelectTableName();
  }

  /**
   * @param entityID the entity ID
   * @return the name of the table on which entities identified by {@code entityID} are based
   * @throws IllegalArgumentException if the entity is undefined
   */
  public final String getTableName(final String entityID) {
    return getDefinition(entityID).getTableName();
  }

  /**
   * @param entityID the entity ID
   * @return the sql query used when selecting entities identified by {@code entityID}
   * @throws IllegalArgumentException if the entity is undefined
   */
  public final String getSelectQuery(final String entityID) {
    return getDefinition(entityID).getSelectQuery();
  }

  /**
   * @param entityID the entity ID
   * @return true if the select query for the given entity, if any, contains a where clause
   */
  public final boolean selectQueryContainsWhereClause(final String entityID) {
    return getDefinition(entityID).selectQueryContainsWhereClause();
  }

  /**
   * @param entityID the entity ID
   * @return the query string used to select entities identified by {@code entityID}
   * @throws IllegalArgumentException if the entity is undefined
   */
  public final String getSelectColumnsString(final String entityID) {
    return getDefinition(entityID).getSelectColumnsString();
  }

  /**
   * @param entityID the entity ID
   * @return the primary key generator for entities identified by {@code entityID}
   * @throws IllegalArgumentException if the entity is undefined
   */
  public final Entity.KeyGenerator getKeyGenerator(final String entityID) {
    return getDefinition(entityID).getKeyGenerator();
  }

  /**
   * @param entityID the entity ID
   * @return the type of primary key generator used by entities identified by {@code entityID}
   * @throws IllegalArgumentException if the entity is undefined
   */
  public final Entity.KeyGenerator.Type getKeyGeneratorType(final String entityID) {
    return getDefinition(entityID).getKeyGeneratorType();
  }

  /**
   * @param entityID the entity ID
   * @return the {@link Entity.ToString} instance used to provide string representations
   * of entities of the given type
   * @throws IllegalArgumentException if the entity is undefined
   */
  public final Entity.ToString getStringProvider(final String entityID) {
    return getDefinition(entityID).getStringProvider();
  }

  /**
   * @param entityID the entity ID
   * @return the default Comparator to use when sorting entities of the given type
   */
  public final Comparator<Entity> getComparator(final String entityID) {
    return getDefinition(entityID).getComparator();
  }

  /**
   * Returns true if the value for the primary key of this entity is automatically generated, either by the framework,
   * such as values queried from sequences or set by triggers. If not the primary key value must be set manually
   * before the entity is inserted.
   * @param entityID the entity ID
   * @return true if the value for the primary key is automatically generated
   */
  public final boolean isPrimaryKeyAutoGenerated(final String entityID) {
    return !getKeyGeneratorType(entityID).isManual();
  }

  /**
   * @param entityID the entity ID
   * @return true if the primary key of the given type of entity is comprised of a single integer value
   */
  public final boolean hasSingleIntegerPrimaryKey(final String entityID) {
    final List<Property.ColumnProperty> primaryKeyProperties = getDefinition(entityID).getPrimaryKeyProperties();
    return primaryKeyProperties.size() == 1 && primaryKeyProperties.get(0).isInteger();
  }

  /**
   * Retrieves the column properties comprising the entity identified by {@code entityID}
   * @param entityID the entity ID
   * @param includePrimaryKeyProperties if true primary key properties are included
   * @param includeReadOnly if true then properties that are marked as 'read only' are included
   * @param includeNonUpdatable if true then non updatable properties are included
   * @return a list containing the database properties (properties that map to database columns) comprising
   * the entity identified by {@code entityID}
   */
  public final List<Property.ColumnProperty> getColumnProperties(final String entityID,
                                                                 final boolean includePrimaryKeyProperties,
                                                                 final boolean includeReadOnly,
                                                                 final boolean includeNonUpdatable) {
    final List<Property.ColumnProperty> properties = new ArrayList<>(getDefinition(entityID).getColumnProperties());
    properties.removeIf(property ->
            !includeReadOnly && property.isReadOnly()
                    || !includeNonUpdatable && !property.isUpdatable()
                    || !includePrimaryKeyProperties && property.isPrimaryKeyProperty());

    return properties;
  }

  /**
   * @param entityID the entity ID
   * @return a list containing the visible (non-hidden) properties
   * in the entity identified by {@code entityID}
   */
  public final List<Property> getVisibleProperties(final String entityID) {
    Objects.requireNonNull(entityID, ENTITY_ID_PARAM);
    return getDefinition(entityID).getVisibleProperties();
  }

  /**
   * @param entityID the entityID
   * @param propertyIDs the IDs of the properties to retrieve
   * @return the {@link Property.ColumnProperty}s specified by the given property IDs
   * @throws IllegalArgumentException in case a given propertyID does not represent a {@link Property.ColumnProperty}
   */
  public final List<Property.ColumnProperty> getColumnProperties(final String entityID, final Collection<String> propertyIDs) {
    if (propertyIDs == null || propertyIDs.isEmpty()) {
      return Collections.emptyList();
    }

    final List<Property.ColumnProperty> columnProperties = new ArrayList<>();
    for (final String propertyID : propertyIDs) {
      columnProperties.add(getColumnProperty(entityID, propertyID));
    }

    return columnProperties;
  }

  /**
   * @param entityID the entity ID
   * @param propertyID the property ID
   * @return the column property identified by property ID
   * @throws IllegalArgumentException in case the propertyID does not represent a {@link Property.ColumnProperty}
   */
  public final Property.ColumnProperty getColumnProperty(final String entityID, final String propertyID) {
    final Property property = getProperty(entityID, propertyID);
    if (!(property instanceof Property.ColumnProperty)) {
      throw new IllegalArgumentException(propertyID + ", " + property.getClass() + " does not implement Property.ColumnProperty");
    }

    return (Property.ColumnProperty) property;
  }

  /**
   * @param entityID the entity ID
   * @param propertyID the property ID
   * @return the property identified by {@code propertyID} in the entity identified by {@code entityID}
   * @throws IllegalArgumentException in case no such property exists
   */
  public final Property getProperty(final String entityID, final String propertyID) {
    Objects.requireNonNull(entityID, ENTITY_ID_PARAM);
    Objects.requireNonNull(propertyID, PROPERTY_ID_PARAM);
    final Property property = getDefinition(entityID).getPropertyMap().get(propertyID);
    if (property == null) {
      throw new IllegalArgumentException("Property '" + propertyID + "' not found in entity: " + entityID);
    }

    return property;
  }

  /**
   * @param entityID the entity ID
   * @param propertyIDs the IDs of the properties to retrieve
   * @return a list containing the properties identified by {@code propertyIDs}, found in
   * the entity identified by {@code entityID}
   */
  public final List<Property> getProperties(final String entityID, final Collection<String> propertyIDs) {
    Objects.requireNonNull(propertyIDs, PROPERTY_ID_PARAM);
    return getProperties(entityID, propertyIDs.toArray(new String[propertyIDs.size()]));
  }

  /**
   * @param entityID the entity ID
   * @param propertyIDs the IDs of the properties to retrieve
   * @return a list containing the properties identified by {@code propertyIDs}, found in
   * the entity identified by {@code entityID}
   */
  public final List<Property> getProperties(final String entityID, final String... propertyIDs) {
    Objects.requireNonNull(entityID, ENTITY_ID_PARAM);
    Objects.requireNonNull(propertyIDs, PROPERTY_ID_PARAM);
    final List<Property> properties = new ArrayList<>();
    for (final String propertyID : propertyIDs) {
      properties.add(getProperty(entityID, propertyID));
    }

    return properties;
  }

  /**
   * @param entityID the entity ID
   * @param includeHidden true if hidden properties should be included in the result
   * @return a collection containing the properties found in the entity identified by {@code entityID}
   */
  public final Collection<Property> getProperties(final String entityID, final boolean includeHidden) {
    return includeHidden ? getProperties(entityID) : getVisibleProperties(entityID);
  }

  /**
   * @param entityID the entity ID
   * @return a list containing all database properties found in the entity identified by {@code entityID},
   * that is, properties that map to database columns
   */
  public final List<Property.ColumnProperty> getColumnProperties(final String entityID) {
    return getDefinition(entityID).getColumnProperties();
  }

  /**
   * @param entityID the entity ID
   * @return a list containing all transient database properties found in the entity identified by {@code entityID},
   * that is, properties that do not map to database columns
   */
  public final List<Property.TransientProperty> getTransientProperties(final String entityID) {
    return getDefinition(entityID).getTransientProperties();
  }

  /**
   * @param entityID the entity ID
   * @return a list containing all the foreign key properties found in the entity
   * identified by {@code entityID}
   */
  public final List<Property.ForeignKeyProperty> getForeignKeyProperties(final String entityID) {
    return getDefinition(entityID).getForeignKeyProperties();
  }

  /**
   * @param entityID the entity ID
   * @return true if the given entity contains denormalized properties
   */
  public final boolean hasDenormalizedProperties(final String entityID) {
    return getDefinition(entityID).hasDenormalizedProperties();
  }

  /**
   * @param entityID the entity ID
   * @param foreignKeyPropertyID the foreign key id
   * @return a list containing all denormalized properties of the entity identified by {@code entityID}
   * which source is the entity identified by {@code propertyOwnerEntityID}
   */
  public final List<Property.DenormalizedProperty> getDenormalizedProperties(final String entityID,
                                                                             final String foreignKeyPropertyID) {
    return getDefinition(entityID).getDenormalizedProperties(foreignKeyPropertyID);
  }

  /**
   * @param entityID the entity ID
   * @param foreignKeyPropertyID the foreign key id
   * @return true if the entity identified by {@code entityID} contains denormalized properties
   * which source is the entity identified by {@code propertyOwnerEntityID}
   */
  public final boolean hasDenormalizedProperties(final String entityID, final String foreignKeyPropertyID) {
    return getDefinition(entityID).hasDenormalizedProperties(foreignKeyPropertyID);
  }

  /**
   * Returns true if this entity contains properties which values are derived from the value of the given property
   * @param entityID the entityID
   * @param propertyID the ID of the property
   * @return true if any properties are derived from the given property
   */
  public final boolean hasDerivedProperties(final String entityID, final String propertyID) {
    return getDefinition(entityID).hasDerivedProperties(propertyID);
  }

  /**
   * Returns the properties which values are derived from the value of the given property,
   * an empty collection if no such derived properties exist
   * @param entityID the entityID
   * @param propertyID the ID of the property
   * @return a collection containing the properties which are derived from the given property
   */
  public final Collection<Property.DerivedProperty> getDerivedProperties(final String entityID, final String propertyID) {
    return getDefinition(entityID).getDerivedProperties(propertyID);
  }

  /**
   * Returns the foreign key properties referencing entities of the given type
   * @param entityID the ID of the entity from which to retrieve the foreign key properties
   * @param foreignEntityID the ID of the referenced entity
   * @return a List containing the properties, an empty list is returned in case no properties fit the condition
   */
  public final List<Property.ForeignKeyProperty> getForeignKeyProperties(final String entityID, final String foreignEntityID) {
    final List<Property.ForeignKeyProperty> properties = new ArrayList<>();
    for (final Property.ForeignKeyProperty foreignKeyProperty : getForeignKeyProperties(entityID)) {
      if (foreignKeyProperty.getForeignEntityID().equals(foreignEntityID)) {
        properties.add(foreignKeyProperty);
      }
    }

    return properties;
  }

  /**
   * @param entityID the entity ID
   * @param propertyID the property ID
   * @return the Property.ForeignKeyProperty with the given propertyID
   * @throws IllegalArgumentException in case no such property exists
   */
  public final Property.ForeignKeyProperty getForeignKeyProperty(final String entityID, final String propertyID) {
    final List<Property.ForeignKeyProperty> foreignKeyProperties = getForeignKeyProperties(entityID);
    for (int i = 0; i < foreignKeyProperties.size(); i++) {
      final Property.ForeignKeyProperty foreignKeyProperty = foreignKeyProperties.get(i);
      if (foreignKeyProperty.is(propertyID)) {
        return foreignKeyProperty;
      }
    }

    throw new IllegalArgumentException("Foreign key property with id: " + propertyID + " not found in entity of type: " + entityID);
  }

  /**
   * @param entityID the entityID
   * @return all foreign keys referencing entities of type {@code entityID}
   */
  public final Collection<Property.ForeignKeyProperty> getForeignKeyReferences(final String entityID) {
    List<Property.ForeignKeyProperty> foreignKeyReferences = foreignKeyReferenceMap.get(entityID);
    if (foreignKeyReferences == null) {
      foreignKeyReferences = new ArrayList<>();
      for (final String definedEntityID : entityDefinitions.keySet()) {
        for (final Property.ForeignKeyProperty foreignKeyProperty : getDefinition(definedEntityID).getForeignKeyProperties()) {
          if (foreignKeyProperty.getForeignEntityID().equals(entityID)) {
            foreignKeyReferences.add(foreignKeyProperty);
          }
        }
      }
      foreignKeyReferenceMap.put(entityID, foreignKeyReferences);
    }

    return foreignKeyReferences;
  }

  /**
   * @param entityID the entity ID
   * @return the properties comprising the given entity type
   */
  public final List<Property> getProperties(final String entityID) {
    return getDefinition(entityID).getProperties();
  }

  /**
   * @param entityID the entity ID
   * @return the caption associated with the given entity type
   */
  public final String getCaption(final String entityID) {
    return getDefinition(entityID).getCaption();
  }

  /**
   * @param entityID the entityID
   * @return the validator for the given entity type
   */
  public final Entity.Validator getValidator(final String entityID) {
    return getDefinition(entityID).getValidator();
  }

  /**
   * @param entityID the entityID
   * @return the ResultPacker responsible for packing this entity type
   */
  public final ResultPacker<Entity> getResultPacker(final String entityID) {
    return getDefinition(entityID).getResultPacker();
  }

  /**
   * @return the entityIDs of all defined entities
   */
  public final Collection<String> getDefinedEntities() {
    return new ArrayList<>(entityDefinitions.keySet());
  }

  /**
   * @param entityID the entity ID
   * @return true if the entity is defined
   */
  public final boolean isDefined(final String entityID) {
    return entityDefinitions.containsKey(entityID);
  }

  /**
   * @param foreignKeyProperty the foreign key property
   * @return the primary key properties of the referenced entity type
   */
  public final List<Property.ColumnProperty> getForeignProperties(final Property.ForeignKeyProperty foreignKeyProperty) {
    return getPrimaryKeyProperties(foreignKeyProperty.getForeignEntityID());
  }

  /**
   * Populates an entity of the given type using the values provided by {@code valueProvider}.
   * Values are fetched for {@link Property.ColumnProperty} and its descendants, {@link Property.TransientProperty}
   * excluding its descendants and {@link Property.ForeignKeyProperty}.
   * @param entityID the entity ID
   * @param valueProvider the value provider
   * @return the populated entity
   */
  public final Entity getEntity(final String entityID, final ValueProvider<Property, Object> valueProvider) {
    final Entity entity = entity(entityID);
    final Collection<Property.ColumnProperty> columnProperties = getColumnProperties(entityID);
    for (final Property.ColumnProperty property : columnProperties) {
      if (!property.isForeignKeyProperty() && !property.isDenormalized()) {//these are set via their respective parent properties
        entity.put(property, valueProvider.get(property));
      }
    }
    final Collection<Property.TransientProperty> transientProperties = getTransientProperties(entityID);
    for (final Property.TransientProperty transientProperty : transientProperties) {
      if (!(transientProperty instanceof Property.DerivedProperty) && !(transientProperty instanceof Property.DenormalizedViewProperty)) {
        entity.put(transientProperty, valueProvider.get(transientProperty));
      }
    }
    final Collection<Property.ForeignKeyProperty> foreignKeyProperties = getForeignKeyProperties(entityID);
    for (final Property.ForeignKeyProperty foreignKeyProperty : foreignKeyProperties) {
      entity.put(foreignKeyProperty, valueProvider.get(foreignKeyProperty));
    }
    entity.saveAll();

    return entity;
  }

  /**
   * @param entityID the entity ID
   * @return a list containing all updatable properties associated with the given entity ID
   */
  public final List<Property> getUpdatableProperties(final String entityID) {
    final List<Property.ColumnProperty> columnProperties = getColumnProperties(entityID,
            getKeyGeneratorType(entityID).isManual(), false, false);
    columnProperties.removeIf(property -> property.isForeignKeyProperty() || property.isDenormalized());
    final List<Property> updatable = new ArrayList<>(columnProperties);
    final Collection<Property.ForeignKeyProperty> foreignKeyProperties = getForeignKeyProperties(entityID);
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
    for (final Entity entity : entities) {
      if (entity != null) {
        for (final Property.ColumnProperty property : getPrimaryKeyProperties(entity.getEntityID())) {
          if (entity.isModified(property)) {
            return true;
          }
        }
      }
    }

    return false;
  }

  /**
   * Creates an empty Entity instance returning the given string on a call to toString(), all other
   * method calls are routed to an empty Entity instance.
   * @param entityID the entityID
   * @param toStringValue the string to return by a call to toString() on the resulting entity
   * @return an empty entity wrapping a string
   */
  public final Entity createToStringEntity(final String entityID, final String toStringValue) {
    final Entity entity = entity(entityID);
    return Util.initializeProxy(Entity.class, (proxy, method, args) -> {
      if ("toString".equals(method.getName())) {
        return toStringValue;
      }

      return method.invoke(entity, args);
    });
  }

  /**
   * @param entity the entity instance to check
   * @param comparison the entity instance to compare with
   * @return the first property which value is missing or the original value differs from the one in the comparison
   * entity, returns null if all of {@code entity}s original values match the values found in {@code comparison}
   */
  public final Property getModifiedProperty(final Entity entity, final Entity comparison) {
    for (final Property property : comparison.keySet()) {
      //BLOB property values are not loaded, so we can't compare those
      if (!property.isType(Types.BLOB) && isValueMissingOrModified(entity, comparison, property.getPropertyID())) {
        return property;
      }
    }

    return null;
  }

  /**
   * @param exception the record modified exception
   * @return a human-readable String describing the modification
   */
  public final String getModifiedExceptionMessage(final RecordModifiedException exception) {
    final Entity entity = (Entity) exception.getRow();
    final Entity modified = (Entity) exception.getModifiedRow();
    if (modified == null) {//record has been deleted
      return entity + " " + FrameworkMessages.get(FrameworkMessages.HAS_BEEN_DELETED);
    }
    final Property modifiedProperty = getModifiedProperty(entity, modified);

    return getCaption(entity.getEntityID()) + ", " + modifiedProperty + ": " +
            entity.getOriginal(modifiedProperty) + " -> " + modified.get(modifiedProperty);
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
   * @param entityID the entity ID
   * @param propertyIDs the property IDs
   * @return the given properties sorted by caption, or if that is not available, property ID
   */
  public final List<Property> getSortedProperties(final String entityID, final Collection<String> propertyIDs) {
    final List<Property> properties = new ArrayList<>(getProperties(entityID, propertyIDs));
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
    if (databaseOperations.containsKey(operation.getID())) {
      throw new IllegalArgumentException("Operation already defined: " + databaseOperations.get(operation.getID()).getName());
    }

    databaseOperations.put(operation.getID(), operation);
  }

  /**
   * @param <C> the type of the database connection this procedure requires
   * @param procedureID the procedure ID
   * @return the procedure
   * @throws IllegalArgumentException in case the procedure is not found
   */
  public final <C> DatabaseConnection.Procedure<C> getProcedure(final String procedureID) {
    final DatabaseConnection.Operation operation = databaseOperations.get(procedureID);
    if (operation == null) {
      throw new IllegalArgumentException("Procedure not found: " + procedureID);
    }

    return (DatabaseConnection.Procedure) operation;
  }

  /**
   * @param <C> the type of the database connection this function requires
   * @param functionID the function ID
   * @return the function
   * @throws IllegalArgumentException in case the function is not found
   */
  public final <C> DatabaseConnection.Function<C> getFunction(final String functionID) {
    final DatabaseConnection.Operation operation = databaseOperations.get(functionID);
    if (operation == null) {
      throw new IllegalArgumentException("Function not found: " + functionID);
    }

    return (DatabaseConnection.Function) operation;
  }

  /**
   * Registers this instance for lookup via {@link Entities#getDomainEntities(String)}
   * @return this Entities instance
   */
  public final Entities registerDomain() {
    return setDomainEntities(getClass().getName(), this);
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
    final List<Entity> modifiedEntities = new ArrayList<>();
    for (final Entity entity : entities) {
      if (entity.isModified()) {
        modifiedEntities.add(entity);
      }
    }

    return modifiedEntities;
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
    final List<Entity.Key> keys = new ArrayList<>(entities.size());
    for (final Entity entity : entities) {
      keys.add(originalValue ? entity.getOriginalKey() : entity.getKey());
    }

    return keys;
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
   * @param propertyID the ID of the property for which to retrieve the values
   * @param entities the entities from which to retrieve the property value
   * @return a Collection containing the values of the property with the given ID from the given entities,
   * null values are included
   */
  public static <T> Collection<T> getValues(final String propertyID, final Collection<Entity> entities) {
    return getValues(propertyID, entities, true);
  }

  /**
   * @param <T> the value type
   * @param propertyID the ID of the property for which to retrieve the values
   * @param entities the entities from which to retrieve the property value
   * @param includeNullValues if true then null values are included
   * @return a Collection containing the values of the property with the given ID from the given entities
   */
  public static <T> Collection<T> getValues(final String propertyID, final Collection<Entity> entities,
                                            final boolean includeNullValues) {
    return collectValues(new ArrayList<T>(entities == null ? 0 : entities.size()), propertyID, entities, includeNullValues);
  }

  /**
   * Returns a Collection containing the distinct values of {@code propertyID} from the given entities, excluding null values.
   * If the {@code entities} list is null an empty Collection is returned.
   * @param <T> the value type
   * @param propertyID the ID of the property for which to retrieve the values
   * @param entities the entities from which to retrieve the values
   * @return a Collection containing the distinct property values, excluding null values
   */
  public static <T> Collection<T> getDistinctValues(final String propertyID, final Collection<Entity> entities) {
    return getDistinctValues(propertyID, entities, false);
  }

  /**
   * Returns a Collection containing the distinct values of {@code propertyID} from the given entities.
   * If the {@code entities} list is null an empty Collection is returned.
   * @param <T> the value type
   * @param propertyID the ID of the property for which to retrieve the values
   * @param entities the entities from which to retrieve the values
   * @param includeNullValue if true then null is considered a value
   * @return a Collection containing the distinct property values
   */
  public static <T> Collection<T> getDistinctValues(final String propertyID, final Collection<Entity> entities,
                                                    final boolean includeNullValue) {
    return collectValues(new HashSet<T>(), propertyID, entities, includeNullValue);
  }

  /**
   * Sets the value of the property with ID {@code propertyID} to {@code value}
   * in the given entities
   * @param propertyID the ID of the property for which to set the value
   * @param value the value
   * @param entities the entities for which to set the value
   * @return the old property values mapped to their respective primary key
   */
  public static Map<Entity.Key, Object> put(final String propertyID, final Object value,
                                            final Collection<Entity> entities) {
    Objects.requireNonNull(entities, ENTITIES_PARAM);
    final Map<Entity.Key, Object> oldValues = new HashMap<>(entities.size());
    for (final Entity entity : entities) {
      oldValues.put(entity.getKey(), entity.put(propertyID, value));
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
   * Returns a LinkedHashMap containing the given entities mapped to the value of the property with ID {@code propertyID},
   * respecting the iteration order of the given collection
   * @param <K> the key type
   * @param propertyID the ID of the property which value should be used for mapping
   * @param entities the entities to map by property value
   * @return a Map of entities mapped to property value
   */
  public static <K> LinkedHashMap<K, Collection<Entity>> mapToValue(final String propertyID, final Collection<Entity> entities) {
    return Util.map(entities, value -> (K) value.get(propertyID));
  }

  /**
   * Returns a LinkedHashMap containing the given entities mapped to their entityIDs,
   * respecting the iteration order of the given collection
   * @param entities the entities to map by entityID
   * @return a Map of entities mapped to entityID
   */
  public static LinkedHashMap<String, Collection<Entity>> mapToEntityID(final Collection<Entity> entities) {
    return Util.map(entities, Entity::getEntityID);
  }

  /**
   * Returns a LinkedHashMap containing the given entity keys mapped to their entityIDs,
   * respecting the iteration order of the given collection
   * @param keys the entity keys to map by entityID
   * @return a Map of entity keys mapped to entityID
   */
  public static LinkedHashMap<String, Collection<Entity.Key>> mapKeysToEntityID(final Collection<Entity.Key> keys) {
    return Util.map(keys, Entity.Key::getEntityID);
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

  private static <T> Collection<T> collectValues(final Collection<T> collection, final String propertyID,
                                                 final Collection<Entity> entities, final boolean includeNullValues) {
    Objects.requireNonNull(collection);
    Objects.requireNonNull(propertyID);
    if (!Util.nullOrEmpty(entities)) {
      for (final Entity entity : entities) {
        final Object value = entity.get(propertyID);
        if (value != null || includeNullValues) {
          collection.add((T) value);
        }
      }
    }

    return collection;
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

      data[i] = line.toArray(new String[line.size()]);
    }

    return data;
  }

  /**
   * @param entities the entities to copy
   * @return deep copies of the entities, in the same order as they are received
   */
  public static List<Entity> copyEntities(final List<Entity> entities) {
    Objects.requireNonNull(entities, ENTITIES_PARAM);
    final List<Entity> copies = new ArrayList<>(entities.size());
    for (final Entity entity : entities) {
      copies.add((Entity) entity.getCopy());
    }

    return copies;
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
   * @param domainID the ID of the domain for which to retrieve the entity definitions
   * @return the Entities instance registered for the given domainID
   * @throws IllegalArgumentException in case the domain has not been registered
   * @see #registerDomain()
   */
  public static Entities getDomainEntities(final String domainID) {
    final Entities entities = DOMAIN_ENTITIES.get(domainID);
    if (entities == null) {
      throw new IllegalArgumentException("Domain '" + domainID + "' has not been registered");
    }

    return entities;
  }

  /**
   * @return all domains that have been registered via {@link #registerDomain()}
   */
  public static Collection<Entities> getAllDomains() {
    return Collections.unmodifiableCollection(DOMAIN_ENTITIES.values());
  }

  /**
   * @param entity the entity instance to check
   * @param comparison the entity instance to compare with
   * @param propertyID the property to check
   * @return true if the value is missing or the original value differs from the one in the comparison entity
   */
  static boolean isValueMissingOrModified(final Entity entity, final Entity comparison, final String propertyID) {
    return !entity.containsKey(propertyID) || !Objects.equals(comparison.get(propertyID), entity.getOriginal(propertyID));
  }

  Entity.Definition getDefinition(final String entityID) {
    final Entity.Definition definition = entityDefinitions.get(entityID);
    if (definition == null) {
      throw new IllegalArgumentException("Undefined entity: " + entityID);
    }

    return definition;
  }

  private Map<String, Property> initializeProperties(final String domainID, final String entityID, final Property... properties) {
    final Map<String, Property> propertyMap = new LinkedHashMap<>(properties.length);
    for (final Property property : properties) {
      validateAndAddProperty(property, domainID, entityID, propertyMap);
      if (property instanceof Property.ForeignKeyProperty) {
        initializeForeignKeyProperty(domainID, entityID, propertyMap, (Property.ForeignKeyProperty) property);
      }
    }
    checkIfPrimaryKeyIsSpecified(entityID, propertyMap);

    return Collections.unmodifiableMap(propertyMap);
  }

  private void initializeForeignKeyProperty(final String domainID, final String entityID, final Map<String, Property> propertyMap,
                                            final Property.ForeignKeyProperty foreignKeyProperty) {
    final List<Property.ColumnProperty> properties = foreignKeyProperty.getProperties();
    if (!entityID.equals(foreignKeyProperty.getForeignEntityID()) && Entity.Definition.STRICT_FOREIGN_KEYS.get()) {
      final Entity.Definition foreignEntity = entityDefinitions.get(foreignKeyProperty.getForeignEntityID());
      if (foreignEntity == null) {
        throw new IllegalArgumentException("Entity '" + foreignKeyProperty.getForeignEntityID()
                + "' referenced by entity '" + entityID + "' via foreign key property '"
                + foreignKeyProperty.getPropertyID() + "' has not been defined");
      }
      if (properties.size() != foreignEntity.getPrimaryKeyProperties().size()) {
        throw new IllegalArgumentException("Number of column properties in '" + entityID + "." + foreignKeyProperty.getPropertyID() +
                "' does not match the number of foreign properties in the referenced entity '" + foreignKeyProperty.getForeignEntityID() + "'");
      }
    }
    for (final Property.ColumnProperty property : properties) {
      if (!(property instanceof Property.MirrorProperty)) {
        validateAndAddProperty(property, domainID, entityID, propertyMap);
      }
    }
  }

  private static void validateAndAddProperty(final Property property, final String domainID, final String entityID,
                                             final Map<String, Property> propertyMap) {
    checkIfUniquePropertyID(property, entityID, propertyMap);
    property.setDomainID(domainID);
    property.setEntityID(entityID);
    propertyMap.put(property.getPropertyID(), property);
  }

  private static void checkIfUniquePropertyID(final Property property, final String entityID, final Map<String, Property> propertyMap) {
    if (propertyMap.containsKey(property.getPropertyID())) {
      throw new IllegalArgumentException("Property with ID " + property.getPropertyID()
              + (property.getCaption() != null ? " (caption: " + property.getCaption() + ")" : "")
              + " has already been defined as: " + propertyMap.get(property.getPropertyID()) + " in entity: " + entityID);
    }
  }

  private static void checkIfPrimaryKeyIsSpecified(final String entityID, final Map<String, Property> propertyMap) {
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

    throw new IllegalArgumentException("Entity is missing a primary key: " + entityID);
  }

  private Entities setDomainEntities(final String domainID, final Entities entities) {
    synchronized (DOMAIN_ENTITIES) {
      DOMAIN_ENTITIES.put(domainID, entities);
    }

    return entities;
  }

  /**
   * Processes {@link Entity.Table}, {@link Property.Column}
   * and {@link org.jminor.common.db.Databases.Operation} annotations found in the given class
   * @param domainClass the domain class to process
   */
  public void processAnnotations(final Class<? extends Entities> domainClass) {
    final Map<Field, Annotation> tableAnnotations = new HashMap<>();
    final Map<Field, Annotation> columnAnnotations = new HashMap<>();
    final Map<Field, Annotation> operationAnnotations = new HashMap<>();
    final List<Field> staticStringFields = getStaticStringFields(domainClass);
    for (final Field field : staticStringFields) {
      final Annotation tableAnnotation = field.getAnnotation(Entity.Table.class);
      final Annotation columnAnnotation = field.getAnnotation(Property.Column.class);
      final Annotation operationAnnotation = field.getAnnotation(Databases.Operation.class);
      if (tableAnnotation != null) {
        tableAnnotations.put(field, tableAnnotation);
      }
      if (columnAnnotation != null) {
        columnAnnotations.put(field, columnAnnotation);
      }
      if (operationAnnotation != null) {
        operationAnnotations.put(field, operationAnnotation);
      }
    }
    processFieldAnnotations(tableAnnotations, columnAnnotations, operationAnnotations);
  }

  private void processFieldAnnotations(final Map<Field, Annotation> tableAnnotations,
                                       final Map<Field, Annotation> columnAnnotations,
                                       final Map<Field, Annotation> operationAnnotations) {
    try {
      for (final Map.Entry<Field, Annotation> entityAnnotation : tableAnnotations.entrySet()) {
        final String entityID = (String) entityAnnotation.getKey().get(null);
        final Collection<Map.Entry<Field, Property.Column>> entityPropertyAnnotations =
                getPropertyAnnotations(entityID, columnAnnotations);
        addTableAndColumnDefinitions(entityID, (Entity.Table) entityAnnotation.getValue(), entityPropertyAnnotations);
      }
      for (final Map.Entry<Field, Annotation> operationAnnotation : operationAnnotations.entrySet()) {
        final String operationID = (String) operationAnnotation.getKey().get(null);
        addDatabaseOperation(operationID, (Databases.Operation) operationAnnotation.getValue());
      }
    }
    catch (final IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  private static Collection<Map.Entry<Field, Property.Column>> getPropertyAnnotations(
          final String entityID, final Map<Field, Annotation> propertyAnnotations) {
    final Collection<Map.Entry<Field, Property.Column>> ret = new ArrayList<>();
    for (final Map.Entry<Field, Annotation> entry : propertyAnnotations.entrySet()) {
      final Property.Column propertyColumn = (Property.Column) entry.getValue();
      if (entityID.equals(propertyColumn.entityID())) {
        ret.add(new AbstractMap.SimpleEntry<>(entry.getKey(), (Property.Column) entry.getValue()));
      }
    }

    return ret;
  }

  private void addTableAndColumnDefinitions(final String entityID, final Entity.Table entityTable,
                                            final Collection<Map.Entry<Field, Property.Column>> entityColumnAnnotations) {
    final Entity.Definition definition = getDefinition(entityID);
    setTableConfiguration(entityTable, definition);
    setKeyGenerator(definition, entityTable);
    try {
      final LinkedHashMap<String, Collection<Property.ColumnProperty>> columnProperties =
              Util.map(definition.getColumnProperties(), Property::getPropertyID);
      for (final Map.Entry<Field, Property.Column> propertyColumn : entityColumnAnnotations) {
        final String propertyID = (String) propertyColumn.getKey().get(null);
        final Property.ColumnProperty property = columnProperties.get(propertyID).iterator().next();
        property.setColumnName(propertyColumn.getValue().columnName());
      }
    }
    catch (final IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  private static void setTableConfiguration(final Entity.Table entityTable, final Entity.Definition definition) {
    if (!Util.nullOrEmpty(entityTable.tableName())) {
      definition.setTableName(entityTable.tableName());
    }
    if (!Util.nullOrEmpty(entityTable.selectTableName())) {
      definition.setSelectTableName(entityTable.selectTableName());
    }
    if (!Util.nullOrEmpty(entityTable.selectQuery())) {
      definition.setSelectQuery(entityTable.selectQuery(), entityTable.selectQueryContainsWhereClause());
    }
    if (!Util.nullOrEmpty(entityTable.orderByClause())) {
      definition.setOrderByClause(entityTable.orderByClause());
    }
    if (!Util.nullOrEmpty(entityTable.havingClause())) {
      definition.setHavingClause(entityTable.havingClause());
    }
  }

  private void setKeyGenerator(final Entity.Definition definition, final Entity.Table entityTable) {
    switch (entityTable.keyGenerator()) {
      case AUTOMATIC:
        definition.setKeyGenerator(automaticKeyGenerator(entityTable.keyGeneratorSource()));
        break;
      case SEQUENCE:
        definition.setKeyGenerator(sequenceKeyGenerator(entityTable.keyGeneratorSource()));
        break;
      case QUERY:
        definition.setKeyGenerator(queriedKeyGenerator(entityTable.keyGeneratorSource()));
        break;
      case INCREMENT:
        definition.setKeyGenerator(incrementKeyGenerator(entityTable.keyGeneratorSource(),
                entityTable.keyGeneratorIncrementColumnName()));
        break;
      default:
        break;
    }
  }

  private void addDatabaseOperation(final String operationID, final Databases.Operation operation)
          throws IllegalAccessException {
    try {
      final Constructor constructor = Class.forName(operation.className()).getConstructor(String.class);
      addOperation((DatabaseConnection.Operation) constructor.newInstance(operationID));
    }
    catch (final NoSuchMethodException | ClassNotFoundException | InstantiationException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  private static List<Field> getStaticStringFields(final Class domainClass) {
    final List<Field> staticFields = new ArrayList<>();
    for (final Field field : domainClass.getDeclaredFields()) {
      if (Modifier.isStatic(field.getModifiers()) || field.getType().equals(String.class)) {
        staticFields.add(field);
      }
    }

    return staticFields;
  }

  private static Map<String, List<Property.DenormalizedProperty>> getDenormalizedProperties(final Collection<Property> properties) {
    final Map<String, List<Property.DenormalizedProperty>> denormalizedPropertiesMap = new HashMap<>(properties.size());
    for (final Property property : properties) {
      if (property instanceof Property.DenormalizedProperty) {
        final Property.DenormalizedProperty denormalizedProperty = (Property.DenormalizedProperty) property;
        final Collection<Property.DenormalizedProperty> denormalizedProperties =
                denormalizedPropertiesMap.computeIfAbsent(denormalizedProperty.getForeignKeyPropertyID(), k -> new ArrayList<>());
        denormalizedProperties.add(denormalizedProperty);
      }
    }

    return denormalizedPropertiesMap;
  }

  private static List<Property.ColumnProperty> getPrimaryKeyProperties(final Collection<Property> properties) {
    final List<Property.ColumnProperty> primaryKeyProperties = new ArrayList<>(properties.size());
    for (final Property property : properties) {
      if (property instanceof Property.ColumnProperty && ((Property.ColumnProperty) property).isPrimaryKeyProperty()) {
        primaryKeyProperties.add((Property.ColumnProperty) property);
      }
    }
    primaryKeyProperties.sort((pk1, pk2) -> {
      final Integer index1 = pk1.getPrimaryKeyIndex();
      final Integer index2 = pk2.getPrimaryKeyIndex();

      return index1.compareTo(index2);
    });

    return primaryKeyProperties;
  }

  private static List<Property.ForeignKeyProperty> getForeignKeyProperties(final Collection<Property> properties) {
    final List<Property.ForeignKeyProperty> foreignKeyProperties = new ArrayList<>(properties.size());
    for (final Property property : properties) {
      if (property instanceof Property.ForeignKeyProperty) {
        foreignKeyProperties.add((Property.ForeignKeyProperty) property);
      }
    }

    return foreignKeyProperties;
  }

  private static List<Property.ColumnProperty> getColumnProperties(final Collection<Property> properties) {
    final List<Property.ColumnProperty> columnProperties = new ArrayList<>(properties.size());
    for (final Property property : properties) {
      if (property instanceof Property.ColumnProperty) {
        columnProperties.add((Property.ColumnProperty) property);
      }
    }

    final String[] selectColumnNames = initializeSelectColumnNames(columnProperties);
    for (int idx = 0; idx < selectColumnNames.length; idx++) {
      columnProperties.get(idx).setSelectIndex(idx + 1);
    }

    return columnProperties;
  }

  private static List<Property.TransientProperty> getTransientProperties(final Collection<Property> properties) {
    final List<Property.TransientProperty> transientProperties = new ArrayList<>(properties.size());
    for (final Property property : properties) {
      if (property instanceof Property.TransientProperty) {
        transientProperties.add((Property.TransientProperty) property);
      }
    }

    return transientProperties;
  }

  private static List<Property> getVisibleProperties(final Collection<Property> properties) {
    final List<Property> visibleProperties = new ArrayList<>(properties.size());
    for (final Property property : properties) {
      if (!property.isHidden()) {
        visibleProperties.add(property);
      }
    }

    return visibleProperties;
  }

  private static Map<String, Set<Property.DerivedProperty>> initializeDerivedProperties(final Collection<Property> properties) {
    final Map<String, Set<Property.DerivedProperty>> derivedProperties = new HashMap<>();
    for (final Property property : properties) {
      if (property instanceof Property.DerivedProperty) {
        final Collection<String> derived = ((Property.DerivedProperty) property).getSourcePropertyIDs();
        if (!Util.nullOrEmpty(derived)) {
          for (final String parentLinkPropertyID : derived) {
            linkProperties(derivedProperties, parentLinkPropertyID, (Property.DerivedProperty) property);
          }
        }
      }
    }

    return derivedProperties;
  }

  private static void linkProperties(final Map<String, Set<Property.DerivedProperty>> derivedProperties,
                                     final String parentPropertyID, final Property.DerivedProperty derivedProperty) {
    if (!derivedProperties.containsKey(parentPropertyID)) {
      derivedProperties.put(parentPropertyID, new HashSet<>());
    }
    derivedProperties.get(parentPropertyID).add(derivedProperty);
  }

  /**
   * @param columnProperties the properties to base the column names on
   * @return the column names used to select an entity of this type from the database
   */
  private static String[] initializeSelectColumnNames(final Collection<Property.ColumnProperty> columnProperties) {
    final List<String> columnNames = new ArrayList<>();
    columnProperties.forEach(columnProperty -> columnNames.add(columnProperty.getPropertyID()));

    return columnNames.toArray(new String[columnNames.size()]);
  }

  private static String initializeSelectColumnsString(final List<Property.ColumnProperty> columnProperties) {
    final StringBuilder stringBuilder = new StringBuilder();
    int i = 0;
    for (final Property.ColumnProperty property : columnProperties) {
      if (property instanceof Property.SubqueryProperty) {
        stringBuilder.append("(").append(((Property.SubqueryProperty) property).getSubQuery()).append(
                ") as ").append(property.getColumnName());
      }
      else {
        stringBuilder.append(property.getColumnName());
      }

      if (i++ < columnProperties.size() - 1) {
        stringBuilder.append(", ");
      }
    }

    return stringBuilder.toString();
  }

  /**
   * @param columnProperties the column properties
   * @return a list of grouping columns separated with a comma, to serve as a group by clause,
   * null if no grouping properties are defined
   */
  private static String initializeGroupByClause(final Collection<Property.ColumnProperty> columnProperties) {
    final List<Property> groupingProperties = new ArrayList<>(columnProperties.size());
    for (final Property.ColumnProperty property : columnProperties) {
      if (property.isGroupingColumn()) {
        groupingProperties.add(property);
      }
    }
    if (groupingProperties.isEmpty()) {
      return null;
    }

    final StringBuilder stringBuilder = new StringBuilder();
    int i = 0;
    for (final Property property : groupingProperties) {
      stringBuilder.append(property.getPropertyID());
      if (i++ < groupingProperties.size() - 1) {
        stringBuilder.append(", ");
      }
    }

    return stringBuilder.toString();
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
     * @param propertyID the ID of the property which value should be used for a string representation
     */
    public StringProvider(final String propertyID) {
      addValue(propertyID);
    }

    /** {@inheritDoc} */
    @Override
    public String toString(final Entity entity) {
      Objects.requireNonNull(entity, ENTITY_PARAM);
      final StringBuilder builder = new StringBuilder();
      for (final ValueProvider valueProvider : valueProviders) {
        builder.append(valueProvider.toString(entity));
      }

      return builder.toString();
    }

    /**
     * Adds the value mapped to the given key to this {@link StringProvider}
     * @param propertyID the ID of the property which value should be added to the string representation
     * @return this {@link StringProvider} instance
     */
    public StringProvider addValue(final String propertyID) {
      Objects.requireNonNull(propertyID, PROPERTY_ID_PARAM);
      valueProviders.add(new StringValueProvider(propertyID));
      return this;
    }

    /**
     * Adds the value mapped to the given key to this StringProvider
     * @param propertyID the ID of the property which value should be added to the string representation
     * @param format the Format to use when appending the value
     * @return this {@link StringProvider} instance
     */
    public StringProvider addFormattedValue(final String propertyID, final Format format) {
      Objects.requireNonNull(propertyID, PROPERTY_ID_PARAM);
      Objects.requireNonNull(format, "format");
      valueProviders.add(new FormattedValueProvider(propertyID, format));
      return this;
    }

    /**
     * Adds the value mapped to the given property in the {@link Entity} instance mapped to the given foreignKeyPropertyID
     * to this {@link StringProvider}
     * @param foreignKeyProperty the foreign key property
     * @param propertyID the ID of the property in the referenced entity to use
     * @return this {@link StringProvider} instance
     */
    public StringProvider addForeignKeyValue(final Property.ForeignKeyProperty foreignKeyProperty,
                                             final String propertyID) {
      Objects.requireNonNull(foreignKeyProperty, "foreignKeyProperty");
      Objects.requireNonNull(propertyID, PROPERTY_ID_PARAM);
      valueProviders.add(new ForeignKeyValueProvider(foreignKeyProperty, propertyID));
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
      private final String propertyID;
      private final Format format;

      private FormattedValueProvider(final String propertyID, final Format format) {
        this.propertyID = propertyID;
        this.format = format;
      }

      @Override
      public String toString(final Entity entity) {
        if (entity.isValueNull(propertyID)) {
          return "";
        }

        return format.format(entity.get(propertyID));
      }
    }

    private static final class ForeignKeyValueProvider implements StringProvider.ValueProvider {
      private static final long serialVersionUID = 1;
      private final Property.ForeignKeyProperty foreignKeyProperty;
      private final String propertyID;

      private ForeignKeyValueProvider(final Property.ForeignKeyProperty foreignKeyProperty,
                                      final String propertyID) {
        this.foreignKeyProperty = foreignKeyProperty;
        this.propertyID = propertyID;
      }

      @Override
      public String toString(final Entity entity) {
        if (entity.isValueNull(foreignKeyProperty)) {
          return "";
        }

        return entity.getForeignKey(foreignKeyProperty).getAsString(propertyID);
      }
    }

    private static final class StringValueProvider implements StringProvider.ValueProvider {
      private static final long serialVersionUID = 1;
      private final String propertyID;

      private StringValueProvider(final String propertyID) {
        this.propertyID = propertyID;
      }

      @Override
      public String toString(final Entity entity) {
        return entity.getAsString(propertyID);
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

    private final Entities entities;
    private final String entityID;
    private final boolean performNullValidation = PERFORM_NULL_VALIDATION.get();

    /**
     * Instantiates a new {@link Entity.Validator}
     * @param entities the domain model entities
     * @param entityID the ID of the entities to validate
     */
    public Validator(final Entities entities, final String entityID) {
      this.entities = Objects.requireNonNull(entities, ENTITIES_PARAM);
      this.entityID = Objects.requireNonNull(entityID, ENTITY_ID_PARAM);
    }

    /** {@inheritDoc} */
    @Override
    public final String getEntityID() {
      return entityID;
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
      for (final Property property : entities.getProperties(entityID)) {
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
    }

    /** {@inheritDoc} */
    @Override
    public final void performRangeValidation(final Entity entity, final Property property) throws RangeValidationException {
      Objects.requireNonNull(entity, ENTITY_PARAM);
      Objects.requireNonNull(property, "property");
      if (entity.isValueNull(property)) {
        return;
      }

      final Number value = (Number) entity.get(property);
      if (value.doubleValue() < (property.getMin() == null ? Double.NEGATIVE_INFINITY : property.getMin())) {
        throw new RangeValidationException(property.getPropertyID(), value, "'" + property + "' " +
                FrameworkMessages.get(FrameworkMessages.PROPERTY_VALUE_TOO_SMALL) + " " + property.getMin());
      }
      if (value.doubleValue() > (property.getMax() == null ? Double.POSITIVE_INFINITY : property.getMax())) {
        throw new RangeValidationException(property.getPropertyID(), value, "'" + property + "' " +
                FrameworkMessages.get(FrameworkMessages.PROPERTY_VALUE_TOO_LARGE) + " " + property.getMax());
      }
    }

    /** {@inheritDoc} */
    @Override
    public final void performNullValidation(final Entity entity, final Property property) throws NullValidationException {
      Objects.requireNonNull(entity, ENTITY_PARAM);
      Objects.requireNonNull(property, "property");
      if (!isNullable(entity, property) && entity.isValueNull(property)) {
        if ((entity.getKey().isNull() || entity.getOriginalKey().isNull()) && !(property instanceof Property.ForeignKeyProperty)) {
          //a new entity being inserted, allow null for columns with default values and auto generated primary key values
          final boolean nonKeyColumnPropertyWithoutDefaultValue = isNonKeyColumnPropertyWithoutDefaultValue(property);
          final boolean primaryKeyPropertyWithoutAutoGenerate = isPrimaryKeyPropertyWithoutAutoGenerate(entityID, property);
          if (nonKeyColumnPropertyWithoutDefaultValue || primaryKeyPropertyWithoutAutoGenerate) {
            throw new NullValidationException(property.getPropertyID(),
                    FrameworkMessages.get(FrameworkMessages.PROPERTY_VALUE_IS_REQUIRED) + ": " + property);
          }
        }
        else {
          throw new NullValidationException(property.getPropertyID(),
                  FrameworkMessages.get(FrameworkMessages.PROPERTY_VALUE_IS_REQUIRED) + ": " + property);
        }
      }
    }

    private boolean isPrimaryKeyPropertyWithoutAutoGenerate(final String entityID, final Property property) {
      return (property instanceof Property.ColumnProperty
              && ((Property.ColumnProperty) property).isPrimaryKeyProperty()) && entities.getKeyGeneratorType(entityID).isManual();
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

    private final Entities entities;
    private final String entityID;
    private final List<Property.ColumnProperty> properties;
    private final List<Property.TransientProperty> transientProperties;
    private final boolean hasTransientProperties;
    private final int propertyCount;

    /**
     * Instantiates a new EntityResultPacker.
     * @param entityID the ID of the entities this packer packs
     */
    private EntityResultPacker(final Entities entities, final String entityID, final List<Property.ColumnProperty> columnProperties,
                               final List<Property.TransientProperty> transientProperties, final int propertyCount) {
      this.entities = entities;
      this.entityID = entityID;
      this.properties = columnProperties;
      this.transientProperties = transientProperties;
      this.hasTransientProperties = !Util.nullOrEmpty(this.transientProperties);
      this.propertyCount = propertyCount;
    }

    /**
     * Packs the contents of {@code resultSet} into a List of Entity objects.
     * The resulting entities do not contain values for foreign key properties (Property.ForeignKeyProperty).
     * This method does not close the ResultSet object.
     * @param resultSet the ResultSet object
     * @param fetchCount the maximum number of records to retrieve from the result set
     * @return a List of Entity objects representing the contents of {@code resultSet}
     * @throws java.sql.SQLException in case of an exception
     */
    @Override
    public List<Entity> pack(final ResultSet resultSet, final int fetchCount) throws SQLException {
      Objects.requireNonNull(resultSet, "resultSet");
      final List<Entity> result = new ArrayList<>();
      int counter = 0;
      while (resultSet.next() && (fetchCount < 0 || counter++ < fetchCount)) {
        result.add(loadEntity(resultSet));
      }

      return result;
    }

    private Entity loadEntity(final ResultSet resultSet) throws SQLException {
      final Map<Property, Object> values = new HashMap<>(propertyCount);
      if (hasTransientProperties) {
        for (int i = 0; i < transientProperties.size(); i++) {
          final Property.TransientProperty transientProperty = transientProperties.get(i);
          if (!(transientProperty instanceof Property.DenormalizedViewProperty)
                  && !(transientProperty instanceof Property.DerivedProperty)) {
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
          throw new SQLException("Exception fetching: " + property + ", entity: " + entityID + " [" + e.getMessage() + "]", e);
        }
      }

      return new DefaultEntity(entities, entityID, values);
    }
  }

  static class DefaultKeyGenerator implements Entity.KeyGenerator {

    @Override
    public Type getType() {
      return Type.NONE;
    }

    @Override
    public void beforeInsert(final Entity entity, final Property.ColumnProperty primaryKeyProperty,
                             final DatabaseConnection connection) throws SQLException {/*Provided for subclasses*/}

    @Override
    public void afterInsert(final Entity entity, final Property.ColumnProperty primaryKeyProperty,
                            final DatabaseConnection connection) throws SQLException {/*Provided for subclasses*/}
  }

  abstract static class QueriedKeyGenerator extends DefaultKeyGenerator {

    @Override
    public Type getType() {
      return Type.QUERY;
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

  static final class IncrementKeyGenerator extends QueriedKeyGenerator {

    private final String query;

    IncrementKeyGenerator(final String tableName, final String columnName) {
      this.query = "select max(" + columnName + ") + 1 from " + tableName;
    }

    @Override
    public Type getType() {
      return Type.INCREMENT;
    }

    @Override
    public void beforeInsert(final Entity entity, final Property.ColumnProperty primaryKeyProperty,
                             final DatabaseConnection connection) throws SQLException {
      if (entity.isValueNull(primaryKeyProperty)) {
        queryAndSet(entity, primaryKeyProperty, connection);
      }
    }

    @Override
    protected String getQuery(final Database database) {
      return query;
    }
  }

  static final class SequenceKeyGenerator extends QueriedKeyGenerator {

    private final String sequenceName;

    SequenceKeyGenerator(final String sequenceName) {
      this.sequenceName = sequenceName;
    }

    @Override
    public Type getType() {
      return Type.SEQUENCE;
    }

    @Override
    public void beforeInsert(final Entity entity, final Property.ColumnProperty primaryKeyProperty,
                             final DatabaseConnection connection) throws SQLException {
      if (entity.isValueNull(primaryKeyProperty)) {
        queryAndSet(entity, primaryKeyProperty, connection);
      }
    }

    @Override
    protected String getQuery(final Database database) {
      return database.getSequenceQuery(sequenceName);
    }
  }

  static final class AutomaticKeyGenerator extends QueriedKeyGenerator {

    private final String valueSource;

    AutomaticKeyGenerator(final String valueSource) {
      this.valueSource = valueSource;
    }

    @Override
    public Type getType() {
      return Type.AUTOMATIC;
    }

    @Override
    public void afterInsert(final Entity entity, final Property.ColumnProperty primaryKeyProperty,
                            final DatabaseConnection connection) throws SQLException {
      queryAndSet(entity, primaryKeyProperty, connection);
    }

    @Override
    protected String getQuery(final Database database) {
      return database.getAutoIncrementQuery(valueSource);
    }
  }
}
