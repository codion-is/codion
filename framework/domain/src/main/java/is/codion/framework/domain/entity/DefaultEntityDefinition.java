/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.common.Text;
import is.codion.framework.domain.property.Attribute;
import is.codion.framework.domain.property.BlobProperty;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.DenormalizedProperty;
import is.codion.framework.domain.property.DerivedProperty;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.domain.property.MirrorProperty;
import is.codion.framework.domain.property.Property;
import is.codion.framework.domain.property.TransientProperty;

import java.io.Serializable;
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

import static is.codion.common.Util.rejectNullOrEmpty;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

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
   * The {@link Function} to use when toString() is called for this entity type
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
  private EntityValidator validator = new DefaultEntityValidator();

  /**
   * The bean helper
   */
  private BeanHelper beanHelper = new DefaultBeanHelper();

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
   * The {@link ConditionProvider}s mapped to their respective conditionIds
   */
  private transient Map<String, ConditionProvider> conditionProviders;

  /**
   * Maps the definition of a referenced entity to its foreign key propertyId.
   */
  private final Map<Attribute<?>, EntityDefinition> foreignEntityDefinitions = new HashMap<>();

  /**
   * The properties associated with this entity.
   */
  private final EntityProperties entityProperties;

  /**
   * True if this entity type contains one or more denormalized properties.
   */
  private final boolean hasDenormalizedProperties;

  /**
   * Defines a new entity type with the entityId serving as the initial entity caption.
   */
  DefaultEntityDefinition(final String entityId, final String tableName, final Property.Builder... propertyBuilders) {
    this.entityId = rejectNullOrEmpty(entityId, "entityId");
    this.tableName = rejectNullOrEmpty(tableName, "tableName");
    this.entityProperties = new EntityProperties(entityId, propertyBuilders);
    this.hasDenormalizedProperties = !entityProperties.denormalizedProperties.isEmpty();
    this.groupByClause = initializeGroupByClause();
    this.caption = entityId;
  }

  @Override
  public String getEntityId() {
    return entityId;
  }

  @Override
  public String getTableName() {
    return tableName;
  }

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

  @Override
  public String getDomainId() {
    return domainId;
  }

  @Override
  public String getCaption() {
    return caption;
  }

  @Override
  public Class getBeanClass() {
    return beanClass;
  }

  @Override
  public boolean isSmallDataset() {
    return smallDataset;
  }

  @Override
  public boolean isStaticData() {
    return staticData;
  }

  @Override
  public boolean isReadOnly() {
    return readOnly;
  }

  @Override
  public KeyGenerator getKeyGenerator() {
    return keyGenerator;
  }

  @Override
  public boolean isKeyGenerated() {
    return keyGenerated;
  }

  @Override
  public OrderBy getOrderBy() {
    return orderBy;
  }

  @Override
  public String getGroupByClause() {
    return groupByClause;
  }

  @Override
  public String getHavingClause() {
    return havingClause;
  }

  @Override
  public String getSelectTableName() {
    return selectTableName == null ? tableName : selectTableName;
  }

  @Override
  public String getSelectQuery() {
    return selectQuery;
  }

  @Override
  public boolean selectQueryContainsWhereClause() {
    return selectQueryContainsWhereClause;
  }

  @Override
  public Function<Entity, String> getStringProvider() {
    return stringProvider;
  }

  @Override
  public Comparator<Entity> getComparator() {
    return comparator;
  }

  @Override
  public Collection<ColumnProperty> getSearchProperties() {
    return entityProperties.columnProperties.stream().filter(ColumnProperty::isSearchProperty).collect(toList());
  }

  @Override
  public ColumnProperty getColumnProperty(final Attribute<?> propertyId) {
    final Property property = getProperty(propertyId);
    if (!(property instanceof ColumnProperty)) {
      throw new IllegalArgumentException(propertyId + " is not a ColumnProperty");
    }

    return (ColumnProperty) property;
  }

  @Override
  public Property getProperty(final Attribute<?> propertyId) {
    requireNonNull(propertyId, "propertyId");
    final Property property = entityProperties.propertyMap.get(propertyId);
    if (property == null) {
      throw new IllegalArgumentException("Property '" + propertyId + "' not found in entity: " + entityId);
    }

    return property;
  }

  @Override
  public ColumnProperty getPrimaryKeyProperty(final Attribute<?> propertyId) {
    final ColumnProperty property = entityProperties.primaryKeyPropertyMap.get(propertyId);
    if (property == null) {
      throw new IllegalArgumentException("Primary key property " + propertyId + " not found in entity: " + entityId);
    }

    return property;
  }

  @Override
  public List<Property> getProperties(final Collection<Attribute<?>> propertyIds) {
    requireNonNull(propertyIds, "propertyIds");

    return propertyIds.stream().map(this::getProperty).collect(toList());
  }

  @Override
  public ColumnProperty getSelectableColumnProperty(final Attribute<?> propertyId) {
    final ColumnProperty property = getColumnProperty(propertyId);
    if (!property.isSelectable()) {
      throw new IllegalArgumentException(propertyId + " is not selectable");
    }

    return property;
  }

  @Override
  public List<ColumnProperty> getColumnProperties(final List<Attribute<?>> propertyIds) {
    requireNonNull(propertyIds, "propertyIds");
    final List<ColumnProperty> theProperties = new ArrayList<>(propertyIds.size());
    for (int i = 0; i < propertyIds.size(); i++) {
      theProperties.add(getColumnProperty(propertyIds.get(i)));
    }

    return theProperties;
  }

  @Override
  public boolean hasSingleIntegerPrimaryKey() {
    return entityProperties.primaryKeyProperties.size() == 1 && entityProperties.primaryKeyProperties.get(0).isInteger();
  }

  @Override
  public List<ColumnProperty> getWritableColumnProperties(final boolean includePrimaryKeyProperties,
                                                          final boolean includeNonUpdatable) {
    return entityProperties.columnProperties.stream()
            .filter(property -> property.isInsertable() &&
                    (includeNonUpdatable || property.isUpdatable()) &&
                    (includePrimaryKeyProperties || !property.isPrimaryKeyProperty()))
            .collect(toList());
  }

  @Override
  public List<Property> getUpdatableProperties() {
    final List<ColumnProperty> writableColumnProperties = getWritableColumnProperties(!isKeyGenerated(), false);
    writableColumnProperties.removeIf(property -> property.isForeignKeyProperty() || property.isDenormalized());
    final List<Property> updatable = new ArrayList<>(writableColumnProperties);
    for (final ForeignKeyProperty foreignKeyProperty : entityProperties.foreignKeyProperties) {
      if (foreignKeyProperty.isUpdatable()) {
        updatable.add(foreignKeyProperty);
      }
    }

    return updatable;
  }

  @Override
  public List<ColumnProperty> getSelectableColumnProperties(final List<Attribute<?>> propertyIds) {
    final List<ColumnProperty> theProperties = new ArrayList<>(propertyIds.size());
    for (int i = 0; i < propertyIds.size(); i++) {
      theProperties.add(getSelectableColumnProperty(propertyIds.get(i)));
    }

    return theProperties;
  }

  @Override
  public List<ForeignKeyProperty> getForeignKeyReferences(final String foreignEntityId) {
    return getForeignKeyProperties().stream().filter(foreignKeyProperty ->
            foreignKeyProperty.getForeignEntityId().equals(foreignEntityId)).collect(toList());
  }

  @Override
  public ForeignKeyProperty getForeignKeyProperty(final Attribute<?> propertyId) {
    final ForeignKeyProperty property = entityProperties.foreignKeyPropertyMap.get(propertyId);
    if (property == null) {
      throw new IllegalArgumentException("Foreign key property with id: " + propertyId + " not found in entity of type: " + entityId);
    }

    return property;
  }

  @Override
  public List<ForeignKeyProperty> getForeignKeyProperties(final Attribute<?> columnPropertyId) {
    return entityProperties.columnPropertyForeignKeyProperties.computeIfAbsent(columnPropertyId, propertyId -> Collections.emptyList());
  }

  @Override
  public List<Property> getProperties() {
    return entityProperties.properties;
  }

  @Override
  public Set<Property> getPropertySet() {
    return entityProperties.propertySet;
  }

  @Override
  public boolean hasPrimaryKey() {
    return !entityProperties.primaryKeyProperties.isEmpty();
  }

  @Override
  public boolean hasDerivedProperties() {
    return !entityProperties.derivedProperties.isEmpty();
  }

  @Override
  public boolean hasDerivedProperties(final Attribute<?> propertyId) {
    return entityProperties.derivedProperties.containsKey(propertyId);
  }

  @Override
  public Collection<DerivedProperty> getDerivedProperties(final Attribute<?> property) {
    final Collection<DerivedProperty> derived = entityProperties.derivedProperties.get(property);

    return derived == null ? emptyList() : derived;
  }

  @Override
  public List<ColumnProperty> getPrimaryKeyProperties() {
    return entityProperties.primaryKeyProperties;
  }

  @Override
  public List<Property> getVisibleProperties() {
    return entityProperties.visibleProperties;
  }

  @Override
  public List<ColumnProperty> getColumnProperties() {
    return entityProperties.columnProperties;
  }

  @Override
  public List<ColumnProperty> getSelectableColumnProperties() {
    return entityProperties.selectableColumnProperties;
  }

  @Override
  public List<ColumnProperty> getLazyLoadedBlobProperties() {
    return entityProperties.lazyLoadedBlobProperties;
  }

  @Override
  public List<TransientProperty> getTransientProperties() {
    return entityProperties.transientProperties;
  }

  @Override
  public List<ForeignKeyProperty> getForeignKeyProperties() {
    return entityProperties.foreignKeyProperties;
  }

  @Override
  public EntityDefinition getForeignDefinition(final Attribute<?> foreignKeyPropertyId) {
    requireNonNull(foreignKeyPropertyId, "foreignKeyPropertyId");
    final EntityDefinition definition = foreignEntityDefinitions.get(foreignKeyPropertyId);
    if (definition == null) {
      throw new IllegalArgumentException("Referenced entity not found for foreign key property: " + foreignKeyPropertyId);
    }

    return definition;
  }

  @Override
  public boolean hasDenormalizedProperties() {
    return hasDenormalizedProperties;
  }

  @Override
  public boolean hasDenormalizedProperties(final Attribute<?> foreignKeyPropertyId) {
    return hasDenormalizedProperties && entityProperties.denormalizedProperties.containsKey(foreignKeyPropertyId);
  }

  @Override
  public List<DenormalizedProperty> getDenormalizedProperties(final Attribute<?> foreignKeyPropertyId) {
    return entityProperties.denormalizedProperties.get(foreignKeyPropertyId);
  }

  @Override
  public String toString() {
    return entityId;
  }

  @Override
  public EntityValidator getValidator() {
    return validator;
  }

  @Override
  public ColorProvider getColorProvider() {
    return colorProvider;
  }

  @Override
  public Entity entity() {
    return entity(null, null);
  }

  @Override
  public Entity entity(final Entity.Key key) {
    return new DefaultEntity(this, key);
  }

  @Override
  public Entity entity(final Function<Property, Object> valueProvider) {
    requireNonNull(valueProvider);
    final Entity entity = entity();
    for (final ColumnProperty property : entityProperties.columnProperties) {
      if (!property.isForeignKeyProperty() && !property.isDenormalized()//these are set via their respective parent properties
              && (!property.columnHasDefaultValue() || property.hasDefaultValue())) {
        entity.put(property, valueProvider.apply(property));
      }
    }
    for (final TransientProperty transientProperty : entityProperties.transientProperties) {
      if (!(transientProperty instanceof DerivedProperty)) {
        entity.put(transientProperty, valueProvider.apply(transientProperty));
      }
    }
    for (final ForeignKeyProperty foreignKeyProperty : entityProperties.foreignKeyProperties) {
      entity.put(foreignKeyProperty, valueProvider.apply(foreignKeyProperty));
    }
    entity.saveAll();

    return entity;
  }

  @Override
  public Entity entity(final Map<Property, Object> values, final Map<Property, Object> originalValues) {
    return new DefaultEntity(this, values, originalValues);
  }

  @Override
  public Entity.Key key() {
    if (hasPrimaryKey()) {
      return new DefaultEntityKey(this, null);
    }

    return new DefaultEntityKey(this);
  }

  @Override
  public Entity.Key key(final Integer value) {
    return new DefaultEntityKey(this, value);
  }

  @Override
  public Entity.Key key(final Long value) {
    return new DefaultEntityKey(this, value);
  }

  @Override
  public <V> BeanHelper<V> getBeanHelper() {
    return beanHelper;
  }

  /**
   * Returns true if a entity definition has been associated with the given foreign key.
   * @param foreignKeyPropertyId the foreign key property id
   * @return true if the referenced entity definition has been set for the given foreign key property
   */
  boolean hasForeignDefinition(final Attribute<?> foreignKeyPropertyId) {
    return foreignEntityDefinitions.containsKey(foreignKeyPropertyId);
  }

  /**
   * Associates the given definition with the given foreign key.
   * @param foreignKeyPropertyId the foreign key property id
   * @param definition the entity definition referenced by the given foreign key
   * @throws IllegalStateException in case the foreign definition has already been set
   * @throws IllegalArgumentException in case the definition does not match the foreign key
   */
  void setForeignDefinition(final Attribute<?> foreignKeyPropertyId, final EntityDefinition definition) {
    requireNonNull(foreignKeyPropertyId, "foreignKeyPropertyId");
    requireNonNull(definition, "definition");
    final ForeignKeyProperty foreignKeyProperty = getForeignKeyProperty(foreignKeyPropertyId);
    if (foreignEntityDefinitions.containsKey(foreignKeyPropertyId)) {
      throw new IllegalStateException("Foreign definition has already been set for " + foreignKeyPropertyId);
    }
    if (!foreignKeyProperty.getForeignEntityId().equals(definition.getEntityId())) {
      throw new IllegalArgumentException("Definition for entity " + foreignKeyProperty.getForeignEntityId() +
              " expected for " + foreignKeyPropertyId);
    }
    foreignEntityDefinitions.put(foreignKeyPropertyId, definition);
  }

  /**
   * @return a {@link EntityDefinition.Builder} for this definition instance
   */
  Builder builder() {
    return new DefaultBuilder(this);
  }

  /**
   * @return a list of grouping columns separated with a comma, to serve as a group by clause,
   * null if no grouping properties are defined
   */
  private String initializeGroupByClause() {
    final List<String> groupingColumnNames = entityProperties.columnProperties.stream().filter(ColumnProperty::isGroupingColumn)
            .map(ColumnProperty::getColumnName).collect(toList());
    if (groupingColumnNames.isEmpty()) {
      return null;
    }

    return String.join(", ", groupingColumnNames);
  }

  private static final class EntityProperties implements Serializable {

    private static final long serialVersionUID = 1;

    private final String entityId;

    private final Map<Attribute<?>, Property> propertyMap;
    private final List<Property> properties;
    private final Set<Property> propertySet;
    private final List<Property> visibleProperties;
    private final List<ColumnProperty> columnProperties;
    private final List<ColumnProperty> lazyLoadedBlobProperties;
    private final List<ColumnProperty> selectableColumnProperties;
    private final List<ColumnProperty> primaryKeyProperties;
    private final Map<Attribute<?>, ColumnProperty> primaryKeyPropertyMap;
    private final List<ForeignKeyProperty> foreignKeyProperties;
    private final Map<Attribute<?>, ForeignKeyProperty> foreignKeyPropertyMap;
    private final Map<Attribute<?>, List<ForeignKeyProperty>> columnPropertyForeignKeyProperties;
    private final Map<Attribute<?>, Set<DerivedProperty>> derivedProperties;
    private final List<TransientProperty> transientProperties;
    private final Map<Attribute<?>, List<DenormalizedProperty>> denormalizedProperties;

    private EntityProperties(final String entityId, final Property.Builder... propertyBuilders) {
      this.entityId = entityId;
      this.propertyMap = initializePropertyMap(propertyBuilders);
      this.properties = unmodifiableList(new ArrayList<>(propertyMap.values()));
      this.propertySet = new HashSet<>(propertyMap.values());
      this.visibleProperties = unmodifiableList(getVisibleProperties());
      this.columnProperties = unmodifiableList(getColumnProperties());
      this.lazyLoadedBlobProperties = initializeLazyLoadedBlobProperties();
      this.selectableColumnProperties = unmodifiableList(getSelectableProperties());
      this.primaryKeyProperties = unmodifiableList(getPrimaryKeyProperties());
      this.primaryKeyPropertyMap = initializePrimaryKeyPropertyMap();
      this.foreignKeyProperties = unmodifiableList(getForeignKeyProperties());
      this.foreignKeyPropertyMap = initializeForeignKeyPropertyMap();
      this.columnPropertyForeignKeyProperties = initializeColumnPropertyForeignKeyProperties();
      this.derivedProperties = initializeDerivedProperties();
      this.transientProperties = unmodifiableList(getTransientProperties());
      this.denormalizedProperties = unmodifiableMap(getDenormalizedProperties());
    }

    private Map<Attribute<?>, Property> initializePropertyMap(final Property.Builder... propertyBuilders) {
      final Map<Attribute<?>, Property> propertyMap = new LinkedHashMap<>(propertyBuilders.length);
      for (final Property.Builder propertyBuilder : propertyBuilders) {
        validateAndAddProperty(propertyBuilder, propertyMap);
        if (propertyBuilder instanceof ForeignKeyProperty.Builder) {
          initializeForeignKeyProperty(propertyMap, (ForeignKeyProperty.Builder) propertyBuilder);
        }
      }
      validatePrimaryKeyProperties(propertyMap);

      return unmodifiableMap(propertyMap);
    }

    private void initializeForeignKeyProperty(final Map<Attribute<?>, Property> propertyMap,
                                              final ForeignKeyProperty.Builder foreignKeyPropertyBuilder) {
      for (final ColumnProperty.Builder propertyBuilder : foreignKeyPropertyBuilder.getColumnPropertyBuilders()) {
        if (!(propertyBuilder.get() instanceof MirrorProperty)) {
          validateAndAddProperty(propertyBuilder, propertyMap);
        }
      }
    }

    private void validateAndAddProperty(final Property.Builder propertyBuilder, final Map<Attribute<?>, Property> propertyMap) {
      final Property property = propertyBuilder.get();
      checkIfUniquePropertyId(property, propertyMap);
      propertyBuilder.entityId(entityId);
      propertyMap.put(property.getPropertyId(), property);
    }

    private void checkIfUniquePropertyId(final Property property, final Map<Attribute<?>, Property> propertyMap) {
      if (propertyMap.containsKey(property.getPropertyId())) {
        throw new IllegalArgumentException("Property with id " + property.getPropertyId()
                + (property.getCaption() != null ? " (caption: " + property.getCaption() + ")" : "")
                + " has already been defined as: " + propertyMap.get(property.getPropertyId()) + " in entity: " + entityId);
      }
    }

    private Map<Attribute<?>, ForeignKeyProperty> initializeForeignKeyPropertyMap() {
      final Map<Attribute<?>, ForeignKeyProperty> foreignKeyMap = new HashMap<>(foreignKeyProperties.size());
      foreignKeyProperties.forEach(foreignKeyProperty ->
              foreignKeyMap.put(foreignKeyProperty.getPropertyId(), foreignKeyProperty));

      return foreignKeyMap;
    }

    private Map<Attribute<?>, List<ForeignKeyProperty>> initializeColumnPropertyForeignKeyProperties() {
      final Map<Attribute<?>, List<ForeignKeyProperty>> foreignKeyMap = new HashMap<>();
      foreignKeyProperties.forEach(foreignKeyProperty ->
              foreignKeyProperty.getColumnProperties().forEach(columnProperty ->
                      foreignKeyMap.computeIfAbsent(columnProperty.getPropertyId(),
                              columnPropertyId -> new ArrayList<>()).add(foreignKeyProperty)));

      return foreignKeyMap;
    }

    private Map<Attribute<?>, ColumnProperty> initializePrimaryKeyPropertyMap() {
      final Map<Attribute<?>, ColumnProperty> map = new HashMap<>(this.primaryKeyProperties.size());
      this.primaryKeyProperties.forEach(property -> map.put(property.getPropertyId(), property));

      return unmodifiableMap(map);
    }

    private List<Property> getVisibleProperties() {
      return properties.stream().filter(property -> !property.isHidden()).collect(toList());
    }

    private List<ForeignKeyProperty> getForeignKeyProperties() {
      return properties.stream().filter(property -> property instanceof ForeignKeyProperty)
              .map(property -> (ForeignKeyProperty) property).collect(toList());
    }

    private List<ColumnProperty> getColumnProperties() {
      return properties.stream().filter(property -> property instanceof ColumnProperty)
              .map(property -> (ColumnProperty) property).collect(toList());
    }

    private List<TransientProperty> getTransientProperties() {
      return properties.stream().filter(property -> property instanceof TransientProperty)
              .map(property -> (TransientProperty) property).collect(toList());
    }

    private List<ColumnProperty> initializeLazyLoadedBlobProperties() {
      return columnProperties.stream().filter(Property::isBlob).filter(property ->
              !(property instanceof BlobProperty) || !((BlobProperty) property).isEagerlyLoaded()).collect(toList());
    }

    private Map<Attribute<?>, List<DenormalizedProperty>> getDenormalizedProperties() {
      final Map<Attribute<?>, List<DenormalizedProperty>> denormalizedPropertiesMap = new HashMap<>(properties.size());
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

    private Map<Attribute<?>, Set<DerivedProperty>> initializeDerivedProperties() {
      final Map<Attribute<?>, Set<DerivedProperty>> derivedPropertyMap = new HashMap<>();
      for (final Property property : properties) {
        if (property instanceof DerivedProperty) {
          for (final Attribute<?> sourcePropertyId : ((DerivedProperty) property).getSourcePropertyIds()) {
            derivedPropertyMap.computeIfAbsent(sourcePropertyId, propertyId -> new HashSet<>()).add((DerivedProperty) property);
          }
        }
      }

      return derivedPropertyMap;
    }

    private List<ColumnProperty> getPrimaryKeyProperties() {
      return properties.stream().filter(property -> property instanceof ColumnProperty
              && ((ColumnProperty) property).isPrimaryKeyProperty()).map(property -> (ColumnProperty) property)
              .sorted((pk1, pk2) -> {
                final Integer index1 = pk1.getPrimaryKeyIndex();
                final Integer index2 = pk2.getPrimaryKeyIndex();

                return index1.compareTo(index2);
              }).collect(toList());
    }

    private List<ColumnProperty> getSelectableProperties() {
      return columnProperties.stream().filter(property ->
              !lazyLoadedBlobProperties.contains(property)).filter(ColumnProperty::isSelectable).collect(toList());
    }
  }

  private static void validatePrimaryKeyProperties(final Map<Attribute<?>, Property> propertyMap) {
    final Collection<Integer> usedPrimaryKeyIndexes = new ArrayList<>();
    for (final Property property : propertyMap.values()) {
      if (property instanceof ColumnProperty && ((ColumnProperty) property).isPrimaryKeyProperty()) {
        final Integer index = ((ColumnProperty) property).getPrimaryKeyIndex();
        if (usedPrimaryKeyIndexes.contains(index)) {
          throw new IllegalArgumentException("Primary key index " + index + " in property " + property + " has already been used");
        }
        usedPrimaryKeyIndexes.add(index);
      }
    }
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
      rejectNullOrEmpty(conditionId, "conditionId");
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
    public Builder colorProvider(final ColorProvider colorProvider) {
      definition.colorProvider = requireNonNull(colorProvider, "colorProvider");
      return this;
    }

    @Override
    public Builder validator(final EntityValidator validator) {
      definition.validator = requireNonNull(validator, "validator");
      return this;
    }

    @Override
    public <V> Builder beanHelper(final BeanHelper<V> beanHelper) {
      definition.beanHelper = requireNonNull(beanHelper, "beanHelper");
      return this;
    }
  }

  private static final class DefaultBeanHelper implements BeanHelper {

    private static final long serialVersionUID = 1;
  }
}
