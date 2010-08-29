/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.model.IdSource;
import org.jminor.common.model.Util;
import org.jminor.common.model.valuemap.ValueMap;

import java.awt.Color;
import java.text.Collator;
import java.text.Format;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A class encapsulating a entity definition, such as table name, order by clause and properties.
 */
final class EntityDefinitionImpl implements EntityDefinition {
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
   * The StringProvider used when toString() is called for this entity
   * @see org.jminor.common.model.valuemap.ValueMap.ToString
   */
  private ValueMap.ToString<String> stringProvider = new ValueMap.ToString<String>() {
    /** {@inheritDoc} */
    public String toString(final ValueMap<String, ?> valueMap) {
      Util.rejectNullValue(valueMap, "entity");
      final Entity entity = (Entity) valueMap;
      return new StringBuilder(entityID).append(": ").append(entity.getPrimaryKey()).toString();
    }
  };
  /**
   * Provides the background color
   */
  private Entity.BackgroundColorProvider backgroundColorProvider = new BackgroundColorProviderImpl();
  /**
   * Provides values derived from other properties
   */
  private Entity.DerivedValueProvider derivedValueProvider;
  /**
   * Provides formatted values
   */
  private Entity.FormattedValueProvider formattedValueProvider = new FormattedValueProviderImpl();
  /**
   * The comparator
   */
  private Entity.Comparator comparator = new ComparatorImpl();
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
  private final Map<String, Set<String>> linkedProperties = new HashMap<String, Set<String>>();

  private List<Property.PrimaryKeyProperty> primaryKeyProperties;
  private List<Property.ForeignKeyProperty> foreignKeyProperties;
  private List<Property.TransientProperty> transientProperties;
  private List<Property> visibleProperties;
  private List<Property.ColumnProperty> columnProperties;
  private Map<String, Collection<Property.DenormalizedProperty>> denormalizedProperties;
  private String selectColumnsString;
  private boolean hasDenormalizedProperties;

  static Map<String, EntityDefinition> ENTITY_DEFINITIONS;

  /**
   * Defines a new entity type, with the entityID serving as the initial entity caption
   * as well as the table name.
   * @param entityID the ID uniquely identifying the entity
   * @param propertyDefinitions the Property objects this entity should encompass
   */
  EntityDefinitionImpl(final String entityID, final Property... propertyDefinitions) {
    this(entityID, entityID, propertyDefinitions);
  }

  /**
   * Defines a new entity type with the entityID serving as the initial entity caption.
   * @param entityID the ID uniquely identifying the entity
   * @param tableName the name of the underlying table
   * @param propertyDefinitions the Property objects this entity should encompass
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
    final String[] selectColumnNames = initSelectColumnNames(getColumnProperties());
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
  public EntityDefinition setDomainID(final String domainID) {
    this.domainID = domainID;
    return this;
  }

  /** {@inheritDoc} */
  public String getCaption() {
    return caption;
  }

  /** {@inheritDoc} */
  public EntityDefinition setCaption(final String caption) {
    Util.rejectNullValue(caption, "caption");
    this.caption = caption;
    return this;
  }

  /** {@inheritDoc} */
  public boolean isSmallDataset() {
    return smallDataset;
  }

  /** {@inheritDoc} */
  public EntityDefinition setSmallDataset(final boolean smallDataset) {
    this.smallDataset = smallDataset;
    return this;
  }

  /** {@inheritDoc} */
  public boolean isReadOnly() {
    return readOnly;
  }

  /** {@inheritDoc} */
  public EntityDefinition setReadOnly(final boolean readOnly) {
    this.readOnly = readOnly;
    return this;
  }

  /** {@inheritDoc} */
  public IdSource getIdSource() {
    return idSource;
  }

