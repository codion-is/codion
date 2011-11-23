/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.model.IdSource;
import org.jminor.common.model.Util;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A class encapsulating a entity definition, such as table name, order by clause and properties.
 */
final class EntityDefinitionImpl implements Entity.Definition {

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
   * The domainID
   */
  private String domainID;

  /**
   * The caption to use for the entity type
   */
  private String caption;

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
   * Holds the group by clause
   */
  private String groupByClause;

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
   * The smallDataset value
   */
  private boolean smallDataset = false;

  /**
   * The default Entity.ToString instance used when toString() is called for this entity type
   */
  private Entity.ToString stringProvider = new Entity.ToString() {
    /** {@inheritDoc} */
    public String toString(final Entity entity) {
      Util.rejectNullValue(entity, "entity");
      return new StringBuilder(entityID).append(": ").append(entity.getPrimaryKey()).toString();
    }
  };

  /**
   * Provides the background color
   */
  private Entity.BackgroundColorProvider backgroundColorProvider = null;

  /**
   * The comparator
   */
  private Comparator<Entity> comparator = Util.getSpaceAwareCollator();

  /**
   * The validator
   */
  private Entity.Validator validator;

  /**
   * A custom sql query used when selecting entities of this type
   */
  private String selectQuery;

  /**
   * The IDs of the properties to use when performing a string based lookup on this entity
   */
  private Collection<String> searchPropertyIDs;

  /**
   * Links a set of derived property ids to a parent property id
   */
  private final Map<String, Set<String>> linkedProperties = new HashMap<String, Set<String>>();

  private List<Property.PrimaryKeyProperty> primaryKeyProperties;
  private List<Property.ForeignKeyProperty> foreignKeyProperties;
  private List<Property.TransientProperty> transientProperties;
  private List<Property> visibleProperties;
  private List<Property.ColumnProperty> columnProperties;
  private Map<String, Collection<Property.DenormalizedProperty>> denormalizedProperties;
  private String selectColumnsString;
  private boolean hasDenormalizedProperties;

  private static final Map<String, Entity.Definition> ENTITY_DEFINITIONS = new HashMap<String, Entity.Definition>();

  /**
   * Defines a new entity type, with the entityID serving as the initial entity caption
   * as well as the table name.
   * @param entityID the ID uniquely identifying the entity
   * @param propertyDefinitions the Property objects this entity should encompass
   * @throws IllegalArgumentException if no primary key property is specified
   */
  EntityDefinitionImpl(final String entityID, final Property... propertyDefinitions) {
    this(entityID, entityID, propertyDefinitions);
  }

  /**
   * Defines a new entity type with the entityID serving as the initial entity caption.
   * @param entityID the ID uniquely identifying the entity
   * @param tableName the name of the underlying table
   * @param propertyDefinitions the Property objects this entity should encompass
   * @throws IllegalArgumentException if no primary key property is specified
   */
  EntityDefinitionImpl(final String entityID, final String tableName, final Property... propertyDefinitions) {
    Util.rejectNullValue(entityID, "entityID");
    Util.rejectNullValue(tableName, "tableName");
    Util.rejectNullValue(propertyDefinitions, "entityDefinitions");
    this.domainID = entityID;
    this.entityID = entityID;
    this.caption = entityID;
    this.tableName = tableName;
    this.selectTableName = tableName;
    this.properties = Collections.unmodifiableMap(initializeProperties(entityID, propertyDefinitions));
    this.groupByClause = initializeGroupByClause(getColumnProperties());
    final String[] selectColumnNames = initializeSelectColumnNames(getColumnProperties());
    for (int idx = 0; idx < selectColumnNames.length; idx++) {
      ((Property.ColumnProperty) properties.get(selectColumnNames[idx])).setSelectIndex(idx + 1);
    }
    initializeDerivedPropertyChangeLinks();
  }

  /** {@inheritDoc} */
  public String getEntityID() {
    return entityID;
  }

  /** {@inheritDoc} */
  public String getTableName() {
    return tableName;
  }

  /** {@inheritDoc} */
  public String getDomainID() {
    return domainID;
  }

  /** {@inheritDoc} */
  public Entity.Definition setDomainID(final String domainID) {
    this.domainID = domainID;
    return this;
  }

