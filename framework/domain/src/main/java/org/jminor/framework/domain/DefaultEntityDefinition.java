/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.TextUtil;
import org.jminor.framework.domain.property.ColumnProperty;
import org.jminor.framework.domain.property.DenormalizedProperty;
import org.jminor.framework.domain.property.DerivedProperty;
import org.jminor.framework.domain.property.ForeignKeyProperty;
import org.jminor.framework.domain.property.Property;
import org.jminor.framework.domain.property.TransientProperty;

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
  private final transient String tableName;

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
  private final Map<String, Set<DerivedProperty>> derivedProperties;
  private final List<ColumnProperty> primaryKeyProperties;
  private final Map<String, ColumnProperty> primaryKeyPropertyMap;
  private final List<ForeignKeyProperty> foreignKeyProperties;
  private final List<TransientProperty> transientProperties;
  private final List<Property> visibleProperties;
  private final List<ColumnProperty> columnProperties;
  private final List<ColumnProperty> selectableColumnProperties;
  private final Map<String, List<DenormalizedProperty>> denormalizedProperties;
  private final boolean hasDenormalizedProperties;

  /**
   * Defines a new entity type with the entityId serving as the initial entity caption.
   * @throws IllegalArgumentException if no primary key property is specified
   */
  DefaultEntityDefinition(final String domainId, final String entityId, final String tableName,
                          final Map<String, Property> propertyMap,
                          final List<ColumnProperty> columnProperties,
                          final List<ForeignKeyProperty> foreignKeyProperties,
                          final List<TransientProperty> transientProperties,
                          final Entity.Validator validator) {
    this.domainId = rejectNullOrEmpty(domainId, "domainId");
    this.entityId = rejectNullOrEmpty(entityId, "entityId");
    this.tableName = rejectNullOrEmpty(tableName, "tableName");
    this.caption = entityId;
    this.propertyMap = propertyMap;
    this.columnProperties = columnProperties;
    this.foreignKeyProperties = foreignKeyProperties;
    this.transientProperties = transientProperties;
    this.selectableColumnProperties = unmodifiableList(getSelectableProperties(columnProperties));
    this.properties = unmodifiableList(new ArrayList<>(this.propertyMap.values()));
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
  public String getTableName() {
    return tableName;
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
  public Class getBeanClass() {
    return beanClass;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isSmallDataset() {
    return smallDataset;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isStaticData() {
    return staticData;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isReadOnly() {
    return readOnly;
  }

  /** {@inheritDoc} */
  @Override
  public Entity.KeyGenerator getKeyGenerator() {
    return keyGenerator;
  }

  /** {@inheritDoc} */
  @Override
  public Entity.KeyGenerator.Type getKeyGeneratorType() {
    return keyGeneratorType;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isPrimaryKeyAutoGenerated() {
    return !getKeyGeneratorType().isManual();
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
  public String getHavingClause() {
    return havingClause;
  }

  /** {@inheritDoc} */
  @Override
  public String getSelectTableName() {
    return selectTableName == null ? tableName : selectTableName;
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
  public Entity.ToString getStringProvider() {
    return stringProvider;
  }

  /** {@inheritDoc} */
  @Override
  public Comparator<Entity> getComparator() {
    return comparator;
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
  public Collection<ColumnProperty> getSearchProperties() {
    return getSearchPropertyIds().stream().map(this::getColumnProperty).collect(toList());
  }

  /** {@inheritDoc} */
  @Override
  public ColumnProperty getColumnProperty(final String propertyId) {
    final Property property = getProperty(propertyId);
    if (!(property instanceof ColumnProperty)) {
      throw new IllegalArgumentException(propertyId + ", " + property.getClass() + " does not implement Property.ColumnProperty");
    }

    return (ColumnProperty) property;
  }

  /** {@inheritDoc} */
  @Override
  public Property getProperty(final String propertyId) {
    requireNonNull(propertyId, "propertyId");
    final Property property = getPropertyMap().get(propertyId);
    if (property == null) {
      throw new IllegalArgumentException("Property '" + propertyId + "' not found in entity: " + entityId);
    }

    return property;
  }

  /** {@inheritDoc} */
  @Override
  public List<Property> getProperties(final Collection<String> propertyIds) {
    requireNonNull(propertyIds, "propertyIds");

    return propertyIds.stream().map(this::getProperty).collect(toList());
  }

  /** {@inheritDoc} */
  @Override
  public ColumnProperty getSelectableColumnProperty(final String propertyId) {
    final ColumnProperty property = getColumnProperty(propertyId);
    if (!property.isSelectable()) {
      throw new IllegalArgumentException(propertyId + " is not selectable");
    }

    return property;
  }

  /** {@inheritDoc} */
  @Override
  public List<ColumnProperty> getColumnProperties(final Collection<String> propertyIds) {
    return propertyIds.stream().map(this::getColumnProperty).collect(toList());
  }

  /** {@inheritDoc} */
  @Override
  public boolean hasSingleIntegerPrimaryKey() {
    final List<ColumnProperty> primaryKeyProperties = getPrimaryKeyProperties();
    return primaryKeyProperties.size() == 1 && primaryKeyProperties.get(0).isInteger();
  }

  /** {@inheritDoc} */
  @Override
  public List<ColumnProperty> getWritableColumnProperties(final boolean includePrimaryKeyProperties,
                                                          final boolean includeNonUpdatable) {
    return getColumnProperties().stream()
            .filter(property -> !property.isReadOnly() &&
                    (includeNonUpdatable || property.isUpdatable()) &&
                    (includePrimaryKeyProperties || !property.isPrimaryKeyProperty()))
            .collect(toList());
  }

  /** {@inheritDoc} */
  @Override
  public List<Property> getUpdatableProperties() {
    final List<ColumnProperty> columnProperties = getWritableColumnProperties(
            getKeyGeneratorType().isManual(), false);
    columnProperties.removeIf(property -> property.isForeignKeyProperty() || property.isDenormalized());
    final List<Property> updatable = new ArrayList<>(columnProperties);
    final Collection<ForeignKeyProperty> foreignKeyProperties = getForeignKeyProperties();
    for (final ForeignKeyProperty foreignKeyProperty : foreignKeyProperties) {
      if (!foreignKeyProperty.isReadOnly() && foreignKeyProperty.isUpdatable()) {
        updatable.add(foreignKeyProperty);
      }
    }

    return updatable;
  }

  /** {@inheritDoc} */
  @Override
  public List<Property> getProperties(final boolean includeHidden) {
    return includeHidden ? getProperties() : getVisibleProperties();
  }

  /** {@inheritDoc} */
  @Override
  public List<ColumnProperty> getSelectableColumnProperties(final Collection<String> propertyIds) {

    return propertyIds.stream().map(this::getSelectableColumnProperty).collect(toList());
  }

  /** {@inheritDoc} */
  @Override
  public List<ForeignKeyProperty> getForeignKeyProperties(final String foreignEntityId) {
    return getForeignKeyProperties().stream().filter(foreignKeyProperty ->
            foreignKeyProperty.getForeignEntityId().equals(foreignEntityId)).collect(toList());
  }

  /** {@inheritDoc} */
  @Override
  public final ForeignKeyProperty getForeignKeyProperty(final String propertyId) {
    final List<ForeignKeyProperty> foreignKeyProperties = getForeignKeyProperties();
    for (int i = 0; i < foreignKeyProperties.size(); i++) {
      final ForeignKeyProperty foreignKeyProperty = foreignKeyProperties.get(i);
      if (foreignKeyProperty.is(propertyId)) {
        return foreignKeyProperty;
      }
    }

    throw new IllegalArgumentException("Foreign key property with id: " + propertyId + " not found in entity of type: " + entityId);
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
  public Collection<DerivedProperty> getDerivedProperties(final String property) {
    final Collection<DerivedProperty> derived = derivedProperties.get(property);

    return derived == null ? emptyList() : derived;
  }

  /** {@inheritDoc} */
  @Override
  public List<ColumnProperty> getPrimaryKeyProperties() {
    return primaryKeyProperties;
  }

  @Override
  public Map<String, ColumnProperty> getPrimaryKeyPropertyMap() {
    return primaryKeyPropertyMap;
  }

  /** {@inheritDoc} */
  @Override
  public List<Property> getVisibleProperties() {
    return visibleProperties;
  }

  /** {@inheritDoc} */
  @Override
  public List<ColumnProperty> getColumnProperties() {
    return columnProperties;
  }

  /** {@inheritDoc} */
  @Override
  public List<ColumnProperty> getSelectableColumnProperties() {
    return selectableColumnProperties;
  }

  /** {@inheritDoc} */
  @Override
  public List<TransientProperty> getTransientProperties() {
    return transientProperties;
  }

  /** {@inheritDoc} */
  @Override
  public List<ForeignKeyProperty> getForeignKeyProperties() {
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
  public List<DenormalizedProperty> getDenormalizedProperties(final String foreignKeyPropertyId) {
    return denormalizedProperties.get(foreignKeyPropertyId);
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return entityId;
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

  private void addConditionProvider(final String conditionId, final Entity.ConditionProvider conditionProvider) {
    rejectNullOrEmpty(conditionId, "contitionId");
    requireNonNull(conditionProvider, "conditionProvider");
    if (conditionProviders == null) {
      conditionProviders = new HashMap<>();
    }
    if (conditionProviders.containsKey(conditionId)) {
      throw new IllegalStateException("ConditionProvider with id " + conditionId + " has already been added");
    }
    conditionProviders.put(conditionId, conditionProvider);
  }

  private void setCaption(final String caption) {
    this.caption = requireNonNull(caption, "caption");
  }

  private void setBeanClass(final Class beanClass) {
    this.beanClass = requireNonNull(beanClass, "beanClass");
  }

  private void setSmallDataset(final boolean smallDataset) {
    this.smallDataset = smallDataset;
  }

  private void setStaticData(final boolean staticData) {
    this.staticData = staticData;
  }

  private void setReadOnly(final boolean readOnly) {
    this.readOnly = readOnly;
  }

  private void setKeyGenerator(final Entity.KeyGenerator keyGenerator) {
    this.keyGenerator = requireNonNull(keyGenerator, "keyGenerator");
    this.keyGeneratorType = keyGenerator.getType();
  }

  private void setOrderBy(final Entity.OrderBy orderBy) {
    requireNonNull(orderBy, "orderBy");
    if (this.orderBy != null) {
      throw new IllegalStateException("Order by has already been set: " + this.orderBy);
    }
    this.orderBy = orderBy;
  }

  private void setGroupByClause(final String groupByClause) {
    requireNonNull(groupByClause, "groupByClause");
    if (this.groupByClause != null) {
      throw new IllegalStateException("Group by clause has already been set: " + this.groupByClause);
    }
    this.groupByClause = groupByClause;
  }

  private void setHavingClause(final String havingClause) {
    requireNonNull(havingClause, "havingClause");
    if (this.havingClause != null) {
      throw new IllegalStateException("Having clause has already been set: " + this.havingClause);
    }
    this.havingClause = havingClause;
  }

  private void setSelectTableName(final String selectTableName) {
    this.selectTableName = requireNonNull(selectTableName, "selectTableName");
  }

  private void setSelectQuery(final String selectQuery, final boolean containsWhereClause) {
    this.selectQuery = requireNonNull(selectQuery, "selectQuery");
    this.selectQueryContainsWhereClause = containsWhereClause;
  }

  private void setComparator(final Comparator<Entity> comparator) {
    this.comparator = requireNonNull(comparator, "comparator");
  }

  private void setStringProvider(final Entity.ToString stringProvider) {
    this.stringProvider = requireNonNull(stringProvider, "stringProvider");
  }

  private void setSearchPropertyIds(final String... searchPropertyIds) {
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
  }

  private void setBackgroundColorProvider(final Entity.BackgroundColorProvider colorProvider) {
    this.backgroundColorProvider = requireNonNull(colorProvider, "colorProvider");
  }

  private void setValidator(final Entity.Validator validator) {
    this.validator = requireNonNull(validator, "validator");
  }

  private Map<String, ColumnProperty> initializePrimaryKeyPropertyMap() {
    final Map<String, ColumnProperty> map = new HashMap<>(this.primaryKeyProperties.size());
    this.primaryKeyProperties.forEach(property -> map.put(property.getPropertyId(), property));

    return unmodifiableMap(map);
  }

  private static Map<String, List<DenormalizedProperty>> getDenormalizedProperties(final Collection<Property> properties) {
    final Map<String, List<DenormalizedProperty>> denormalizedPropertiesMap = new HashMap<>(properties.size());
    for (final Property property : properties) {
      if (property instanceof DenormalizedProperty) {
        final DenormalizedProperty denormalizedProperty = (DenormalizedProperty) property;
        final Collection<DenormalizedProperty> denormalizedProperties =
                denormalizedPropertiesMap.computeIfAbsent(denormalizedProperty.getForeignKeyPropertyId(), k -> new ArrayList<>());
        denormalizedProperties.add(denormalizedProperty);
      }
    }

    return denormalizedPropertiesMap;
  }

  private static Map<String, Set<DerivedProperty>> initializeDerivedProperties(final Collection<Property> properties) {
    final Map<String, Set<DerivedProperty>> derivedProperties = new HashMap<>();
    for (final Property property : properties) {
      if (property instanceof DerivedProperty) {
        final Collection<String> derived = ((DerivedProperty) property).getSourcePropertyIds();
        if (!nullOrEmpty(derived)) {
          for (final String parentLinkPropertyId : derived) {
            linkProperties(derivedProperties, parentLinkPropertyId, (DerivedProperty) property);
          }
        }
      }
    }

    return derivedProperties;
  }

  private static void linkProperties(final Map<String, Set<DerivedProperty>> derivedProperties,
                                     final String parentPropertyId, final DerivedProperty derivedProperty) {
    if (!derivedProperties.containsKey(parentPropertyId)) {
      derivedProperties.put(parentPropertyId, new HashSet<>());
    }
    derivedProperties.get(parentPropertyId).add(derivedProperty);
  }

  private static List<ColumnProperty> getPrimaryKeyProperties(final Collection<Property> properties) {
    return properties.stream().filter(property -> property instanceof ColumnProperty
            && ((ColumnProperty) property).isPrimaryKeyProperty()).map(property -> (ColumnProperty) property)
            .sorted((pk1, pk2) -> {
              final Integer index1 = pk1.getPrimaryKeyIndex();
              final Integer index2 = pk2.getPrimaryKeyIndex();

              return index1.compareTo(index2);
            }).collect(toList());
  }

  private static List<ColumnProperty> getSelectableProperties(final List<ColumnProperty> columnProperties) {
    return columnProperties.stream().filter(ColumnProperty::isSelectable).collect(toList());
  }

  private static List<Property> getVisibleProperties(final Collection<Property> properties) {
    return properties.stream().filter(property -> !property.isHidden()).collect(toList());
  }

  /**
   * @param columnProperties the column properties
   * @return a list of grouping columns separated with a comma, to serve as a group by clause,
   * null if no grouping properties are defined
   */
  private static String initializeGroupByClause(final Collection<ColumnProperty> columnProperties) {
    final List<Property> groupingProperties = columnProperties.stream()
            .filter(ColumnProperty::isGroupingColumn).collect(toList());
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

  static class EntityDefiner implements Entity.Definer {

    private final DefaultEntityDefinition definition;

    EntityDefiner(final DefaultEntityDefinition definition) {
      this.definition = definition;
    }

    @Override
    public Entity.Definer addConditionProvider(final String conditionId, final Entity.ConditionProvider conditionProvider) {
      definition.addConditionProvider(conditionId, conditionProvider);
      return this;
    }

    @Override
    public Entity.Definer setCaption(final String caption) {
      definition.setCaption(caption);
      return this;
    }

    @Override
    public Entity.Definer setBeanClass(final Class beanClass) {
      definition.setBeanClass(beanClass);
      return this;
    }

    @Override
    public Entity.Definer setSmallDataset(final boolean smallDataset) {
      definition.setSmallDataset(smallDataset);
      return this;
    }

    @Override
    public Entity.Definer setStaticData(final boolean staticData) {
      definition.setStaticData(staticData);
      return this;
    }

    @Override
    public Entity.Definer setReadOnly(final boolean readOnly) {
      definition.setReadOnly(readOnly);
      return this;
    }

    @Override
    public Entity.Definer setKeyGenerator(final Entity.KeyGenerator keyGenerator) {
      definition.setKeyGenerator(keyGenerator);
      return this;
    }

    @Override
    public Entity.Definer setOrderBy(final Entity.OrderBy orderBy) {
      definition.setOrderBy(orderBy);
      return this;
    }

    @Override
    public Entity.Definer setGroupByClause(final String groupByClause) {
      definition.setGroupByClause(groupByClause);
      return this;
    }

    @Override
    public Entity.Definer setHavingClause(final String havingClause) {
      definition.setHavingClause(havingClause);
      return this;
    }

    @Override
    public Entity.Definer setSelectTableName(final String selectTableName) {
      definition.setSelectTableName(selectTableName);
      return this;
    }

    @Override
    public Entity.Definer setSelectQuery(final String selectQuery, final boolean containsWhereClause) {
      definition.setSelectQuery(selectQuery, containsWhereClause);
      return this;
    }

    @Override
    public Entity.Definer setComparator(final Comparator<Entity> comparator) {
      definition.setComparator(comparator);
      return this;
    }

    @Override
    public Entity.Definer setStringProvider(final Entity.ToString stringProvider) {
      definition.setStringProvider(stringProvider);
      return this;
    }

    @Override
    public Entity.Definer setSearchPropertyIds(final String... searchPropertyIds) {
      definition.setSearchPropertyIds(searchPropertyIds);
      return this;
    }

    @Override
    public Entity.Definer setBackgroundColorProvider(final Entity.BackgroundColorProvider colorProvider) {
      definition.setBackgroundColorProvider(colorProvider);
      return this;
    }

    @Override
    public Entity.Definer setValidator(final Entity.Validator validator) {
      definition.setValidator(validator);
      return this;
    }
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
