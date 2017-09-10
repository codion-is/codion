/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.TextUtil;
import org.jminor.common.Util;
import org.jminor.common.db.ResultPacker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A class encapsulating a entity definition, such as table name, order by clause and properties.
 */
final class DefaultEntityDefinition implements Entity.Definition {

  private static final Entity.KeyGenerator DEFAULT_KEY_GENERATOR = new Entities.DefaultKeyGenerator();

  /**
   * The domainId
   */
  private final String domainId;

  /**
   * The entityId
   */
  private final String entityId;

  /**
   * The properties
   */
  private final Map<String, Property> properties;

  /**
   * A list view of the properties
   */
  private final List<Property> propertyList;

  /**
   * The ResultPacker responsible for packing entities of this type
   */
  private final ResultPacker<Entity> resultPacker;

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
   * The caption to use for the entity type
   */
  private String caption;

  /**
   * Holds the order by clause
   */
  private String orderByClause;

  /**
   * Holds the group by clause
   */
  private String groupByClause;

  /**
   * Holds the having clause
   */
  private String havingClause;

  /**
   * The primary key value generator
   */
  private Entity.KeyGenerator keyGenerator = DEFAULT_KEY_GENERATOR;

  /**
   * The key generator type
   */
  private Entity.KeyGenerator.Type keyGeneratorType = DEFAULT_KEY_GENERATOR.getType();

  /**
   * If true then it should not be possible to insert, update or delete entities of this type
   */
  private boolean readOnly;

  /**
   * A somewhat subjective indicator, useful in deciding if all entities of this type
   * would fit in, say, a combo box
   */
  private boolean smallDataset = false;

  /**
   * Another somewhat subjective indicator, indicating if the data in the underlying table can be regarded as static,
   * this is useful in deciding how often to refresh, say, a combo box based on the entity
   */
  private boolean staticData = false;

  /**
   * The Entity.ToString instance used when toString() is called for this entity type
   */
  private Entity.ToString stringProvider = entity -> DefaultEntityDefinition.this.entityId + ": " +
          Objects.requireNonNull(entity, "entity").getKey();

  /**
   * Provides the background color
   */
  private Entity.BackgroundColorProvider backgroundColorProvider = null;

  /**
   * The comparator
   */
  private Comparator<Entity> comparator = TextUtil.getSpaceAwareCollator();

  /**
   * The validator
   */
  private Entity.Validator validator;

  /**
   * A custom sql query used when selecting entities of this type
   */
  private String selectQuery;

  /**
   * Specifies whether or not the select query, if any, contains a where clause
   */
  private boolean selectQueryContainsWhereClause = false;

  /**
   * The IDs of the properties to use when performing a string based lookup on this entity
   */
  private Collection<String> searchPropertyIds;

  /**
   * Links a set of derived property ids to a parent property id
   */
  private final Map<String, Set<Property.DerivedProperty>> derivedProperties;
  private final List<Property.ColumnProperty> primaryKeyProperties;
  private final List<Property.ForeignKeyProperty> foreignKeyProperties;
  private final List<Property.TransientProperty> transientProperties;
  private final List<Property> visibleProperties;
  private final List<Property.ColumnProperty> columnProperties;
  private final Map<String, List<Property.DenormalizedProperty>> denormalizedProperties;
  private final String selectColumnsString;
  private final boolean hasDenormalizedProperties;

  /**
   * Defines a new entity type with the entityId serving as the initial entity caption.
   * @param propertyDefinitions the Property objects this entity should encompass
   * @param entityId the ID uniquely identifying the entity
   * @param tableName the name of the underlying table
   * @param derivedProperties
   * @param primaryKeyProperties
   * @param foreignKeyProperties
   * @param transientProperties
   * @param visibleProperties
   * @param columnProperties
   * @param denormalizedProperties
   * @param selectColumnsString
   * @throws IllegalArgumentException if no primary key property is specified
   */
  DefaultEntityDefinition(final String domainId, final String entityId, final String tableName,
                          final Map<String, Property> properties,
                          final ResultPacker<Entity> resultPacker,
                          final Map<String, Set<Property.DerivedProperty>> derivedProperties,
                          final List<Property.ColumnProperty> primaryKeyProperties,
                          final List<Property.ForeignKeyProperty> foreignKeyProperties,
                          final List<Property .TransientProperty> transientProperties,
                          final List<Property> visibleProperties,
                          final List<Property.ColumnProperty> columnProperties,
                          final Map<String, List<Property.DenormalizedProperty>> denormalizedProperties,
                          final String selectColumnsString, final String groupByClause) {
    Util.rejectNullOrEmpty(entityId, "entityId");
    Util.rejectNullOrEmpty(tableName, "tableName");
    this.domainId = domainId;
    this.entityId = entityId;
    this.caption = entityId;
    this.tableName = tableName;
    this.properties = properties;
    this.derivedProperties = derivedProperties;
    this.primaryKeyProperties = primaryKeyProperties;
    this.foreignKeyProperties = foreignKeyProperties;
    this.transientProperties = transientProperties;
    this.visibleProperties = visibleProperties;
    this.columnProperties = columnProperties;
    this.denormalizedProperties = denormalizedProperties;
    this.selectColumnsString = selectColumnsString;
    this.hasDenormalizedProperties = !this.denormalizedProperties.isEmpty();
    this.propertyList = Collections.unmodifiableList(new ArrayList(this.properties.values()));
    this.groupByClause = groupByClause;
    this.resultPacker = resultPacker;
  }

