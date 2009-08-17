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
public class EntityRepository implements Serializable {

  private static final long serialVersionUID = 1;

  private Map<String, EntityDefinition> entityInfo = new HashMap<String, EntityDefinition>();

  private static EntityRepository instance;

  private EntityRepository() {}

  public static EntityRepository get() {
    if (instance == null)
      instance = new EntityRepository();

    return instance;
  }

  public void add(final EntityRepository repository) {
    entityInfo.putAll(repository.entityInfo);
  }

  /**
   * @param entityID the entity ID
   * @param propertyID the property ID
   * @param description a string describing the property
   */
  public void setPropertyDescription(final String entityID, final String propertyID, final String description) {
    if (!entityInfo.containsKey(entityID))
      throw new RuntimeException("Undefined entity: " + entityID);
    if (entityInfo.get(entityID).propertyDescriptions == null)
      entityInfo.get(entityID).propertyDescriptions = new HashMap<String, String>();

    entityInfo.get(entityID).propertyDescriptions.put(propertyID, description);
  }

  /**
   * @param entityID the entity ID
   * @param property the property
   * @return the description string for the given property, null if none is defined
   */
  public String getPropertyDescription(final String entityID, final Property property) {
    return getPropertyDescription(entityID, property.propertyID);
  }

  /**
   * @param entityID the entity ID
   * @param propertyID the property ID
   * @return the description string for the given property, null if none is defined
   */
  public String getPropertyDescription(final String entityID, final String propertyID) {
    if (!entityInfo.containsKey(entityID))
      throw new RuntimeException("Undefined entity: " + entityID);

    return entityInfo.get(entityID).propertyDescriptions != null ? entityInfo.get(entityID).propertyDescriptions.get(propertyID) : null;
  }

  /**
   * @param entityID the entity ID
   * @param searchPropertyIDs the IDs of the properties to use as default lookup properties for
   * entities identified by <code>entityID</code>, these must be STRING properties
   * @throws RuntimeException in case of a non-string property ID
   */
  public void setEntitySearchProperties(final String entityID, final String... searchPropertyIDs) {
    if (!entityInfo.containsKey(entityID))
      throw new RuntimeException("Undefined entity: " + entityID);
    for (final String propertyID : searchPropertyIDs)
      if (getProperty(entityID, propertyID).propertyType != Type.STRING)
        throw new RuntimeException("Entity search property must be of type String: " + getProperty(entityID, propertyID));

    entityInfo.get(entityID).entitySearchPropertyIDs = searchPropertyIDs;
  }

  /**
   * @param entityID the entity ID
   * @return a String array containing the IDs of the properties used as default search properties
   * for entities identified by <code>entityID</code>
   */
  public String[] getEntitySearchPropertyIDs(final String entityID) {
    if (!entityInfo.containsKey(entityID))
      throw new RuntimeException("Undefined entity: " + entityID);

    return entityInfo.get(entityID).entitySearchPropertyIDs;
  }

  /**
   * @param entityID the entity ID
   * @return a list containing the primary key properties of the entity identified by <code>entityID</code>
   */
  public List<Property.PrimaryKeyProperty> getPrimaryKeyProperties(final String entityID) {
    if (!entityInfo.containsKey(entityID))
      throw new RuntimeException("Undefined entity: " + entityID);

    return entityInfo.get(entityID).getPrimaryKeyProperties();
  }

  /**
   * @param entityID the entity ID
   * @return a String array containing the primary column names of the entity identified by <code>entityID</code>
   */
  public String[] getPrimaryKeyColumnNames(final String entityID) {
    if (!entityInfo.containsKey(entityID))
      throw new RuntimeException("Undefined entity: " + entityID);

    return entityInfo.get(entityID).getPrimaryKeyColumnNames();
  }

  /**
   * @param entityID the entity ID
   * @param index the index of the property to retrieve
   * @return the property found at index <code>idx</code> in the entity identified by <code>entityID</code>,
   * null if no property is at the given index
   * @throws RuntimeException if property indexes are not defined for the given entity
   */
  public Property getPropertyAtViewIndex(final String entityID, final int index) {
    if (!entityInfo.containsKey(entityID))
      throw new RuntimeException("Undefined entity: " + entityID);

    return entityInfo.get(entityID).getPropertyAtViewIndex(index);
  }

