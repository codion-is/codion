/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.model.IdSource;
import org.jminor.common.model.valuemap.ValueMap;

import java.io.Serializable;
import java.text.Collator;
import java.util.*;

/**
 * A class encapsulating a entity definition, such as table name, order by clause and properties.
 * Also acts as a proxy for retrieving values from Entity objects, allowing for plugged
 * in entity specific functionality, such as providing toString() and compareTo() implementations.
 */
public class EntityDefinition implements Serializable {

  protected transient final Collator collator = Collator.getInstance();

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
  private String tableName;
  /**
   * The table (view, query) from which to select the entity
   * Used if it differs from the one used for inserts/updates
   */
  private String selectTableName;
  /**
   * Holds the order by clause
   */
  private String orderByClause;
  /**
   * The source of the entity's id (primary key), i.e. sequence name
   */
  private String idValueSource;
  /**
   * The IdSource
   */
  private IdSource idSource = IdSource.NONE;
  /**
   * The readOnly value
   */
  private boolean readOnly;
  /**
   * The largeDataset value
   */
  private boolean largeDataset;
  /**
   * The StringProvider used when toString() is called for this entity
   * @see org.jminor.common.model.valuemap.ValueMap.ToString
   */
  private ValueMap.ToString<String, Object> stringProvider;
  /**
   * A custom sql query used when selecting entities of this type
   */
  private String selectQuery;
  /**
   * True if this entity should be color specifically in table views
   */
  private boolean rowColoring;
  /**
   * The IDs of the properties to use when performing a string based lookup on this entity
   */
  private List<String> searchPropertyIDs;
  /**
   * Links a set of derived property ids to a parent property id
   */
  private Map<String, Set<String>> derivedPropertyChangeLinks = new HashMap<String, Set<String>>();

  private transient List<Property.PrimaryKeyProperty> primaryKeyProperties;
  private transient List<Property.ForeignKeyProperty> foreignKeyProperties;
  private transient List<Property.TransientProperty> transientProperties;
  private transient List<Property> visibleProperties;
  private transient List<Property> databaseProperties;
  private transient Map<String, Collection<Property.DenormalizedProperty>> denormalizedProperties;
  private transient String selectColumnsString;
  private transient boolean hasDenormalizedProperties;

  /**
   * Defines a new entity, by default the <code>entityID</code> is used as the underlying table name
   * @param entityID the ID uniquely identifying the entity
   * @param propertyDefinitions the Property objects this entity should encompass
   */
  public EntityDefinition(final String entityID, final Property... propertyDefinitions) {
    this(entityID, entityID, propertyDefinitions);
  }

  /**
   * Defines a new entity
   * @param entityID the ID uniquely identifying the entity
   * @param tableName the name of the underlying table
   * @param propertyDefinitions the Property objects this entity should encompass
   */
  public EntityDefinition(final String entityID, final String tableName, final Property... propertyDefinitions) {
    this.entityID = entityID;
    this.tableName = tableName;
    this.selectTableName = tableName;
    this.properties = Collections.unmodifiableMap(initializeProperties(entityID, propertyDefinitions));
    final String[] selectColumnNames = initSelectColumnNames(getDatabaseProperties());
    for (int idx = 0; idx < selectColumnNames.length; idx++)
      properties.get(selectColumnNames[idx]).setSelectIndex(idx + 1);
    initializeDerivedPropertyChangeLinks();
  }

  public String getEntityID() {
    return entityID;
  }

  public boolean isLargeDataset() {
    return largeDataset;
  }

  public EntityDefinition setLargeDataset(final boolean largeDataset) {
    this.largeDataset = largeDataset;
    return this;
  }

  public boolean isReadOnly() {
    return readOnly;
  }

  public EntityDefinition setReadOnly(final boolean readOnly) {
    this.readOnly = readOnly;
    return this;
  }

  public IdSource getIdSource() {
    return idSource;
  }

  public EntityDefinition setIdSource(final IdSource idSource) {
    this.idSource = idSource;
    if ((idSource == IdSource.SEQUENCE || idSource == IdSource.AUTO_INCREMENT) && getIdValueSource() == null)
      setIdValueSource(getTableName() + "_seq");//todo remove

    return this;
  }

  public String getIdValueSource() {
    return idValueSource;
  }

  public EntityDefinition setIdValueSource(final String idValueSource) {
    this.idValueSource = idValueSource;
    return this;
  }

  public String getOrderByClause() {
    return orderByClause;
  }

  public EntityDefinition setOrderByClause(final String orderByClause) {
    this.orderByClause = orderByClause;
    return this;
  }

  public String getSelectTableName() {
    return selectTableName;
  }

  public EntityDefinition setSelectTableName(final String selectTableName) {
    this.selectTableName = selectTableName;
    return this;
  }

  public String getTableName() {
    return tableName;
  }

  public EntityDefinition setTableName(final String tableName) {
    this.tableName = tableName;
    return this;
  }

  public String getSelectQuery() {
    return selectQuery;
  }

