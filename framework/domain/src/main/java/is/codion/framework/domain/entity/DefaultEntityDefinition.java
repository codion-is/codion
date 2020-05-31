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
  private Class<?> beanClass;

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
  private BeanHelper<?> beanHelper = new DefaultBeanHelper();

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
   * Maps the definition of a referenced entity to its foreign key attribute.
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
  DefaultEntityDefinition(final String entityId, final String tableName, final Property.Builder<?>... propertyBuilders) {
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
  public <V> Class<V> getBeanClass() {
    return (Class<V>) beanClass;
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
  public Collection<ColumnProperty<?>> getSearchProperties() {
    return entityProperties.columnProperties.stream().filter(ColumnProperty::isSearchProperty).collect(toList());
  }

  @Override
  public <T> ColumnProperty<T> getColumnProperty(final Attribute<T> attribute) {
    final Property<T> property = getProperty(attribute);
    if (!(property instanceof ColumnProperty)) {
      throw new IllegalArgumentException(attribute + " is not a ColumnProperty");
    }

    return (ColumnProperty<T>) property;
  }

  @Override
  public <T> Property<T> getProperty(final Attribute<T> attribute) {
    requireNonNull(attribute, "attribute");
    final Property<T> property = (Property<T>) entityProperties.propertyMap.get(attribute);
    if (property == null) {
      throw new IllegalArgumentException("Property '" + attribute + "' not found in entity: " + entityId);
    }

    return property;
  }

  @Override
  public <T> ColumnProperty<T> getPrimaryKeyProperty(final Attribute<T> attribute) {
    final ColumnProperty<T> property = (ColumnProperty<T>) entityProperties.primaryKeyPropertyMap.get(attribute);
    if (property == null) {
      throw new IllegalArgumentException("Primary key property " + attribute + " not found in entity: " + entityId);
    }

    return property;
  }

  @Override
  public List<Property<?>> getProperties(final Collection<Attribute<?>> attributes) {
    requireNonNull(attributes, "attributes");

    return attributes.stream().map(this::getProperty).collect(toList());
  }

  @Override
  public <T> ColumnProperty<T> getSelectableColumnProperty(final Attribute<T> attribute) {
    final ColumnProperty<T> property = getColumnProperty(attribute);
    if (!property.isSelectable()) {
      throw new IllegalArgumentException(attribute + " is not selectable");
    }

    return property;
  }

  @Override
  public List<ColumnProperty<?>> getColumnProperties(final List<Attribute<?>> attributes) {
    requireNonNull(attributes, "attributes");
    final List<ColumnProperty<?>> theProperties = new ArrayList<>(attributes.size());
    for (int i = 0; i < attributes.size(); i++) {
      theProperties.add(getColumnProperty(attributes.get(i)));
    }

    return theProperties;
  }

  @Override
  public boolean hasSingleIntegerPrimaryKey() {
    return entityProperties.primaryKeyProperties.size() == 1 && entityProperties.primaryKeyProperties.get(0).getAttribute().isInteger();
  }

  @Override
  public List<ColumnProperty<?>> getWritableColumnProperties(final boolean includePrimaryKeyProperties,
                                                             final boolean includeNonUpdatable) {
    return entityProperties.columnProperties.stream()
            .filter(property -> property.isInsertable() &&
                    (includeNonUpdatable || property.isUpdatable()) &&
                    (includePrimaryKeyProperties || !property.isPrimaryKeyProperty()))
            .collect(toList());
  }

  @Override
  public List<Property<?>> getUpdatableProperties() {
    final List<ColumnProperty<?>> writableColumnProperties = getWritableColumnProperties(!isKeyGenerated(), false);
    writableColumnProperties.removeIf(property -> property.isForeignKeyProperty() || property.isDenormalized());
    final List<Property<?>> updatable = new ArrayList<>(writableColumnProperties);
    for (final ForeignKeyProperty foreignKeyProperty : entityProperties.foreignKeyProperties) {
      if (foreignKeyProperty.isUpdatable()) {
        updatable.add(foreignKeyProperty);
      }
    }

    return updatable;
  }

  @Override
  public List<ColumnProperty<?>> getSelectableColumnProperties(final List<Attribute<?>> attributes) {
    final List<ColumnProperty<?>> theProperties = new ArrayList<>(attributes.size());
    for (int i = 0; i < attributes.size(); i++) {
      theProperties.add(getSelectableColumnProperty(attributes.get(i)));
    }

    return theProperties;
  }

  @Override
  public List<ForeignKeyProperty> getForeignKeyReferences(final String foreignEntityId) {
    return getForeignKeyProperties().stream().filter(foreignKeyProperty ->
            foreignKeyProperty.getForeignEntityId().equals(foreignEntityId)).collect(toList());
  }

  @Override
  public ForeignKeyProperty getForeignKeyProperty(final Attribute<Entity> attribute) {
    final ForeignKeyProperty property = entityProperties.foreignKeyPropertyMap.get(attribute);
    if (property == null) {
      throw new IllegalArgumentException("Foreign key property with id: " + attribute + " not found in entity of type: " + entityId);
    }

    return property;
  }

  @Override
  public List<ForeignKeyProperty> getForeignKeyProperties(final Attribute<?> columnAttribute) {
    return entityProperties.columnPropertyForeignKeyProperties.computeIfAbsent(columnAttribute, attribute -> Collections.emptyList());
  }

  @Override
  public List<Property<?>> getProperties() {
    return entityProperties.properties;
  }

  @Override
  public Set<Property<?>> getPropertySet() {
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
  public boolean hasDerivedProperties(final Attribute<?> attribute) {
    return entityProperties.derivedProperties.containsKey(attribute);
  }

  @Override
  public Collection<DerivedProperty<?>> getDerivedProperties(final Attribute<?> property) {
    final Collection<DerivedProperty<?>> derived = entityProperties.derivedProperties.get(property);

    return derived == null ? emptyList() : derived;
  }

  @Override
  public List<ColumnProperty<?>> getPrimaryKeyProperties() {
    return entityProperties.primaryKeyProperties;
  }

  @Override
  public List<Property<?>> getVisibleProperties() {
    return entityProperties.visibleProperties;
  }

  @Override
  public List<ColumnProperty<?>> getColumnProperties() {
    return entityProperties.columnProperties;
  }

  @Override
  public List<ColumnProperty<?>> getSelectableColumnProperties() {
    return entityProperties.selectableColumnProperties;
  }

  @Override
  public List<ColumnProperty<?>> getLazyLoadedBlobProperties() {
    return entityProperties.lazyLoadedBlobProperties;
  }

  @Override
  public List<TransientProperty<?>> getTransientProperties() {
    return entityProperties.transientProperties;
  }

  @Override
  public List<ForeignKeyProperty> getForeignKeyProperties() {
    return entityProperties.foreignKeyProperties;
  }

  @Override
  public EntityDefinition getForeignDefinition(final Attribute<Entity> foreignKeyAttribute) {
    requireNonNull(foreignKeyAttribute, "foreignKeyAttribute");
    final EntityDefinition definition = foreignEntityDefinitions.get(foreignKeyAttribute);
    if (definition == null) {
      throw new IllegalArgumentException("Referenced entity not found for foreign key property: " + foreignKeyAttribute);
    }

    return definition;
  }

  @Override
  public boolean hasDenormalizedProperties() {
    return hasDenormalizedProperties;
  }

  @Override
  public boolean hasDenormalizedProperties(final Attribute<?> foreignKeyAttribute) {
    return hasDenormalizedProperties && entityProperties.denormalizedProperties.containsKey(foreignKeyAttribute);
  }

  @Override
  public List<DenormalizedProperty<?>> getDenormalizedProperties(final Attribute<?> foreignKeyAttribute) {
    return entityProperties.denormalizedProperties.get(foreignKeyAttribute);
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
  public Entity entity(final Function<Property<?>, Object> valueProvider) {
    requireNonNull(valueProvider);
    final Entity entity = entity();
    for (final ColumnProperty property : entityProperties.columnProperties) {
      if (!property.isForeignKeyProperty() && !property.isDenormalized()//these are set via their respective parent properties
              && (!property.columnHasDefaultValue() || property.hasDefaultValue())) {
        entity.put(property.getAttribute(), valueProvider.apply(property));
      }
    }
    for (final TransientProperty transientProperty : entityProperties.transientProperties) {
      if (!(transientProperty instanceof DerivedProperty)) {
        entity.put(transientProperty.getAttribute(), valueProvider.apply(transientProperty));
      }
    }
    for (final ForeignKeyProperty foreignKeyProperty : entityProperties.foreignKeyProperties) {
      entity.put(foreignKeyProperty.getAttribute(), (Entity) valueProvider.apply(foreignKeyProperty));
    }
    entity.saveAll();

    return entity;
  }

  @Override
  public Entity entity(final Map<Attribute<?>, Object> values, final Map<Attribute<?>, Object> originalValues) {
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
    return (BeanHelper<V>) beanHelper;
  }

  /**
   * Returns true if a entity definition has been associated with the given foreign key.
   * @param foreignKeyAttribute the foreign key attribute
   * @return true if the referenced entity definition has been set for the given foreign key property
   */
  boolean hasForeignDefinition(final Attribute<?> foreignKeyAttribute) {
    return foreignEntityDefinitions.containsKey(foreignKeyAttribute);
  }

  /**
   * Associates the given definition with the given foreign key.
   * @param foreignKeyAttribute the foreign key attribute
   * @param definition the entity definition referenced by the given foreign key
   * @throws IllegalStateException in case the foreign definition has already been set
   * @throws IllegalArgumentException in case the definition does not match the foreign key
   */
  void setForeignDefinition(final Attribute<Entity> foreignKeyAttribute, final EntityDefinition definition) {
    requireNonNull(foreignKeyAttribute, "foreignKeyAttribute");
    requireNonNull(definition, "definition");
    final ForeignKeyProperty foreignKeyProperty = getForeignKeyProperty(foreignKeyAttribute);
    if (foreignEntityDefinitions.containsKey(foreignKeyAttribute)) {
      throw new IllegalStateException("Foreign definition has already been set for " + foreignKeyAttribute);
    }
    if (!foreignKeyProperty.getForeignEntityId().equals(definition.getEntityId())) {
      throw new IllegalArgumentException("Definition for entity " + foreignKeyProperty.getForeignEntityId() +
              " expected for " + foreignKeyAttribute);
    }
    foreignEntityDefinitions.put(foreignKeyAttribute, definition);
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

    private final Map<Attribute<?>, Property<?>> propertyMap;
    private final List<Property<?>> properties;
    private final Set<Property<?>> propertySet;
    private final List<Property<?>> visibleProperties;
    private final List<ColumnProperty<?>> columnProperties;
    private final List<ColumnProperty<?>> lazyLoadedBlobProperties;
    private final List<ColumnProperty<?>> selectableColumnProperties;
    private final List<ColumnProperty<?>> primaryKeyProperties;
    private final Map<Attribute<?>, ColumnProperty<?>> primaryKeyPropertyMap;
    private final List<ForeignKeyProperty> foreignKeyProperties;
    private final Map<Attribute<Entity>, ForeignKeyProperty> foreignKeyPropertyMap;
    private final Map<Attribute<?>, List<ForeignKeyProperty>> columnPropertyForeignKeyProperties;
    private final Map<Attribute<?>, Set<DerivedProperty<?>>> derivedProperties;
    private final List<TransientProperty<?>> transientProperties;
    private final Map<Attribute<?>, List<DenormalizedProperty<?>>> denormalizedProperties;

    private EntityProperties(final String entityId, final Property.Builder<?>... propertyBuilders) {
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

    private Map<Attribute<?>, Property<?>> initializePropertyMap(final Property.Builder<?>... propertyBuilders) {
      final Map<Attribute<?>, Property<?>> propertyMap = new LinkedHashMap<>(propertyBuilders.length);
      for (final Property.Builder<?> propertyBuilder : propertyBuilders) {
        validateAndAddProperty(propertyBuilder, propertyMap);
        if (propertyBuilder instanceof ForeignKeyProperty.Builder) {
          initializeForeignKeyProperty(propertyMap, (ForeignKeyProperty.Builder) propertyBuilder);
        }
      }
      validatePrimaryKeyProperties(propertyMap);

      return unmodifiableMap(propertyMap);
    }

    private void initializeForeignKeyProperty(final Map<Attribute<?>, Property<?>> propertyMap,
                                              final ForeignKeyProperty.Builder foreignKeyPropertyBuilder) {
      for (final ColumnProperty.Builder<?> propertyBuilder : foreignKeyPropertyBuilder.getColumnPropertyBuilders()) {
        if (!(propertyBuilder.get() instanceof MirrorProperty)) {
          validateAndAddProperty(propertyBuilder, propertyMap);
        }
      }
    }

    private void validateAndAddProperty(final Property.Builder<?> propertyBuilder, final Map<Attribute<?>, Property<?>> propertyMap) {
      final Property<?> property = propertyBuilder.get();
      validate(property, propertyMap);
      propertyMap.put(property.getAttribute(), property);
    }

    private void validate(final Property<?> property, final Map<Attribute<?>, Property<?>> propertyMap) {
      if (!entityId.equals(property.getAttribute().getEntityId())) {
        throw new IllegalArgumentException("Attribute entityId (" +
                property.getAttribute().getEntityId() + ") does not match the definition entityId: " + entityId);
      }
      if (propertyMap.containsKey(property.getAttribute())) {
        throw new IllegalArgumentException("Property with id " + property.getAttribute()
                + (property.getCaption() != null ? " (caption: " + property.getCaption() + ")" : "")
                + " has already been defined as: " + propertyMap.get(property.getAttribute()) + " in entity: " + entityId);
      }
    }

    private Map<Attribute<Entity>, ForeignKeyProperty> initializeForeignKeyPropertyMap() {
      final Map<Attribute<Entity>, ForeignKeyProperty> foreignKeyMap = new HashMap<>(foreignKeyProperties.size());
      foreignKeyProperties.forEach(foreignKeyProperty ->
              foreignKeyMap.put(foreignKeyProperty.getAttribute(), foreignKeyProperty));

      return foreignKeyMap;
    }

    private Map<Attribute<?>, List<ForeignKeyProperty>> initializeColumnPropertyForeignKeyProperties() {
      final Map<Attribute<?>, List<ForeignKeyProperty>> foreignKeyMap = new HashMap<>();
      foreignKeyProperties.forEach(foreignKeyProperty ->
              foreignKeyProperty.getColumnProperties().forEach(columnProperty ->
                      foreignKeyMap.computeIfAbsent(columnProperty.getAttribute(),
                              columnAttribute -> new ArrayList<>()).add(foreignKeyProperty)));

      return foreignKeyMap;
    }

    private Map<Attribute<?>, ColumnProperty<?>> initializePrimaryKeyPropertyMap() {
      final Map<Attribute<?>, ColumnProperty<?>> map = new HashMap<>(this.primaryKeyProperties.size());
      this.primaryKeyProperties.forEach(property -> map.put(property.getAttribute(), property));

      return unmodifiableMap(map);
    }

    private List<Property<?>> getVisibleProperties() {
      return properties.stream().filter(property -> !property.isHidden()).collect(toList());
    }

    private List<ForeignKeyProperty> getForeignKeyProperties() {
      return properties.stream().filter(property -> property instanceof ForeignKeyProperty)
              .map(property -> (ForeignKeyProperty) property).collect(toList());
    }

    private List<ColumnProperty<?>> getColumnProperties() {
      return properties.stream().filter(property -> property instanceof ColumnProperty)
              .map(property -> (ColumnProperty<?>) property).collect(toList());
    }

    private List<TransientProperty<?>> getTransientProperties() {
      return properties.stream().filter(property -> property instanceof TransientProperty)
              .map(property -> (TransientProperty<?>) property).collect(toList());
    }

    private List<ColumnProperty<?>> initializeLazyLoadedBlobProperties() {
      return columnProperties.stream().filter(property -> property.getAttribute().isBlob()).filter(property ->
              !(property instanceof BlobProperty) || !((BlobProperty) property).isEagerlyLoaded()).collect(toList());
    }

    private Map<Attribute<?>, List<DenormalizedProperty<?>>> getDenormalizedProperties() {
      final Map<Attribute<?>, List<DenormalizedProperty<?>>> denormalizedPropertiesMap = new HashMap<>(properties.size());
      for (final Property<?> property : properties) {
        if (property instanceof DenormalizedProperty) {
          final DenormalizedProperty<?> denormalizedProperty = (DenormalizedProperty<?>) property;
          final Collection<DenormalizedProperty<?>> denormalizedProperties =
                  denormalizedPropertiesMap.computeIfAbsent(denormalizedProperty.getEntityAttribute(), attribute -> new ArrayList<>());
          denormalizedProperties.add(denormalizedProperty);
        }
      }

      return denormalizedPropertiesMap;
    }

    private Map<Attribute<?>, Set<DerivedProperty<?>>> initializeDerivedProperties() {
      final Map<Attribute<?>, Set<DerivedProperty<?>>> derivedPropertyMap = new HashMap<>();
      for (final Property<?> property : properties) {
        if (property instanceof DerivedProperty) {
          for (final Attribute<?> sourceAttribute : ((DerivedProperty<?>) property).getSourceAttributes()) {
            derivedPropertyMap.computeIfAbsent(sourceAttribute, attribute -> new HashSet<>()).add((DerivedProperty<?>) property);
          }
        }
      }

      return derivedPropertyMap;
    }

    private List<ColumnProperty<?>> getPrimaryKeyProperties() {
      return properties.stream().filter(property -> property instanceof ColumnProperty
              && ((ColumnProperty<?>) property).isPrimaryKeyProperty()).map(property -> (ColumnProperty<?>) property)
              .sorted((pk1, pk2) -> {
                final Integer index1 = pk1.getPrimaryKeyIndex();
                final Integer index2 = pk2.getPrimaryKeyIndex();

                return index1.compareTo(index2);
              }).collect(toList());
    }

    private List<ColumnProperty<?>> getSelectableProperties() {
      return columnProperties.stream().filter(property ->
              !lazyLoadedBlobProperties.contains(property)).filter(ColumnProperty::isSelectable).collect(toList());
    }
  }

  private static void validatePrimaryKeyProperties(final Map<Attribute<?>, Property<?>> propertyMap) {
    final Collection<Integer> usedPrimaryKeyIndexes = new ArrayList<>();
    for (final Property<?> property : propertyMap.values()) {
      if (property instanceof ColumnProperty && ((ColumnProperty<?>) property).isPrimaryKeyProperty()) {
        final Integer index = ((ColumnProperty<?>) property).getPrimaryKeyIndex();
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
    public <V> Builder beanClass(final Class<V> beanClass) {
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

  private static final class DefaultBeanHelper implements BeanHelper<Object> {

    private static final long serialVersionUID = 1;
  }
}
