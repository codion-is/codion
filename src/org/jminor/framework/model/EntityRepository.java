/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

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

  private final Map<String, Map<String, Property>> properties = new HashMap<String, Map<String, Property>>();

  /**
   * Maps the name of the table each entity type is based on to its entityID
   */
  private final Map<String, String> entityTableNames = new HashMap<String, String>();

  /**
   * Maps the table (view, query) from which to select the entity to its entityID.
   * Used if it differs from the one used for inserts/updates
   */
  private final Map<String, String> entitySelectTableNames = new HashMap<String, String>();

  /**
   * Holds the order by string for each entity type
   */
  private final Map<String, String> entityOrderByColumns = new HashMap<String, String>();

  /**
   * Maps the source of the entities ids (primary key) to each entity type, i.e. sequence names
   */
  private final Map<String, String> entityIdSources = new HashMap<String, String>();

  /**
   * Maps the IdSource to each entities entityID
   * @see IdSource
   */
  private final Map<String, IdSource> idSources = new HashMap<String, IdSource>();

  /**
   * Maps the readOnly value to each entities entityID
   */
  private final Map<String, Boolean> readOnly = new HashMap<String, Boolean>();

  /**
   * Maps the name of the create date columns (if any) in the underlying table to each entities entityID
   */
  private final Map<String, String> createDateColumns = new HashMap<String, String>();

  private final Map<String, Map<String, String>> propertyDescriptions = new HashMap<String, Map<String, String>>();
  private final Map<String, String[]> entitySearchPropertyIDs = new HashMap<String, String[]>();

  private transient Map<String, LinkedHashMap<String, Property>> visibleProperties;
  private transient Map<String, Map<Integer, Property>> visiblePropertyIndexes;
  private transient Map<String, LinkedHashMap<String, Property>> databaseProperties;

  private transient Map<String, Map<String, Property.EntityProperty>> entityProperties;
  private transient Map<String, Map<String, Collection<Property.DenormalizedProperty>>> denormalizedProperties;
  private transient Map<String, List<Property.PrimaryKeyProperty>> primaryKeyProperties;

  private transient Map<String, String> entitySelectStrings;
  private transient Map<String, String[]> primaryKeyColumnNames;

  private static EntityRepository instance;

  private EntityRepository() {}

  public static EntityRepository get() {
    if (instance == null)
      instance = new EntityRepository();

    return instance;
  }

  public void add(final EntityRepository repository) {
    initContainers();
    instance.readOnly.putAll(repository.readOnly);
    instance.properties.putAll(repository.properties);
    instance.visibleProperties.putAll(repository.visibleProperties);
    instance.visiblePropertyIndexes.putAll(repository.visiblePropertyIndexes);
    instance.primaryKeyProperties.putAll(repository.primaryKeyProperties);
    instance.primaryKeyColumnNames.putAll(repository.primaryKeyColumnNames);
    instance.databaseProperties.putAll(repository.databaseProperties);
    instance.idSources.putAll(repository.idSources);
    instance.entityOrderByColumns.putAll(repository.entityOrderByColumns);
    instance.entityProperties.putAll(repository.entityProperties);
    instance.entitySelectStrings.putAll(repository.entitySelectStrings);
    instance.entityTableNames.putAll(repository.entityTableNames);
    instance.entitySelectTableNames.putAll(repository.entitySelectTableNames);
    instance.entityIdSources.putAll(repository.entityIdSources);
    instance.createDateColumns.putAll(repository.createDateColumns);
  }

  /**
   * @param entityID the entity ID
   * @param propertyID the property ID
   * @param description a string describing the property
   */
  public void setPropertyDescription(final String entityID, final String propertyID, final String description) {
    if (!propertyDescriptions.containsKey(entityID))
      propertyDescriptions.put(entityID, new HashMap<String, String>());

    propertyDescriptions.get(entityID).put(propertyID, description);
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
    if (!propertyDescriptions.containsKey(entityID) || !propertyDescriptions.get(entityID).containsKey(propertyID))
      return null;

    return propertyDescriptions.get(entityID).get(propertyID);
  }

  /**
   * @param entityID the entity ID
   * @param searchPropertyIDs the IDs of the properties to use as default search properties for
   * entities identified by <code>entityID</code>, these must be STRING properties
   * @throws RuntimeException in case of a non-string property ID
   */
  public void setEntitySearchProperties(final String entityID, final String... searchPropertyIDs) {
    for (final String propertyID : searchPropertyIDs)
      if (getProperty(entityID, propertyID).propertyType != Type.STRING)
        throw new RuntimeException("Entity search property must be of type String: " + getProperty(entityID, propertyID));

    entitySearchPropertyIDs.put(entityID, searchPropertyIDs);
  }

  /**
   * @param entityID the entity ID
   * @return a String array containing the IDs of the properties used as default search properties
   * for entities identified by <code>entityID</code>
   */
  public String[] getEntitySearchPropertyIDs(final String entityID) {
    return entitySearchPropertyIDs.get(entityID);
  }

  /**
   * @param entityID the entity ID
   * @return a list containing the primary key properties of the entity identified by <code>entityID</code>
   */
  public List<Property.PrimaryKeyProperty> getPrimaryKeyProperties(final String entityID) {
    final List<Property.PrimaryKeyProperty> ret = primaryKeyProperties.get(entityID);
    if (ret == null)
      throw new RuntimeException("No primary key properties defined for entity: " + entityID);

    return ret;
  }

  /**
   * @param entityID the entity ID
   * @return a String array containing the primary column names of the entity identified by <code>entityID</code>
   */
  public String[] getPrimaryKeyColumnNames(final String entityID) {
    final String[] ret = primaryKeyColumnNames.get(entityID);
    if (ret == null)
      throw new RuntimeException("No primary key column names defined for entity: " + entityID);

    return ret;
  }

  /**
   * @param entityID the entity ID
   * @param idx the index of the property to retrieve
   * @return the property found at index <code>idx</code> in the entity identified by <code>entityID</code>,
   * null if no property is at the given index
   * @throws RuntimeException if property indexes are not defined for the given entity
   */
  public Property getPropertyAtViewIndex(final String entityID, final int idx) {
    final Map<Integer, Property> indexes = visiblePropertyIndexes.get(entityID);
    if (indexes == null)
      throw new RuntimeException("No property indexes defined for entity: " + entityID);

    return indexes.get(idx);
  }

  /**
   * @param entityID the entity ID
   * @return true if the entity identified by <code>entityID</code> is read only
   * @throws RuntimeException if the read only value is undefined
   */
  public boolean isReadOnly(final String entityID) {
    final Boolean ret = readOnly.get(entityID);
    if (ret == null)
      throw new RuntimeException("Read only value not defined for entity: " + entityID);

    return ret;
  }

  /**
   * @param entityID the entity ID
   * @return a comma seperated list of columns to use in the order by clause
   */
  public String getOrderByColumnNames(final String entityID) {
    final String ret = entityOrderByColumns.get(entityID);
    if (ret == null)
      throw new RuntimeException("No order by columns defined for entity: " + entityID);

    return ret;
  }

  /**
   * @param entityID the entity ID
   * @return the name of the table used to select entities identified by <code>entityID</code>
   * @throws RuntimeException if none is defined
   */
  public String getSelectTableName(final String entityID) {
    final String ret = entitySelectTableNames.get(entityID);
    if (ret == null)
      throw new RuntimeException("No select table name defined for entity: " + entityID);

    return ret;
  }

  /**
   * @param entityID the entity ID
   * @return the name of the table on which entities identified by <code>entityID</code> are based
   * @throws RuntimeException if none is defined
   */
  public String getTableName(final String entityID) {
    final String ret = entityTableNames.get(entityID);
    if (ret == null)
      throw new RuntimeException("No table name defined for entity: " + entityID);

    return ret;
  }

  /**
   * @param entityID the entity ID
   * @return the query string used to select entities identified by <code>entityID</code>
   * @throws RuntimeException if none is defined
   */
  public String getSelectString(final String entityID) {
    final String ret = entitySelectStrings.get(entityID);
    if (ret == null)
      throw new RuntimeException("No select string defined for entity: " + entityID);

    return ret;
  }

  /**
   * @param entityID the entity ID
   * @return the IdSource of the entity identified by <code>entityID</code>
   * @throws RuntimeException if none is defined
   */
  public IdSource getIdSource(final String entityID) {
    final IdSource ret = idSources.get(entityID);
    if (ret == null)
      throw new RuntimeException("No id source defined for entity: " + entityID);

    return ret;
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
    final Collection<Property> ret = visibleProperties.get(entityID).values();
    if (ret == null)
      throw new RuntimeException("No visible properties defined for entity: " + entityID);

    return ret;
  }

  /**
   * @param entityID the entity ID
   * @return a list containing the visible (non-hidden) properties
   * in the entity identified by <code>entityID</code>
   * @throws RuntimeException if no visible properties are defined for the given entity
   */
  public List<Property> getVisiblePropertyList(final String entityID) {
    if (!visibleProperties.containsKey(entityID))
      throw new RuntimeException("No visible properties defined for entity: " + entityID);

    final List<Property> ret = new ArrayList<Property>();
    for (final Property property : visibleProperties.get(entityID).values())
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
    final Collection<Property> ret = databaseProperties.get(entityID).values();
    if (ret == null)
      throw new RuntimeException("No database properties defined for entity: " + entityID);

    return ret;
  }

  /**
   * @param entityID the entity ID
   * @return a collection containing all the reference properties (entity properties) found in the entity
   * identified by <code>entityID</code>
   */
  public Collection<Property.EntityProperty> getEntityProperties(final String entityID) {
    return entityProperties.containsKey(entityID) ?
            entityProperties.get(entityID).values() : new ArrayList<Property.EntityProperty>(0);
  }

  /**
   * @param entityID the entity ID
   * @return true if the given entity contains denormalized properties
   */
  public boolean hasDenormalizedProperties(final String entityID) {
    return denormalizedProperties.size() > 0 && denormalizedProperties.containsKey(entityID);
  }

  /**
   * @param entityID the entity ID
   * @param propertyOwnerEntityID the entity ID of the actual property owner entity
   * @return a collection containing all denormalized properties of the entity identified by <code>entityID</code>
   * which source is the entity identified by <code>propertyOwnerEntityID</code>
   */
  public Collection<Property.DenormalizedProperty> getDenormalizedProperties(final String entityID,
                                                                             final String propertyOwnerEntityID) {
    if (denormalizedProperties.containsKey(entityID))
      return denormalizedProperties.get(entityID).get(propertyOwnerEntityID);

    return null;
  }

  /**
   * Returns the properties referencing entities of the given type
   * @param entityID the ID of the entity from which to retrieve the reference properties
   * @param referenceEntityID the ID of the reference entity
   * @return a List containing the properties, an empty list is returned in case no properties fit the criteria
   */
  public List<Property.EntityProperty> getEntityProperties(final String entityID, final String referenceEntityID) {
    final List<Property.EntityProperty > ret = new ArrayList<Property.EntityProperty>();
    for (final Property.EntityProperty property : getEntityProperties(entityID))
      if (property.referenceEntityID.equals(referenceEntityID))
        ret.add(property);

    return ret;
  }

  /**
   * @param entityID the entity ID
   * @param propertyID the property ID
   * @return the Property.EntityProperty with the given propertyID
   * @throws RuntimeException in case no such property exists
   */
  public Property.EntityProperty getEntityProperty(final String entityID, final String propertyID) {
    for (final Property.EntityProperty entityProperty : getEntityProperties(entityID))
      if (entityProperty.propertyID.equals(propertyID))
        return entityProperty;

    throw new RuntimeException("Entity property with id: " + propertyID + " not found in entity of type: " + entityID);
  }

  /**
   * @param entityID the entity ID
   * @return a map containing the properties the given entity is comprised of, mapped to their respective propertyIDs
   */
  public Map<String, Property> getProperties(final String entityID) {
    final Map<String, Property> ret = properties.get(entityID);
    if (ret == null)
      throw new RuntimeException("No properties defined for entity: " + entityID);

    return ret;
  }

  /**
   * @param entityID the entity ID
   * @return the name of the primary key value source for the given entity
   * @throws RuntimeException in case no id source name is specified
   */
  public String getEntityIdSource(final String entityID) {
    final String idSource = entityIdSources.get(entityID);
    if (idSource == null)
      throw new RuntimeException("No ID source defined for entity: " + entityID);

    return idSource;
  }

  /**
   * @param entityGroup a group of related entities (from the same domain fx), for which
   * we can deduce that if one has been initialized all have.
   * @return true if any one of the entities in the group have already initialzed, hmm?
   */
  public boolean contains(final Collection<String> entityGroup) {
    for (final String entityID : entityGroup)
      if (readOnly.containsKey(entityID))
        return true;

    return false;
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
            isReadOnly, initialPropertyDefinitions);
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
    if (this.readOnly.containsKey(entityID))
      throw new IllegalArgumentException("Entity with ID '" + entityID + "' has already been initialized!");
    this.readOnly.put(entityID, isReadOnly);
    this.entityTableNames.put(entityID, dbTableName == null ? entityID : dbTableName.toLowerCase());
    this.entitySelectTableNames.put(entityID,
            dbSelectTableName == null ? this.entityTableNames.get(entityID) : dbSelectTableName.toLowerCase());
    this.idSources.put(entityID, idSource);
    this.entityOrderByColumns.put(entityID, orderByColumns == null ? "" : orderByColumns);
    this.entityIdSources.put(entityID,
            (idSource == IdSource.SEQUENCE || idSource == IdSource.AUTO_INCREMENT) ?
                    (entityIdSource == null || entityIdSource.length() == 0 ? (entityID + "_seq") : entityIdSource) : null);

    final HashMap<String, Property> properties = new LinkedHashMap<String, Property>(initialPropertyDefinitions.length);
    for (final Property property : initialPropertyDefinitions) {
      if (properties.containsKey(property.propertyID))
        throw new IllegalArgumentException("Property with ID " + property.propertyID
                + (property.getCaption() != null ? " (caption: " + property.getCaption() + ")" : "")
                + " has already been defined as: " + properties.get(property.propertyID) + " in entity: " + entityID);
      properties.put(property.propertyID, property);
      if (property instanceof Property.EntityProperty) {
        for (final Property referenceProperty : ((Property.EntityProperty) property).referenceProperties) {
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

    this.properties.put(entityID, properties);

    initialize(entityID);
  }

  /**
   * @return the IDs of all the entities defined in this repository
   */
  public Collection<String> getEntityIDs() {
    return properties.keySet();
  }

  /**
   * Initializes all the entities specified in this repository
   * @return this EntityRepository instance
   */
  public EntityRepository initializeAll() {
    for (final String entityID : properties.keySet())
      initialize(entityID);

    return this;
  }

  /**
   * @param entityID the entityID
   * @return true if the table on which the entity identified by <code>entityID</code> is based
   * contains a create date column
   */
  public boolean hasCreateDateColumn(final String entityID) {
    return createDateColumns.containsKey(entityID);
  }

  /**
   * Specify the name of the create date column for the given entity
   * @param entityID the entityID of the entity for which to specify the create date column name
   * @param createDateColumnName the name of the create date column
   */
  public void setCreateDateColumn(final String entityID, final String createDateColumnName) {
    if (createDateColumnName == null || createDateColumnName.length() == 0)
      createDateColumns.remove(entityID);
    else
      createDateColumns.put(entityID, createDateColumnName);
  }

  /**
   * @param entityID the entityID
   * @return the create date column name of the table on which the entity identified
   * by <code>entityID</code> is based on, null if none is specified
   */
  public String getCreateDateColumn(final String entityID) {
    return createDateColumns.get(entityID);
  }

  public String[] getInitializedEntities() {
    return properties.keySet().toArray(new String[properties.keySet().size()]);
  }

  private void initialize(final String entityID) {
    final Map<String, Property> initialPropertyDefinitions = properties.get(entityID);

    final LinkedHashMap<String, Property> visibleProperties =
            new LinkedHashMap<String, Property>(initialPropertyDefinitions.size());
    final Map<Integer, Property> visiblePropertyIndexes =
            new HashMap<Integer, Property>(initialPropertyDefinitions.size());
    final LinkedHashMap<String, Property> databaseProperties =
            new LinkedHashMap<String, Property>(initialPropertyDefinitions.size());
    final Map<String, Property.EntityProperty> entityProperties =
            new HashMap<String, Property.EntityProperty>(initialPropertyDefinitions.size());
    final Map<String, Collection<Property.DenormalizedProperty>> denormalizedProperties =
            new HashMap<String, Collection<Property.DenormalizedProperty>>(initialPropertyDefinitions.size());
    final List<Property.PrimaryKeyProperty > primaryKeyProperties =
            new ArrayList<Property.PrimaryKeyProperty>(initialPropertyDefinitions.size());
    final List<String> primaryKeyColumnNames = new ArrayList<String>();

    for (final Property property : initialPropertyDefinitions.values())
      addProperty(visibleProperties, visiblePropertyIndexes, databaseProperties,
              entityProperties, denormalizedProperties, primaryKeyProperties, primaryKeyColumnNames, property);

    initContainers();

    this.databaseProperties.put(entityID, databaseProperties);
    this.visibleProperties.put(entityID, visibleProperties);
    this.visiblePropertyIndexes.put(entityID, visiblePropertyIndexes);
    this.entityProperties.put(entityID, entityProperties);
    if (denormalizedProperties.size() > 0)
      this.denormalizedProperties.put(entityID, denormalizedProperties);
    this.primaryKeyProperties.put(entityID, primaryKeyProperties);
    this.primaryKeyColumnNames.put(entityID, primaryKeyColumnNames.toArray(new String[primaryKeyColumnNames.size()]));

    final String[] selectColumnNames = initSelectColumnNames(entityID);
    for (int idx = 0; idx < selectColumnNames.length; idx++)
      initialPropertyDefinitions.get(selectColumnNames[idx]).setSelectIndex(idx+1);

    this.entitySelectStrings.put(entityID, getSelectColumnsString(entityID));
  }

  private void addProperty(final Map<String, Property> visibleProperties,
                           final Map<Integer, Property> visiblePropertyIndexes,
                           final Map<String, Property> databaseProperties,
                           final Map<String, Property.EntityProperty> entityProperties,
                           final Map<String, Collection<Property.DenormalizedProperty>> denormalizedProperties,
                           final List<Property.PrimaryKeyProperty> primaryKeyProperties,
                           final List<String> primaryKeyColumnNames, final Property property) {
    if (property instanceof Property.PrimaryKeyProperty) {
      primaryKeyProperties.add((Property.PrimaryKeyProperty) property);
      primaryKeyColumnNames.add(property.propertyID);
    }
    if (property instanceof Property.EntityProperty)
      entityProperties.put(property.propertyID, (Property.EntityProperty) property);
    if (property.isDatabaseProperty())
      databaseProperties.put(property.propertyID, property);
    if (property instanceof Property.DenormalizedProperty) {
      final Property.DenormalizedProperty denormalizedProperty = (Property.DenormalizedProperty) property;
      Collection<Property.DenormalizedProperty> denormProps = denormalizedProperties.get(denormalizedProperty.ownerEntityID);
      if (denormProps == null)
        denormalizedProperties.put(denormalizedProperty.ownerEntityID, denormProps = new ArrayList<Property.DenormalizedProperty>());
      denormProps.add(denormalizedProperty);
    }
    if (!property.isHidden()) {
      visibleProperties.put(property.propertyID, property);
      visiblePropertyIndexes.put(visiblePropertyIndexes.size(), property);
    }
  }

  /**
   * @param entityID the entity class
   * @return the column names used to select an entity of this type from the database
   */
  private String[] initSelectColumnNames(final String entityID) {
    final Collection<Property> dbProperties = getDatabaseProperties(entityID);
    final List<String> ret = new ArrayList<String>(dbProperties.size());
    for (final Property property : dbProperties)
      if (!(property instanceof Property.EntityProperty))
        ret.add(property.propertyID);

    return ret.toArray(new String[ret.size()]);
  }

  private String getSelectColumnsString(final String entityID) {
    final Collection<Property> dbProperties = getDatabaseProperties(entityID);
    final List<Property> selectProperties = new ArrayList<Property>(dbProperties.size());
    for (final Property property : dbProperties)
      if (!(property instanceof Property.EntityProperty))
        selectProperties.add(property);

    final StringBuffer ret = new StringBuffer();
    int i = 0;
    for (final Property property : selectProperties) {
      if (property instanceof Property.SubQueryProperty)
        ret.append("(").append(((Property.SubQueryProperty)property).getSubQuery()).append(
                ") ").append(property.propertyID);
      else
        ret.append(property.propertyID);

      if (i++ < selectProperties.size() - 1)
        ret.append(", ");
    }

    return ret.toString();
  }

  private void initContainers() {
    if (this.databaseProperties == null)
      this.databaseProperties = new HashMap<String, LinkedHashMap<String, Property>>();
    if (this.visibleProperties == null)
      this.visibleProperties = new HashMap<String, LinkedHashMap<String, Property>>();
    if (this.visiblePropertyIndexes == null)
      this.visiblePropertyIndexes = new HashMap<String, Map<Integer, Property>>();
    if (this.entityProperties == null)
      this.entityProperties = new HashMap<String, Map<String, Property.EntityProperty>>();
    if (this.denormalizedProperties == null)
      this.denormalizedProperties = new HashMap<String, Map<String, Collection<Property.DenormalizedProperty>>>();
    if (this.primaryKeyProperties == null)
      this.primaryKeyProperties = new HashMap<String, List<Property.PrimaryKeyProperty>>();
    if (this.primaryKeyColumnNames == null)
      this.primaryKeyColumnNames = new HashMap<String, String[]>();
    if (this.entitySelectStrings == null)
      this.entitySelectStrings = new HashMap<String, String>();
  }
}
