/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.model.Util;
import org.jminor.common.model.valuemap.DefaultValueMapValidator;
import org.jminor.common.model.valuemap.exception.NullValidationException;
import org.jminor.common.model.valuemap.exception.RangeValidationException;
import org.jminor.common.model.valuemap.exception.ValidationException;
import org.jminor.framework.Configuration;
import org.jminor.framework.i18n.FrameworkMessages;

import java.io.Serializable;
import java.text.Format;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * A {@link Entity} factory class
 */
public final class Entities {

  private static final String ENTITY_PARAM = "entity";
  private static final String ENTITY_ID_PARAM = "entityID";

  private Entities() {}

  /**
   * Creates a new {@link Entity} instance with the given entityID
   * @param entityID the entity ID
   * @return a new {@link Entity} instance
   */
  public static Entity entity(final String entityID) {
    return new EntityImpl(EntityDefinitionImpl.getDefinition(entityID));
  }

  /**
   * Creates a new {@link Entity} instance with the given primary key
   * @param key the primary key
   * @return a new {@link Entity} instance
   */
  public static Entity entity(final Entity.Key key) {
    return new EntityImpl(EntityDefinitionImpl.getDefinition(key.getEntityID()), key);
  }

  /**
   * Creates a new {@link Entity} instance with the given entityID and the given values/originalValues
   * @param entityID the entity ID
   * @param values the values
   * @param originalValues the original values
   * @return a new {@link Entity} instance
   */
  public static Entity entity(final String entityID, final Map<String, Object> values, final Map<String, Object> originalValues) {
    return EntityImpl.entityInstance(EntityDefinitionImpl.getDefinition(entityID), values, originalValues);
  }

  /**
   * Creates a new {@link Entity.Key} instance with the given entityID
   * @param entityID the entity ID
   * @return a new {@link Entity.Key} instance
   */
  public static Entity.Key key(final String entityID) {
    return new EntityImpl.KeyImpl(EntityDefinitionImpl.getDefinition(entityID));
  }

  /**
   * Defines a new entity, by default the <code>entityID</code> is used as the underlying table name
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
    if (EntityDefinitionImpl.getDefinitionMap().containsKey(entityID)) {
      throw new IllegalArgumentException("Entity has already been defined: " + entityID + ", for table: " + tableName);
    }
    final EntityDefinitionImpl entityImpl = new EntityDefinitionImpl(entityID, tableName, propertyDefinitions);
    entityImpl.setValidator(new Validator(entityID));
    EntityDefinitionImpl.getDefinitionMap().put(entityID, entityImpl);

    return entityImpl;
  }

  /**
   * Instantiates a primary key generator which fetches the current maximum primary key value and increments
   * it by one prior to insert.
   * @param tableName the table name
   * @param columnName the primary key column name
   * @return a incrementing primary key generator
   */
  public static Entity.KeyGenerator incrementKeyGenerator(final String tableName, final String columnName) {
    return new EntityDefinitionImpl.IncrementKeyGenerator(tableName, columnName);
  }

  /**
   * Instantiates a primary key generator which fetches primary key values from a sequence prior to insert
   * @param sequenceName the sequence name
   * @return a sequence based primary key generator
   */
  public static Entity.KeyGenerator sequenceKeyGenerator(final String sequenceName) {
    return new EntityDefinitionImpl.SequenceKeyGenerator(sequenceName);
  }

  /**
   * Instantiates a primary key generator which fetches primary key values using the given query prior to insert
   * @param query the query
   * @return a query based primary key generator
   */
  public static Entity.KeyGenerator queriedKeyGenerator(final String query) {
    return EntityDefinitionImpl.queriedKeyGenerator(query);
  }

  /**
   * Instantiates a primary key generator which fetches automatically incremented primary key values after insert
   * @param valueSource the value source, whether a sequence or a table name
   * @return a auto-increment based primary key generator
   */
  public static Entity.KeyGenerator automaticKeyGenerator(final String valueSource) {
    return new EntityDefinitionImpl.AutomaticKeyGenerator(valueSource);
  }

