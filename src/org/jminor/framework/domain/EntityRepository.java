/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.db.IdSource;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * A static repository for all Entity related meta-data
 */
public class EntityRepository {

  private static Map<String, EntityDefinition> entityDefinitions = new HashMap<String, EntityDefinition>();

  private EntityRepository() {}

  public static void putAll(final Map<String, EntityDefinition> repository) {
    entityDefinitions.putAll(repository);
  }

  /**
   * @param entityID the entity ID
   * @param propertyID the property ID
   * @param description a string describing the property
   */
  public static void setPropertyDescription(final String entityID, final String propertyID, final String description) {
    if (!entityDefinitions.containsKey(entityID))
      throw new RuntimeException("Undefined entity: " + entityID);
    if (entityDefinitions.get(entityID).propertyDescriptions == null)
      entityDefinitions.get(entityID).propertyDescriptions = new HashMap<String, String>();

    entityDefinitions.get(entityID).propertyDescriptions.put(propertyID, description);
  }

  /**
   * @param entityID the entity ID
   * @param property the property
   * @return the description string for the given property, null if none is defined
   */
  public static String getPropertyDescription(final String entityID, final Property property) {
    return getPropertyDescription(entityID, property.propertyID);
  }

  /**
   * @param entityID the entity ID
   * @param propertyID the property ID
   * @return the description string for the given property, null if none is defined
   */
  public static String getPropertyDescription(final String entityID, final String propertyID) {
    if (!entityDefinitions.containsKey(entityID))
      throw new RuntimeException("Undefined entity: " + entityID);

    return entityDefinitions.get(entityID).propertyDescriptions != null ?
            entityDefinitions.get(entityID).propertyDescriptions.get(propertyID) : null;
  }

  /**
   * @param entityID the entity ID
   * @param searchPropertyIDs the IDs of the properties to use as default lookup properties for
   * entities identified by <code>entityID</code>, these must be STRING properties
   * @throws RuntimeException in case of a non-string property ID
   */
  public static void setEntitySearchProperties(final String entityID, final String... searchPropertyIDs) {
    if (!entityDefinitions.containsKey(entityID))
      throw new RuntimeException("Undefined entity: " + entityID);
    for (final String propertyID : searchPropertyIDs)
      if (getProperty(entityID, propertyID).propertyType != Type.STRING)
        throw new RuntimeException("Entity search property must be of type String: " + getProperty(entityID, propertyID));

    entityDefinitions.get(entityID).searchPropertyIDs = searchPropertyIDs;
  }

  /**
   * @param entityID the entity ID
   * @return a String array containing the IDs of the properties used as default search properties
   * for entities identified by <code>entityID</code>
   */
  public static String[] getEntitySearchPropertyIDs(final String entityID) {
    if (!entityDefinitions.containsKey(entityID))
      throw new RuntimeException("Undefined entity: " + entityID);

    return entityDefinitions.get(entityID).searchPropertyIDs;
  }

  /**
   * @param entityID the entity ID
   * @return a list containing the primary key properties of the entity identified by <code>entityID</code>
   */
  public static List<Property.PrimaryKeyProperty> getPrimaryKeyProperties(final String entityID) {
    if (!entityDefinitions.containsKey(entityID))
      throw new RuntimeException("Undefined entity: " + entityID);

    return entityDefinitions.get(entityID).getPrimaryKeyProperties();
  }

  /**
   * @param entityID the entity ID
   * @return true if the entity identified by <code>entityID</code> is read only
   * @throws RuntimeException if the read only value is undefined
   */
  public static boolean isReadOnly(final String entityID) {
    if (!entityDefinitions.containsKey(entityID))
      throw new RuntimeException("Undefined entity: " + entityID);

    return entityDefinitions.get(entityID).isReadOnly();
  }

  /**
   * @param entityID the entity ID
   * @return true if the entity identified by <code>entityID</code> is based on a large dataset
   * @throws RuntimeException if the large dataset value is undefined
   */
  public static boolean isLargeDataset(final String entityID) {
    if (!entityDefinitions.containsKey(entityID))
      throw new RuntimeException("Undefined entity: " + entityID);

    return entityDefinitions.get(entityID).isLargeDataset();
  }