  public EntityDefinition setSelectQuery(final String selectQuery) {
    this.selectQuery = selectQuery;
    return this;
  }

  public ValueMap.ToString<String, Object> getStringProvider() {
    return stringProvider;
  }

  public EntityDefinition setStringProvider(final ValueMap.ToString<String, Object> stringProvider) {
    this.stringProvider = stringProvider;
    return this;
  }

  public boolean isRowColoring() {
    return rowColoring;
  }

  public EntityDefinition setRowColoring(final boolean rowColoring) {
    this.rowColoring = rowColoring;
    return this;
  }

  public List<String> getSearchPropertyIDs() {
    return searchPropertyIDs;
  }

  public EntityDefinition setSearchPropertyIDs(final String... searchPropertyIDs) {
    for (final String propertyID : searchPropertyIDs)
      if (!properties.get(propertyID).isString())
        throw new RuntimeException("Entity search property must be of type String: " + properties.get(propertyID));

    this.searchPropertyIDs = Arrays.asList(searchPropertyIDs);
    return this;
  }

  public Map<String, Property> getProperties() {
    return properties;
  }

  public boolean hasLinkedProperties(final String propertyID) {
    return derivedPropertyChangeLinks.containsKey(propertyID);
  }

  public Collection<String> getLinkedPropertyIDs(final String propertyID) {
    return derivedPropertyChangeLinks.get(propertyID);
  }

  public List<Property.PrimaryKeyProperty> getPrimaryKeyProperties() {
    if (primaryKeyProperties == null)
      primaryKeyProperties = Collections.unmodifiableList(getPrimaryKeyProperties(properties.values()));
    return primaryKeyProperties;
  }

  public String getSelectColumnsString() {
    if (selectColumnsString == null)
      selectColumnsString = initSelectColumnsString(getDatabaseProperties());
    return selectColumnsString;
  }

  public List<Property> getVisibleProperties() {
    if (visibleProperties == null)
      visibleProperties = Collections.unmodifiableList(getVisibleProperties(properties.values()));
    return visibleProperties;
  }

  public List<Property> getDatabaseProperties() {
    if (databaseProperties == null)
      databaseProperties = Collections.unmodifiableList(getDatabaseProperties(properties.values()));
    return databaseProperties;
  }

  public List<Property.TransientProperty> getTransientProperties() {
    if (transientProperties == null)
      transientProperties = Collections.unmodifiableList(getTransientProperties(properties.values()));
    return transientProperties;
  }

  public List<Property.ForeignKeyProperty> getForeignKeyProperties() {
    if (foreignKeyProperties == null)
      foreignKeyProperties = Collections.unmodifiableList(getForeignKeyProperties(properties.values()));
    return foreignKeyProperties;
  }

  public boolean hasDenormalizedProperties() {
    if (denormalizedProperties == null) {
      denormalizedProperties = Collections.unmodifiableMap(getDenormalizedProperties(properties.values()));
      hasDenormalizedProperties = denormalizedProperties.size() > 0;
    }
    return hasDenormalizedProperties;
  }

  public boolean hasDenormalizedProperties(final String foreignKeyPropertyID) {
    if (denormalizedProperties == null)
      denormalizedProperties = Collections.unmodifiableMap(getDenormalizedProperties(properties.values()));
    return hasDenormalizedProperties && denormalizedProperties.containsKey(foreignKeyPropertyID);
  }

  public Collection<Property.DenormalizedProperty> getDenormalizedProperties(final String foreignKeyPropertyID) {
    if (denormalizedProperties == null)
      denormalizedProperties = Collections.unmodifiableMap(getDenormalizedProperties(properties.values()));
    return denormalizedProperties.get(foreignKeyPropertyID);
  }

  @Override
  public String toString() {
    return entityID;
  }

  private static Map<String, Property> initializeProperties(final String entityID, final Property... propertyDefinitions) {
    final Map<String, Property> properties = new LinkedHashMap<String, Property>(propertyDefinitions.length);
    for (final Property property : propertyDefinitions) {
      if (properties.containsKey(property.getPropertyID()))
        throw new IllegalArgumentException("Property with ID " + property.getPropertyID()
                + (property.getCaption() != null ? " (caption: " + property.getCaption() + ")" : "")
                + " has already been defined as: " + properties.get(property.getPropertyID()) + " in entity: " + entityID);
      properties.put(property.getPropertyID(), property);
      if (property instanceof Property.ForeignKeyProperty) {
        for (final Property referenceProperty : ((Property.ForeignKeyProperty) property).getReferenceProperties()) {
          if (!(referenceProperty instanceof Property.MirrorProperty)) {
            if (properties.containsKey(referenceProperty.getPropertyID()))
              throw new IllegalArgumentException("Property with ID " + referenceProperty.getPropertyID()
                      + (referenceProperty.getCaption() != null ? " (caption: " + referenceProperty.getCaption() + ")" : "")
                      + " has already been defined as: " + properties.get(referenceProperty.getPropertyID()) + " in entity: " + entityID);
            properties.put(referenceProperty.getPropertyID(), referenceProperty);
          }
        }
      }
    }
    return properties;
  }

