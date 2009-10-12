package org.jminor.framework.domain;

import org.jminor.common.db.IdSource;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EntityDefinition implements Serializable {

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
   * The source of the entitys id (primary key), i.e. sequence name
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

  private List<Property.PrimaryKeyProperty> primaryKeyProperties;
  private List<Property.ForeignKeyProperty> foreignKeyProperties;

  private List<Property> visibleProperties;
  private List<Property> databaseProperties;

  private Map<String, Collection<Property.DenormalizedProperty>> denormalizedProperties;

  private List<String> searchPropertyIDs;

  private String selectColumnsString;

  public EntityDefinition(final String entityID, final Property... propertyDefinitions) {
    this.entityID = entityID;
    this.tableName = entityID;
    this.selectTableName = entityID;
    this.properties = new LinkedHashMap<String, Property>(propertyDefinitions.length);
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
    initialize();
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
      setIdValueSource(getTableName() + "_seq");

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

  public List<String> getSearchPropertyIDs() {
    return searchPropertyIDs;
  }

  public EntityDefinition setSearchPropertyIDs(final String... searchPropertyIDs) {
    this.searchPropertyIDs = Arrays.asList(searchPropertyIDs);
    return this;
  }

  public List<Property.PrimaryKeyProperty> getPrimaryKeyProperties() {
    return primaryKeyProperties;
  }

  public String getSelectColumnsString() {
    return selectColumnsString;
  }

  public Map<String, Property> getProperties() {
    return new HashMap<String, Property>(properties);
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
    return denormalizedProperties.size() > 0;
  }

  public Collection<Property.DenormalizedProperty> getDenormalizedProperties(final String propertyOwnerEntityID) {
    return denormalizedProperties.get(propertyOwnerEntityID);
  }

  private void initialize() {
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
        Collection<Property.DenormalizedProperty> denormProps = denormalizedProperties.get(denormalizedProperty.getForeignKeyPropertyID());
        if (denormProps == null)
          denormalizedProperties.put(denormalizedProperty.getForeignKeyPropertyID(), denormProps = new ArrayList<Property.DenormalizedProperty>());
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
        ret.add(property.getPropertyID());

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
                ") ").append(property.getPropertyID());
      else
        ret.append(property.getPropertyID());

      if (i++ < selectProperties.size() - 1)
        ret.append(", ");
    }

    return ret.toString();
  }
}