  /** {@inheritDoc} */
  public String getCaption() {
    return caption;
  }

  /** {@inheritDoc} */
  public Entity.Definition setCaption(final String caption) {
    Util.rejectNullValue(caption, "caption");
    this.caption = caption;
    return this;
  }

  /** {@inheritDoc} */
  public boolean isSmallDataset() {
    return smallDataset;
  }

  /** {@inheritDoc} */
  public Entity.Definition setSmallDataset(final boolean smallDataset) {
    this.smallDataset = smallDataset;
    return this;
  }

  /** {@inheritDoc} */
  public boolean isReadOnly() {
    return readOnly;
  }

  /** {@inheritDoc} */
  public Entity.Definition setReadOnly(final boolean readOnly) {
    this.readOnly = readOnly;
    return this;
  }

  /** {@inheritDoc} */
  public IdSource getIdSource() {
    return idSource;
  }

  /** {@inheritDoc} */
  public Entity.Definition setIdSource(final IdSource idSource) {
    Util.rejectNullValue(idSource, "idSource");
    this.idSource = idSource;
    if ((idSource == IdSource.SEQUENCE || idSource == IdSource.AUTO_INCREMENT) && idValueSource == null) {
      setIdValueSource(tableName + "_seq");
    }//todo remove

    return this;
  }

  /** {@inheritDoc} */
  public String getIdValueSource() {
    return idValueSource;
  }

  /** {@inheritDoc} */
  public Entity.Definition setIdValueSource(final String idValueSource) {
    Util.rejectNullValue(idValueSource, "idValueSource");
    this.idValueSource = idValueSource;
    return this;
  }

  /** {@inheritDoc} */
  public String getOrderByClause() {
    return orderByClause;
  }

  /** {@inheritDoc} */
  public Entity.Definition setOrderByClause(final String orderByClause) {
    Util.rejectNullValue(orderByClause, "orderByClause");
    this.orderByClause = orderByClause;
    return this;
  }

  /** {@inheritDoc} */
  public String getGroupByClause() {
    return groupByClause;
  }

  /** {@inheritDoc} */
  public Entity.Definition setGroupByClause(final String groupByClause) {
    Util.rejectNullValue(groupByClause, "groupByClause");
    if (this.groupByClause != null) {
      throw new IllegalStateException("Group by clause has already been set: " + this.groupByClause);
    }
    this.groupByClause = groupByClause;
    return this;
  }

  /** {@inheritDoc} */
  public String getSelectTableName() {
    return selectTableName;
  }

  /** {@inheritDoc} */
  public Entity.Definition setSelectTableName(final String selectTableName) {
    Util.rejectNullValue(selectTableName, "selectTableName");
    this.selectTableName = selectTableName;
    return this;
  }

  /** {@inheritDoc} */
  public String getSelectQuery() {
    return selectQuery;
  }

  /** {@inheritDoc} */
  public Entity.Definition setSelectQuery(final String selectQuery) {
    Util.rejectNullValue(selectQuery, "selectQuery");
    this.selectQuery = selectQuery;
    return this;
  }

  /** {@inheritDoc} */
  public Entity.ToString getStringProvider() {
    return stringProvider;
  }

  /** {@inheritDoc} */
  public Entity.Definition setStringProvider(final Entity.ToString stringProvider) {
    Util.rejectNullValue(stringProvider, "stringProvider");
    this.stringProvider = stringProvider;
    return this;
  }

  /** {@inheritDoc} */
  public Comparator<Entity> getComparator() {
    return comparator;
  }

  /** {@inheritDoc} */
  public Entity.Definition setComparator(final Comparator<Entity> comparator) {
    Util.rejectNullValue(comparator, "comparator");
    this.comparator = comparator;
    return this;
  }

  /** {@inheritDoc} */
  public Collection<String> getSearchPropertyIDs() {
    if (searchPropertyIDs == null) {
      return Collections.emptyList();
    }
    return Collections.unmodifiableCollection(searchPropertyIDs);
  }

  /** {@inheritDoc} */
  public Entity.Definition setSearchPropertyIDs(final String... searchPropertyIDs) {
    Util.rejectNullValue(searchPropertyIDs, "searchPropertyIDs");
    for (final String propertyID : searchPropertyIDs) {
      if (!properties.get(propertyID).isString()) {
        throw new IllegalArgumentException("Entity search property must be of type String: " + properties.get(propertyID));
      }
    }

    this.searchPropertyIDs = Arrays.asList(searchPropertyIDs);
    return this;
  }