  /** {@inheritDoc} */
  public EntityDefinition setIdSource(final IdSource idSource) {
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
  public EntityDefinition setIdValueSource(final String idValueSource) {
    Util.rejectNullValue(idValueSource, "idValueSource");
    this.idValueSource = idValueSource;
    return this;
  }

  /** {@inheritDoc} */
  public String getOrderByClause() {
    return orderByClause;
  }

  /** {@inheritDoc} */
  public EntityDefinition setOrderByClause(final String orderByClause) {
    Util.rejectNullValue(orderByClause, "orderByClause");
    this.orderByClause = orderByClause;
    return this;
  }

  /** {@inheritDoc} */
  public String getSelectTableName() {
    return selectTableName;
  }

  /** {@inheritDoc} */
  public EntityDefinition setSelectTableName(final String selectTableName) {
    Util.rejectNullValue(selectTableName, "selectTableName");
    this.selectTableName = selectTableName;
    return this;
  }

  /** {@inheritDoc} */
  public String getSelectQuery() {
    return selectQuery;
  }

  /** {@inheritDoc} */
  public EntityDefinition setSelectQuery(final String selectQuery) {
    Util.rejectNullValue(selectQuery, "selectQuery");
    this.selectQuery = selectQuery;
    return this;
  }

  /** {@inheritDoc} */
  public ValueMap.ToString<String> getStringProvider() {
    return stringProvider;
  }

  /** {@inheritDoc} */
  public EntityDefinition setStringProvider(final ValueMap.ToString<String> stringProvider) {
    Util.rejectNullValue(stringProvider, "stringProvider");
    this.stringProvider = stringProvider;
    return this;
  }

  /** {@inheritDoc} */
  public boolean isRowColoring() {
    return rowColoring;
  }

  /** {@inheritDoc} */
  public EntityDefinition setRowColoring(final boolean rowColoring) {
    this.rowColoring = rowColoring;
    return this;
  }

  /** {@inheritDoc} */
  public List<String> getSearchPropertyIDs() {
    if (searchPropertyIDs == null) {
      return Collections.emptyList();
    }
    return Collections.unmodifiableList(searchPropertyIDs);
  }

  /** {@inheritDoc} */
  public EntityDefinition setSearchPropertyIDs(final String... searchPropertyIDs) {
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
      selectColumnsString = initSelectColumnsString(getColumnProperties());
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
  public EntityDefinition setDerivedValueProvider(final Entity.DerivedValueProvider valueProvider) {
    this.derivedValueProvider = valueProvider;
    return this;
  }

  /** {@inheritDoc} */
  public EntityDefinition setToStringProvider(final ValueMap.ToString<String> toString) {
    this.stringProvider = toString;
    return this;
  }

  /** {@inheritDoc} */
  public EntityDefinition setBackgroundColorProvider(final Entity.BackgroundColorProvider colorProvider) {
    this.backgroundColorProvider = colorProvider;
    return this;
  }

  /** {@inheritDoc} */
  public EntityDefinition setFormattedValueProvider(final Entity.FormattedValueProvider formattedProvider) {
    this.formattedValueProvider = formattedProvider;
    return this;
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
  public Object getDerivedValue(final Entity entity, final Property.DerivedProperty property) {
    if (derivedValueProvider != null) {
      return derivedValueProvider.getDerivedValue(entity, property);
    }

    throw new IllegalStateException("DerivedValueProvider has not been set for: " + entity);
  }

  /** {@inheritDoc} */
  public String getFormattedValue(final Entity entity, final Property property, final Format format) {
    return formattedValueProvider.getFormattedValue(entity, property, format);
  }

  /** {@inheritDoc} */
  @SuppressWarnings({"UnusedDeclaration"})
  public Color getBackgroundColor(final Entity entity) {
    return backgroundColorProvider.getBackgroundColor(entity);
  }

  /**
   * Returns the EntityDefinition object associated with <code>entityID</code>
   * @param entityID the entityID
   * @param propertyDefinitions the property definitions
   * @return the EntityDefinition for the given entityID
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
    return properties;
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
  private static String[] initSelectColumnNames(final Collection<Property.ColumnProperty> databaseProperties) {
    final List<String> columnNames = new ArrayList<String>();
    for (final Property property : databaseProperties) {
      columnNames.add(property.getPropertyID());
    }

    return columnNames.toArray(new String[columnNames.size()]);
  }

  private static String initSelectColumnsString(final Collection<Property.ColumnProperty> databaseProperties) {
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
  
  private static final class BackgroundColorProviderImpl implements Entity.BackgroundColorProvider {
    /** {@inheritDoc} */
    public Color getBackgroundColor(final Entity entity) {
      return null;
    }
  }

  private static final class FormattedValueProviderImpl implements Entity.FormattedValueProvider {
    /** {@inheritDoc} */
    public String getFormattedValue(final Entity entity, final Property property, final Format format) {
      Util.rejectNullValue(entity, "entity");
      final Object value = entity.getValue(property);
      if (value == null) {
        return "";
      }

      if (format == null) {
        return value.toString();
      }

      return format.format(value);
    }
  }

  private static final class ComparatorImpl implements Entity.Comparator {
    private final Collator collator = Collator.getInstance();
    /** {@inheritDoc} */
    public int compare(final Entity entity, final Entity entityToCompare) {
      return collator.compare(entity.toString(), entityToCompare.toString());
    }
  }
}