  /**
   * @param entityID the entity ID
   * @param value true if the entity identified by <code>entityID</code> is based on a large dataset
   */
  public static void setIsLargeDataset(final String entityID, final boolean value) {
    if (!entityDefinitions.containsKey(entityID))
      throw new RuntimeException("Undefined entity: " + entityID);

    entityDefinitions.get(entityID).setLargeDataset(value);
  }

  /**
   * @param entityID the entity ID
   * @return a comma seperated list of columns to use in the order by clause
   */
  public static String getOrderByClause(final String entityID) {
    if (!entityDefinitions.containsKey(entityID))
      throw new RuntimeException("Undefined entity: " + entityID);

    return entityDefinitions.get(entityID).orderByClause;
  }

  /**
   * @param entityID the entity ID
   * @return the name of the table used to select entities identified by <code>entityID</code>
   * @throws RuntimeException if none is defined
   */
  public static String getSelectTableName(final String entityID) {
    if (!entityDefinitions.containsKey(entityID))
      throw new RuntimeException("Undefined entity: " + entityID);

    return entityDefinitions.get(entityID).selectTableName;
  }

  /**
   * @param entityID the entity ID
   * @return the name of the table on which entities identified by <code>entityID</code> are based
   * @throws RuntimeException if none is defined
   */
  public static String getTableName(final String entityID) {
    if (!entityDefinitions.containsKey(entityID))
      throw new RuntimeException("Undefined entity: " + entityID);

    return entityDefinitions.get(entityID).tableName;
  }

  /**
   * @param entityID the entity ID
   * @return the query string used to select entities identified by <code>entityID</code>
   * @throws RuntimeException if none is defined
   */
  public static String getSelectColumnsString(final String entityID) {
    if (!entityDefinitions.containsKey(entityID))
      throw new RuntimeException("Undefined entity: " + entityID);

    return entityDefinitions.get(entityID).getSelectColumnsString();
  }

  /**
   * @param entityID the entity ID
   * @return the IdSource of the entity identified by <code>entityID</code>
   * @throws RuntimeException if none is defined
   */
  public static IdSource getIdSource(final String entityID) {
    if (!entityDefinitions.containsKey(entityID))
      throw new RuntimeException("No id source defined for entity: " + entityID);

    return entityDefinitions.get(entityID).idSource;
  }

  /**
   * Retrive the database properties comprising the entity identified by <code>entityID</code>
   * @param entityID the entity ID
   * @param includePrimaryKeyProperties if true primary key properties are included
   * @param includeSelectOnly if true then properties that are marked as 'select only' are included
   * @param includeNonUpdatable if true then non updatable properties are included
   * @return a list containing the database properties (properties that map to database columns) comprising
   * the entity identified by <code>entityID</code>
   */
  public static List<Property> getDatabaseProperties(final String entityID,
                                                     final boolean includePrimaryKeyProperties,
                                                     final boolean includeSelectOnly,
                                                     final boolean includeNonUpdatable) {
    final List<Property> properties = new ArrayList<Property>(getDatabaseProperties(entityID));
    final LinkedHashSet<Property> propertyHashSet = new LinkedHashSet<Property>(properties);
    for (final Property property: properties) {
      if (!includeSelectOnly && property.isSelectOnly())
        propertyHashSet.remove(property);
      if (!includeNonUpdatable && !property.isUpdatable())
        propertyHashSet.remove(property);
    }
    if (includePrimaryKeyProperties)
      for (final Property.PrimaryKeyProperty primaryKeyProperty : getPrimaryKeyProperties(entityID))
        propertyHashSet.add(primaryKeyProperty);

    return new ArrayList<Property>(propertyHashSet);
  }

  /**
   * @param entityID the entity ID
   * @return a collection containing the visible (non-hidden) properties
   * in the entity identified by <code>entityID</code>
   * @throws RuntimeException if no visible properties are defined for the given entity
   */
  public static Collection<Property> getVisibleProperties(final String entityID) {
    if (!entityDefinitions.containsKey(entityID))
      throw new RuntimeException("Undefined entity: " + entityID);

    return entityDefinitions.get(entityID).getVisibleProperties();
  }

  /**
   * @param entityID the entity ID
   * @return a list containing the visible (non-hidden) properties
   * in the entity identified by <code>entityID</code>
   * @throws RuntimeException if no visible properties are defined for the given entity
   */
  public static List<Property> getVisiblePropertyList(final String entityID) {
    final List<Property> ret = new ArrayList<Property>();
    for (final Property property : getVisibleProperties(entityID))
      ret.add(property);

    return ret;
  }