  /** {@inheritDoc} */
  public Map<String, Property> getProperties() {
    return properties;
  }

  /** {@inheritDoc} */
  public boolean hasLinkedProperties(final String propertyID) {
    return linkedProperties.containsKey(propertyID);
  }

  /** {@inheritDoc} */
  public Collection<String> getLinkedPropertyIDs(final String propertyID) {
    final Collection<String> linked = linkedProperties.get(propertyID);
    if (linked == null) {
      return Collections.emptyList();
    }

    return linked;
  }

  /** {@inheritDoc} */
  public List<Property.PrimaryKeyProperty> getPrimaryKeyProperties() {
    if (primaryKeyProperties == null) {
      primaryKeyProperties = Collections.unmodifiableList(getPrimaryKeyProperties(properties.values()));
    }
    return primaryKeyProperties;
  }

  /** {@inheritDoc} */
  public String getSelectColumnsString() {
    if (selectColumnsString == null) {
      selectColumnsString = initializeSelectColumnsString(getColumnProperties());
    }
    return selectColumnsString;
  }

  /** {@inheritDoc} */
  public List<Property> getVisibleProperties() {
    if (visibleProperties == null) {
      visibleProperties = Collections.unmodifiableList(getVisibleProperties(properties.values()));
    }
    return visibleProperties;
  }

  /** {@inheritDoc} */
  public List<Property.ColumnProperty> getColumnProperties() {
    if (columnProperties == null) {
      columnProperties = Collections.unmodifiableList(getColumnProperties(properties.values()));
    }
    return columnProperties;
  }

  /** {@inheritDoc} */
  public List<Property.TransientProperty> getTransientProperties() {
    if (transientProperties == null) {
      transientProperties = Collections.unmodifiableList(getTransientProperties(properties.values()));
    }
    return transientProperties;
  }

  /** {@inheritDoc} */
  public List<Property.ForeignKeyProperty> getForeignKeyProperties() {
    if (foreignKeyProperties == null) {
      foreignKeyProperties = Collections.unmodifiableList(getForeignKeyProperties(properties.values()));
    }
    return foreignKeyProperties;
  }

  /** {@inheritDoc} */
  public boolean hasDenormalizedProperties() {
    if (denormalizedProperties == null) {
      denormalizedProperties = Collections.unmodifiableMap(getDenormalizedProperties(properties.values()));
      hasDenormalizedProperties = !denormalizedProperties.isEmpty();
    }
    return hasDenormalizedProperties;
  }

  /** {@inheritDoc} */
  public boolean hasDenormalizedProperties(final String foreignKeyPropertyID) {
    if (denormalizedProperties == null) {
      denormalizedProperties = Collections.unmodifiableMap(getDenormalizedProperties(properties.values()));
    }
    return hasDenormalizedProperties && denormalizedProperties.containsKey(foreignKeyPropertyID);
  }

  /** {@inheritDoc} */
  public Collection<Property.DenormalizedProperty> getDenormalizedProperties(final String foreignKeyPropertyID) {
    if (denormalizedProperties == null) {
      denormalizedProperties = Collections.unmodifiableMap(getDenormalizedProperties(properties.values()));
    }
    return denormalizedProperties.get(foreignKeyPropertyID);
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return entityID;
  }

  /** {@inheritDoc} */
  public Entity.Definition setToStringProvider(final Entity.ToString toString) {
    this.stringProvider = toString;
    return this;
  }

  /** {@inheritDoc} */
  public Entity.Definition setBackgroundColorProvider(final Entity.BackgroundColorProvider colorProvider) {
    this.backgroundColorProvider = colorProvider;
    return this;
  }

  /** {@inheritDoc} */
  public Entity.Definition setValidator(final Entity.Validator validator) {
    this.validator = validator;
    return this;
  }

  /** {@inheritDoc} */
  public Entity.Validator getValidator() {
    return validator;
  }

  /** {@inheritDoc} */
  public int compareTo(final Entity entity, final Entity entityToCompare) {
    Util.rejectNullValue(entity, "entity");
    Util.rejectNullValue(entityToCompare, "entityToCompare");
    return comparator.compare(entity, entityToCompare);
  }