  private void initializeDerivedPropertyChangeLinks() {
    for (final Property property : properties.values()) {
      if (property instanceof Property.DerivedProperty) {
        final Collection<String> linkedProperties = ((Property.DerivedProperty) property).getLinkedPropertyIDs();
        if (linkedProperties != null && linkedProperties.size()  > 0) {
          for (final String parentLinkPropertyID : linkedProperties)
            addDerivedPropertyChangeLink(parentLinkPropertyID, property.getPropertyID());
        }
      }
    }
  }

  private void addDerivedPropertyChangeLink(final String parentPropertyID, final String transientPropertyID) {
    if (!derivedPropertyChangeLinks.containsKey(parentPropertyID))
      derivedPropertyChangeLinks.put(parentPropertyID, new HashSet<String>());
    derivedPropertyChangeLinks.get(parentPropertyID).add(transientPropertyID);
  }

  private static Map<String, Collection<Property.DenormalizedProperty>> getDenormalizedProperties(final Collection<Property> properties) {
    final Map<String, Collection<Property.DenormalizedProperty>> denormalizedPropertiesMap = new HashMap<String, Collection<Property.DenormalizedProperty>>(properties.size());
    for (final Property property : properties) {
      if (property instanceof Property.DenormalizedProperty) {
        final Property.DenormalizedProperty denormalizedProperty = (Property.DenormalizedProperty) property;
        Collection<Property.DenormalizedProperty> denormalizedProperties = denormalizedPropertiesMap.get(denormalizedProperty.getForeignKeyPropertyID());
        if (denormalizedProperties == null)
          denormalizedPropertiesMap.put(denormalizedProperty.getForeignKeyPropertyID(), denormalizedProperties = new ArrayList<Property.DenormalizedProperty>());
        denormalizedProperties.add(denormalizedProperty);
      }
    }

    return denormalizedPropertiesMap;
  }

  private static List<Property.PrimaryKeyProperty> getPrimaryKeyProperties(final Collection<Property> properties) {
    final List<Property.PrimaryKeyProperty> primaryKeyProperties = new ArrayList<Property.PrimaryKeyProperty>(properties.size());
    for (final Property property : properties)
      if (property instanceof Property.PrimaryKeyProperty)
        primaryKeyProperties.add((Property.PrimaryKeyProperty) property);

    return primaryKeyProperties;
  }

  private static List<Property.ForeignKeyProperty> getForeignKeyProperties(final Collection<Property> properties) {
    final List<Property.ForeignKeyProperty> foreignKeyProperties = new ArrayList<Property.ForeignKeyProperty>(properties.size());
    for (final Property property : properties)
      if (property instanceof Property.ForeignKeyProperty)
        foreignKeyProperties.add((Property.ForeignKeyProperty) property);

    return foreignKeyProperties;
  }

  private static List<Property> getDatabaseProperties(final Collection<Property> properties) {
    final List<Property> databaseProperties = new ArrayList<Property>(properties.size());
    for (final Property property : properties)
      if (property.isDatabaseProperty())
        databaseProperties.add(property);

    return databaseProperties;
  }

  private static List<Property.TransientProperty> getTransientProperties(final Collection<Property> properties) {
    final List<Property.TransientProperty> transientProperties = new ArrayList<Property.TransientProperty>(properties.size());
    for (final Property property : properties)
      if (property instanceof Property.TransientProperty)
        transientProperties.add((Property.TransientProperty) property);

    return transientProperties;
  }

  private static List<Property> getVisibleProperties(final Collection<Property> properties) {
    final List<Property> visibleProperties = new ArrayList<Property>(properties.size());
    for (final Property property : properties)
      if (!property.isHidden())
        visibleProperties.add(property);

    return visibleProperties;
  }

  /**
   * @param databaseProperties the properties to base the column names on
   * @return the column names used to select an entity of this type from the database
   */
  private static String[] initSelectColumnNames(final Collection<Property> databaseProperties) {
    final List<String> columnNames = new ArrayList<String>();
    for (final Property property : databaseProperties)
      if (!(property instanceof Property.ForeignKeyProperty))
        columnNames.add(property.getPropertyID());

    return columnNames.toArray(new String[columnNames.size()]);
  }

  private static String initSelectColumnsString(final Collection<Property> databaseProperties) {
    final List<Property> selectProperties = new ArrayList<Property>(databaseProperties.size());
    for (final Property property : databaseProperties)
      if (!(property instanceof Property.ForeignKeyProperty))
        selectProperties.add(property);

    final StringBuilder stringBuilder = new StringBuilder();
    int i = 0;
    for (final Property property : selectProperties) {
      if (property instanceof Property.SubqueryProperty)
        stringBuilder.append("(").append(((Property.SubqueryProperty)property).getSubQuery()).append(
                ") as ").append(property.getPropertyID());
      else
        stringBuilder.append(property.getPropertyID());

      if (i++ < selectProperties.size() - 1)
        stringBuilder.append(", ");
    }

    return stringBuilder.toString();
  }
}
