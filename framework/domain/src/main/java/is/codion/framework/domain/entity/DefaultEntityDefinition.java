/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.common.Text;
import is.codion.framework.domain.property.BlobProperty;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.DenormalizedProperty;
import is.codion.framework.domain.property.DerivedProperty;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.domain.property.Property;
import is.codion.framework.domain.property.TransientProperty;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static is.codion.common.Util.nullOrEmpty;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * A class encapsulating a entity definition, such as table name, order by clause and properties.
 */
final class DefaultEntityDefinition implements EntityDefinition, Serializable {

  private static final long serialVersionUID = 1;

  private static final String METHOD = "method";

  /**
   * The domain name
   */
  private final String domainName;

  /**
   * The entity type
   */
  private final EntityType<?> entityType;

  /**
   * Bean property getters
   */
  private final Map<String, Attribute<?>> getters = new HashMap<>();

  /**
   * Bean property setters
   */
  private final Map<String, Attribute<?>> setters = new HashMap<>();

  /**
   * Entity class default method handles
   */
  private transient Map<String, MethodHandle> defaultMethodHandles = new ConcurrentHashMap<>();

  /**
   * The caption to use for the entity type
   */
  private String caption;

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
  private transient Map<ConditionType, ConditionProvider> conditionProviders;

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
   * Defines a new entity type with the entityType name serving as the initial entity caption.
   */
  DefaultEntityDefinition(final String domainName, final EntityType<?> entityType, final String tableName,
                          final List<Property.Builder<?>> propertyBuilders) {
    if (propertyBuilders.isEmpty()) {
      throw new IllegalArgumentException("An entity must have one or more properties");
    }
    this.domainName = requireNonNull(domainName, "domainName");
    this.entityType = requireNonNull(entityType, "entityType");
    if (nullOrEmpty(tableName)) {
      throw new IllegalArgumentException("Table name must be non-empty");
    }
    this.tableName = tableName;
    this.entityProperties = new EntityProperties(entityType, propertyBuilders);
    this.hasDenormalizedProperties = !entityProperties.denormalizedProperties.isEmpty();
    this.groupByClause = initializeGroupByClause();
    this.caption = entityType.getName();
    resolveEntityClassMethods();
  }

  @Override
  public EntityType<?> getEntityType() {
    return entityType;
  }

  @Override
  public Attribute<?> getGetterAttribute(final Method method) {
    return getters.get(requireNonNull(method, METHOD).getName());
  }

  @Override
  public Attribute<?> getSetterAttribute(final Method method) {
    return setters.get(requireNonNull(method, METHOD).getName());
  }

  @Override
  public MethodHandle getDefaultMethodHandle(final Method method) {
    return defaultMethodHandles.computeIfAbsent(requireNonNull(method, METHOD).getName(),
            methodName -> createDefaultMethodHandle(method));
  }

  @Override
  public String getTableName() {
    return tableName;
  }

  @Override
  public ConditionProvider getConditionProvider(final ConditionType conditionType) {
    requireNonNull(conditionType);
    if (conditionProviders != null) {
      final ConditionProvider conditionProvider = conditionProviders.get(conditionType);
      if (conditionProvider != null) {
        return conditionProvider;
      }
    }

    throw new IllegalArgumentException("ConditionProvider for type " + conditionType + " not found");
  }

  @Override
  public String getDomainName() {
    return domainName;
  }

  @Override
  public String getCaption() {
    return caption;
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
  public <T> Attribute<T> getAttribute(final String attributeName) {
    return (Attribute<T>) entityProperties.attributeMap.get(attributeName);
  }

  @Override
  public Collection<Attribute<String>> getSearchAttributes() {
    return entityProperties.columnProperties.stream().filter(ColumnProperty::isSearchProperty)
            .map(property -> ((ColumnProperty<String>) property).getAttribute()).collect(toList());
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
      throw new IllegalArgumentException("Property '" + attribute + "' not found in entity: " + entityType);
    }

    return property;
  }

  @Override
  public <T> ColumnProperty<T> getPrimaryKeyProperty(final Attribute<T> attribute) {
    requireNonNull(attribute, "attribute");
    final ColumnProperty<T> property = (ColumnProperty<T>) entityProperties.primaryKeyPropertyMap.get(attribute);
    if (property == null) {
      throw new IllegalArgumentException("Primary key property " + attribute + " not found in entity: " + entityType);
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
                    (includePrimaryKeyProperties || !property.isPrimaryKeyColumn()))
            .collect(toList());
  }

