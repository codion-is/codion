/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.TextUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.jminor.common.Util.nullOrEmpty;
import static org.jminor.common.Util.rejectNullOrEmpty;

/**
 * A class encapsulating a entity definition, such as table name, order by clause and properties.
 */
final class DefaultEntityDefinition implements Entity.Definition {

  private static final long serialVersionUID = 1;

  /**
   * The domainId
   */
  private final String domainId;

  /**
   * The entityId
   */
  private final String entityId;

  /**
   * The properties mapped to their respective ids
   */
  private final Map<String, Property> propertyMap;

  /**
   * A list view of the properties
   */
  private final List<Property> properties;

  /**
   * The name of the underlying table
   */
  private transient String tableName;

  /**
   * The table (view, query) from which to select the entity
   * Used if it differs from the one used for inserts/updates
   */
  private transient String selectTableName;

  /**
   * The caption to use for the entity type
   */
  private String caption;

  /**
   * The bean class, if any
   */
  private Class beanClass;

  /**
   * Holds the order by clause
   */
  private Entity.OrderBy orderBy;

  /**
   * Holds the group by clause
   */
  private transient String groupByClause;

  /**
   * Holds the having clause
   */
  private transient String havingClause;

  /**
   * The primary key value generator
   */
  private transient Entity.KeyGenerator keyGenerator = new DefaultKeyGenerator();

  /**
   * The key generator type
   */
  private Entity.KeyGenerator.Type keyGeneratorType = keyGenerator.getType();

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
  private Entity.ToString stringProvider = new DefaultStringProvider();

  /**
   * Provides the background color
   */
  private Entity.BackgroundColorProvider backgroundColorProvider;

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
  private transient String selectQuery;

  /**
   * Specifies whether or not the select query, if any, contains a where clause
   */
  private transient boolean selectQueryContainsWhereClause = false;

  /**
   * The IDs of the properties to use when performing a string based lookup on this entity
   */
  private Collection<String> searchPropertyIds;

  /**
   * The {@link org.jminor.framework.domain.Entity.ConditionProvider}s
   * mapped to their respective conditionIds
   */
  private transient Map<String, Entity.ConditionProvider> conditionProviders;

  /**
   * Links a set of derived property ids to a parent property id
   */
  private final Map<String, Set<Property.DerivedProperty>> derivedProperties;
  private final List<Property.ColumnProperty> primaryKeyProperties;
  private final Map<String, Property.ColumnProperty> primaryKeyPropertyMap;
  private final List<Property.ForeignKeyProperty> foreignKeyProperties;
  private final List<Property.TransientProperty> transientProperties;
  private final List<Property> visibleProperties;
  private final List<Property.ColumnProperty> columnProperties;
  private final Map<String, List<Property.DenormalizedProperty>> denormalizedProperties;
  private final boolean hasDenormalizedProperties;

  /**
   * Defines a new entity type with the entityId serving as the initial entity caption.
   * @throws IllegalArgumentException if no primary key property is specified
   */
  DefaultEntityDefinition(final String domainId, final String entityId, final String tableName,
                          final Map<String, Property> propertyMap,
                          final List<Property.ColumnProperty> columnProperties,
                          final List<Property.ForeignKeyProperty> foreignKeyProperties,
                          final List<Property.TransientProperty> transientProperties,
                          final Entity.Validator validator) {
    this.domainId = rejectNullOrEmpty(domainId, "domainId");
    this.entityId = rejectNullOrEmpty(entityId, "entityId");
    this.tableName = rejectNullOrEmpty(tableName, "tableName");
    this.caption = entityId;
    this.propertyMap = propertyMap;
    this.columnProperties = columnProperties;
    this.foreignKeyProperties = foreignKeyProperties;
    this.transientProperties = transientProperties;
    this.properties = unmodifiableList(new ArrayList(this.propertyMap.values()));
    this.primaryKeyProperties = unmodifiableList(getPrimaryKeyProperties(this.propertyMap.values()));
    this.primaryKeyPropertyMap = initializePrimaryKeyPropertyMap();
    this.visibleProperties = unmodifiableList(getVisibleProperties(this.propertyMap.values()));
    this.denormalizedProperties = unmodifiableMap(getDenormalizedProperties(this.propertyMap.values()));
    this.derivedProperties = initializeDerivedProperties(this.propertyMap.values());
    this.groupByClause = initializeGroupByClause(columnProperties);
    this.hasDenormalizedProperties = !this.denormalizedProperties.isEmpty();
    this.validator = validator;
  }

  /** {@inheritDoc} */
  @Override
  public String getEntityId() {
    return entityId;
  }