  /**
   * @param entityID the entity ID
   * @return true if the entity identified by <code>entityID</code> is read only
   * @throws RuntimeException if the read only value is undefined
   */
  public boolean isReadOnly(final String entityID) {
    if (!entityInfo.containsKey(entityID))
      throw new RuntimeException("Undefined entity: " + entityID);

    return entityInfo.get(entityID).isReadOnly();
  }

  /**
   * @param entityID the entity ID
   * @return true if the entity identified by <code>entityID</code> is based on a large dataset
   * @throws RuntimeException if the large dataset value is undefined
   */
  public boolean isLargeDataset(final String entityID) {
    if (!entityInfo.containsKey(entityID))
      throw new RuntimeException("Undefined entity: " + entityID);

    return entityInfo.get(entityID).isLargeDataset();
  }

  /**
   * @param entityID the entity ID
   * @param value true if the entity identified by <code>entityID</code> is based on a large dataset
   */
  public void setIsLargeDataset(final String entityID, final boolean value) {
    if (!entityInfo.containsKey(entityID))
      throw new RuntimeException("Undefined entity: " + entityID);

    entityInfo.get(entityID).setLargeDataset(value);
  }

  /**
   * @param entityID the entity ID
   * @return a comma seperated list of columns to use in the order by clause
   */
  public String getOrderByColumnNames(final String entityID) {
    if (!entityInfo.containsKey(entityID))
      throw new RuntimeException("Undefined entity: " + entityID);

    return entityInfo.get(entityID).orderByClause;
  }

  /**
   * @param entityID the entity ID
   * @return the name of the table used to select entities identified by <code>entityID</code>
   * @throws RuntimeException if none is defined
   */
  public String getSelectTableName(final String entityID) {
    if (!entityInfo.containsKey(entityID))
      throw new RuntimeException("Undefined entity: " + entityID);

    return entityInfo.get(entityID).selectTableName;
  }

  /**
   * @param entityID the entity ID
   * @return the name of the table on which entities identified by <code>entityID</code> are based
   * @throws RuntimeException if none is defined
   */
  public String getTableName(final String entityID) {
    if (!entityInfo.containsKey(entityID))
      throw new RuntimeException("Undefined entity: " + entityID);

    return entityInfo.get(entityID).tableName;
  }

  /**
   * @param entityID the entity ID
   * @return the query string used to select entities identified by <code>entityID</code>
   * @throws RuntimeException if none is defined
   */
  public String getSelectString(final String entityID) {
    if (!entityInfo.containsKey(entityID))
      throw new RuntimeException("Undefined entity: " + entityID);

    return entityInfo.get(entityID).getSelectString();
  }

