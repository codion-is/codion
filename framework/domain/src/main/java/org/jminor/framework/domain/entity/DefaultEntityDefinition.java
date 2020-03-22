/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.entity;

import org.jminor.common.Text;
import org.jminor.framework.domain.property.BlobProperty;
import org.jminor.framework.domain.property.ColumnProperty;
import org.jminor.framework.domain.property.DenormalizedProperty;
import org.jminor.framework.domain.property.DerivedProperty;
import org.jminor.framework.domain.property.ForeignKeyProperty;
import org.jminor.framework.domain.property.MirrorProperty;
import org.jminor.framework.domain.property.Property;
import org.jminor.framework.domain.property.TransientProperty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.jminor.common.Util.nullOrEmpty;
import static org.jminor.common.Util.rejectNullOrEmpty;

/**
 * A class encapsulating a entity definition, such as table name, order by clause and properties.
 */
final class DefaultEntityDefinition implements EntityDefinition {

  private static final long serialVersionUID = 1;

  /**
   * The entityId
   */
  private final String entityId;

  /**
   * The domainId
   */
  private String domainId;

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
  private OrderBy orderBy;

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
   * True if a key generator has been set for this entity type
   */
  private boolean keyGenerated;

  /**
   * The Entity.ToString instance used when toString() is called for this entity type
   */
  private Function<Entity, String> stringProvider = new DefaultStringProvider();

  /**
   * Provides the color
   */
  private ColorProvider colorProvider = new NullColorProvider();

  /**
   * The comparator
   */
  private Comparator<Entity> comparator = Text.getSpaceAwareCollator();

  /**
   * The validator
   */
  private Validator validator = new DefaultEntityValidator();

  /**
   * The IDs of the properties to use when performing a string based lookup on this entity
   */
  private Collection<String> searchPropertyIds;

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
  private transient KeyGenerator keyGenerator = new KeyGenerator() {};

  /**
   * A custom sql query used when selecting entities of this type
   */
  private transient String selectQuery;

  /**
   * Specifies whether or not the select query, if any, contains a where clause
   */
  private transient boolean selectQueryContainsWhereClause = false;

  /**
   * The {@link ConditionProvider}s
   * mapped to their respective conditionIds
   */
  private transient Map<String, ConditionProvider> conditionProviders;

  private final Map<String, Property> propertyMap;
  private final List<Property> properties;
  private final Set<Property> propertySet;
  private final List<Property> visibleProperties;
  private final List<ColumnProperty> columnProperties;
  private final List<ColumnProperty> lazyLoadedBlobProperties;
  private final List<ColumnProperty> selectableColumnProperties;
  private final List<ColumnProperty> primaryKeyProperties;
  private final Map<String, ColumnProperty> primaryKeyPropertyMap;
  private final List<ForeignKeyProperty> foreignKeyProperties;
  private final Map<String, ForeignKeyProperty> foreignKeyPropertyMap;
  private final Map<String, List<ForeignKeyProperty>> columnPropertyForeignKeyProperties;
  private final Map<String, Set<DerivedProperty>> derivedProperties;
  private final List<TransientProperty> transientProperties;
  private final Map<String, List<DenormalizedProperty>> denormalizedProperties;
  private final boolean hasDenormalizedProperties;