  /** {@inheritDoc} */
  @Override
  public String getEntityId() {
    return entityId;
  }

  /** {@inheritDoc} */
  @Override
  public Entity.Definition setTableName(final String tableName) {
    Util.rejectNullOrEmpty(tableName, "tableName");
    this.tableName = tableName;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public String getTableName() {
    return tableName;
  }

  /** {@inheritDoc} */
  @Override
  public String getDomainId() {
    return domainId;
  }

  /** {@inheritDoc} */
  @Override
  public String getCaption() {
    return caption;
  }

  /** {@inheritDoc} */
  @Override
  public Entity.Definition setCaption(final String caption) {
    Objects.requireNonNull(caption, "caption");
    this.caption = caption;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isSmallDataset() {
    return smallDataset;
  }

  /** {@inheritDoc} */
  @Override
  public Entity.Definition setSmallDataset(final boolean smallDataset) {
    this.smallDataset = smallDataset;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isStaticData() {
    return staticData;
  }

  /** {@inheritDoc} */
  @Override
  public Entity.Definition setStaticData(final boolean staticData) {
    this.staticData = staticData;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isReadOnly() {
    return readOnly;
  }

  /** {@inheritDoc} */
  @Override
  public Entity.Definition setReadOnly(final boolean readOnly) {
    this.readOnly = readOnly;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public Entity.KeyGenerator getKeyGenerator() {
    return keyGenerator;
  }

  /** {@inheritDoc} */
  @Override
  public Entity.Definition setKeyGenerator(final Entity.KeyGenerator keyGenerator) {
    Objects.requireNonNull(keyGenerator, "keyGenerator");
    this.keyGenerator = keyGenerator;
    this.keyGeneratorType = keyGenerator.getType();
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public Entity.KeyGenerator.Type getKeyGeneratorType() {
    return keyGeneratorType;
  }

  /** {@inheritDoc} */
  @Override
  public Entity.Definition setKeyGeneratorType(final Entity.KeyGenerator.Type keyGeneratorType) {
    Objects.requireNonNull(keyGeneratorType, "keyGeneratorType");
    this.keyGeneratorType = keyGeneratorType;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public String getOrderByClause() {
    return orderByClause;
  }

  /** {@inheritDoc} */
  @Override
  public Entity.Definition setOrderByClause(final String orderByClause) {
    Objects.requireNonNull(orderByClause, "orderByClause");
    this.orderByClause = orderByClause;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public String getGroupByClause() {
    return groupByClause;
  }

  /** {@inheritDoc} */
  @Override
  public Entity.Definition setGroupByClause(final String groupByClause) {
    Objects.requireNonNull(groupByClause, "groupByClause");
    if (this.groupByClause != null) {
      throw new IllegalStateException("Group by clause has already been set: " + this.groupByClause);
    }
    this.groupByClause = groupByClause;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public String getHavingClause() {
    return havingClause;
  }

  /** {@inheritDoc} */
  @Override
  public Entity.Definition setHavingClause(final String havingClause) {
    Objects.requireNonNull(havingClause, "havingClause");
    if (this.havingClause != null) {
      throw new IllegalStateException("Having clause has already been set: " + this.havingClause);
    }
    this.havingClause = havingClause;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public String getSelectTableName() {
    return selectTableName == null ? tableName : selectTableName;
  }

  /** {@inheritDoc} */
  @Override
  public Entity.Definition setSelectTableName(final String selectTableName) {
    Objects.requireNonNull(selectTableName, "selectTableName");
    this.selectTableName = selectTableName;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public String getSelectQuery() {
    return selectQuery;
  }

  /** {@inheritDoc} */
  @Override
  public boolean selectQueryContainsWhereClause() {
    return selectQueryContainsWhereClause;
  }

  /** {@inheritDoc} */
  @Override
  public Entity.Definition setSelectQuery(final String selectQuery, final boolean containsWhereClause) {
    Objects.requireNonNull(selectQuery, "selectQuery");
    this.selectQuery = selectQuery;
    this.selectQueryContainsWhereClause = containsWhereClause;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public Entity.ToString getStringProvider() {
    return stringProvider;
  }

  /** {@inheritDoc} */
  @Override
  public Entity.Definition setStringProvider(final Entity.ToString stringProvider) {
    Objects.requireNonNull(stringProvider, "stringProvider");
    this.stringProvider = stringProvider;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public Comparator<Entity> getComparator() {
    return comparator;
  }

  /** {@inheritDoc} */
  @Override
  public Entity.Definition setComparator(final Comparator<Entity> comparator) {
    Objects.requireNonNull(comparator, "comparator");
    this.comparator = comparator;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public Collection<String> getSearchPropertyIds() {
    if (searchPropertyIds == null) {
      return Collections.emptyList();
    }
    return Collections.unmodifiableCollection(searchPropertyIds);
  }

  /** {@inheritDoc} */
  @Override
  public Entity.Definition setSearchPropertyIds(final String... searchPropertyIds) {
    Objects.requireNonNull(searchPropertyIds, "searchPropertyIds");
    for (final String propertyId : searchPropertyIds) {
      final Property property = properties.get(propertyId);
      if (property == null) {
        throw new IllegalArgumentException("Property with ID '" + propertyId + "' not found in entity '" + getEntityId() + "'");
      }
      if (!properties.get(propertyId).isString()) {
        throw new IllegalArgumentException("Entity search property must be of type String: " + properties.get(propertyId));
      }
    }

    this.searchPropertyIds = Arrays.asList(searchPropertyIds);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public Map<String, Property> getPropertyMap() {
    return properties;
  }

  /** {@inheritDoc} */
  @Override
  public List<Property> getProperties() {
    return propertyList;
  }

  /** {@inheritDoc} */
  @Override
  public boolean hasDerivedProperties() {
    return !derivedProperties.isEmpty();
  }

  /** {@inheritDoc} */
  @Override
  public boolean hasDerivedProperties(final String propertyId) {
    return derivedProperties.containsKey(propertyId);
  }

  /** {@inheritDoc} */
  @Override
  public Collection<Property.DerivedProperty> getDerivedProperties(final String property) {
    final Collection<Property.DerivedProperty> derived = derivedProperties.get(property);
    if (derived == null) {
      return Collections.emptyList();
    }
    return derived;
  }

  /** {@inheritDoc} */
  @Override
  public List<Property.ColumnProperty> getPrimaryKeyProperties() {
    return primaryKeyProperties;
  }

  /** {@inheritDoc} */
  @Override
  public String getSelectColumnsString() {
    return selectColumnsString;
  }

  /** {@inheritDoc} */
  @Override
  public List<Property> getVisibleProperties() {
    return visibleProperties;
  }

  /** {@inheritDoc} */
  @Override
  public List<Property.ColumnProperty> getColumnProperties() {
    return columnProperties;
  }

  /** {@inheritDoc} */
  @Override
  public List<Property.TransientProperty> getTransientProperties() {
    return transientProperties;
  }

  /** {@inheritDoc} */
  @Override
  public List<Property.ForeignKeyProperty> getForeignKeyProperties() {
    return foreignKeyProperties;
  }

  /** {@inheritDoc} */
  @Override
  public boolean hasDenormalizedProperties() {
    return hasDenormalizedProperties;
  }

  /** {@inheritDoc} */
  @Override
  public boolean hasDenormalizedProperties(final String foreignKeyPropertyId) {
    return hasDenormalizedProperties && denormalizedProperties.containsKey(foreignKeyPropertyId);
  }

  /** {@inheritDoc} */
  @Override
  public List<Property.DenormalizedProperty> getDenormalizedProperties(final String foreignKeyPropertyId) {
    return denormalizedProperties.get(foreignKeyPropertyId);
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return entityId;
  }

  /** {@inheritDoc} */
  @Override
  public Entity.Definition setBackgroundColorProvider(final Entity.BackgroundColorProvider colorProvider) {
    this.backgroundColorProvider = colorProvider;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public Entity.Definition setValidator(final Entity.Validator validator) {
    this.validator = validator;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public Entity.Validator getValidator() {
    return validator;
  }

  /** {@inheritDoc} */
  @Override
  public ResultPacker<Entity> getResultPacker() {
    return resultPacker;
  }

  /** {@inheritDoc} */
  @Override
  public int compareTo(final Entity entity, final Entity entityToCompare) {
    Objects.requireNonNull(entity, "entity");
    Objects.requireNonNull(entityToCompare, "entityToCompare");
    return comparator.compare(entity, entityToCompare);
  }

  /** {@inheritDoc} */
  @Override
  public String toString(final Entity entity) {
    return stringProvider.toString(entity);
  }

  /** {@inheritDoc} */
  @Override
  public Object getBackgroundColor(final Entity entity, final Property property) {
    if (backgroundColorProvider == null) {
      return null;
    }

    return backgroundColorProvider.getBackgroundColor(entity, property);
  }
}