  /** {@inheritDoc} */
  public String toString(final Entity entity) {
    return stringProvider.toString(entity);
  }

  /** {@inheritDoc} */
  public Color getBackgroundColor(final Entity entity, final Property property) {
    if (backgroundColorProvider == null) {
      return null;
    }

    return backgroundColorProvider.getBackgroundColor(entity, property);
  }

  static Map<String, Entity.Definition> getDefinitionMap() {
    return ENTITY_DEFINITIONS;
  }

  /**
   * Returns the Entity.Definition object associated with <code>entityID</code>
   * @param entityID the entityID
   * @param propertyDefinitions the property definitions
   * @return the Entity.Definition for the given entityID
   * @throws IllegalArgumentException in case the entity has not been defined
   */
  private static Map<String, Property> initializeProperties(final String entityID, final Property... propertyDefinitions) {
    final Map<String, Property> properties = new LinkedHashMap<String, Property>(propertyDefinitions.length);
    for (final Property property : propertyDefinitions) {
      if (properties.containsKey(property.getPropertyID())) {
        throw new IllegalArgumentException("Property with ID " + property.getPropertyID()
                + (property.getCaption() != null ? " (caption: " + property.getCaption() + ")" : "")
                + " has already been defined as: " + properties.get(property.getPropertyID()) + " in entity: " + entityID);
      }
      properties.put(property.getPropertyID(), property);
      if (property instanceof Property.ForeignKeyProperty) {
        for (final Property referenceProperty : ((Property.ForeignKeyProperty) property).getReferenceProperties()) {
          if (!(referenceProperty instanceof Property.MirrorProperty)) {
            if (properties.containsKey(referenceProperty.getPropertyID())) {
              throw new IllegalArgumentException("Property with ID " + referenceProperty.getPropertyID()
                      + (referenceProperty.getCaption() != null ? " (caption: " + referenceProperty.getCaption() + ")" : "")
                      + " has already been defined as: " + properties.get(referenceProperty.getPropertyID()) + " in entity: " + entityID);
            }
            properties.put(referenceProperty.getPropertyID(), referenceProperty);
          }
        }
      }
    }
    checkForPrimaryKey(entityID, properties);

    return properties;
  }

  private static void checkForPrimaryKey(final String entityID, final Map<String, Property> propertyDefinitions) {
    for (final Property property : propertyDefinitions.values()) {
      if (property instanceof Property.PrimaryKeyProperty) {
        return;
      }
    }
    throw new IllegalArgumentException("Entity is missing a primary key: " + entityID);
  }

  private void initializeDerivedPropertyChangeLinks() {
    for (final Property property : properties.values()) {
      if (property instanceof Property.DerivedProperty) {
        final Collection<String> linked = ((Property.DerivedProperty) property).getLinkedPropertyIDs();
        if (linked != null && !linked.isEmpty()) {
          for (final String parentLinkPropertyID : linked) {
            addDerivedPropertyChangeLink(parentLinkPropertyID, property.getPropertyID());
          }
        }
      }
    }
  }

  private void addDerivedPropertyChangeLink(final String parentPropertyID, final String transientPropertyID) {
    if (!linkedProperties.containsKey(parentPropertyID)) {
      linkedProperties.put(parentPropertyID, new HashSet<String>());
    }
    linkedProperties.get(parentPropertyID).add(transientPropertyID);
  }

  static Entity.Definition getDefinition(final String entityID) {
    final Entity.Definition definition = ENTITY_DEFINITIONS.get(entityID);
    if (definition == null) {
      throw new IllegalArgumentException("Undefined entity: " + entityID);
    }

    return definition;
  }

  private static Map<String, Collection<Property.DenormalizedProperty>> getDenormalizedProperties(final Collection<Property> properties) {
    final Map<String, Collection<Property.DenormalizedProperty>> denormalizedPropertiesMap = new HashMap<String, Collection<Property.DenormalizedProperty>>(properties.size());
    for (final Property property : properties) {
      if (property instanceof Property.DenormalizedProperty) {
        final Property.DenormalizedProperty denormalizedProperty = (Property.DenormalizedProperty) property;
        Collection<Property.DenormalizedProperty> denormalizedProperties = denormalizedPropertiesMap.get(denormalizedProperty.getForeignKeyPropertyID());
        if (denormalizedProperties == null) {
          denormalizedProperties = new ArrayList<Property.DenormalizedProperty>();
          denormalizedPropertiesMap.put(denormalizedProperty.getForeignKeyPropertyID(), denormalizedProperties);
        }
        denormalizedProperties.add(denormalizedProperty);
      }
    }

    return denormalizedPropertiesMap;
  }