  /**
   * Defines a new entity type with the entityId serving as the initial entity caption.
   */
  DefaultEntityDefinition(final String entityId, final String tableName, final Property.Builder... propertyBuilders) {
    this.entityId = rejectNullOrEmpty(entityId, "entityId");
    this.tableName = rejectNullOrEmpty(tableName, "tableName");
    this.caption = entityId;
    this.propertyMap = initializePropertyMap(entityId, propertyBuilders);
    this.properties = unmodifiableList(new ArrayList<>(propertyMap.values()));
    this.propertySet = new HashSet<>(propertyMap.values());
    this.visibleProperties = unmodifiableList(getVisibleProperties(propertyMap.values()));
    this.columnProperties = unmodifiableList(getColumnProperties(propertyMap.values()));
    this.lazyLoadedBlobProperties = initializeLazyLoadedBlobProperties(columnProperties);
    this.selectableColumnProperties = unmodifiableList(getSelectableProperties(columnProperties, lazyLoadedBlobProperties));
    this.primaryKeyProperties = unmodifiableList(getPrimaryKeyProperties(propertyMap.values()));
    this.primaryKeyPropertyMap = initializePrimaryKeyPropertyMap();
    this.foreignKeyProperties = unmodifiableList(getForeignKeyProperties(propertyMap.values()));
    this.foreignKeyPropertyMap = initializeForeignKeyPropertyMap(foreignKeyProperties);
    this.columnPropertyForeignKeyProperties = initializeColumnPropertyForeignKeyProperties(foreignKeyProperties);
    this.derivedProperties = initializeDerivedProperties(propertyMap.values());
    this.transientProperties = unmodifiableList(getTransientProperties(propertyMap.values()));
    this.denormalizedProperties = unmodifiableMap(getDenormalizedProperties(propertyMap.values()));
    this.groupByClause = initializeGroupByClause(columnProperties);
    this.hasDenormalizedProperties = !denormalizedProperties.isEmpty();
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
  public ConditionProvider getConditionProvider(final String conditionId) {
    requireNonNull(conditionId);
    if (conditionProviders != null) {
      final ConditionProvider conditionProvider = conditionProviders.get(conditionId);
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
  public KeyGenerator getKeyGenerator() {
    return keyGenerator;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isKeyGenerated() {
    return keyGenerated;
  }

  /** {@inheritDoc} */
  @Override
  public OrderBy getOrderBy() {
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
  public Function<Entity, String> getStringProvider() {
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
      throw new IllegalArgumentException(propertyId + " is not a ColumnProperty");
    }

    return (ColumnProperty) property;
  }

  /** {@inheritDoc} */
  @Override
  public Property getProperty(final String propertyId) {
    requireNonNull(propertyId, "propertyId");
    final Property property = propertyMap.get(propertyId);
    if (property == null) {
      throw new IllegalArgumentException("Property '" + propertyId + "' not found in entity: " + entityId);
    }

    return property;
  }

  @Override
  public ColumnProperty getPrimaryKeyProperty(final String propertyId) {
    final ColumnProperty property = primaryKeyPropertyMap.get(propertyId);
    if (property == null) {
      throw new IllegalArgumentException("Primary key property " + propertyId + " not found in entity: " + entityId);
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
  public List<ColumnProperty> getColumnProperties(final List<String> propertyIds) {
    requireNonNull(propertyIds, "propertyIds");
    final List<ColumnProperty> theProperties = new ArrayList<>(propertyIds.size());
    for (int i = 0; i < propertyIds.size(); i++) {
      theProperties.add(getColumnProperty(propertyIds.get(i)));
    }

    return theProperties;
  }

  /** {@inheritDoc} */
  @Override
  public boolean hasSingleIntegerPrimaryKey() {
    return primaryKeyProperties.size() == 1 && primaryKeyProperties.get(0).isInteger();
  }

  /** {@inheritDoc} */
  @Override
  public List<ColumnProperty> getWritableColumnProperties(final boolean includePrimaryKeyProperties,
                                                          final boolean includeNonUpdatable) {
    return columnProperties.stream()
            .filter(property -> property.isInsertable() &&
                    (includeNonUpdatable || property.isUpdatable()) &&
                    (includePrimaryKeyProperties || !property.isPrimaryKeyProperty()))
            .collect(toList());
  }

  /** {@inheritDoc} */
  @Override
  public List<Property> getUpdatableProperties() {
    final List<ColumnProperty> writableColumnProperties = getWritableColumnProperties(
            !isKeyGenerated(), false);
    writableColumnProperties.removeIf(property -> property.isForeignKeyProperty() || property.isDenormalized());
    final List<Property> updatable = new ArrayList<>(writableColumnProperties);
    for (final ForeignKeyProperty foreignKeyProperty : foreignKeyProperties) {
      if (foreignKeyProperty.isUpdatable()) {
        updatable.add(foreignKeyProperty);
      }
    }

    return updatable;
  }

  /** {@inheritDoc} */
  @Override
  public List<ColumnProperty> getSelectableColumnProperties(final List<String> propertyIds) {
    final List<ColumnProperty> theProperties = new ArrayList<>(propertyIds.size());
    for (int i = 0; i < propertyIds.size(); i++) {
      theProperties.add(getSelectableColumnProperty(propertyIds.get(i)));
    }

    return theProperties;
  }

  /** {@inheritDoc} */
  @Override
  public List<ForeignKeyProperty> getForeignKeyReferences(final String foreignEntityId) {
    return getForeignKeyProperties().stream().filter(foreignKeyProperty ->
            foreignKeyProperty.getForeignEntityId().equals(foreignEntityId)).collect(toList());
  }

  /** {@inheritDoc} */
  @Override
  public ForeignKeyProperty getForeignKeyProperty(final String propertyId) {
    final ForeignKeyProperty property = foreignKeyPropertyMap.get(propertyId);
    if (property == null) {
      throw new IllegalArgumentException("Foreign key property with id: " + propertyId + " not found in entity of type: " + entityId);
    }

    return property;
  }

  /** {@inheritDoc} */
  @Override
  public List<ForeignKeyProperty> getForeignKeyProperties(final String columnPropertyId) {
    return columnPropertyForeignKeyProperties.computeIfAbsent(columnPropertyId, propertyId -> Collections.emptyList());
  }

  /** {@inheritDoc} */
  @Override
  public List<Property> getProperties() {
    return properties;
  }

  /** {@inheritDoc} */
  @Override
  public Set<Property> getPropertySet() {
    return propertySet;
  }

  /** {@inheritDoc} */
  @Override
  public boolean hasPrimaryKey() {
    return !primaryKeyProperties.isEmpty();
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
  public List<ColumnProperty> getLazyLoadedBlobProperties() {
    return lazyLoadedBlobProperties;
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
  public Validator getValidator() {
    return validator;
  }

  /** {@inheritDoc} */
  @Override
  public ColorProvider getColorProvider() {
    return colorProvider;
  }

  /**
   * @return a {@link EntityDefinition.Builder} for this definition instance
   */
  Builder builder() {
    return new DefaultBuilder(this);
  }

  private Map<String, ColumnProperty> initializePrimaryKeyPropertyMap() {
    final Map<String, ColumnProperty> map = new HashMap<>(this.primaryKeyProperties.size());
    this.primaryKeyProperties.forEach(property -> map.put(property.getPropertyId(), property));

    return unmodifiableMap(map);
  }

  private static Map<String, Property> initializePropertyMap(final String entityId, final Property.Builder... propertyBuilders) {
    final Map<String, Property> propertyMap = new LinkedHashMap<>(propertyBuilders.length);
    for (final Property.Builder propertyBuilder : propertyBuilders) {
      validateAndAddProperty(propertyBuilder, entityId, propertyMap);
      if (propertyBuilder instanceof ForeignKeyProperty.Builder) {
        initializeForeignKeyProperty(entityId, propertyMap, (ForeignKeyProperty.Builder) propertyBuilder);
      }
    }
    validatePrimaryKeyProperties(propertyMap);

    return unmodifiableMap(propertyMap);
  }

  private static void initializeForeignKeyProperty(final String entityId, final Map<String, Property> propertyMap,
                                                   final ForeignKeyProperty.Builder foreignKeyPropertyBuilder) {
    for (final ColumnProperty.Builder propertyBuilder : foreignKeyPropertyBuilder.getColumnPropertyBuilders()) {
      if (!(propertyBuilder.get() instanceof MirrorProperty)) {
        validateAndAddProperty(propertyBuilder, entityId, propertyMap);
      }
    }
  }

  private static void validateAndAddProperty(final Property.Builder propertyBuilder, final String entityId,
                                             final Map<String, Property> propertyMap) {
    final Property property = propertyBuilder.get();
    checkIfUniquePropertyId(property, entityId, propertyMap);
    propertyBuilder.entityId(entityId);
    propertyMap.put(property.getPropertyId(), property);
  }

  private static void checkIfUniquePropertyId(final Property property, final String entityId,
                                              final Map<String, Property> propertyMap) {
    if (propertyMap.containsKey(property.getPropertyId())) {
      throw new IllegalArgumentException("Property with id " + property.getPropertyId()
              + (property.getCaption() != null ? " (caption: " + property.getCaption() + ")" : "")
              + " has already been defined as: " + propertyMap.get(property.getPropertyId()) + " in entity: " + entityId);
    }
  }

  private static void validatePrimaryKeyProperties(final Map<String, Property> propertyMap) {
    final Collection<Integer> usedPrimaryKeyIndexes = new ArrayList<>();
    boolean primaryKeyPropertyFound = false;
    for (final Property property : propertyMap.values()) {
      if (property instanceof ColumnProperty && ((ColumnProperty) property).isPrimaryKeyProperty()) {
        final Integer index = ((ColumnProperty) property).getPrimaryKeyIndex();
        if (usedPrimaryKeyIndexes.contains(index)) {
          throw new IllegalArgumentException("Primary key index " + index + " in property " + property + " has already been used");
        }
        usedPrimaryKeyIndexes.add(index);
        primaryKeyPropertyFound = true;
      }
    }
    if (primaryKeyPropertyFound) {
      return;
    }
  }

  private static List<ForeignKeyProperty> getForeignKeyProperties(final Collection<Property> properties) {
    return properties.stream().filter(property -> property instanceof ForeignKeyProperty)
            .map(property -> (ForeignKeyProperty) property).collect(toList());
  }

  private static List<ColumnProperty> getColumnProperties(final Collection<Property> properties) {
    return properties.stream().filter(property -> property instanceof ColumnProperty)
            .map(property -> (ColumnProperty) property).collect(toList());
  }

  private static List<TransientProperty> getTransientProperties(final Collection<Property> properties) {
    return properties.stream().filter(property -> property instanceof TransientProperty)
            .map(property -> (TransientProperty) property).collect(toList());
  }

  private static List<ColumnProperty> initializeLazyLoadedBlobProperties(final List<ColumnProperty> columnProperties) {
    return columnProperties.stream().filter(Property::isBlob).filter(property ->
            !(property instanceof BlobProperty) || !((BlobProperty) property).isEagerlyLoaded()).collect(toList());
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
        final Collection<String> sourcePropertyIds = ((DerivedProperty) property).getSourcePropertyIds();
        if (!nullOrEmpty(sourcePropertyIds)) {
          for (final String sourcePropertyId : sourcePropertyIds) {
            linkProperties(derivedProperties, sourcePropertyId, (DerivedProperty) property);
          }
        }
      }
    }

    return derivedProperties;
  }

  private static void linkProperties(final Map<String, Set<DerivedProperty>> derivedProperties,
                                     final String sourcePropertyId, final DerivedProperty derivedProperty) {
    if (!derivedProperties.containsKey(sourcePropertyId)) {
      derivedProperties.put(sourcePropertyId, new HashSet<>());
    }
    derivedProperties.get(sourcePropertyId).add(derivedProperty);
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

  private static List<ColumnProperty> getSelectableProperties(final List<ColumnProperty> columnProperties,
                                                              final List<ColumnProperty> lazyLoadedBlobProperties) {
    return columnProperties.stream().filter(property ->
            !lazyLoadedBlobProperties.contains(property)).filter(ColumnProperty::isSelectable).collect(toList());
  }

  private static List<Property> getVisibleProperties(final Collection<Property> properties) {
    return properties.stream().filter(property -> !property.isHidden()).collect(toList());
  }

  /**
   * @param columnProperties the column properties
   * @return a list of grouping columns separated with a comma, to serve as a group by clause,
   * null if no grouping properties are defined
   */
  private static String initializeGroupByClause(final List<ColumnProperty> columnProperties) {
    final List<String> groupingColumnNames = columnProperties.stream().filter(ColumnProperty::isGroupingColumn)
            .map(ColumnProperty::getColumnName).collect(toList());
    if (groupingColumnNames.isEmpty()) {
      return null;
    }

    return String.join(", ", groupingColumnNames);
  }

  private static Map<String, ForeignKeyProperty> initializeForeignKeyPropertyMap(final List<ForeignKeyProperty> foreignKeyProperties) {
    final Map<String, ForeignKeyProperty> foreignKeyMap = new HashMap<>(foreignKeyProperties.size());
    foreignKeyProperties.forEach(foreignKeyProperty ->
            foreignKeyMap.put(foreignKeyProperty.getPropertyId(), foreignKeyProperty));

    return foreignKeyMap;
  }

  private static Map<String, List<ForeignKeyProperty>> initializeColumnPropertyForeignKeyProperties(final List<ForeignKeyProperty> foreignKeyProperties) {
    final Map<String, List<ForeignKeyProperty>> foreignKeyMap = new HashMap<>();
    foreignKeyProperties.forEach(foreignKeyProperty ->
            foreignKeyProperty.getColumnProperties().forEach(columnProperty ->
                    foreignKeyMap.computeIfAbsent(columnProperty.getPropertyId(),
                            columnPropertyId -> new ArrayList<>()).add(foreignKeyProperty)));

    return foreignKeyMap;
  }

  private static final class DefaultBuilder implements Builder {

    private final DefaultEntityDefinition definition;

    private DefaultBuilder(final DefaultEntityDefinition definition) {
      this.definition = definition;
    }

    @Override
    public EntityDefinition get() {
      return definition;
    }

    @Override
    public Builder domainId(final String domainId) {
      rejectNullOrEmpty(domainId, "domainId");
      if (definition.domainId != null) {
        throw new IllegalStateException("Domain id has already been set: " + definition.domainId);
      }
      definition.domainId = domainId;
      return this;
    }

    @Override
    public Builder conditionProvider(final String conditionId, final ConditionProvider conditionProvider) {
      rejectNullOrEmpty(conditionId, "contitionId");
      requireNonNull(conditionProvider, "conditionProvider");
      if (definition.conditionProviders == null) {
        definition.conditionProviders = new HashMap<>();
      }
      if (definition.conditionProviders.containsKey(conditionId)) {
        throw new IllegalStateException("ConditionProvider with id " + conditionId + " has already been added");
      }
      definition.conditionProviders.put(conditionId, conditionProvider);
      return this;
    }

    @Override
    public Builder caption(final String caption) {
      definition.caption = requireNonNull(caption, "caption");
      return this;
    }

    @Override
    public Builder beanClass(final Class beanClass) {
      definition.beanClass = requireNonNull(beanClass, "beanClass");
      return this;
    }

    @Override
    public Builder smallDataset(final boolean smallDataset) {
      definition.smallDataset = smallDataset;
      return this;
    }

    @Override
    public Builder staticData(final boolean staticData) {
      definition.staticData = staticData;
      return this;
    }

    @Override
    public Builder readOnly(final boolean readOnly) {
      definition.readOnly = readOnly;
      return this;
    }

    @Override
    public Builder keyGenerator(final KeyGenerator keyGenerator) {
      definition.keyGenerator = requireNonNull(keyGenerator, "keyGenerator");
      definition.keyGenerated = true;
      return this;
    }

    @Override
    public Builder orderBy(final OrderBy orderBy) {
      requireNonNull(orderBy, "orderBy");
      if (definition.orderBy != null) {
        throw new IllegalStateException("Order by has already been set: " + definition.orderBy);
      }
      definition.orderBy = orderBy;
      return this;
    }

    @Override
    public Builder groupByClause(final String groupByClause) {
      requireNonNull(groupByClause, "groupByClause");
      if (definition.groupByClause != null) {
        throw new IllegalStateException("Group by clause has already been set: " + definition.groupByClause);
      }
      definition.groupByClause = groupByClause;
      return this;
    }

    @Override
    public Builder havingClause(final String havingClause) {
      requireNonNull(havingClause, "havingClause");
      if (definition.havingClause != null) {
        throw new IllegalStateException("Having clause has already been set: " + definition.havingClause);
      }
      definition.havingClause = havingClause;
      return this;
    }

    @Override
    public Builder selectTableName(final String selectTableName) {
      definition.selectTableName = requireNonNull(selectTableName, "selectTableName");
      return this;
    }

    @Override
    public Builder selectQuery(final String selectQuery, final boolean containsWhereClause) {
      definition.selectQuery = requireNonNull(selectQuery, "selectQuery");
      definition.selectQueryContainsWhereClause = containsWhereClause;
      return this;
    }

    @Override
    public Builder comparator(final Comparator<Entity> comparator) {
      definition.comparator = requireNonNull(comparator, "comparator");
      return this;
    }

    @Override
    public Builder stringProvider(final Function<Entity, String> stringProvider) {
      definition.stringProvider = requireNonNull(stringProvider, "stringProvider");
      return this;
    }

    @Override
    public Builder searchPropertyIds(final String... searchPropertyIds) {
      requireNonNull(searchPropertyIds, "searchPropertyIds");
      for (final String propertyId : searchPropertyIds) {
        final Property property = definition.propertyMap.get(propertyId);
        if (property == null) {
          throw new IllegalArgumentException("Property with ID '" + propertyId + "' not found in entity '" +
                  definition.getEntityId() + "'");
        }
        if (!property.isString()) {
          throw new IllegalArgumentException("Entity search property must be of type String: " +
                  definition.propertyMap.get(propertyId));
        }
      }
      definition.searchPropertyIds = asList(searchPropertyIds);
      return this;
    }

    @Override
    public Builder colorProvider(final ColorProvider colorProvider) {
      definition.colorProvider = requireNonNull(colorProvider, "colorProvider");
      return this;
    }

    @Override
    public Builder validator(final Validator validator) {
      definition.validator = requireNonNull(validator, "validator");
      return this;
    }
  }
}