  /**
   * @param entityID the entity ID
   * @param propertyID the property ID
   * @return the property identified by <code>propertyID</code> in the entity identified by <code>entityID</code>
   * @throws RuntimeException in case no such property exists
   */
  public static Property getProperty(final String entityID, final String propertyID) {
    final Property ret = getProperties(entityID).get(propertyID);
    if (ret == null)
      throw new RuntimeException("Property '" + propertyID + "' not found in entity: " + entityID);

    return ret;
  }

  /**
   * @param entityID the entity ID
   * @param propertyIDs the property IDs of the properties to retrieve
   * @return a list containing the properties identified by <code>propertyIDs</code>, found in
   * the entity identified by <code>entityID</code>
   */
  public static List<Property> getProperties(final String entityID, final String... propertyIDs) {
    final List<Property> ret = new ArrayList<Property>();
    for (final String propertyID : propertyIDs)
      ret.add(getProperty(entityID, propertyID));

    return ret;
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
  public static Collection<Property> getDatabaseProperties(final String entityID) {
    if (!entityDefinitions.containsKey(entityID))
      throw new RuntimeException("Undefined entity: " + entityID);

    return entityDefinitions.get(entityID).getDatabaseProperties();
  }

  /**
   * @param entityID the entity ID
   * @return a collection containing all the foreign key properties found in the entity
   * identified by <code>entityID</code>
   */
  public static Collection<Property.ForeignKeyProperty> getForeignKeyProperties(final String entityID) {
    if (!entityDefinitions.containsKey(entityID))
      throw new RuntimeException("Undefined entity: " + entityID);

    return entityDefinitions.get(entityID).getForeignKeyProperties();
  }

  /**
   * @param entityID the entity ID
   * @return true if the given entity contains denormalized properties
   */
  public static boolean hasDenormalizedProperties(final String entityID) {
    if (!entityDefinitions.containsKey(entityID))
      throw new RuntimeException("Undefined entity: " + entityID);

    return entityDefinitions.get(entityID).hasDenormalizedProperties();
  }

  /**
   * @param entityID the entity ID
   * @param propertyOwnerEntityID the entity ID of the actual property owner entity
   * @return a collection containing all denormalized properties of the entity identified by <code>entityID</code>
   * which source is the entity identified by <code>propertyOwnerEntityID</code>
   */
  public static Collection<Property.DenormalizedProperty> getDenormalizedProperties(final String entityID,
                                                                                    final String propertyOwnerEntityID) {
    if (!entityDefinitions.containsKey(entityID))
      throw new RuntimeException("Undefined entity: " + entityID);

    return entityDefinitions.get(entityID).getDenormalizedProperties(propertyOwnerEntityID);
  }

  /**
   * Returns the foreign key properties referencing entities of the given type
   * @param entityID the ID of the entity from which to retrieve the foreign key properties
   * @param referenceEntityID the ID of the reference entity
   * @return a List containing the properties, an empty list is returned in case no properties fit the criteria
   */
  public static List<Property.ForeignKeyProperty> getForeignKeyProperties(final String entityID, final String referenceEntityID) {
    final List<Property.ForeignKeyProperty> ret = new ArrayList<Property.ForeignKeyProperty>();
    for (final Property.ForeignKeyProperty foreignKeyProperty : getForeignKeyProperties(entityID))
      if (foreignKeyProperty.referenceEntityID.equals(referenceEntityID))
        ret.add(foreignKeyProperty);

    return ret;
  }

  /**
   * @param entityID the entity ID
   * @param propertyID the property ID
   * @return the Property.ForeignKeyProperty with the given propertyID
   * @throws RuntimeException in case no such property exists
   */
  public static Property.ForeignKeyProperty getForeignKeyProperty(final String entityID, final String propertyID) {
    for (final Property.ForeignKeyProperty foreignKeyProperty : getForeignKeyProperties(entityID))
      if (foreignKeyProperty.propertyID.equals(propertyID))
        return foreignKeyProperty;

    throw new RuntimeException("Entity property with id: " + propertyID + " not found in entity of type: " + entityID);
  }

  /**
   * @param entityID the entity ID
   * @return a map containing the properties the given entity is comprised of, mapped to their respective propertyIDs
   */
  public static Map<String, Property> getProperties(final String entityID) {
    if (!entityDefinitions.containsKey(entityID))
      throw new RuntimeException("Undefined entity: " + entityID);

    return entityDefinitions.get(entityID).properties;
  }

  /**
   * @param entityID the entity ID
   * @return the name of the primary key value source for the given entity
   * @throws RuntimeException in case no id source name is specified
   */
  public static String getEntityIdSource(final String entityID) {
    if (!entityDefinitions.containsKey(entityID))
      throw new RuntimeException("Undefined entity: " + entityID);

    return entityDefinitions.get(entityID).idValueSource;
  }

  /**
   * @return the IDs of all the entities defined in this repository
   */
  public static Collection<String> getEntityIDs() {
    return entityDefinitions.keySet();
  }

  public static String[] getInitializedEntities() {
    return entityDefinitions.keySet().toArray(new String[entityDefinitions.keySet().size()]);
  }

  public static Map<String, EntityDefinition> getRepository() {
    return entityDefinitions;
  }

  /**
   * @param entityGroup a group of related entities (from the same domain fx), for which
   * we can deduce that if one has been initialized all have.
   * @return true if any one of the entities in the group have already initialized, hmm?
   */
  public static boolean contains(final Map<String, EntityDefinition> entityGroup) {
    return entityDefinitions.containsKey(entityGroup.keySet().iterator().next());
  }

  /**
   * Initializes a entity type, identified by the string <code>entityID</code> and based on the table
   * specified by that same string
   * @param entityID the full table name of the entity being specified, serves as the entity ID
   * @param orderByClause the default order by clause used when selecting multiple entities of this type
   * @param initialPropertyDefinitions the properties comprising this entity
   */
  public static void initialize(final String entityID, final String orderByClause,
                                final Property... initialPropertyDefinitions) {
    initialize(entityID, IdSource.AUTO_INCREMENT, null, orderByClause, initialPropertyDefinitions);
  }

  /**
   * Initializes a entity type, identified by the string <code>entityID</code> and based on the table
   * specified by that same string
   * @param entityID the full table name of the entity being specified, serves as the entity ID
   * @param initialPropertyDefinitions the properties comprising this entity
   */
  public static void initialize(final String entityID,
                                final Property... initialPropertyDefinitions) {
    initialize(entityID, IdSource.AUTO_INCREMENT, initialPropertyDefinitions);
  }

  /**
   * Initializes a entity type, identified by the string <code>entityID</code> and based on the table
   * specified by that same string
   * @param entityID the full table name of the entity being specified, serves as the entity ID
   * @param idSource specifies the primary key value source for the table this entity is based on
   * @param initialPropertyDefinitions the properties comprising this entity
   */
  public static void initialize(final String entityID, final IdSource idSource,
                                final Property... initialPropertyDefinitions) {
    initialize(entityID, idSource, null, initialPropertyDefinitions);
  }

  /**
   * Initializes a entity type, identified by the string <code>entityID</code> and based on the table
   * specified by that same string
   * @param entityID the full table name of the entity being specified, serves as the entity ID
   * @param idSource specifies the primary key value source for the table this entity is based on
   * @param entityIdSource the name of the primary key value source, such as a sequence name
   * @param initialPropertyDefinitions the properties comprising this entity
   */
  public static void initialize(final String entityID, final IdSource idSource,
                                final String entityIdSource, final Property... initialPropertyDefinitions) {
    initialize(entityID, idSource, entityIdSource, null, initialPropertyDefinitions);
  }

  /**
   * Initializes a entity type, identified by the string <code>entityID</code> and based on the table
   * specified by that same string
   * @param entityID the full table name of the entity being specified, serves as the entity ID
   * @param idSource specifies the primary key value source for the table this entity is based on
   * @param entityIdSource the name of the primary key value source, such as a sequence name
   * @param orderByClause the default order by clause used when selecting multiple entities of this type
   * @param initialPropertyDefinitions the properties comprising this entity
   */
  public static void initialize(final String entityID, final IdSource idSource,
                                final String entityIdSource, final String orderByClause,
                                final Property... initialPropertyDefinitions) {
    initialize(entityID, null, idSource, entityIdSource, orderByClause, null, false, initialPropertyDefinitions);
  }

  /**
   * Initializes a entity type, identified by the string <code>entityID</code> and based on the table
   * specified by that same string
   * @param entityID the full table name of the entity being specified, serves as the entity ID
   * @param idSource specifies the primary key value source for the table this entity is based on
   * @param entityIdSource the name of the primary key value source, such as a sequence name
   * @param orderByClause the default order by clause used when selecting multiple entities of this type
   * @param dbSelectTableName the name of the table or view from which entities of this type should be selected,
   * in case it differs from the table used to insert/update/delete entities
   * @param initialPropertyDefinitions the properties comprising this entity
   */
  public static void initialize(final String entityID, final IdSource idSource,
                                final String entityIdSource, final String orderByClause, final String dbSelectTableName,
                                final Property... initialPropertyDefinitions) {
    initialize(entityID, idSource, entityIdSource, orderByClause, dbSelectTableName,
            false, initialPropertyDefinitions);
  }

  /**
   * Initializes a entity type, identified by the string <code>entityID</code> and based on the table
   * specified by that same string
   * @param entityID the full table name of the entity being specified, serves as the entity ID
   * @param idSource specifies the primary key value source for the table this entity is based on
   * @param entityIdSource the name of the primary key value source, such as a sequence name
   * @param orderByClause the default order by clause used when selecting multiple entities of this type
   * @param dbSelectTableName the name of the table or view from which entities of this type should be selected,
   * in case it differs from the table used to insert/update/delete entities
   * @param isReadOnly true if entities of this type should be regarded as read-only
   * @param initialPropertyDefinitions the properties comprising this entity
   */
  public static void initialize(final String entityID, final IdSource idSource,
                                final String entityIdSource, final String orderByClause,
                                final String dbSelectTableName, final boolean isReadOnly,
                                final Property... initialPropertyDefinitions) {
    initialize(entityID, null, idSource, entityIdSource, orderByClause, dbSelectTableName,
            isReadOnly, false, initialPropertyDefinitions);
  }

  /**
   * Initializes a entity type, identified by the string <code>entityID</code> and based on the table
   * specified by that same string
   * @param entityID the full table name of the entity being specified, serves as the entity ID
   * @param idSource specifies the primary key value source for the table this entity is based on
   * @param entityIdSource the name of the primary key value source, such as a sequence name
   * @param orderByClause the default order by clause used when selecting multiple entities of this type
   * @param dbSelectTableName the name of the table or view from which entities of this type should be selected,
   * in case it differs from the table used to insert/update/delete entities
   * @param isReadOnly true if entities of this type should be regarded as read-only
   * @param largeDataset true if the dataset this entity is based on will become large, mostly used
   * to judge if the dataset can be shown in a combo box or list component
   * @param initialPropertyDefinitions the properties comprising this entity
   */
  public static void initialize(final String entityID, final IdSource idSource,
                                final String entityIdSource, final String orderByClause,
                                final String dbSelectTableName, final boolean isReadOnly,
                                final boolean largeDataset, final Property... initialPropertyDefinitions) {
    initialize(entityID, null, idSource, entityIdSource, orderByClause, dbSelectTableName,
            isReadOnly, largeDataset, initialPropertyDefinitions);
  }

  /**
   * Initializes a entity type, identified by the string <code>entityID</code> and based on the table
   * specified by that same string
   * @param entityID the full table name of the entity being specified, serves as the entity ID
   * @param dbTableName the name of the table used to insert/update/delete entities of this type
   * @param idSource specifies the primary key value source for the table this entity is based on
   * @param entityIdSource the name of the primary key value source, such as a sequence name
   * @param orderByClause the default order by clause used when selecting multiple entities of this type
   * @param dbSelectTableName the name of the table or view from which entities of this type should be selected,
   * in case it differs from the table used to insert/update/delete entities
   * @param isReadOnly true if entities of this type should be regarded as read-only
   * @param initialPropertyDefinitions the properties comprising this entity
   */
  public static void initialize(final String entityID, final String dbTableName, final IdSource idSource,
                                final String entityIdSource, final String orderByClause,
                                final String dbSelectTableName, final boolean isReadOnly,
                                final Property... initialPropertyDefinitions) {
    initialize(entityID, dbTableName, idSource, entityIdSource, orderByClause, dbSelectTableName,
            isReadOnly, false, initialPropertyDefinitions);
  }

  /**
   * Initializes a entity type, identified by the string <code>entityID</code> and based on the table
   * specified by that same string
   * @param entityID the full table name of the entity being specified, serves as the entity ID
   * @param dbTableName the name of the table used to insert/update/delete entities of this type
   * @param idSource specifies the primary key value source for the table this entity is based on
   * @param entityIdSource the name of the primary key value source, such as a sequence name
   * @param orderByClause the default order by clause used when selecting multiple entities of this type
   * @param dbSelectTableName the name of the table or view from which entities of this type should be selected,
   * in case it differs from the table used to insert/update/delete entities
   * @param isReadOnly true if entities of this type should be regarded as read-only
   * @param largeDataset true if the dataset this entity is based on will become large, mostly used
   * to judge if the dataset can be shown in a combo box or list component
   * @param initialPropertyDefinitions the properties comprising this entity
   */
  public static void initialize(final String entityID, final String dbTableName, final IdSource idSource,
                                final String entityIdSource, final String orderByClause,
                                final String dbSelectTableName, final boolean isReadOnly,
                                final boolean largeDataset, final Property... initialPropertyDefinitions) {
    if (entityDefinitions.containsKey(entityID))
      throw new IllegalArgumentException("Entity with ID '" + entityID + "' has already been initialized!");

    entityDefinitions.put(entityID, new EntityDefinition(entityID, initialPropertyDefinitions,
            dbTableName == null ? entityID : dbTableName.toLowerCase(), dbSelectTableName == null ?
                    (dbTableName == null ? entityID : dbTableName.toLowerCase()) : dbSelectTableName.toLowerCase(),
            orderByClause, idSource, (idSource == IdSource.SEQUENCE || idSource == IdSource.AUTO_INCREMENT) ?
                    (entityIdSource == null || entityIdSource.length() == 0 ? (entityID + "_seq") : entityIdSource) : null,
            isReadOnly, largeDataset));
  }

  public static class EntityDefinition implements Serializable {

    private static final long serialVersionUID = 1;
    /**
     * The entityID
     */
    private final String entityID;
    /**
     * The properties
     */
    private final Map<String, Property> properties;
    /**
     * The name of the underlying table
     */
    private final String tableName;
    /**
     * The table (view, query) from which to select the entity
     * Used if it differs from the one used for inserts/updates
     */
    private final String selectTableName;
    /**
     * Holds the order by clause
     */
    private final String orderByClause;
    /**
     * The source of the entitys id (primary key), i.e. sequence name
     */
    private final String idValueSource;
    /**
     * The IdSource
     */
    private final IdSource idSource;
    /**
     * The readOnly value
     */
    private final boolean readOnly;
    /**
     * The largeDataset value
     */
    private boolean largeDataset;

    private List<Property.PrimaryKeyProperty> primaryKeyProperties;
    private List<Property.ForeignKeyProperty> foreignKeyProperties;

    private List<Property> visibleProperties;
    private List<Property> databaseProperties;

    private Map<String, Collection<Property.DenormalizedProperty>> denormalizedProperties;

    private Map<String, String> propertyDescriptions;
    private String[] searchPropertyIDs;

    private String selectColumnsString;

    public EntityDefinition(final String entityID, final Property[] propertyDefinitions, final String tableName,
                            final String selectTableName, final String orderByClause, final IdSource idSource,
                            final String idValueSource, final boolean readOnly, final boolean largeDataset) {
      this.entityID = entityID;
      this.tableName = tableName;
      this.selectTableName = selectTableName;
      this.orderByClause = orderByClause;
      this.idSource = idSource;
      this.idValueSource = idValueSource;
      this.readOnly = readOnly;
      this.largeDataset = largeDataset;

      this.properties = new LinkedHashMap<String, Property>(propertyDefinitions.length);
      for (final Property property : propertyDefinitions) {
        if (properties.containsKey(property.propertyID))
          throw new IllegalArgumentException("Property with ID " + property.propertyID
                  + (property.getCaption() != null ? " (caption: " + property.getCaption() + ")" : "")
                  + " has already been defined as: " + properties.get(property.propertyID) + " in entity: " + entityID);
        properties.put(property.propertyID, property);
        if (property instanceof Property.ForeignKeyProperty) {
          for (final Property referenceProperty : ((Property.ForeignKeyProperty) property).referenceProperties) {
            if (!(referenceProperty instanceof Property.MirrorProperty)) {
              if (properties.containsKey(referenceProperty.propertyID))
                throw new IllegalArgumentException("Property with ID " + referenceProperty.propertyID
                        + (referenceProperty.getCaption() != null ? " (caption: " + referenceProperty.getCaption() + ")" : "")
                        + " has already been defined as: " + properties.get(referenceProperty.propertyID) + " in entity: " + entityID);
              properties.put(referenceProperty.propertyID, referenceProperty);
            }
          }
        }
      }
      initialize();
    }

    public String getEntityID() {
      return entityID;
    }

    public void setLargeDataset(final boolean largeDataset) {
      this.largeDataset = largeDataset;
    }

    public boolean isLargeDataset() {
      return largeDataset;
    }

    public boolean isReadOnly() {
      return readOnly;
    }

    public List<Property.PrimaryKeyProperty> getPrimaryKeyProperties() {
      return primaryKeyProperties;
    }

    public String getSelectColumnsString() {
      return selectColumnsString;
    }

    public Collection<Property> getVisibleProperties() {
      return visibleProperties;
    }

    public Collection<Property> getDatabaseProperties() {
      return databaseProperties;
    }

    public Collection<Property.ForeignKeyProperty> getForeignKeyProperties() {
      return foreignKeyProperties != null ? foreignKeyProperties : new ArrayList<Property.ForeignKeyProperty>(0);
    }

    public boolean hasDenormalizedProperties() {
      return denormalizedProperties.size() > 0 && denormalizedProperties.containsKey(entityID);
    }

    public Collection<Property.DenormalizedProperty> getDenormalizedProperties(final String propertyOwnerEntityID) {
      return denormalizedProperties != null ? denormalizedProperties.get(propertyOwnerEntityID) : null;
    }

    public void initialize() {
      visibleProperties = new ArrayList<Property>(properties.size());
      databaseProperties = new ArrayList<Property>(properties.size());
      foreignKeyProperties = new ArrayList<Property.ForeignKeyProperty>(properties.size());
      primaryKeyProperties = new ArrayList<Property.PrimaryKeyProperty>(properties.size());
      denormalizedProperties = new HashMap<String, Collection<Property.DenormalizedProperty>>(properties.size());

      for (final Property property : properties.values()) {
        if (property instanceof Property.PrimaryKeyProperty)
          primaryKeyProperties.add((Property.PrimaryKeyProperty) property);
        if (property instanceof Property.ForeignKeyProperty)
          foreignKeyProperties.add((Property.ForeignKeyProperty) property);
        if (property.isDatabaseProperty())
          databaseProperties.add(property);
        if (property instanceof Property.DenormalizedProperty) {
          final Property.DenormalizedProperty denormalizedProperty = (Property.DenormalizedProperty) property;
          Collection<Property.DenormalizedProperty> denormProps = denormalizedProperties.get(denormalizedProperty.foreignKeyPropertyID);
          if (denormProps == null)
            denormalizedProperties.put(denormalizedProperty.foreignKeyPropertyID, denormProps = new ArrayList<Property.DenormalizedProperty>());
          denormProps.add(denormalizedProperty);
        }
        if (!property.isHidden())
          visibleProperties.add(property);
      }

      final String[] selectColumnNames = initSelectColumnNames();
      for (int idx = 0; idx < selectColumnNames.length; idx++)
        properties.get(selectColumnNames[idx]).setSelectIndex(idx+1);

      this.selectColumnsString = initSelectColumnsString();
    }

    /**
     * @return the column names used to select an entity of this type from the database
     */
    private String[] initSelectColumnNames() {
      final List<String> ret = new ArrayList<String>();
      for (final Property property : getDatabaseProperties())
        if (!(property instanceof Property.ForeignKeyProperty))
          ret.add(property.propertyID);

      return ret.toArray(new String[ret.size()]);
    }

    private String initSelectColumnsString() {
      final Collection<Property> dbProperties = getDatabaseProperties();
      final List<Property> selectProperties = new ArrayList<Property>(dbProperties.size());
      for (final Property property : dbProperties)
        if (!(property instanceof Property.ForeignKeyProperty))
          selectProperties.add(property);

      final StringBuilder ret = new StringBuilder();
      int i = 0;
      for (final Property property : selectProperties) {
        if (property instanceof Property.SubqueryProperty)
          ret.append("(").append(((Property.SubqueryProperty)property).getSubQuery()).append(
                  ") ").append(property.propertyID);
        else
          ret.append(property.propertyID);

        if (i++ < selectProperties.size() - 1)
          ret.append(", ");
      }

      return ret.toString();
    }
  }
}