  private static List<Property.PrimaryKeyProperty> getPrimaryKeyProperties(final Collection<Property> properties) {
    final List<Property.PrimaryKeyProperty> primaryKeyProperties = new ArrayList<Property.PrimaryKeyProperty>(properties.size());
    for (final Property property : properties) {
      if (property instanceof Property.PrimaryKeyProperty) {
        primaryKeyProperties.add((Property.PrimaryKeyProperty) property);
      }
    }

    return primaryKeyProperties;
  }

  private static List<Property.ForeignKeyProperty> getForeignKeyProperties(final Collection<Property> properties) {
    final List<Property.ForeignKeyProperty> foreignKeyProperties = new ArrayList<Property.ForeignKeyProperty>(properties.size());
    for (final Property property : properties) {
      if (property instanceof Property.ForeignKeyProperty) {
        foreignKeyProperties.add((Property.ForeignKeyProperty) property);
      }
    }

    return foreignKeyProperties;
  }

  private static List<Property.ColumnProperty> getColumnProperties(final Collection<Property> properties) {
    final List<Property.ColumnProperty> columnProperties = new ArrayList<Property.ColumnProperty>(properties.size());
    for (final Property property : properties) {
      if (property instanceof Property.ColumnProperty) {
        columnProperties.add((Property.ColumnProperty) property);
      }
    }

    return columnProperties;
  }

  private static List<Property.TransientProperty> getTransientProperties(final Collection<Property> properties) {
    final List<Property.TransientProperty> transientProperties = new ArrayList<Property.TransientProperty>(properties.size());
    for (final Property property : properties) {
      if (property instanceof Property.TransientProperty) {
        transientProperties.add((Property.TransientProperty) property);
      }
    }

    return transientProperties;
  }

  private static List<Property> getVisibleProperties(final Collection<Property> properties) {
    final List<Property> visibleProperties = new ArrayList<Property>(properties.size());
    for (final Property property : properties) {
      if (!property.isHidden()) {
        visibleProperties.add(property);
      }
    }

    return visibleProperties;
  }

  /**
   * @param databaseProperties the properties to base the column names on
   * @return the column names used to select an entity of this type from the database
   */
  private static String[] initializeSelectColumnNames(final Collection<Property.ColumnProperty> databaseProperties) {
    final List<String> columnNames = new ArrayList<String>();
    for (final Property property : databaseProperties) {
      columnNames.add(property.getPropertyID());
    }

    return columnNames.toArray(new String[columnNames.size()]);
  }

  /**
   * @param databaseProperties the column properties
   * @return a list of grouping columns separated with a comma, to serve as a group by clause,
   * null if no grouping properties are defined
   */
  private static String initializeGroupByClause(final Collection<Property.ColumnProperty> databaseProperties) {
    final List<Property> groupingProperties = new ArrayList<Property>(databaseProperties.size());
    for (final Property.ColumnProperty property : databaseProperties) {
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

  private static String initializeSelectColumnsString(final Collection<Property.ColumnProperty> databaseProperties) {
    final List<Property> selectProperties = new ArrayList<Property>(databaseProperties.size());
    for (final Property.ColumnProperty property : databaseProperties) {
      selectProperties.add(property);
    }

    final StringBuilder stringBuilder = new StringBuilder();
    int i = 0;
    for (final Property property : selectProperties) {
      if (property instanceof Property.SubqueryProperty) {
        stringBuilder.append("(").append(((Property.SubqueryProperty) property).getSubQuery()).append(
                ") as ").append(property.getPropertyID());
      }
      else {
        stringBuilder.append(property.getPropertyID());
      }

      if (i++ < selectProperties.size() - 1) {
        stringBuilder.append(", ");
      }
    }

    return stringBuilder.toString();
  }
}