  /**
   * @param domainID the domain ID
   * @return all entity IDs associated with the given domain
   */
  public static Collection<String> getDomainEntityIDs(final String domainID) {
    final Collection<String> entityIDs = new ArrayList<String>();
    for (final Entity.Definition definition : EntityDefinitionImpl.getDefinitionMap().values()) {
      if (definition.getDomainID().equals(domainID)) {
        entityIDs.add(definition.getEntityID());
      }
    }

    return entityIDs;
  }

  /**
   * @param entityID the entity ID
   * @return a String array containing the IDs of the properties used as default search properties
   * for entities identified by <code>entityID</code>
   */
  public static Collection<String> getSearchPropertyIDs(final String entityID) {
    return EntityDefinitionImpl.getDefinition(entityID).getSearchPropertyIDs();
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
      final List<Property.ColumnProperty> searchProperties = new ArrayList<Property.ColumnProperty>();
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
    final Collection<String> searchProperties = new ArrayList<String>();
    for (final Property.ColumnProperty property : getColumnProperties(entityID)) {
      if (property.isString() && property.isSearchable()) {
        searchProperties.add(property.getPropertyID());
      }
    }

    return searchProperties;
  }

  /**
   * @param entityID the entity ID
   * @return a list containing the primary key properties of the entity identified by <code>entityID</code>
   */
  public static List<Property.PrimaryKeyProperty> getPrimaryKeyProperties(final String entityID) {
    return EntityDefinitionImpl.getDefinition(entityID).getPrimaryKeyProperties();
  }

  /**
   * @param entityID the entity ID
   * @return true if the entity identified by <code>entityID</code> is read only
   * @throws IllegalArgumentException if the entity is undefined
   */
  public static boolean isReadOnly(final String entityID) {
    return EntityDefinitionImpl.getDefinition(entityID).isReadOnly();
  }

  /**
   * @param entityID the entity ID
   * @return true if the entity identified by <code>entityID</code> is based on a small dataset
   * @throws IllegalArgumentException if the entity is undefined
   */
  public static boolean isSmallDataset(final String entityID) {
    return EntityDefinitionImpl.getDefinition(entityID).isSmallDataset();
  }

  /**
   * @param entityID the entity ID
   * @return a comma separated list of columns to use in the order by clause
   */
  public static String getOrderByClause(final String entityID) {
    return EntityDefinitionImpl.getDefinition(entityID).getOrderByClause();
  }

  /**
   * @param entityID the entity ID
   * @return a comma separated list of columns to use in the group by clause
   */
  public static String getGroupByClause(final String entityID) {
    return EntityDefinitionImpl.getDefinition(entityID).getGroupByClause();
  }

  /**
   * @param entityID the entity ID
   * @return the name of the table used to select entities identified by <code>entityID</code>
   * @throws IllegalArgumentException if the entity is undefined
   */
  public static String getSelectTableName(final String entityID) {
    return EntityDefinitionImpl.getDefinition(entityID).getSelectTableName();
  }

  /**
   * @param entityID the entity ID
   * @return the name of the table on which entities identified by <code>entityID</code> are based
   * @throws IllegalArgumentException if the entity is undefined
   */
  public static String getTableName(final String entityID) {
    return EntityDefinitionImpl.getDefinition(entityID).getTableName();
  }

  /**
   * @param entityID the entity ID
   * @return the sql query used when selecting entities identified by <code>entityID</code>
   * @throws IllegalArgumentException if the entity is undefined
   */
  public static String getSelectQuery(final String entityID) {
    return EntityDefinitionImpl.getDefinition(entityID).getSelectQuery();
  }

  /**
   * @param entityID the entity ID
   * @return the query string used to select entities identified by <code>entityID</code>
   * @throws IllegalArgumentException if the entity is undefined
   */
  public static String getSelectColumnsString(final String entityID) {
    return EntityDefinitionImpl.getDefinition(entityID).getSelectColumnsString();
  }

  /**
   * @param entityID the entity ID
   * @return the primary key generator for entities identified by <code>entityID</code>
   * @throws IllegalArgumentException if the entity is undefined
   */
  public static Entity.KeyGenerator getKeyGenerator(final String entityID) {
    return EntityDefinitionImpl.getDefinition(entityID).getKeyGenerator();
  }