  @Override
  public List<Property<?>> getUpdatableProperties() {
    final List<ColumnProperty<?>> writableColumnProperties = getWritableColumnProperties(!isKeyGenerated(), false);
    writableColumnProperties.removeIf(property -> isForeignKeyAttribute(property.getAttribute()) || property.isDenormalized());
    final List<Property<?>> updatable = new ArrayList<>(writableColumnProperties);
    for (final ForeignKeyProperty foreignKeyProperty : entityProperties.foreignKeyProperties) {
      if (isUpdatable(foreignKeyProperty)) {
        updatable.add(foreignKeyProperty);
      }
    }

    return updatable;
  }

  @Override
  public boolean isUpdatable(final ForeignKeyProperty foreignKeyProperty) {
    return foreignKeyProperty.getReferences().stream()
            .map(reference -> getColumnProperty(reference.getAttribute()))
            .allMatch(ColumnProperty::isUpdatable);
  }

  @Override
  public boolean isForeignKeyAttribute(final Attribute<?> attribute) {
    return entityProperties.foreignKeyColumnAttributes.contains(attribute);
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
  public List<ForeignKeyProperty> getForeignKeyReferences(final EntityType<?> foreignEntityType) {
    return getForeignKeyProperties().stream().filter(foreignKeyProperty ->
            foreignKeyProperty.getReferencedEntityType().equals(foreignEntityType)).collect(toList());
  }

  @Override
  public ForeignKeyProperty getForeignKeyProperty(final Attribute<Entity> attribute) {
    requireNonNull(attribute, "attribute");
    final ForeignKeyProperty property = entityProperties.foreignKeyPropertyMap.get(attribute);
    if (property == null) {
      throw new IllegalArgumentException("Foreign key attribute: " + attribute + " not found in entity of type: " + entityType);
    }

    return property;
  }

  @Override
  public <T> List<ForeignKeyProperty> getForeignKeyProperties(final Attribute<T> columnAttribute) {
    requireNonNull(columnAttribute, "columnAttribute");
    return entityProperties.columnPropertyForeignKeyProperties.computeIfAbsent(columnAttribute, attribute -> Collections.emptyList());
  }

  @Override
  public List<Property<?>> getProperties() {
    return entityProperties.properties;
  }

  @Override
  public boolean hasPrimaryKey() {
    return !entityProperties.primaryKeyProperties.isEmpty();
  }

  @Override
  public boolean hasDerivedAttributes() {
    return !entityProperties.derivedAttributes.isEmpty();
  }

  @Override
  public <T> boolean hasDerivedAttributes(final Attribute<T> attribute) {
    return entityProperties.derivedAttributes.containsKey(attribute);
  }

  @Override
  public <T> Collection<Attribute<?>> getDerivedAttributes(final Attribute<T> attribute) {
    final Collection<Attribute<?>> derived = entityProperties.derivedAttributes.get(attribute);

    return derived == null ? emptyList() : derived;
  }

  @Override
  public List<Attribute<?>> getPrimaryKeyAttributes() {
    return entityProperties.primaryKeyAttribues;
  }

  @Override
  public List<ColumnProperty<?>> getPrimaryKeyProperties() {
    return entityProperties.primaryKeyProperties;
  }

  @Override
  public List<Property<?>> getVisibleProperties() {
    return entityProperties.properties.stream().filter(property -> !property.isHidden()).collect(toList());
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
  public <T> boolean hasDenormalizedProperties(final Attribute<T> foreignKeyAttribute) {
    return hasDenormalizedProperties && entityProperties.denormalizedProperties.containsKey(foreignKeyAttribute);
  }

  @Override
  public <T> List<DenormalizedProperty<?>> getDenormalizedProperties(final Attribute<T> foreignKeyAttribute) {
    return entityProperties.denormalizedProperties.get(foreignKeyAttribute);
  }

  @Override
  public String toString() {
    return entityType.getName();
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
  public Entity entity(final Key key) {
    return new DefaultEntity(this, key);
  }

  @Override
  public Entity entity(final Function<Attribute<?>, Object> valueProvider) {
    requireNonNull(valueProvider);
    final Entity entity = entity();
    for (@SuppressWarnings("rawtypes") final ColumnProperty property : entityProperties.columnProperties) {
      if (!isForeignKeyAttribute(property.getAttribute()) && !property.isDenormalized()//these are set via their respective parent properties
              && (!property.columnHasDefaultValue() || property.hasDefaultValue())) {
        entity.put(property.getAttribute(), valueProvider.apply(property.getAttribute()));
      }
    }
    for (@SuppressWarnings("rawtypes") final TransientProperty transientProperty : entityProperties.transientProperties) {
      if (!(transientProperty instanceof DerivedProperty)) {
        entity.put(transientProperty.getAttribute(), valueProvider.apply(transientProperty.getAttribute()));
      }
    }
    for (final ForeignKeyProperty foreignKeyProperty : entityProperties.foreignKeyProperties) {
      entity.put(foreignKeyProperty.getAttribute(), (Entity) valueProvider.apply(foreignKeyProperty.getAttribute()));
    }
    entity.saveAll();

    return entity;
  }

  @Override
  public Entity entity(final Map<Attribute<?>, Object> values, final Map<Attribute<?>, Object> originalValues) {
    return new DefaultEntity(this, values, originalValues);
  }

  @Override
  public Key primaryKey() {
    if (hasPrimaryKey()) {
      return new DefaultKey(this, getPrimaryKeyAttributes(), true);
    }

    return new DefaultKey(this, emptyList(), true);
  }

  @Override
  public Key primaryKey(final Integer value) {
    return createPrimaryKey(value);
  }

  @Override
  public Key primaryKey(final Long value) {
    return createPrimaryKey(value);
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
    if (!foreignKeyProperty.getReferencedEntityType().equals(definition.getEntityType())) {
      throw new IllegalArgumentException("Definition for entity " + foreignKeyProperty.getReferencedEntityType() +
              " expected for " + foreignKeyAttribute);
    }
    foreignEntityDefinitions.put(foreignKeyAttribute, definition);
  }

  /**
   * @return a {@link Builder} for this definition instance
   */
  DefaultBuilder builder() {
    return new DefaultBuilder(this);
  }

  private void readObject(final ObjectInputStream stream) throws IOException, ClassNotFoundException {
    stream.defaultReadObject();
    defaultMethodHandles = new ConcurrentHashMap<>();
  }

  private Key createPrimaryKey(final Object value) {
    if (!hasPrimaryKey()) {
      throw new IllegalArgumentException("Entity '" + entityType + "' has no primary key defined");
    }
    if (getPrimaryKeyAttributes().size() > 1) {
      throw new IllegalArgumentException(entityType + " has a composite primary key");
    }
    final Attribute<Object> attribute = (Attribute<Object>) getPrimaryKeyAttributes().get(0);
    attribute.validateType(value);

    return new DefaultKey(this, attribute, value, true);
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

  private void resolveEntityClassMethods() {
    if (!entityType.getEntityClass().equals(Entity.class)) {
      for (final Method method : entityType.getEntityClass().getDeclaredMethods()) {
        if (method.isDefault()) {
          defaultMethodHandles.put(method.getName(), createDefaultMethodHandle(method));
        }
        else {
          getProperties().stream().filter(property -> isGetter(method, property)).findFirst()
                  .ifPresent(property -> getters.put(method.getName(), property.getAttribute()));
          getProperties().stream().filter(property -> isSetter(method, property)).findFirst()
                  .ifPresent(property -> setters.put(method.getName(), property.getAttribute()));
        }
      }
    }
  }

  /**
   * Hacky way to use default methods in interfaces via dynamic proxy.
   * @param method the default method
   * @return a MethodHandle for the given method
   */
  private static MethodHandle createDefaultMethodHandle(final Method method) {
    try {
      final Method privateLookupIn = MethodHandles.class.getMethod("privateLookupIn", Class.class, MethodHandles.Lookup.class);

      final MethodHandles.Lookup lookup = (MethodHandles.Lookup) privateLookupIn.invoke(MethodHandles.class,
              method.getDeclaringClass(), MethodHandles.lookup());

			return lookup.findSpecial(method.getDeclaringClass(), method.getName(),
              MethodType.methodType(method.getReturnType(), method.getParameterTypes()), method.getDeclaringClass());
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static boolean isGetter(final Method method, final Property<?> property) {
    final String beanProperty = property.getBeanProperty();
    if (beanProperty == null || method.getParameterCount() > 0) {
      return false;
    }

    final String beanPropertyCamelCase = beanProperty.substring(0, 1).toUpperCase() + beanProperty.substring(1);
    final String methodName = method.getName();
    final Class<?> typeClass = getAttributeTypeClass(property);

    return (method.getReturnType().equals(typeClass) || method.getReturnType().equals(Optional.class))
            && (methodName.equals(beanProperty) || methodName.equals("get" + beanPropertyCamelCase) ||
            (methodName.equals("is" + beanPropertyCamelCase) && Boolean.class.equals(typeClass)));
  }

  private static boolean isSetter(final Method method, final Property<?> property) {
    final String beanProperty = property.getBeanProperty();
    if (beanProperty == null || method.getParameterCount() != 1) {
      return false;
    }

    final String beanPropertyCamelCase = beanProperty.substring(0, 1).toUpperCase() + beanProperty.substring(1);
    final String methodName = method.getName();
    final Class<?> typeClass = getAttributeTypeClass(property);

    return method.getParameterTypes()[0].equals(typeClass) && (methodName.equals(beanProperty) || methodName.equals("set" + beanPropertyCamelCase));
  }

  private static Class<?> getAttributeTypeClass(final Property<?> property) {
    Class<?> typeClass = property.getAttribute().getTypeClass();
    if (property instanceof ForeignKeyProperty) {
      typeClass = ((ForeignKeyProperty) property).getReferencedEntityType().getEntityClass();
    }

    return typeClass;
  }

  private static final class EntityProperties implements Serializable {

    private static final long serialVersionUID = 1;

    private final EntityType<?> entityType;

    private final Map<String, Attribute<?>> attributeMap;
    private final Map<Attribute<?>, Property<?>> propertyMap;
    private final List<Property<?>> properties;
    private final List<ColumnProperty<?>> columnProperties;
    private final List<ColumnProperty<?>> lazyLoadedBlobProperties;
    private final List<ColumnProperty<?>> selectableColumnProperties;
    private final List<Attribute<?>> primaryKeyAttribues;
    private final List<ColumnProperty<?>> primaryKeyProperties;
    private final Map<Attribute<?>, ColumnProperty<?>> primaryKeyPropertyMap;
    private final List<ForeignKeyProperty> foreignKeyProperties;
    private final Map<Attribute<Entity>, ForeignKeyProperty> foreignKeyPropertyMap;
    private final Map<Attribute<?>, List<ForeignKeyProperty>> columnPropertyForeignKeyProperties;
    private final Set<Attribute<?>> foreignKeyColumnAttributes = new HashSet<>();
    private final Map<Attribute<?>, Set<Attribute<?>>> derivedAttributes;
    private final List<TransientProperty<?>> transientProperties;
    private final Map<Attribute<?>, List<DenormalizedProperty<?>>> denormalizedProperties;

    private EntityProperties(final EntityType<?> entityType, final List<Property.Builder<?>> propertyBuilders) {
      this.entityType = entityType;
      this.propertyMap = initializePropertyMap(propertyBuilders);
      this.attributeMap = initializeAttributeMap();
      this.properties = unmodifiableList(new ArrayList<>(propertyMap.values()));
      this.columnProperties = unmodifiableList(getColumnProperties());
      this.lazyLoadedBlobProperties = initializeLazyLoadedBlobProperties();
      this.selectableColumnProperties = unmodifiableList(getSelectableProperties());
      this.primaryKeyProperties = unmodifiableList(getPrimaryKeyProperties());
      this.primaryKeyAttribues = unmodifiableList(getPrimaryKeyAttributes());
      this.primaryKeyPropertyMap = initializePrimaryKeyPropertyMap();
      this.foreignKeyProperties = unmodifiableList(getForeignKeyProperties());
      this.foreignKeyPropertyMap = initializeForeignKeyPropertyMap();
      this.columnPropertyForeignKeyProperties = initializeColumnPropertyForeignKeyProperties();
      initializeForeignKeyColumnProperties(propertyBuilders.stream()
              .filter(property -> property instanceof ForeignKeyProperty.Builder)
              .map(property -> (ForeignKeyProperty.Builder) property).collect(toList()));
      this.derivedAttributes = initializeDerivedAttributes();
      this.transientProperties = unmodifiableList(getTransientProperties());
      this.denormalizedProperties = unmodifiableMap(getDenormalizedProperties());
    }

    private Map<Attribute<?>, Property<?>> initializePropertyMap(final List<Property.Builder<?>> propertyBuilders) {
      final Map<Attribute<?>, Property<?>> map = new LinkedHashMap<>(propertyBuilders.size());
      for (final Property.Builder<?> builder : propertyBuilders) {
        validateAndAddProperty(builder.get(), map);
      }
      validatePrimaryKeyProperties(map);

      return unmodifiableMap(map);
    }

    private Map<String, Attribute<?>> initializeAttributeMap() {
      final Map<String, Attribute<?>> map = new HashMap<>();
      propertyMap.values().forEach(property -> map.put(property.getAttribute().getName(), property.getAttribute()));

      return map;
    }

    private void validateAndAddProperty(final Property<?> property, final Map<Attribute<?>, Property<?>> propertyMap) {
      validate(property, propertyMap);
      propertyMap.put(property.getAttribute(), property);
    }

    private static void validatePrimaryKeyProperties(final Map<Attribute<?>, Property<?>> propertyMap) {
      final Collection<Integer> usedPrimaryKeyIndexes = new ArrayList<>();
      for (final Property<?> property : propertyMap.values()) {
        if (property instanceof ColumnProperty && ((ColumnProperty<?>) property).isPrimaryKeyColumn()) {
          final Integer index = ((ColumnProperty<?>) property).getPrimaryKeyIndex();
          if (usedPrimaryKeyIndexes.contains(index)) {
            throw new IllegalArgumentException("Primary key index " + index + " in property " + property + " has already been used");
          }
          usedPrimaryKeyIndexes.add(index);
        }
      }
    }

    private void validate(final Property<?> property, final Map<Attribute<?>, Property<?>> propertyMap) {
      if (!entityType.equals(property.getEntityType())) {
        throw new IllegalArgumentException("Attribute entityType (" +
                property.getEntityType() + ") does not match the definition entityType: " + entityType);
      }
      if (propertyMap.containsKey(property.getAttribute())) {
        throw new IllegalArgumentException("Property " + property.getAttribute()
                + (property.getCaption() != null ? " (caption: " + property.getCaption() + ")" : "")
                + " has already been defined as: " + propertyMap.get(property.getAttribute()) + " in entity: " + entityType);
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
              foreignKeyProperty.getReferences().forEach(reference ->
                      foreignKeyMap.computeIfAbsent(reference.getAttribute(),
                              columnAttribute -> new ArrayList<>()).add(foreignKeyProperty)));

      return foreignKeyMap;
    }

    private void initializeForeignKeyColumnProperties(final List<ForeignKeyProperty.Builder> builders) {
      final Map<Attribute<Entity>, List<ColumnProperty<?>>> foreignKeyColumnPropertyMap = new HashMap<>();
      foreignKeyProperties.forEach(foreignKeyProperty ->
              foreignKeyColumnPropertyMap.put(foreignKeyProperty.getAttribute(),
              foreignKeyProperty.getReferences().stream().map(reference -> {
                final ColumnProperty<?> columnProperty = (ColumnProperty<?>) propertyMap.get(reference.getAttribute());
                if (columnProperty == null) {
                  throw new IllegalArgumentException("Property based on attribute: " + reference.getAttribute()
                          + " not found when initializing foreign key");
                }

                return columnProperty;
              }).collect(toList())));
      foreignKeyColumnPropertyMap.values().forEach(fkColumnProperties -> fkColumnProperties.forEach(columnProperty ->
              foreignKeyColumnAttributes.add(columnProperty.getAttribute())));
      builders.forEach(builder -> builder.nullable(foreignKeyColumnPropertyMap.get(builder.get().getAttribute())
              .stream().anyMatch(Property::isNullable)));
    }

    private Map<Attribute<?>, ColumnProperty<?>> initializePrimaryKeyPropertyMap() {
      final Map<Attribute<?>, ColumnProperty<?>> map = new HashMap<>(this.primaryKeyProperties.size());
      this.primaryKeyProperties.forEach(property -> map.put(property.getAttribute(), property));

      return unmodifiableMap(map);
    }

    private List<ForeignKeyProperty> getForeignKeyProperties() {
      final List<ForeignKeyProperty> foreignKeys = properties.stream()
              .filter(property -> property instanceof ForeignKeyProperty)
              .map(property -> (ForeignKeyProperty) property).collect(toList());
      for (final ForeignKeyProperty foreignKeyProperty : foreignKeys) {
        if (foreignKeyProperty.getReferences().isEmpty()) {
          throw new IllegalArgumentException("Foreign key property: " + foreignKeyProperty + " contains no references");
        }
      }

      return foreignKeys;
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
      return columnProperties.stream().filter(property -> property.getAttribute().isByteArray()).filter(property ->
              !(property instanceof BlobProperty) || !((BlobProperty) property).isEagerlyLoaded()).collect(toList());
    }

    private Map<Attribute<?>, List<DenormalizedProperty<?>>> getDenormalizedProperties() {
      final Map<Attribute<?>, List<DenormalizedProperty<?>>> map = new HashMap<>(properties.size());
      for (final Property<?> property : properties) {
        if (property instanceof DenormalizedProperty) {
          final DenormalizedProperty<?> denormalizedProperty = (DenormalizedProperty<?>) property;
          map.computeIfAbsent(denormalizedProperty.getEntityAttribute(), attribute -> new ArrayList<>()).add(denormalizedProperty);
        }
      }

      return map;
    }

    private Map<Attribute<?>, Set<Attribute<?>>> initializeDerivedAttributes() {
      final Map<Attribute<?>, Set<Attribute<?>>> derivedPropertyMap = new HashMap<>();
      for (final Property<?> property : properties) {
        if (property instanceof DerivedProperty) {
          for (final Attribute<?> sourceAttribute : ((DerivedProperty<?>) property).getSourceAttributes()) {
            derivedPropertyMap.computeIfAbsent(sourceAttribute, attribute -> new HashSet<>()).add(property.getAttribute());
          }
        }
      }

      return derivedPropertyMap;
    }

    private List<ColumnProperty<?>> getPrimaryKeyProperties() {
      return properties.stream().filter(property -> property instanceof ColumnProperty
              && ((ColumnProperty<?>) property).isPrimaryKeyColumn()).map(property -> (ColumnProperty<?>) property)
              .sorted((pk1, pk2) -> {
                final Integer index1 = pk1.getPrimaryKeyIndex();
                final Integer index2 = pk2.getPrimaryKeyIndex();

                return index1.compareTo(index2);
              }).collect(toList());
    }

    private List<Attribute<?>> getPrimaryKeyAttributes() {
      return this.primaryKeyProperties.stream().map(Property::getAttribute).collect(toList());
    }

    private List<ColumnProperty<?>> getSelectableProperties() {
      return columnProperties.stream().filter(property ->
              !lazyLoadedBlobProperties.contains(property)).filter(ColumnProperty::isSelectable).collect(toList());
    }
  }

  static final class DefaultBuilder implements Builder {

    private final DefaultEntityDefinition definition;

    private DefaultBuilder(final DefaultEntityDefinition definition) {
      this.definition = definition;
    }

    @Override
    public DefaultEntityDefinition get() {
      return definition;
    }

    @Override
    public Builder conditionProvider(final ConditionType conditionType, final ConditionProvider conditionProvider) {
      requireNonNull(conditionType, "conditionType");
      requireNonNull(conditionProvider, "conditionProvider");
      if (definition.conditionProviders == null) {
        definition.conditionProviders = new HashMap<>();
      }
      if (definition.conditionProviders.containsKey(conditionType)) {
        throw new IllegalStateException("ConditionProvider for type  " + conditionType + " has already been added");
      }
      definition.conditionProviders.put(conditionType, conditionProvider);
      return this;
    }

    @Override
    public Builder caption(final String caption) {
      definition.caption = requireNonNull(caption, "caption");
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
    public Builder stringFactory(final StringFactory.Builder builder) {
      return stringFactory(requireNonNull(builder, "builder").get());
    }

    @Override
    public Builder stringFactory(final Function<Entity, String> stringFactory) {
      definition.stringProvider = requireNonNull(stringFactory, "stringProvider");
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
  }
}
