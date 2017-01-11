/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.Util;
import org.jminor.common.db.DatabaseConnection;
import org.jminor.common.db.Databases;
import org.jminor.common.db.ResultPacker;
import org.jminor.common.db.valuemap.DefaultValueMap;
import org.jminor.common.db.valuemap.exception.NullValidationException;
import org.jminor.common.db.valuemap.exception.RangeValidationException;
import org.jminor.common.db.valuemap.exception.ValidationException;
import org.jminor.framework.Configuration;
import org.jminor.framework.i18n.FrameworkMessages;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.text.Format;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A {@link Entity} factory class
 */
public final class Entities {

  private static final String ENTITY_PARAM = "entity";
  private static final String ENTITY_ID_PARAM = "entityID";
  private static final String PROPERTY_ID_PARAM = "propertyID";

  private Entities() {}

  /**
   * Creates a new {@link Entity} instance with the given entityID
   * @param entityID the entity ID
   * @return a new {@link Entity} instance
   */
  public static Entity entity(final String entityID) {
    return new DefaultEntity(DefaultEntityDefinition.getDefinition(entityID));
  }

  /**
   * Creates a new {@link Entity} instance with the given primary key
   * @param key the primary key
   * @return a new {@link Entity} instance
   */
  public static Entity entity(final Entity.Key key) {
    return new DefaultEntity(DefaultEntityDefinition.getDefinition(key.getEntityID()), Objects.requireNonNull(key, "key"));
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
  public static Entity entity(final String entityID, final Map<Property, Object> values, final Map<Property, Object> originalValues) {
    return new DefaultEntity(DefaultEntityDefinition.getDefinition(entityID), values, originalValues);
  }

  /**
   * Creates a new {@link Entity.Key} instance with the given entityID
   * @param entityID the entity ID
   * @return a new {@link Entity.Key} instance
   */
  public static Entity.Key key(final String entityID) {
    return new DefaultEntity.DefaultKey(DefaultEntityDefinition.getDefinition(entityID), null);
  }

  /**
   * Defines a new entity, by default the {@code entityID} is used as the underlying table name
   * @param entityID the ID uniquely identifying the entity
   * @param propertyDefinitions the {@link Property} objects to base this entity on. In case a select query is specified
   * for this entity, the property order must match the select column order.
   * @return a new {@link Entity.Definition}
   * @throws IllegalArgumentException in case the entityID has already been used to define an entity type or if
   * no primary key property is specified
   */
  public static Entity.Definition define(final String entityID, final Property... propertyDefinitions) {
    return define(entityID, entityID, propertyDefinitions);
  }

  /**
   * Defines a new entity
   * @param entityID the ID uniquely identifying the entity
   * @param tableName the name of the underlying table
   * @param propertyDefinitions the {@link Property} objects to base the entity on. In case a select query is specified
   * for this entity, the property order must match the select column order.
   * @return a new {@link Entity.Definition}
   * @throws IllegalArgumentException in case the entityID has already been used to define an entity type or if
   * no primary key property is specified
   */
  public static Entity.Definition define(final String entityID, final String tableName, final Property... propertyDefinitions) {
    if (DefaultEntityDefinition.getDefinitionMap().containsKey(entityID) && !Configuration.getBooleanValue(Configuration.ALLOW_REDEFINE_ENTITY)) {
      throw new IllegalArgumentException("Entity has already been defined: " + entityID + ", for table: " + tableName);
    }
    final DefaultEntityDefinition entityDefinition = new DefaultEntityDefinition(entityID, tableName, propertyDefinitions);
    entityDefinition.setValidator(new Validator(entityID));
    DefaultEntityDefinition.getDefinitionMap().put(entityID, entityDefinition);

    return entityDefinition;
  }

  /**
   * Instantiates a primary key generator which fetches the current maximum primary key value and increments
   * it by one prior to insert.
   * @param tableName the table name
   * @param columnName the primary key column name
   * @return a incrementing primary key generator
   */
  public static Entity.KeyGenerator incrementKeyGenerator(final String tableName, final String columnName) {
    return new DefaultEntityDefinition.IncrementKeyGenerator(tableName, columnName);
  }

  /**
   * Instantiates a primary key generator which fetches primary key values from a sequence prior to insert
   * @param sequenceName the sequence name
   * @return a sequence based primary key generator
   */
  public static Entity.KeyGenerator sequenceKeyGenerator(final String sequenceName) {
    return new DefaultEntityDefinition.SequenceKeyGenerator(sequenceName);
  }

  /**
   * Instantiates a primary key generator which fetches primary key values using the given query prior to insert
   * @param query the query
   * @return a query based primary key generator
   */
  public static Entity.KeyGenerator queriedKeyGenerator(final String query) {
    return DefaultEntityDefinition.queriedKeyGenerator(query);
  }

  /**
   * Instantiates a primary key generator which fetches automatically incremented primary key values after insert
   * @param valueSource the value source, whether a sequence or a table name
   * @return a auto-increment based primary key generator
   */
  public static Entity.KeyGenerator automaticKeyGenerator(final String valueSource) {
    return new DefaultEntityDefinition.AutomaticKeyGenerator(valueSource);
  }

  /**
   * @param domainID the domain ID
   * @return all entity IDs associated with the given domain
   */
  public static Collection<String> getDomainEntityIDs(final String domainID) {
    final Collection<String> entityIDs = new ArrayList<>();
    for (final Entity.Definition definition : DefaultEntityDefinition.getDefinitionMap().values()) {
      if (definition.getDomainID().equals(domainID)) {
        entityIDs.add(definition.getEntityID());
      }
    }

    return entityIDs;
  }

  /**
   * @param entityID the entity ID
   * @return a String array containing the IDs of the properties used as default search properties
   * for entities identified by {@code entityID}
   */
  public static Collection<String> getSearchPropertyIDs(final String entityID) {
    return DefaultEntityDefinition.getDefinition(entityID).getSearchPropertyIDs();
  }

  /**
   * Retrieves the properties used when searching for a entity of the given type,
   * if no search property IDs are defined all STRING based properties are returned.
   * @param entityID the entity ID
   * @return the search properties to use
   */
  public static Collection<Property.ColumnProperty> getSearchProperties(final String entityID) {
    final Collection<String> searchPropertyIDs = getSearchPropertyIDs(entityID);
    return getSearchProperties(entityID, searchPropertyIDs.toArray(new String[searchPropertyIDs.size()]));
  }

  /**
   * Retrieves the properties used when searching for a entity of the given type,
   * if no search property IDs are specified all STRING based properties are returned.
   * @param entityID the entity ID
   * @param searchPropertyIds the IDs of the search properties to retrieve
   * @return the search properties to use
   */
  public static Collection<Property.ColumnProperty> getSearchProperties(final String entityID, final String... searchPropertyIds) {
    if (searchPropertyIds != null && searchPropertyIds.length > 0) {
      final List<Property.ColumnProperty> searchProperties = new ArrayList<>();
      for (final String propertyID : searchPropertyIds) {
        searchProperties.add(getColumnProperty(entityID, propertyID));
      }

      return searchProperties;
    }
    else {
      final Collection<String> searchableProperties = getSearchablePropertyIDs(entityID);
      return getColumnProperties(entityID, searchableProperties.toArray(new String[searchableProperties.size()]));
    }
  }

  /**
   * @param entityID the entityID
   * @return all searchable string-based properties for the given entity type
   */
  public static Collection<String> getSearchablePropertyIDs(final String entityID) {
    final Collection<String> searchProperties = new ArrayList<>();
    for (final Property.ColumnProperty property : getColumnProperties(entityID)) {
      if (property.isString() && property.isSearchable()) {
        searchProperties.add(property.getPropertyID());
      }
    }

    return searchProperties;
  }

  /**
   * @param entityID the entity ID
   * @return a list containing the primary key properties of the entity identified by {@code entityID}
   */
  public static List<Property.ColumnProperty> getPrimaryKeyProperties(final String entityID) {
    return DefaultEntityDefinition.getDefinition(entityID).getPrimaryKeyProperties();
  }

  /**
   * @param entityID the entity ID
   * @return true if the entity identified by {@code entityID} is read only
   * @throws IllegalArgumentException if the entity is undefined
   */
  public static boolean isReadOnly(final String entityID) {
    return DefaultEntityDefinition.getDefinition(entityID).isReadOnly();
  }

  /**
   * @param entityID the entity ID
   * @return true if the entity identified by {@code entityID} is based on a small dataset
   * @throws IllegalArgumentException if the entity is undefined
   */
  public static boolean isSmallDataset(final String entityID) {
    return DefaultEntityDefinition.getDefinition(entityID).isSmallDataset();
  }

  /**
   * @param entityID the entity ID
   * @return true if the entity identified by {@code entityID} is based on static data
   * @throws IllegalArgumentException if the entity is undefined
   */
  public static boolean isStaticData(final String entityID) {
    return DefaultEntityDefinition.getDefinition(entityID).isStaticData();
  }

  /**
   * @param entityID the entity ID
   * @return a comma separated list of columns to use in the order by clause
   */
  public static String getOrderByClause(final String entityID) {
    return DefaultEntityDefinition.getDefinition(entityID).getOrderByClause();
  }

  /**
   * @param entityID the entity ID
   * @return a comma separated list of columns to use in the group by clause
   */
  public static String getGroupByClause(final String entityID) {
    return DefaultEntityDefinition.getDefinition(entityID).getGroupByClause();
  }

  /**
   * @param entityID the entity ID
   * @return the having clause associated with this entity
   */
  public static String getHavingClause(final String entityID) {
    return DefaultEntityDefinition.getDefinition(entityID).getHavingClause();
  }

  /**
   * @param entityID the entity ID
   * @return the name of the table used to select entities identified by {@code entityID}
   * @throws IllegalArgumentException if the entity is undefined
   */
  public static String getSelectTableName(final String entityID) {
    return DefaultEntityDefinition.getDefinition(entityID).getSelectTableName();
  }

  /**
   * @param entityID the entity ID
   * @return the name of the table on which entities identified by {@code entityID} are based
   * @throws IllegalArgumentException if the entity is undefined
   */
  public static String getTableName(final String entityID) {
    return DefaultEntityDefinition.getDefinition(entityID).getTableName();
  }

  /**
   * @param entityID the entity ID
   * @return the sql query used when selecting entities identified by {@code entityID}
   * @throws IllegalArgumentException if the entity is undefined
   */
  public static String getSelectQuery(final String entityID) {
    return DefaultEntityDefinition.getDefinition(entityID).getSelectQuery();
  }

  /**
   * @param entityID the entity ID
   * @return true if the select query for the given entity, if any, contains a where clause
   */
  public static boolean selectQueryContainsWhereClause(final String entityID) {
    return DefaultEntityDefinition.getDefinition(entityID).selectQueryContainsWhereClause();
  }

  /**
   * @param entityID the entity ID
   * @return the query string used to select entities identified by {@code entityID}
   * @throws IllegalArgumentException if the entity is undefined
   */
  public static String getSelectColumnsString(final String entityID) {
    return DefaultEntityDefinition.getDefinition(entityID).getSelectColumnsString();
  }

  /**
   * @param entityID the entity ID
   * @return the primary key generator for entities identified by {@code entityID}
   * @throws IllegalArgumentException if the entity is undefined
   */
  public static Entity.KeyGenerator getKeyGenerator(final String entityID) {
    return DefaultEntityDefinition.getDefinition(entityID).getKeyGenerator();
  }

  /**
   * @param entityID the entity ID
   * @return the type of primary key generator used by entities identified by {@code entityID}
   * @throws IllegalArgumentException if the entity is undefined
   */
  public static Entity.KeyGenerator.Type getKeyGeneratorType(final String entityID) {
    return DefaultEntityDefinition.getDefinition(entityID).getKeyGeneratorType();
  }

  /**
   * @param entityID the entity ID
   * @return the {@link Entity.ToString} instance used to provide string representations
   * of entities of the given type
   * @throws IllegalArgumentException if the entity is undefined
   */
  public static Entity.ToString getStringProvider(final String entityID) {
    return DefaultEntityDefinition.getDefinition(entityID).getStringProvider();
  }

  /**
   * @param entityID the entity ID
   * @return the default Comparator to use when sorting entities of the given type
   */
  public static Comparator<Entity> getComparator(final String entityID) {
    return DefaultEntityDefinition.getDefinition(entityID).getComparator();
  }

  /**
   * Returns true if the value for the primary key of this entity is automatically generated, either by the framework,
   * such as values queried from sequences or set by triggers. If not the primary key value must be set manually
   * before the entity is inserted.
   * @param entityID the entity ID
   * @return true if the value for the primary key is automatically generated
   */
  public static boolean isPrimaryKeyAutoGenerated(final String entityID) {
    return !getKeyGeneratorType(entityID).isManual();
  }

  /**
   * @param entityID the entity ID
   * @return true if the primary key of the given type of entity is comprised of a single integer value
   */
  public static boolean hasSingleIntegerPrimaryKey(final String entityID) {
    final List<Property.ColumnProperty> primaryKeyProperties = DefaultEntityDefinition.getDefinition(entityID).getPrimaryKeyProperties();
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
  public static List<Property.ColumnProperty> getColumnProperties(final String entityID,
                                                                  final boolean includePrimaryKeyProperties,
                                                                  final boolean includeReadOnly,
                                                                  final boolean includeNonUpdatable) {
    final List<Property.ColumnProperty> properties = new ArrayList<>(DefaultEntityDefinition.getDefinition(entityID).getColumnProperties());
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
  public static List<Property> getVisibleProperties(final String entityID) {
    Objects.requireNonNull(entityID, ENTITY_ID_PARAM);
    return DefaultEntityDefinition.getDefinition(entityID).getVisibleProperties();
  }

  /**
   * @param entityID the entityID
   * @param propertyIDs the IDs of the properties to retrieve
   * @return the {@link Property.ColumnProperty}s specified by the given property IDs
   * @throws IllegalArgumentException in case a given propertyID does not represent a {@link Property.ColumnProperty}
   */
  public static List<Property.ColumnProperty> getColumnProperties(final String entityID, final String... propertyIDs) {
    final List<Property.ColumnProperty> columnProperties = new ArrayList<>();
    if (propertyIDs == null || propertyIDs.length == 0) {
      return columnProperties;
    }

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
  public static Property.ColumnProperty getColumnProperty(final String entityID, final String propertyID) {
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
  public static Property getProperty(final String entityID, final String propertyID) {
    Objects.requireNonNull(entityID, ENTITY_ID_PARAM);
    Objects.requireNonNull(propertyID, PROPERTY_ID_PARAM);
    final Property property = getProperties(entityID).get(propertyID);
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
  public static List<Property> getProperties(final String entityID, final Collection<String> propertyIDs) {
    Objects.requireNonNull(propertyIDs, PROPERTY_ID_PARAM);
    return getProperties(entityID, propertyIDs.toArray(new String[propertyIDs.size()]));
  }

  /**
   * @param entityID the entity ID
   * @param propertyIDs the IDs of the properties to retrieve
   * @return a list containing the properties identified by {@code propertyIDs}, found in
   * the entity identified by {@code entityID}
   */
  public static List<Property> getProperties(final String entityID, final String... propertyIDs) {
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
  public static Collection<Property> getProperties(final String entityID, final boolean includeHidden) {
    return includeHidden ? getProperties(entityID).values() : getVisibleProperties(entityID);
  }

  /**
   * @param entityID the entity ID
   * @return a list containing all database properties found in the entity identified by {@code entityID},
   * that is, properties that map to database columns
   */
  public static List<Property.ColumnProperty> getColumnProperties(final String entityID) {
    return DefaultEntityDefinition.getDefinition(entityID).getColumnProperties();
  }

  /**
   * @param entityID the entity ID
   * @return a list containing all transient database properties found in the entity identified by {@code entityID},
   * that is, properties that do not map to database columns
   */
  public static List<Property.TransientProperty> getTransientProperties(final String entityID) {
    return DefaultEntityDefinition.getDefinition(entityID).getTransientProperties();
  }

  /**
   * @param entityID the entity ID
   * @return a list containing all the foreign key properties found in the entity
   * identified by {@code entityID}
   */
  public static List<Property.ForeignKeyProperty> getForeignKeyProperties(final String entityID) {
    return DefaultEntityDefinition.getDefinition(entityID).getForeignKeyProperties();
  }

  /**
   * @param entityID the entity ID
   * @return true if the given entity contains denormalized properties
   */
  public static boolean hasDenormalizedProperties(final String entityID) {
    return DefaultEntityDefinition.getDefinition(entityID).hasDenormalizedProperties();
  }

  /**
   * @param entityID the entity ID
   * @param foreignKeyPropertyID the foreign key id
   * @return a collection containing all denormalized properties of the entity identified by {@code entityID}
   * which source is the entity identified by {@code propertyOwnerEntityID}
   */
  public static Collection<Property.DenormalizedProperty> getDenormalizedProperties(final String entityID,
                                                                                    final String foreignKeyPropertyID) {
    return DefaultEntityDefinition.getDefinition(entityID).getDenormalizedProperties(foreignKeyPropertyID);
  }

  /**
   * @param entityID the entity ID
   * @param foreignKeyPropertyID the foreign key id
   * @return true if the entity identified by {@code entityID} contains denormalized properties
   * which source is the entity identified by {@code propertyOwnerEntityID}
   */
  public static boolean hasDenormalizedProperties(final String entityID, final String foreignKeyPropertyID) {
    return DefaultEntityDefinition.getDefinition(entityID).hasDenormalizedProperties(foreignKeyPropertyID);
  }

  /**
   * Returns true if this entity contains properties which values are derived from the value of the given property
   * @param entityID the entityID
   * @param propertyID the ID of the property
   * @return true if any properties are derived from the given property
   */
  public static boolean hasDerivedProperties(final String entityID, final String propertyID) {
    return DefaultEntityDefinition.getDefinition(entityID).hasDerivedProperties(propertyID);
  }

  /**
   * Returns the properties which values are derived from the value of the given property,
   * an empty collection if no such derived properties exist
   * @param entityID the entityID
   * @param propertyID the ID of the property
   * @return a collection containing the properties which are derived from the given property
   */
  public static Collection<Property.DerivedProperty> getDerivedProperties(final String entityID, final String propertyID) {
    return DefaultEntityDefinition.getDefinition(entityID).getDerivedProperties(propertyID);
  }

  /**
   * Returns the foreign key properties referencing entities of the given type
   * @param entityID the ID of the entity from which to retrieve the foreign key properties
   * @param referenceEntityID the ID of the reference entity
   * @return a List containing the properties, an empty list is returned in case no properties fit the condition
   */
  public static List<Property.ForeignKeyProperty> getForeignKeyProperties(final String entityID, final String referenceEntityID) {
    final List<Property.ForeignKeyProperty> properties = new ArrayList<>();
    for (final Property.ForeignKeyProperty foreignKeyProperty : getForeignKeyProperties(entityID)) {
      if (foreignKeyProperty.getReferencedEntityID().equals(referenceEntityID)) {
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
  public static Property.ForeignKeyProperty getForeignKeyProperty(final String entityID, final String propertyID) {
    for (final Property.ForeignKeyProperty foreignKeyProperty : getForeignKeyProperties(entityID)) {
      if (foreignKeyProperty.is(propertyID)) {
        return foreignKeyProperty;
      }
    }

    throw new IllegalArgumentException("Foreign key property with id: " + propertyID + " not found in entity of type: " + entityID);
  }

  /**
   * @param entityID the entity ID
   * @return a map containing the properties the given entity is comprised of, mapped to their respective propertyIDs
   */
  public static Map<String, Property> getProperties(final String entityID) {
    return DefaultEntityDefinition.getDefinition(entityID).getProperties();
  }

  /**
   * @param entityID the entity ID
   * @return the caption associated with the given entity type
   */
  public static String getCaption(final String entityID) {
    return DefaultEntityDefinition.getDefinition(entityID).getCaption();
  }

  /**
   * @param entityID the entityID
   * @return the validator for the given entity type
   */
  public static Entity.Validator getValidator(final String entityID) {
    return DefaultEntityDefinition.getDefinition(entityID).getValidator();
  }

  /**
   * @param entityID the entityID
   * @return the ResultPacker responsible for packing this entity type
   */
  public static ResultPacker<Entity> getResultPacker(final String entityID) {
    return DefaultEntityDefinition.getDefinition(entityID).getResultPacker();
  }

  /**
   * @return the entityIDs of all defined entities
   */
  public static Collection<String> getDefinedEntities() {
    return new ArrayList<>(DefaultEntityDefinition.getDefinitionMap().keySet());
  }

  /**
   * @param entityID the entity ID
   * @return true if the entity is defined
   */
  public static boolean isDefined(final String entityID) {
    return DefaultEntityDefinition.getDefinitionMap().containsKey(entityID);
  }

  /**
   * @return a map containing all defined entityIDs, with their respective table names as an associated value
   */
  public static Map<String, String> getDefinitions() {
    return getDefinitions(null);
  }

  /**
   * @param domainID the ID of the domain for which to retrieve the entity definitions
   * @return a map containing all defined entityIDs, with their respective table names as an associated value
   */
  public static Map<String, String> getDefinitions(final String domainID) {
    final Map<String, String> definitions = new LinkedHashMap<>();
    for (final Entity.Definition definition : DefaultEntityDefinition.getDefinitionMap().values()) {
      if (domainID == null) {
        definitions.put(definition.getEntityID(), definition.getTableName());
      }
      else {
        if (getDomainEntityIDs(domainID).contains(definition.getEntityID())) {
          definitions.put(definition.getEntityID(), definition.getTableName());
        }
      }
    }

    return definitions;
  }

  /**
   * Processes {@link Entity.Table}, {@link Property.Column}
   * and {@link org.jminor.common.db.Databases.Operation} annotations found in the given class
   * @param domainClass the domain class to process
   */
  public static void processAnnotations(final Class domainClass) {
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

  private static void processFieldAnnotations(final Map<Field, Annotation> tableAnnotations,
                                              final Map<Field, Annotation> columnAnnotations,
                                              final Map<Field, Annotation> operationAnnotations) {
    try {
      for (final Map.Entry<Field, Annotation> entityAnnotation : tableAnnotations.entrySet()) {
        final String entityID = (String) entityAnnotation.getKey().get(null);
        final Collection<Map.Entry<Field, Property.Column>> entityPropertyAnnotations =
                getPropertyAnnotations(entityID, columnAnnotations);
        addTablAndColumnDefinitions(entityID, (Entity.Table) entityAnnotation.getValue(), entityPropertyAnnotations);
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

  private static void addTablAndColumnDefinitions(final String entityID, final Entity.Table entityTable,
                                                  final Collection<Map.Entry<Field, Property.Column>> entityColumnAnnotations) {
    final Entity.Definition definition = DefaultEntityDefinition.getDefinition(entityID);
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

  private static void setKeyGenerator(final Entity.Definition definition, final Entity.Table entityTable) {
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

  private static void addDatabaseOperation(final String operationID, final Databases.Operation operation)
          throws IllegalAccessException {
    try {
      final Constructor constructor = Class.forName(operation.className()).getConstructor(String.class);
      Databases.addOperation((DatabaseConnection.Operation) constructor.newInstance(operationID));
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
     * @param foreignKeyPropertyID the ID of the foreign key property
     * @param propertyID the ID of the property in the referenced entity to use
     * @return this {@link StringProvider} instance
     */
    public StringProvider addForeignKeyValue(final String foreignKeyPropertyID, final String propertyID) {
      Objects.requireNonNull(foreignKeyPropertyID, "foreignKeyPropertyID");
      Objects.requireNonNull(propertyID, PROPERTY_ID_PARAM);
      valueProviders.add(new ForeignKeyValueProvider(foreignKeyPropertyID, propertyID));
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

    private static final class FormattedValueProvider implements ValueProvider {
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

    private static final class ForeignKeyValueProvider implements ValueProvider {
      private static final long serialVersionUID = 1;
      private final String foreignKeyPropertyID;
      private final String propertyID;

      private ForeignKeyValueProvider(final String foreignKeyPropertyID, final String propertyID) {
        this.foreignKeyPropertyID = foreignKeyPropertyID;
        this.propertyID = propertyID;
      }

      @Override
      public String toString(final Entity entity) {
        final Property.ForeignKeyProperty foreignKeyProperty = getForeignKeyProperty(entity.getEntityID(), foreignKeyPropertyID);
        if (entity.isValueNull(foreignKeyProperty)) {
          return "";
        }

        return entity.getForeignKey(foreignKeyProperty).getAsString(propertyID);
      }
    }

    private static final class StringValueProvider implements ValueProvider {
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

    private static final class StaticTextProvider implements ValueProvider {
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

    private final String entityID;
    private final boolean performNullValidation = Configuration.getBooleanValue(Configuration.PERFORM_NULL_VALIDATION);

    /**
     * Instantiates a new {@link Entity.Validator}
     * @param entityID the ID of the entities to validate
     */
    public Validator(final String entityID) {
      Objects.requireNonNull(entityID, ENTITY_ID_PARAM);
      this.entityID = entityID;
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
      for (final Property property : getProperties(entityID).values()) {
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
          final boolean columnPropertyWithoutDefaultValue = isColumnPropertyWithoutDefaultValue(property);
          final boolean primaryKeyPropertyWithoutAutoGenerate = isPrimaryKeyPropertyWithoutAutoGenerate(entityID, property);
          if (columnPropertyWithoutDefaultValue || primaryKeyPropertyWithoutAutoGenerate) {
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

    /**
     * @param property the property
     * @return true if the property is a part of a foreign key
     */
    private static boolean isForeignKeyProperty(final Property property) {
      return property instanceof Property.ColumnProperty && ((Property.ColumnProperty) property).isForeignKeyProperty();
    }

    private static boolean isPrimaryKeyPropertyWithoutAutoGenerate(final String entityID, final Property property) {
      return (property instanceof Property.ColumnProperty
              && ((Property.ColumnProperty) property).isPrimaryKeyProperty()) && getKeyGeneratorType(entityID).isManual();
    }

    private static boolean isColumnPropertyWithoutDefaultValue(final Property property) {
      return property instanceof Property.ColumnProperty && !((Property.ColumnProperty) property).columnHasDefaultValue();
    }
  }
}