  /**
   * @param entityID the entity ID
   * @return the {@link Entity.ToString} instance used to provide string representations
   * of entities of the given type
   * @throws IllegalArgumentException if the entity is undefined
   */
  public static Entity.ToString getStringProvider(final String entityID) {
    return EntityDefinitionImpl.getDefinition(entityID).getStringProvider();
  }

  /**
   * Returns true if the value for the primary key of this entity is automatically generated, either by the framework,
   * such as values queried from sequences or set by triggers. If not the primary key value must be set manually
   * before the entity is inserted.
   * @param entityID the entity ID
   * @return true if the value for the primary key is automatically generated
   */
  public static boolean isPrimaryKeyAutoGenerated(final String entityID) {
    return !getKeyGenerator(entityID).isManual();
  }

  /**
   * Retrieves the column properties comprising the entity identified by <code>entityID</code>
   * @param entityID the entity ID
   * @param includePrimaryKeyProperties if true primary key properties are included
   * @param includeReadOnly if true then properties that are marked as 'read only' are included
   * @param includeNonUpdatable if true then non updatable properties are included
   * @return a list containing the database properties (properties that map to database columns) comprising
   * the entity identified by <code>entityID</code>
   */
  public static List<Property.ColumnProperty> getColumnProperties(final String entityID,
                                                                  final boolean includePrimaryKeyProperties,
                                                                  final boolean includeReadOnly,
                                                                  final boolean includeNonUpdatable) {
    final List<Property.ColumnProperty> properties = new ArrayList<Property.ColumnProperty>(EntityDefinitionImpl.getDefinition(entityID).getColumnProperties());
    final ListIterator<Property.ColumnProperty> iterator = properties.listIterator();
    while (iterator.hasNext()) {
      final Property.ColumnProperty property = iterator.next();
      if (!includeReadOnly && property.isReadOnly()
              || !includeNonUpdatable && !property.isUpdatable()
              || !includePrimaryKeyProperties && property instanceof Property.PrimaryKeyProperty) {
        iterator.remove();
      }
    }

    return properties;
  }

  /**
   * @param entityID the entity ID
   * @return a list containing the visible (non-hidden) properties
   * in the entity identified by <code>entityID</code>
   */
  public static List<Property> getVisibleProperties(final String entityID) {
    Util.rejectNullValue(entityID, ENTITY_ID_PARAM);
    return EntityDefinitionImpl.getDefinition(entityID).getVisibleProperties();
  }