  /**
   * @param entityID the entity ID
   * @return the IdSource of the entity identified by <code>entityID</code>
   * @throws RuntimeException if none is defined
   */
  public IdSource getIdSource(final String entityID) {
    if (!entityInfo.containsKey(entityID))
      throw new RuntimeException("No id source defined for entity: " + entityID);

    return entityInfo.get(entityID).idSource;
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
  public List<Property> getDatabaseProperties(final String entityID,
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
  public Collection<Property> getVisibleProperties(final String entityID) {
    if (!entityInfo.containsKey(entityID))
      throw new RuntimeException("Undefined entity: " + entityID);

    return entityInfo.get(entityID).getVisibleProperties();
  }

  /**
   * @param entityID the entity ID
   * @return a list containing the visible (non-hidden) properties
   * in the entity identified by <code>entityID</code>
   * @throws RuntimeException if no visible properties are defined for the given entity
   */
  public List<Property> getVisiblePropertyList(final String entityID) {
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
  public Property getProperty(final String entityID, final String propertyID) {
    final Property ret = getProperties(entityID).get(propertyID);
    if (ret == null)
      throw new RuntimeException("Property '" + propertyID + "' not found in entity: " + entityID);

    return ret;
  }

  /**
   * @param entityID the entity ID
   * @param propertyID the property ID
   * @return true if the entity identified by <code>entityID</code> contains
   * a property identified by <code>propertyId</code>
   */
  public boolean hasProperty(final String entityID, final String propertyID) {
    return getProperties(entityID).get(propertyID) != null;
  }

  /**
   * @param entityID the entity ID
   * @param propertyIDs the property IDs of the properties to retrieve
   * @return a list containing the properties identified by <code>propertyIDs</code>, found in
   * the entity identified by <code>entityID</code>
   */
  public List<Property> getProperties(final String entityID, final String... propertyIDs) {
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
  public Collection<Property> getProperties(final String entityID, final boolean includeHidden) {
    return includeHidden ? getProperties(entityID).values() : getVisibleProperties(entityID);
  }

  /**
   * @param entityID the entity ID
   * @return a collection containing all database properties found in the entity identified by <code>entityID</code>,
   * that is, properties that map to database columns
   */
  public Collection<Property> getDatabaseProperties(final String entityID) {
    if (!entityInfo.containsKey(entityID))
      throw new RuntimeException("Undefined entity: " + entityID);

    return entityInfo.get(entityID).getDatabaseProperties();
  }

  /**
   * @param entityID the entity ID
   * @return a collection containing all the foreign key properties found in the entity
   * identified by <code>entityID</code>
   */
  public Collection<Property.ForeignKeyProperty> getForeignKeyProperties(final String entityID) {
    if (!entityInfo.containsKey(entityID))
      throw new RuntimeException("Undefined entity: " + entityID);

    return entityInfo.get(entityID).getForeignKeyProperties();
  }

  /**
   * @param entityID the entity ID
   * @return true if the given entity contains denormalized properties
   */
  public boolean hasDenormalizedProperties(final String entityID) {
    if (!entityInfo.containsKey(entityID))
      throw new RuntimeException("Undefined entity: " + entityID);

    return entityInfo.get(entityID).hasDenormalizedProperties();
  }

  /**
   * @param entityID the entity ID
   * @param propertyOwnerEntityID the entity ID of the actual property owner entity
   * @return a collection containing all denormalized properties of the entity identified by <code>entityID</code>
   * which source is the entity identified by <code>propertyOwnerEntityID</code>
   */
  public Collection<Property.DenormalizedProperty> getDenormalizedProperties(final String entityID,
                                                                             final String propertyOwnerEntityID) {
    if (!entityInfo.containsKey(entityID))
      throw new RuntimeException("Undefined entity: " + entityID);

    return entityInfo.get(entityID).getDenormalizedProperties(propertyOwnerEntityID);
  }

  /**
   * Returns the foreign key properties referencing entities of the given type
   * @param entityID the ID of the entity from which to retrieve the foreign key properties
   * @param referenceEntityID the ID of the reference entity
   * @return a List containing the properties, an empty list is returned in case no properties fit the criteria
   */
  public List<Property.ForeignKeyProperty> getForeignKeyProperties(final String entityID, final String referenceEntityID) {
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
  public Property.ForeignKeyProperty getForeignKeyProperty(final String entityID, final String propertyID) {
    for (final Property.ForeignKeyProperty foreignKeyProperty : getForeignKeyProperties(entityID))
      if (foreignKeyProperty.propertyID.equals(propertyID))
        return foreignKeyProperty;

    throw new RuntimeException("Entity property with id: " + propertyID + " not found in entity of type: " + entityID);
  }

  /**
   * @param entityID the entity ID
   * @return a map containing the properties the given entity is comprised of, mapped to their respective propertyIDs
   */
  public Map<String, Property> getProperties(final String entityID) {
    if (!entityInfo.containsKey(entityID))
      throw new RuntimeException("Undefined entity: " + entityID);

    return entityInfo.get(entityID).properties;
  }

  /**
   * @param entityID the entity ID
   * @return the name of the primary key value source for the given entity
   * @throws RuntimeException in case no id source name is specified
   */
  public String getEntityIdSource(final String entityID) {
    if (!entityInfo.containsKey(entityID))
      throw new RuntimeException("Undefined entity: " + entityID);

    return entityInfo.get(entityID).idValueSource;
  }

  /**
   * @param entityGroup a group of related entities (from the same domain fx), for which
   * we can deduce that if one has been initialized all have.
   * @return true if any one of the entities in the group have already initialized, hmm?
   */
  public boolean contains(final Collection<String> entityGroup) {
    return entityInfo.containsKey(entityGroup.iterator().next());
  }

  /**
   * Initializes a entity type, identified by the string <code>entityID</code> and based on the table
   * specified by that same string
   * @param entityID the full table name of the entity being specified, serves as the entity ID
   * @param orderByColumns the default order by clause used when selecting multiple entities of this type
   * @param initialPropertyDefinitions the properties comprising this entity
   */
  public void initialize(final String entityID, final String orderByColumns,
                         final Property... initialPropertyDefinitions) {
    initialize(entityID, IdSource.AUTO_INCREMENT, null, orderByColumns, initialPropertyDefinitions);
  }

  /**
   * Initializes a entity type, identified by the string <code>entityID</code> and based on the table
   * specified by that same string
   * @param entityID the full table name of the entity being specified, serves as the entity ID
   * @param initialPropertyDefinitions the properties comprising this entity
   */
  public void initialize(final String entityID,
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
  public void initialize(final String entityID, final IdSource idSource,
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
  public void initialize(final String entityID, final IdSource idSource,
                         final String entityIdSource, final Property... initialPropertyDefinitions) {
    initialize(entityID, idSource, entityIdSource, null, initialPropertyDefinitions);
  }

  /**
   * Initializes a entity type, identified by the string <code>entityID</code> and based on the table
   * specified by that same string
   * @param entityID the full table name of the entity being specified, serves as the entity ID
   * @param idSource specifies the primary key value source for the table this entity is based on
   * @param entityIdSource the name of the primary key value source, such as a sequence name
   * @param orderByColumns the default order by clause used when selecting multiple entities of this type
   * @param initialPropertyDefinitions the properties comprising this entity
   */
  public void initialize(final String entityID, final IdSource idSource,
                         final String entityIdSource, final String orderByColumns,
                         final Property... initialPropertyDefinitions) {
    initialize(entityID, null, idSource, entityIdSource, orderByColumns, null, false, initialPropertyDefinitions);
  }

  /**
   * Initializes a entity type, identified by the string <code>entityID</code> and based on the table
   * specified by that same string
   * @param entityID the full table name of the entity being specified, serves as the entity ID
   * @param idSource specifies the primary key value source for the table this entity is based on
   * @param entityIdSource the name of the primary key value source, such as a sequence name
   * @param orderByColumns the default order by clause used when selecting multiple entities of this type
   * @param dbSelectTableName the name of the table or view from which entities of this type should be selected,
   * in case it differs from the table used to insert/update/delete entities
   * @param initialPropertyDefinitions the properties comprising this entity
   */
  public void initialize(final String entityID, final IdSource idSource,
                         final String entityIdSource, final String orderByColumns, final String dbSelectTableName,
                         final Property... initialPropertyDefinitions) {
    initialize(entityID, idSource, entityIdSource, orderByColumns, dbSelectTableName,
            false, initialPropertyDefinitions);
  }

  /**
   * Initializes a entity type, identified by the string <code>entityID</code> and based on the table
   * specified by that same string
   * @param entityID the full table name of the entity being specified, serves as the entity ID
   * @param idSource specifies the primary key value source for the table this entity is based on
   * @param entityIdSource the name of the primary key value source, such as a sequence name
   * @param orderByColumns the default order by clause used when selecting multiple entities of this type
   * @param dbSelectTableName the name of the table or view from which entities of this type should be selected,
   * in case it differs from the table used to insert/update/delete entities
   * @param isReadOnly true if entities of this type should be regarded as read-only
   * @param initialPropertyDefinitions the properties comprising this entity
   */
  public void initialize(final String entityID, final IdSource idSource,
                         final String entityIdSource, final String orderByColumns,
                         final String dbSelectTableName, final boolean isReadOnly,
                         final Property... initialPropertyDefinitions) {
    initialize(entityID, null, idSource, entityIdSource, orderByColumns, dbSelectTableName,
            isReadOnly, false, initialPropertyDefinitions);
  }

  /**
   * Initializes a entity type, identified by the string <code>entityID</code> and based on the table
   * specified by that same string
   * @param entityID the full table name of the entity being specified, serves as the entity ID
   * @param idSource specifies the primary key value source for the table this entity is based on
   * @param entityIdSource the name of the primary key value source, such as a sequence name
   * @param orderByColumns the default order by clause used when selecting multiple entities of this type
   * @param dbSelectTableName the name of the table or view from which entities of this type should be selected,
   * in case it differs from the table used to insert/update/delete entities
   * @param isReadOnly true if entities of this type should be regarded as read-only
   * @param largeDataset true if the dataset this entity is based on will become large, mostly used
   * to judge if the dataset can be shown in a combo box or list component
   * @param initialPropertyDefinitions the properties comprising this entity
   */
  public void initialize(final String entityID, final IdSource idSource,
                         final String entityIdSource, final String orderByColumns,
                         final String dbSelectTableName, final boolean isReadOnly,
                         final boolean largeDataset, final Property... initialPropertyDefinitions) {
    initialize(entityID, null, idSource, entityIdSource, orderByColumns, dbSelectTableName,
            isReadOnly, largeDataset, initialPropertyDefinitions);
  }

  /**
   * Initializes a entity type, identified by the string <code>entityID</code> and based on the table
   * specified by that same string
   * @param entityID the full table name of the entity being specified, serves as the entity ID
   * @param dbTableName the name of the table used to insert/update/delete entities of this type
   * @param idSource specifies the primary key value source for the table this entity is based on
   * @param entityIdSource the name of the primary key value source, such as a sequence name
   * @param orderByColumns the default order by clause used when selecting multiple entities of this type
   * @param dbSelectTableName the name of the table or view from which entities of this type should be selected,
   * in case it differs from the table used to insert/update/delete entities
   * @param isReadOnly true if entities of this type should be regarded as read-only
   * @param initialPropertyDefinitions the properties comprising this entity
   */
  public void initialize(final String entityID, final String dbTableName, final IdSource idSource,
                         final String entityIdSource, final String orderByColumns,
                         final String dbSelectTableName, final boolean isReadOnly,
                         final Property... initialPropertyDefinitions) {
    initialize(entityID, dbTableName, idSource, entityIdSource, orderByColumns, dbSelectTableName,
            isReadOnly, false, initialPropertyDefinitions);
  }

  /**
   * Initializes a entity type, identified by the string <code>entityID</code> and based on the table
   * specified by that same string
   * @param entityID the full table name of the entity being specified, serves as the entity ID
   * @param dbTableName the name of the table used to insert/update/delete entities of this type
   * @param idSource specifies the primary key value source for the table this entity is based on
   * @param entityIdSource the name of the primary key value source, such as a sequence name
   * @param orderByColumns the default order by clause used when selecting multiple entities of this type
   * @param dbSelectTableName the name of the table or view from which entities of this type should be selected,
   * in case it differs from the table used to insert/update/delete entities
   * @param isReadOnly true if entities of this type should be regarded as read-only
   * @param largeDataset true if the dataset this entity is based on will become large, mostly used
   * to judge if the dataset can be shown in a combo box or list component
   * @param initialPropertyDefinitions the properties comprising this entity
   */
  public void initialize(final String entityID, final String dbTableName, final IdSource idSource,
                         final String entityIdSource, final String orderByColumns,
                         final String dbSelectTableName, final boolean isReadOnly,
                         final boolean largeDataset, final Property... initialPropertyDefinitions) {
    if (entityInfo.containsKey(entityID))
      throw new IllegalArgumentException("Entity with ID '" + entityID + "' has already been initialized!");

    final EntityDefinition info = new EntityDefinition(entityID, initialPropertyDefinitions, dbTableName == null ? entityID : dbTableName.toLowerCase(),
            dbSelectTableName == null ? (dbTableName == null ? entityID : dbTableName.toLowerCase()) : dbSelectTableName.toLowerCase(),
            orderByColumns, idSource, (idSource == IdSource.SEQUENCE || idSource == IdSource.AUTO_INCREMENT) ?
                    (entityIdSource == null || entityIdSource.length() == 0 ? (entityID + "_seq") : entityIdSource) : null,
            isReadOnly, largeDataset);

    info.initialize();
    this.entityInfo.put(entityID, info);
  }

  /**
   * @return the IDs of all the entities defined in this repository
   */
  public Collection<String> getEntityIDs() {
    return entityInfo.keySet();
  }

  public String[] getInitializedEntities() {
    return entityInfo.keySet().toArray(new String[entityInfo.keySet().size()]);
  }

  public static class EntityDefinition implements Serializable {

    /**
     * The entityID
     */
    private final String entityID;
    /**
     * The properties
     */
    private final Map<String, Property> properties;
    /**
     * The name of the underlygin table
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
    private Map<String, String> propertyDescriptions;
    private String[] entitySearchPropertyIDs;

    private LinkedHashMap<String, Property> visibleProperties;
    private Map<Integer, Property> visiblePropertyIndexes;
    private LinkedHashMap<String, Property> databaseProperties;

    private Map<String, Property.ForeignKeyProperty> foreignKeyProperties;
    private Map<String, Collection<Property.DenormalizedProperty>> denormalizedProperties;
    private List<Property.PrimaryKeyProperty> primaryKeyProperties;

    private String entitySelectString;
    private List<String> primaryKeyColumnNames;

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

    public String[] getPrimaryKeyColumnNames() {
      return primaryKeyColumnNames.toArray(new String[primaryKeyColumnNames.size()]);
    }

    public Property getPropertyAtViewIndex(final int idx) {
      return visiblePropertyIndexes.get(idx);
    }

    public String getSelectString() {
      return entitySelectString;
    }

    public Collection<Property> getVisibleProperties() {
      return visibleProperties.values();
    }

    public Collection<Property> getDatabaseProperties() {
      return databaseProperties.values();
    }

    public Collection<Property.ForeignKeyProperty> getForeignKeyProperties() {
      return foreignKeyProperties != null ? foreignKeyProperties.values() : new ArrayList<Property.ForeignKeyProperty>(0);
    }

    public boolean hasDenormalizedProperties() {
      return denormalizedProperties.size() > 0 && denormalizedProperties.containsKey(entityID);
    }

    public Collection<Property.DenormalizedProperty> getDenormalizedProperties(final String propertyOwnerEntityID) {
      return denormalizedProperties != null ? denormalizedProperties.get(propertyOwnerEntityID) : null;
    }

    public void initialize() {
      visibleProperties = new LinkedHashMap<String, Property>(properties.size());
      visiblePropertyIndexes = new HashMap<Integer, Property>(properties.size());
      databaseProperties = new LinkedHashMap<String, Property>(properties.size());
      foreignKeyProperties = new HashMap<String, Property.ForeignKeyProperty>(properties.size());
      denormalizedProperties = new HashMap<String, Collection<Property.DenormalizedProperty>>(properties.size());
      primaryKeyProperties = new ArrayList<Property.PrimaryKeyProperty>(properties.size());
      primaryKeyColumnNames = new ArrayList<String>();

      for (final Property property : properties.values()) {
        if (property instanceof Property.PrimaryKeyProperty) {
          primaryKeyProperties.add((Property.PrimaryKeyProperty) property);
          primaryKeyColumnNames.add(property.propertyID);
        }
        if (property instanceof Property.ForeignKeyProperty)
          foreignKeyProperties.put(property.propertyID, (Property.ForeignKeyProperty) property);
        if (property.isDatabaseProperty())
          databaseProperties.put(property.propertyID, property);
        if (property instanceof Property.DenormalizedProperty) {
          final Property.DenormalizedProperty denormalizedProperty = (Property.DenormalizedProperty) property;
          Collection<Property.DenormalizedProperty> denormProps = denormalizedProperties.get(denormalizedProperty.foreignKeyPropertyID);
          if (denormProps == null)
            denormalizedProperties.put(denormalizedProperty.foreignKeyPropertyID, denormProps = new ArrayList<Property.DenormalizedProperty>());
          denormProps.add(denormalizedProperty);
        }
        if (!property.isHidden()) {
          visibleProperties.put(property.propertyID, property);
          visiblePropertyIndexes.put(visiblePropertyIndexes.size(), property);
        }
      }

      final String[] selectColumnNames = initSelectColumnNames();
      for (int idx = 0; idx < selectColumnNames.length; idx++)
        properties.get(selectColumnNames[idx]).setSelectIndex(idx+1);

      this.entitySelectString = getSelectColumnsString();
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

    private String getSelectColumnsString() {
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