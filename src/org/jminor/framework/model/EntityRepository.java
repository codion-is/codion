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

  private transient Map<String, LinkedHashMap<String, Property>> visibleProperties;
  private transient Map<String, Map<Integer, Property>> visiblePropertyIndexes;
  private transient Map<String, LinkedHashMap<String, Property>> databaseProperties;

  private transient Map<String, Map<String, Property.EntityProperty>> entityProperties;
  private transient Map<String, Map<String, Collection<Property.DenormalizedProperty>>> denormalizedProperties;
  private transient Map<String, List<Property.PrimaryKeyProperty>> primaryKeyProperties;

  private transient Map<String, String> entitySelectStrings;
  private transient Map<String, String[]> primaryKeyColumnNames;

  private final Map<String, String> entityTableNames = new HashMap<String, String>();
  private final Map<String, String> entitySelectTableNames = new HashMap<String, String>();
  private final Map<String, String> entityOrderByColumns = new HashMap<String, String>();
  private final Map<String, String> entityIdSources = new HashMap<String, String>();
  private final Map<String, IdSource> idSources = new HashMap<String, IdSource>();
  private final Map<String, Boolean> readOnly = new HashMap<String, Boolean>();

  private transient final HashMap<String, EntityProxy> entityProxy = new HashMap<String, EntityProxy>();
  private transient EntityProxy defaultEntityProxy = new EntityProxy();

  private static EntityRepository instance;

  private EntityRepository() {}

  public static EntityRepository get() {
    if (instance == null)
      instance = new EntityRepository();

    return instance;
  }

  public void add(final EntityRepository ed) {
    initContainers();
    instance.readOnly.putAll(ed.readOnly);
    instance.properties.putAll(ed.properties);
    instance.visibleProperties.putAll(ed.visibleProperties);
    instance.visiblePropertyIndexes.putAll(ed.visiblePropertyIndexes);
    instance.primaryKeyProperties.putAll(ed.primaryKeyProperties);
    instance.primaryKeyColumnNames.putAll(ed.primaryKeyColumnNames);
    instance.databaseProperties.putAll(ed.databaseProperties);
    instance.idSources.putAll(ed.idSources);
    instance.entityOrderByColumns.putAll(ed.entityOrderByColumns);
    instance.entityProperties.putAll(ed.entityProperties);
    instance.entitySelectStrings.putAll(ed.entitySelectStrings);
    instance.entityTableNames.putAll(ed.entityTableNames);
    instance.entitySelectTableNames.putAll(ed.entitySelectTableNames);
    instance.entityIdSources.putAll(ed.entityIdSources);
  }

  public String[] getInitializedEntities() {
    return properties.keySet().toArray(new String[properties.keySet().size()]);
  }

  /**
   * @param object Value to set for property 'defaultEntityProxy'.
   */
  public void setDefaultEntityProxy(final EntityProxy object) {
    defaultEntityProxy = object;
  }

  public void addEntityProxy(final String entityID, final EntityProxy entityProxy) {
    this.entityProxy.put(entityID, entityProxy);
  }

  public EntityProxy getEntityProxy(final String entityID) {
    if (entityProxy.containsKey(entityID))
      return entityProxy.get(entityID);

    return defaultEntityProxy;
  }

  public String getStringValue(final String entityID, final Entity entity) {
    return new StringBuffer(entityID).append(": ").append(entity.getPrimaryKey()).toString();
  }

  public List<Property.PrimaryKeyProperty> getPrimaryKeyProperties(
          final String entityID) {
    final List<Property.PrimaryKeyProperty> ret = primaryKeyProperties.get(entityID);
    if (ret == null)
      throw new RuntimeException("No primary key properties defined for entity: " + entityID);

    return ret;
  }

  public String[] getPrimaryKeyColumnNames(final String entityID) {
    final String[] ret = primaryKeyColumnNames.get(entityID);
    if (ret == null)
      throw new RuntimeException("No primary key column names defined for entity: " + entityID);

    return ret;
  }

  public int getPropertyViewIndex(final String entityID, final String propertyID) {
    int idx = 0;
    for (final Property property: visibleProperties.get(entityID).values()) {
      if (property.propertyID.equals(propertyID))
        return idx;

      idx++;
    }

    return -1;
  }

  public Property getPropertyAtViewIndex(final String entityID, final int idx) {
    final Map<Integer, Property> indexes = visiblePropertyIndexes.get(entityID);
    if (indexes == null)
      throw new RuntimeException("No property indexes defined for entity: " + entityID);

    return indexes.get(idx);
  }

  public boolean isReadOnly(final String entityID) {
    final Boolean ret = readOnly.get(entityID);
    if (ret == null)
      throw new RuntimeException("Read only value not defined for entity: " + entityID);

    return ret;
  }

  /**
   * @param entityID the entity class
   * @return a comma seperated list of columns to use in the order by clause
   */
  public String getOrderByColumnNames(final String entityID) {
    final String ret = entityOrderByColumns.get(entityID);
    if (ret == null)
      throw new RuntimeException("No order by columns defined for entity: " + entityID);

    return ret;
  }

  public String getSelectTableName(final String entityID) {
    final String ret = entitySelectTableNames.get(entityID);
    if (ret == null)
      throw new RuntimeException("No select table name defined for entity: " + entityID);

    return ret;
  }

  public String getTableName(final String entityID) {
    final String ret = entityTableNames.get(entityID);
    if (ret == null)
      throw new RuntimeException("No table name defined for entity: " + entityID);

    return ret;
  }

  public String getSelectString(final String entityID) {
    final String ret = entitySelectStrings.get(entityID);
    if (ret == null)
      throw new RuntimeException("No select string defined for entity: " + entityID);

    return ret;
  }

  public IdSource getIdSource(final String entityID) {
    final IdSource ret = idSources.get(entityID);
    if (ret == null)
      throw new RuntimeException("No id source defined for entity: " + entityID);

    return ret;
  }

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
    if (includePrimaryKeyProperties) {
      for (final Property.PrimaryKeyProperty primaryKeyProperty : getPrimaryKeyProperties(entityID))
        propertyHashSet.add(primaryKeyProperty);
    }

    return new ArrayList<Property>(propertyHashSet);
  }

  public Collection<Property> getVisibleProperties(final String entityID) {
    final Collection<Property> ret = visibleProperties.get(entityID).values();
    if (ret == null)
      throw new RuntimeException("No visible properties defined for entity: " + entityID);

    return ret;
  }

  public Property getProperty(final String entityID, final Integer index) {
    final Property ret = visiblePropertyIndexes.get(entityID).get(index);
    if (ret == null)
      throw new RuntimeException("No property found at index " + index + " in entity: " + entityID);

    return ret;
  }

  public Property getProperty(final String entityID, final String propertyID) {
    final Property ret = getProperties(entityID).get(propertyID);
    if (ret == null)
      throw new RuntimeException("Property '" + propertyID + "' not found in entity: " + entityID);

    return ret;
  }

  public boolean hasProperty(final String entityID, final String propertyID) {
    return getProperties(entityID).get(propertyID) != null;
  }

  /**
   * @param entityID the class
   * @param includeHidden true if hidden propertyValues should be included in the result
   * @return an array of Property representing the propertyValues of this entity
   */
  public Collection<Property> getProperties(final String entityID, final boolean includeHidden) {
    if (includeHidden)
      return getProperties(entityID).values();
    else
      return getVisibleProperties(entityID);
  }

  public Collection<Property> getDatabaseProperties(final String entityID) {
    final Collection<Property> ret = databaseProperties.get(entityID).values();
    if (ret == null)
      throw new RuntimeException("No database properties defined for entity: " + entityID);

    return ret;
  }

  public Collection<Property.EntityProperty> getEntityProperties(final String entityID) {
    return entityProperties.containsKey(entityID) ?
            entityProperties.get(entityID).values() : new ArrayList<Property.EntityProperty>(0);
  }

  public boolean hasDenormalizedProperties(final String entityID) {
    return denormalizedProperties.size() > 0 && denormalizedProperties.containsKey(entityID);
  }

  public Collection<Property.DenormalizedProperty> getDenormalizedProperties(final String entityID,
                                                                             final String propertyOwnerEntityID) {
    if (denormalizedProperties.containsKey(entityID))
      return denormalizedProperties.get(entityID).get(propertyOwnerEntityID);

    return null;
  }


  public Property.EntityProperty getEntityProperty(final String entityID, final String referenceEntityID) {
    for (final Property.EntityProperty property : getEntityProperties(entityID))
      if (property.referenceEntityID.equals(referenceEntityID))
        return property;

    return null;
  }

  public Map<String, Property> getProperties(final String entityID) {
    final Map<String, Property> ret = properties.get(entityID);
    if (ret == null)
      throw new RuntimeException("No properties defined for entity: " + entityID);

    return ret;
  }

  public String getEntityIdSource(final String entityID) {
    final String idSource = entityIdSources.get(entityID);
    if (idSource == null)
      throw new RuntimeException("No ID source defined for entity: " + entityID);

    return idSource;
  }

  /**
   * @param initializedEntities
   * @return true if any one of the entities is already initialzed, hmm?
   */
  public boolean contains(final Collection<String> initializedEntities) {
    for (final String entityID : initializedEntities)
      if (readOnly.containsKey(entityID))
        return true;

    return false;
  }

  public void initialize(final String entityID, final String orderByColumns,
                         final Property... initialPropertyDefinitions) {
    initialize(entityID, IdSource.ID_AUTO_INCREMENT, null, orderByColumns, initialPropertyDefinitions);
  }

  public void initialize(final String entityID,
                         final Property... initialPropertyDefinitions) {
    initialize(entityID, IdSource.ID_AUTO_INCREMENT, initialPropertyDefinitions);
  }

  public void initialize(final String entityID, final IdSource idSource,
                         final Property... initialPropertyDefinitions) {
    initialize(entityID, idSource, null, initialPropertyDefinitions);
  }

  public void initialize(final String entityID, final IdSource idSource,
                         final String entityIdSource, final Property... initialPropertyDefinitions) {
    initialize(entityID, idSource, entityIdSource, null, initialPropertyDefinitions);
  }

  public void initialize(final String entityID, final IdSource idSource,
                         final String entityIdSource, final String orderByColumns,
                         final Property... initialPropertyDefinitions) {
    initialize(entityID, null, idSource, entityIdSource, orderByColumns, null, false, initialPropertyDefinitions);
  }

  public void initialize(final String entityID, final IdSource idSource,
                         final String entityIdSource, final String orderByColumns, final String dbSelectTableName,
                         final Property... initialPropertyDefinitions) {
    initialize(entityID, idSource, entityIdSource, orderByColumns, dbSelectTableName,
            false, initialPropertyDefinitions);
  }

  public void initialize(final String entityID, final IdSource idSource,
                         final String entityIdSource, final String orderByColumns,
                         final String dbSelectTableName, final boolean isReadOnly,
                         final Property... initialPropertyDefinitions) {
    initialize(entityID, null, idSource, entityIdSource, orderByColumns, dbSelectTableName,
            isReadOnly, initialPropertyDefinitions);
  }

  public void initialize(final String entityID, final String dbTableName, final IdSource idSource,
                         final String entityIdSource, final String orderByColumns,
                         final String dbSelectTableName, final boolean isReadOnly,
                         final Property... initialPropertyDefinitions) {
    this.readOnly.put(entityID, isReadOnly);
    this.entityTableNames.put(entityID, dbTableName == null ? entityID : dbTableName.toLowerCase());
    this.entitySelectTableNames.put(entityID,
            dbSelectTableName == null ? this.entityTableNames.get(entityID) : dbSelectTableName.toLowerCase());
    this.idSources.put(entityID, idSource);
    this.entityOrderByColumns.put(entityID, orderByColumns == null ? "" : orderByColumns);
    this.entityIdSources.put(entityID,
            (idSource == IdSource.ID_SEQUENCE || idSource == IdSource.ID_AUTO_INCREMENT) ?
                    (entityIdSource == null || entityIdSource.length() == 0 ? (entityID + "_seq") : entityIdSource) : null);

    final HashMap<String, Property> properties = new LinkedHashMap<String, Property>(initialPropertyDefinitions.length);
    for (final Property property : initialPropertyDefinitions) {
      if (properties.containsKey(property.propertyID))
        throw new IllegalArgumentException("Property with ID " + property.propertyID
                + " has already been defined as: " + properties.get(property.propertyID));
      properties.put(property.propertyID, property);
      if (property instanceof Property.EntityProperty) {
        for (final Property referenceProperty : ((Property.EntityProperty) property).referenceProperties) {
          if (!(referenceProperty instanceof Property.MirrorProperty))
            properties.put(referenceProperty.propertyID, referenceProperty);
        }
      }
    }

    this.properties.put(entityID, properties);

    initialize(entityID);
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

  public Collection<String> getEntityIDs() {
    return properties.keySet();
  }

  public void initializeAll() {
    for (final String entityID : properties.keySet())
      initialize(entityID);
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
    for (final Property property : dbProperties) {
      if (!(property instanceof Property.EntityProperty))
        ret.add(property.propertyID);
    }

    return ret.toArray(new String[ret.size()]);
  }

  private String getSelectColumnsString(final String entityID) {
    final Collection<Property> dbProperties = getDatabaseProperties(entityID);
    final List<Property> selectProperties = new ArrayList<Property>(dbProperties.size());
    for (final Property property : dbProperties) {
      if (!(property instanceof Property.EntityProperty))
        selectProperties.add(property);
    }
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

  public int getPropertyCount(final String entityID) {
    return getProperties(entityID).size();
  }
}