  /**
   * @param entityID the entityID
   * @param propertyIDs the IDs of the properties to retrieve
   * @return the {@link Property.ColumnProperty}s specified by the given property IDs
   * @throws IllegalArgumentException in case a given propertyID does not represent a {@link Property.ColumnProperty}
   */
  public static List<Property.ColumnProperty> getColumnProperties(final String entityID, final String... propertyIDs) {
    final List<Property.ColumnProperty> columnProperties = new ArrayList<Property.ColumnProperty>();
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
   * @return the property identified by <code>propertyID</code> in the entity identified by <code>entityID</code>
   * @throws IllegalArgumentException in case no such property exists
   */
  public static Property getProperty(final String entityID, final String propertyID) {
    Util.rejectNullValue(entityID, ENTITY_ID_PARAM);
    Util.rejectNullValue(propertyID, "propertyID");
    final Property property = getProperties(entityID).get(propertyID);
    if (property == null) {
      throw new IllegalArgumentException("Property '" + propertyID + "' not found in entity: " + entityID);
    }

    return property;
  }

  /**
   * @param entityID the entity ID
   * @param propertyIDs the IDs of the properties to retrieve
   * @return a list containing the properties identified by <code>propertyIDs</code>, found in
   * the entity identified by <code>entityID</code>
   */
  public static List<Property> getProperties(final String entityID, final Collection<String> propertyIDs) {
    Util.rejectNullValue(propertyIDs, "propertyIDs");
    return getProperties(entityID, propertyIDs.toArray(new String[propertyIDs.size()]));
  }

  /**
   * @param entityID the entity ID
   * @param propertyIDs the IDs of the properties to retrieve
   * @return a list containing the properties identified by <code>propertyIDs</code>, found in
   * the entity identified by <code>entityID</code>
   */
  public static List<Property> getProperties(final String entityID, final String... propertyIDs) {
    Util.rejectNullValue(entityID, ENTITY_ID_PARAM);
    Util.rejectNullValue(propertyIDs, "propertyIDs");
    final List<Property> properties = new ArrayList<Property>();
    for (final String propertyID : propertyIDs) {
      properties.add(getProperty(entityID, propertyID));
    }

    return properties;
  }

  /**
   * @param entityID the entity ID
   * @param includeHidden true if hidden properties should be included in the result
   * @return a collection containing the properties found in the entity identified by <code>entityID</code>
   */
  public static Collection<Property> getProperties(final String entityID, final boolean includeHidden) {
    return includeHidden ? getProperties(entityID).values() : getVisibleProperties(entityID);
  }

  /**
   * @param entityID the entity ID
   * @return a collection containing all database properties found in the entity identified by <code>entityID</code>,
   * that is, properties that map to database columns
   */
  public static Collection<Property.ColumnProperty> getColumnProperties(final String entityID) {
    return EntityDefinitionImpl.getDefinition(entityID).getColumnProperties();
  }

  /**
   * @param entityID the entity ID
   * @return a collection containing all transient database properties found in the entity identified by <code>entityID</code>,
   * that is, properties that do not map to database columns
   */
  public static Collection<Property.TransientProperty> getTransientProperties(final String entityID) {
    return EntityDefinitionImpl.getDefinition(entityID).getTransientProperties();
  }

  /**
   * @param entityID the entity ID
   * @return a collection containing all the foreign key properties found in the entity
   * identified by <code>entityID</code>
   */
  public static Collection<Property.ForeignKeyProperty> getForeignKeyProperties(final String entityID) {
    return EntityDefinitionImpl.getDefinition(entityID).getForeignKeyProperties();
  }

  /**
   * @param entityID the entity ID
   * @return true if the given entity contains denormalized properties
   */
  public static boolean hasDenormalizedProperties(final String entityID) {
    return EntityDefinitionImpl.getDefinition(entityID).hasDenormalizedProperties();
  }

  /**
   * @param entityID the entity ID
   * @param foreignKeyPropertyID the foreign key id
   * @return a collection containing all denormalized properties of the entity identified by <code>entityID</code>
   * which source is the entity identified by <code>propertyOwnerEntityID</code>
   */
  public static Collection<Property.DenormalizedProperty> getDenormalizedProperties(final String entityID,
                                                                                    final String foreignKeyPropertyID) {
    return EntityDefinitionImpl.getDefinition(entityID).getDenormalizedProperties(foreignKeyPropertyID);
  }

  /**
   * @param entityID the entity ID
   * @param foreignKeyPropertyID the foreign key id
   * @return true if the entity identified by <code>entityID</code> contains denormalized properties
   * which source is the entity identified by <code>propertyOwnerEntityID</code>
   */
  public static boolean hasDenormalizedProperties(final String entityID, final String foreignKeyPropertyID) {
    return EntityDefinitionImpl.getDefinition(entityID).hasDenormalizedProperties(foreignKeyPropertyID);
  }

  /**
   * Returns true if the property identified by <code>propertyID</code> in the entity identified
   * by <code>entityID</code> has any linked properties, that is properties which
   * values depend on the value of the given property
   * @param entityID the entityID
   * @param propertyID the propertyID
   * @return true if any derived properties are linked to the given property
   */
  public static boolean hasLinkedProperties(final String entityID, final String propertyID) {
    return EntityDefinitionImpl.getDefinition(entityID).hasLinkedProperties(propertyID);
  }

  /**
   * Returns the IDs of any properties which values are linked to the property identified by <code>propertyID</code>
   * in the entity identified by <code>entityID</code>
   * @param entityID the entityID
   * @param propertyID the propertyID
   * @return the IDs of any properties which values are linked to the given property
   */
  public static Collection<String> getLinkedPropertyIDs(final String entityID, final String propertyID) {
    return EntityDefinitionImpl.getDefinition(entityID).getLinkedPropertyIDs(propertyID);
  }

  /**
   * Returns the foreign key properties referencing entities of the given type
   * @param entityID the ID of the entity from which to retrieve the foreign key properties
   * @param referenceEntityID the ID of the reference entity
   * @return a List containing the properties, an empty list is returned in case no properties fit the criteria
   */
  public static List<Property.ForeignKeyProperty> getForeignKeyProperties(final String entityID, final String referenceEntityID) {
    final List<Property.ForeignKeyProperty> properties = new ArrayList<Property.ForeignKeyProperty>();
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
    return EntityDefinitionImpl.getDefinition(entityID).getProperties();
  }

  /**
   * @param entityID the entity ID
   * @return the caption associated with the given entity type
   */
  public static String getCaption(final String entityID) {
    return EntityDefinitionImpl.getDefinition(entityID).getCaption();
  }

  /**
   * @param entityID the entityID
   * @return the validator for the given entity type
   */
  public static Entity.Validator getValidator(final String entityID) {
    return EntityDefinitionImpl.getDefinition(entityID).getValidator();
  }

  /**
   * @return the entityIDs of all defined entities
   */
  public static Collection<String> getDefinedEntities() {
    return new ArrayList<String>(EntityDefinitionImpl.getDefinitionMap().keySet());
  }

  /**
   * @param entityID the entity ID
   * @return true if the entity is defined
   */
  public static boolean isDefined(final String entityID) {
    return EntityDefinitionImpl.getDefinitionMap().containsKey(entityID);
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
    final Map<String, String> definitions = new LinkedHashMap<String, String>();
    for (final Entity.Definition definition : EntityDefinitionImpl.getDefinitionMap().values()) {
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
   * Provides String representations of {@link Entity} instances.<br>
   * Given a {@link Entity} instance named entity containing the following mappings:
   * <pre>
   * "key1" -> value1
   * "key2" -> value2
   * "key3" -> value3
   * "key4" -> {Entity instance with a single mapping "refKey" -> refValue}
   * </pre>
   * <code>
   * Entities.StringProvider provider = new Entities.StringProvider();<br>
   * provider.addText("key1=").addValue("key1").addText(", key3='").addValue("key3")<br>
   *         .addText("' foreign key value=").addForeignKeyValue("key4", "refKey");<br>
   * System.out.println(provider.toString(entity));<br>
   * </code>
   * <br>
   * outputs the following String:<br><br>
   * <code>key1=value1, key3='value3' foreign key value=refValue</code>
   */
  public static final class StringProvider implements Entity.ToString, Serializable {

    private static final long serialVersionUID = 1;

    /**
     * Holds the ValueProviders used when constructing the String representation
     */
    private final List<ValueProvider> valueProviders = new ArrayList<ValueProvider>();

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
      Util.rejectNullValue(entity, ENTITY_PARAM);
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

      /** {@inheritDoc} */
      @Override
      public String toString(final Entity entity) {
        if (entity.isValueNull(propertyID)) {
          return "";
        }

        return format.format(entity.getValue(propertyID));
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

      /** {@inheritDoc} */
      @Override
      public String toString(final Entity entity) {
        if (entity.isValueNull(foreignKeyPropertyID)) {
          return "";
        }

        return entity.getForeignKeyValue(foreignKeyPropertyID).getValueAsString(propertyID);
      }
    }

    private static final class StringValueProvider implements ValueProvider {
      private static final long serialVersionUID = 1;
      private final String propertyID;

      private StringValueProvider(final String propertyID) {
        this.propertyID = propertyID;
      }

      /** {@inheritDoc} */
      @Override
      public String toString(final Entity entity) {
        return entity.getValueAsString(propertyID);
      }
    }

    private static final class StaticTextProvider implements ValueProvider {
      private static final long serialVersionUID = 1;
      private final String text;

      private StaticTextProvider(final String text) {
        this.text = text;
      }

      /** {@inheritDoc} */
      @Override
      public String toString(final Entity entity) {
        return text;
      }
    }
  }

  /**
   * A default extensible {@link Entity.Validator} implementation.
   */
  public static class Validator extends DefaultValueMapValidator<String, Entity> implements Entity.Validator {

    private final String entityID;
    private final boolean performNullValidation = Configuration.getBooleanValue(Configuration.PERFORM_NULL_VALIDATION);

    /**
     * Instantiates a new {@link Entity.Validator}
     * @param entityID the ID of the entities to validate
     */
    public Validator(final String entityID) {
      Util.rejectNullValue(entityID, ENTITY_ID_PARAM);
      this.entityID = entityID;
    }

    /** {@inheritDoc} */
    @Override
    public final String getEntityID() {
      return entityID;
    }

    /**
     * Returns true if the given property accepts a null value for the given entity,
     * by default this method simply returns <code>property.isNullable()</code>
     * @param entity the entity being validated
     * @param key the property ID
     * @return true if the property accepts a null value
     */
    @Override
    public boolean isNullable(final Entity entity, final String key) {
      return getProperty(entityID, key).isNullable();
    }

    /** {@inheritDoc} */
    @Override
    public final void validate(final Collection<Entity> entities) throws ValidationException {
      for (final Entity entity : entities) {
        validate(entity);
      }
    }

    /** {@inheritDoc} */
    @Override
    public void validate(final Entity entity) throws ValidationException {
      Util.rejectNullValue(entity, ENTITY_PARAM);
      for (final Property property : getProperties(entityID).values()) {
        validate(entity, property.getPropertyID());
      }
    }

    /** {@inheritDoc} */
    @Override
    public void validate(final Entity entity, final String propertyID) throws ValidationException {
      Util.rejectNullValue(entity, ENTITY_PARAM);
      final Property property = entity.getProperty(propertyID);
      if (performNullValidation && !(property instanceof Property.ForeignKeyProperty)) {
        performNullValidation(entity, property);
      }
      if (property.isNumerical()) {
        performRangeValidation(entity, property);
      }
    }

    /** {@inheritDoc} */
    @Override
    public final void performRangeValidation(final Entity entity, final Property property) throws RangeValidationException {
      Util.rejectNullValue(entity, ENTITY_PARAM);
      Util.rejectNullValue(property, "property");
      if (entity.isValueNull(property.getPropertyID())) {
        return;
      }

      final Double value = property.isDouble() ? (Double) entity.getValue(property.getPropertyID())
              : (Integer) entity.getValue(property.getPropertyID());
      if (value < (property.getMin() == null ? Double.NEGATIVE_INFINITY : property.getMin())) {
        throw new RangeValidationException(property.getPropertyID(), value, "'" + property + "' " +
                FrameworkMessages.get(FrameworkMessages.PROPERTY_VALUE_TOO_SMALL) + " " + property.getMin());
      }
      if (value > (property.getMax() == null ? Double.POSITIVE_INFINITY : property.getMax())) {
        throw new RangeValidationException(property.getPropertyID(), value, "'" + property + "' " +
                FrameworkMessages.get(FrameworkMessages.PROPERTY_VALUE_TOO_LARGE) + " " + property.getMax());
      }
    }

    /** {@inheritDoc} */
    @Override
    public final void performNullValidation(final Entity entity, final Property property) throws NullValidationException {
      Util.rejectNullValue(entity, ENTITY_PARAM);
      Util.rejectNullValue(property, "property");
      if (!isNullable(entity, property.getPropertyID()) && entity.isValueNull(property.getPropertyID())) {
        Property exceptionProperty = property;
        if (property instanceof Property.ColumnProperty && ((Property.ColumnProperty) property).isForeignKeyProperty()) {
          exceptionProperty = ((Property.ColumnProperty) property).getForeignKeyProperty();
        }
        if (entity.getPrimaryKey().isNull() || entity.getOriginalPrimaryKey().isNull()) {
          //a new entity being inserted, allow null for columns with default values and auto generated primary key values
          final boolean columnPropertyWithoutDefaultValue = property instanceof Property.ColumnProperty &&
                  !((Property.ColumnProperty) property).columnHasDefaultValue();
          final boolean primaryKeyPropertyWithoutAutoGenerate = property instanceof Property.PrimaryKeyProperty &&
                  getKeyGenerator(entityID).isManual();
          if (columnPropertyWithoutDefaultValue || primaryKeyPropertyWithoutAutoGenerate) {
            throw new NullValidationException(exceptionProperty.getPropertyID(),
                    FrameworkMessages.get(FrameworkMessages.PROPERTY_VALUE_IS_REQUIRED) + ": " + exceptionProperty);
          }
        }
        else {
          throw new NullValidationException(exceptionProperty.getPropertyID(),
                  FrameworkMessages.get(FrameworkMessages.PROPERTY_VALUE_IS_REQUIRED) + ": " + exceptionProperty);

        }
      }
    }
  }
}