  /** {@inheritDoc} */
  @Override
  public Entity.Definition setTableName(final String tableName) {
    this.tableName = rejectNullOrEmpty(tableName, "tableName");
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public String getTableName() {
    return tableName;
  }

  /** {@inheritDoc} */
  @Override
  public Entity.Definition addConditionProvider(final String conditionId, final Entity.ConditionProvider conditionProvider) {
    rejectNullOrEmpty(conditionId, "contitionId");
    requireNonNull(conditionProvider, "conditionProvider");
    if (conditionProviders == null) {
      conditionProviders = new HashMap<>();
    }
    if (conditionProviders.containsKey(conditionId)) {
      throw new IllegalStateException("ConditionProvider with id " + conditionId + " has already been added");
    }
    conditionProviders.put(conditionId, conditionProvider);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public Entity.ConditionProvider getConditionProvider(final String conditionId) {
    requireNonNull(conditionId);
    if (conditionProviders != null) {
      final Entity.ConditionProvider conditionProvider = conditionProviders.get(conditionId);
      if (conditionProvider != null) {
        return conditionProvider;
      }
    }

    throw new IllegalArgumentException("ConditionProvider with id " + conditionId + " not found");
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
    this.caption = requireNonNull(caption, "caption");
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public Class getBeanClass() {
    return beanClass;
  }

  /** {@inheritDoc} */
  @Override
  public Entity.Definition setBeanClass(final Class beanClass) {
    this.beanClass = requireNonNull(beanClass, "beanClass");
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
    this.keyGenerator = requireNonNull(keyGenerator, "keyGenerator");
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
  public Entity.Definition setOrderBy(final Entity.OrderBy orderBy) {
    requireNonNull(orderBy, "orderBy");
    if (this.orderBy != null) {
      throw new IllegalStateException("Order by has already been set: " + this.orderBy);
    }
    this.orderBy = orderBy;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public Entity.OrderBy getOrderBy() {
    return orderBy;
  }

  /** {@inheritDoc} */
  @Override
  public String getGroupByClause() {
    return groupByClause;
  }

  /** {@inheritDoc} */
  @Override
  public Entity.Definition setGroupByClause(final String groupByClause) {
    requireNonNull(groupByClause, "groupByClause");
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
    requireNonNull(havingClause, "havingClause");
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
    this.selectTableName = requireNonNull(selectTableName, "selectTableName");
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
    this.selectQuery = requireNonNull(selectQuery, "selectQuery");
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
    this.stringProvider = requireNonNull(stringProvider, "stringProvider");
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
    this.comparator = requireNonNull(comparator, "comparator");
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public Collection<String> getSearchPropertyIds() {
    if (searchPropertyIds == null) {
      return emptyList();
    }
    return unmodifiableCollection(searchPropertyIds);
  }

  /** {@inheritDoc} */
  @Override
  public Entity.Definition setSearchPropertyIds(final String... searchPropertyIds) {
    requireNonNull(searchPropertyIds, "searchPropertyIds");
    for (final String propertyId : searchPropertyIds) {
      final Property property = propertyMap.get(propertyId);
      if (property == null) {
        throw new IllegalArgumentException("Property with ID '" + propertyId + "' not found in entity '" + getEntityId() + "'");
      }
      if (!propertyMap.get(propertyId).isString()) {
        throw new IllegalArgumentException("Entity search property must be of type String: " + propertyMap.get(propertyId));
      }
    }

    this.searchPropertyIds = asList(searchPropertyIds);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public Map<String, Property> getPropertyMap() {
    return propertyMap;
  }

  /** {@inheritDoc} */
  @Override
  public List<Property> getProperties() {
    return properties;
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

    return derived == null ? emptyList() : derived;
  }

  /** {@inheritDoc} */
  @Override
  public List<Property.ColumnProperty> getPrimaryKeyProperties() {
    return primaryKeyProperties;
  }

  @Override
  public Map<String, Property.ColumnProperty> getPrimaryKeyPropertyMap() {
    return primaryKeyPropertyMap;
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
    this.backgroundColorProvider = requireNonNull(colorProvider, "colorProvider");
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public Entity.Definition setValidator(final Entity.Validator validator) {
    this.validator = requireNonNull(validator, "validator");
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public Entity.Validator getValidator() {
    return validator;
  }

  /** {@inheritDoc} */
  @Override
  public int compareTo(final Entity entity, final Entity entityToCompare) {
    requireNonNull(entity, "entity");
    requireNonNull(entityToCompare, "entityToCompare");
    return comparator.compare(entity, entityToCompare);
  }

  /** {@inheritDoc} */
  @Override
  public String toString(final Entity entity) {
    return stringProvider.toString(requireNonNull(entity, "entity"));
  }

  /** {@inheritDoc} */
  @Override
  public Object getBackgroundColor(final Entity entity, final Property property) {
    if (backgroundColorProvider == null) {
      return null;
    }

    return backgroundColorProvider.getBackgroundColor(entity, property);
  }

  private Map<String, Property.ColumnProperty> initializePrimaryKeyPropertyMap() {
    final Map<String, Property.ColumnProperty> map = new HashMap<>(this.primaryKeyProperties.size());
    this.primaryKeyProperties.forEach(property -> map.put(property.getPropertyId(), property));

    return unmodifiableMap(map);
  }

  private static Map<String, List<Property.DenormalizedProperty>> getDenormalizedProperties(final Collection<Property> properties) {
    final Map<String, List<Property.DenormalizedProperty>> denormalizedPropertiesMap = new HashMap<>(properties.size());
    for (final Property property : properties) {
      if (property instanceof Property.DenormalizedProperty) {
        final Property.DenormalizedProperty denormalizedProperty = (Property.DenormalizedProperty) property;
        final Collection<Property.DenormalizedProperty> denormalizedProperties =
                denormalizedPropertiesMap.computeIfAbsent(denormalizedProperty.getForeignKeyPropertyId(), k -> new ArrayList<>());
        denormalizedProperties.add(denormalizedProperty);
      }
    }

    return denormalizedPropertiesMap;
  }

  private static Map<String, Set<Property.DerivedProperty>> initializeDerivedProperties(final Collection<Property> properties) {
    final Map<String, Set<Property.DerivedProperty>> derivedProperties = new HashMap<>();
    for (final Property property : properties) {
      if (property instanceof Property.DerivedProperty) {
        final Collection<String> derived = ((Property.DerivedProperty) property).getSourcePropertyIds();
        if (!nullOrEmpty(derived)) {
          for (final String parentLinkPropertyId : derived) {
            linkProperties(derivedProperties, parentLinkPropertyId, (Property.DerivedProperty) property);
          }
        }
      }
    }

    return derivedProperties;
  }

  private static void linkProperties(final Map<String, Set<Property.DerivedProperty>> derivedProperties,
                                     final String parentPropertyId, final Property.DerivedProperty derivedProperty) {
    if (!derivedProperties.containsKey(parentPropertyId)) {
      derivedProperties.put(parentPropertyId, new HashSet<>());
    }
    derivedProperties.get(parentPropertyId).add(derivedProperty);
  }

  private static List<Property.ColumnProperty> getPrimaryKeyProperties(final Collection<Property> properties) {
    return properties.stream().filter(property -> property instanceof Property.ColumnProperty
            && ((Property.ColumnProperty) property).isPrimaryKeyProperty()).map(property -> (Property.ColumnProperty) property)
            .sorted((pk1, pk2) -> {
              final Integer index1 = pk1.getPrimaryKeyIndex();
              final Integer index2 = pk2.getPrimaryKeyIndex();

              return index1.compareTo(index2);
            }).collect(toList());
  }

  private static List<Property> getVisibleProperties(final Collection<Property> properties) {
    return properties.stream().filter(property -> !property.isHidden()).collect(toList());
  }

  /**
   * @param columnProperties the column properties
   * @return a list of grouping columns separated with a comma, to serve as a group by clause,
   * null if no grouping properties are defined
   */
  private static String initializeGroupByClause(final Collection<Property.ColumnProperty> columnProperties) {
    final List<Property> groupingProperties = columnProperties.stream()
            .filter(Property.ColumnProperty::isGroupingColumn).collect(toList());
    if (groupingProperties.isEmpty()) {
      return null;
    }

    final StringBuilder stringBuilder = new StringBuilder();
    int i = 0;
    for (final Property property : groupingProperties) {
      stringBuilder.append(property.getPropertyId());
      if (i++ < groupingProperties.size() - 1) {
        stringBuilder.append(", ");
      }
    }

    return stringBuilder.toString();
  }

  /**
   * A ToString implementation using the entityId plus primary key value.
   */
  private static final class DefaultStringProvider implements Entity.ToString {

    private static final long serialVersionUID = 1;

    @Override
    public String toString(final Entity entity) {
      return entity.getEntityId() + ": " + entity.getKey();
    }
  }

  /**
   * A no-op key generator.
   */
  private static final class DefaultKeyGenerator implements Entity.KeyGenerator {

    @Override
    public Type getType() {
      return Type.NONE;
    }
  }
}
