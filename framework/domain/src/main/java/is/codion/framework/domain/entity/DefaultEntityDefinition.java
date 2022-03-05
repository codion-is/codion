/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.common.Text;
import is.codion.common.Util;
import is.codion.framework.domain.entity.query.SelectQuery;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static is.codion.common.Util.nullOrEmpty;
import static java.util.Collections.*;
import static java.util.Comparator.comparingInt;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.*;

/**
 * A class encapsulating an entity definition, such as table name, order by clause and properties.
 */
final class DefaultEntityDefinition implements EntityDefinition, Serializable {

  private static final long serialVersionUID = 1;

  private static final String METHOD = "method";
  private static final String ATTRIBUTE = "attribute";
  private static final String ATTRIBUTES = "attributes";
  private static final String FOREIGN_KEY = "foreignKey";

  /**
   * The domain name
   */
  private final String domainName;

  /**
   * The entity type
   */
  private final EntityType entityType;

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
   * The resource bundle key specifying the caption
   */
  private String captionResourceKey;

  /**
   * The caption from the resource bundle, if any
   */
  private transient String resourceCaption;

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
  private Function<Entity, String> stringFactory = new DefaultStringProvider();

  /**
   * Provides the background color
   */
  private ColorProvider backgroundColorProvider = new NullColorProvider();

  /**
   * Provides the color
   */
  private ColorProvider foregroundColorProvider = new NullColorProvider();

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
   * Used if it differs from the one used for inserts, updates and deletes
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
   * Provides a custom sql query used when selecting entities of this type
   */
  private transient SelectQuery selectQuery;

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
  DefaultEntityDefinition(String domainName, EntityType entityType, String tableName,
                          List<Property.Builder<?, ?>> propertyBuilders) {
    if (propertyBuilders.isEmpty()) {
      throw new IllegalArgumentException("An entity must have one or more properties");
    }
    this.domainName = requireNonNull(domainName, "domainName");
    this.entityType = requireNonNull(entityType, "entityType");
    if (nullOrEmpty(tableName)) {
      throw new IllegalArgumentException("Table name must be non-empty");
    }
    this.tableName = tableName;
    this.captionResourceKey = entityType.getName();
    this.entityProperties = new EntityProperties(entityType, propertyBuilders);
    this.hasDenormalizedProperties = !entityProperties.denormalizedProperties.isEmpty();
    this.groupByClause = initializeGroupByClause();
    resolveEntityClassMethods();
  }

  @Override
  public EntityType getEntityType() {
    return entityType;
  }

  @Override
  public Attribute<?> getGetterAttribute(Method method) {
    return getters.get(requireNonNull(method, METHOD).getName());
  }

  @Override
  public Attribute<?> getSetterAttribute(Method method) {
    return setters.get(requireNonNull(method, METHOD).getName());
  }

  @Override
  public MethodHandle getDefaultMethodHandle(Method method) {
    return defaultMethodHandles.computeIfAbsent(requireNonNull(method, METHOD).getName(),
            methodName -> createDefaultMethodHandle(method));
  }

  @Override
  public int getSerializationVersion() {
    return entityProperties.serializationVersion;
  }

  @Override
  public String getTableName() {
    return tableName;
  }

  @Override
  public ConditionProvider getConditionProvider(ConditionType conditionType) {
    requireNonNull(conditionType);
    if (conditionProviders != null) {
      ConditionProvider conditionProvider = conditionProviders.get(conditionType);
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
    if (entityType.getResourceBundleName() != null) {
      if (resourceCaption == null) {
        ResourceBundle bundle = ResourceBundle.getBundle(entityType.getResourceBundleName());
        resourceCaption = bundle.containsKey(captionResourceKey) ? bundle.getString(captionResourceKey) : "";
      }

      if (!resourceCaption.isEmpty()) {
        return resourceCaption;
      }
    }

    return caption == null ? entityType.getName() : caption;
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
  public SelectQuery getSelectQuery() {
    return selectQuery;
  }

  @Override
  public Function<Entity, String> getStringFactory() {
    return stringFactory;
  }

  @Override
  public Comparator<Entity> getComparator() {
    return comparator;
  }

  @Override
  public boolean containsAttribute(Attribute<?> attribute) {
    return entityProperties.propertyMap.containsKey(requireNonNull(attribute));
  }

  @Override
  public <T> Attribute<T> getAttribute(String attributeName) {
    return (Attribute<T>) entityProperties.attributeMap.get(requireNonNull(attributeName));
  }

  @Override
  public Collection<Attribute<String>> getSearchAttributes() {
    return entityProperties.columnProperties.stream()
            .filter(ColumnProperty::isSearchProperty)
            .map(property -> ((ColumnProperty<String>) property).getAttribute())
            .collect(toList());
  }

  @Override
  public Collection<Attribute<?>> getDefaultSelectAttributes() {
    return entityProperties.defaultSelectAttributes;
  }

  @Override
  public <T> ColumnProperty<T> getColumnProperty(Attribute<T> attribute) {
    Property<T> property = getProperty(attribute);
    if (!(property instanceof ColumnProperty)) {
      throw new IllegalArgumentException("Property based on " + attribute + " is not a ColumnProperty");
    }

    return (ColumnProperty<T>) property;
  }

  @Override
  public <T> Property<T> getProperty(Attribute<T> attribute) {
    requireNonNull(attribute, ATTRIBUTE);
    Property<T> property = (Property<T>) entityProperties.propertyMap.get(attribute);
    if (property == null) {
      throw new IllegalArgumentException("Property based on " + attribute + " not found in entity: " + entityType);
    }

    return property;
  }

  @Override
  public <T> ColumnProperty<T> getPrimaryKeyProperty(Attribute<T> attribute) {
    requireNonNull(attribute, ATTRIBUTE);
    ColumnProperty<T> property = (ColumnProperty<T>) entityProperties.primaryKeyPropertyMap.get(attribute);
    if (property == null) {
      throw new IllegalArgumentException("Primary key property based on " + attribute + " not found in entity: " + entityType);
    }

    return property;
  }

  @Override
  public List<Property<?>> getProperties(Collection<Attribute<?>> attributes) {
    requireNonNull(attributes, ATTRIBUTES);

    return attributes.stream()
            .map(this::getProperty)
            .collect(toList());
  }

  @Override
  public List<ColumnProperty<?>> getColumnProperties(List<Attribute<?>> attributes) {
    requireNonNull(attributes, ATTRIBUTES);
    List<ColumnProperty<?>> theProperties = new ArrayList<>(attributes.size());
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
  public List<ColumnProperty<?>> getWritableColumnProperties(boolean includePrimaryKeyProperties,
                                                             boolean includeNonUpdatable) {
    return entityProperties.columnProperties.stream()
            .filter(property -> isWritable(property, includePrimaryKeyProperties, includeNonUpdatable))
            .collect(toList());
  }

  @Override
  public List<Property<?>> getUpdatableProperties() {
    List<ColumnProperty<?>> writableColumnProperties = getWritableColumnProperties(!isKeyGenerated(), false);
    writableColumnProperties.removeIf(property -> isForeignKeyAttribute(property.getAttribute()) || property.isDenormalized());
    List<Property<?>> updatable = new ArrayList<>(writableColumnProperties);
    for (ForeignKeyProperty foreignKeyProperty : entityProperties.foreignKeyProperties) {
      if (isUpdatable(foreignKeyProperty.getAttribute())) {
        updatable.add(foreignKeyProperty);
      }
    }

    return updatable;
  }

  @Override
  public boolean isUpdatable(ForeignKey foreignKey) {
    return foreignKey.getReferences().stream()
            .map(reference -> getColumnProperty(reference.getAttribute()))
            .allMatch(ColumnProperty::isUpdatable);
  }

  @Override
  public boolean isForeignKeyAttribute(Attribute<?> attribute) {
    return entityProperties.foreignKeyColumnAttributes.contains(attribute);
  }

  @Override
  public List<ForeignKey> getForeignKeys(EntityType referencedEntityType) {
    requireNonNull(referencedEntityType, "referencedEntityType");
    return getForeignKeys().stream()
            .filter(foreignKey -> foreignKey.getReferencedEntityType().equals(referencedEntityType))
            .collect(toList());
  }

  @Override
  public ForeignKeyProperty getForeignKeyProperty(ForeignKey foreignKey) {
    requireNonNull(foreignKey, FOREIGN_KEY);
    ForeignKeyProperty property = entityProperties.foreignKeyPropertyMap.get(foreignKey);
    if (property == null) {
      throw new IllegalArgumentException("Foreign key: " + foreignKey + " not found in entity of type: " + entityType);
    }

    return property;
  }

  @Override
  public <T> List<ForeignKeyProperty> getForeignKeyProperties(Attribute<T> columnAttribute) {
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
  public <T> boolean hasDerivedAttributes(Attribute<T> attribute) {
    return entityProperties.derivedAttributes.containsKey(attribute);
  }

  @Override
  public <T> Collection<Attribute<?>> getDerivedAttributes(Attribute<T> attribute) {
    return entityProperties.derivedAttributes.getOrDefault(attribute, emptySet());
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
    return entityProperties.properties.stream()
            .filter(property -> !property.isHidden())
            .collect(toList());
  }

  @Override
  public List<ColumnProperty<?>> getColumnProperties() {
    return entityProperties.columnProperties;
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
  public Collection<ForeignKey> getForeignKeys() {
    return entityProperties.foreignKeyPropertyMap.keySet();
  }

  @Override
  public EntityDefinition getReferencedEntityDefinition(ForeignKey foreignKey) {
    requireNonNull(foreignKey, FOREIGN_KEY);
    EntityDefinition definition = foreignEntityDefinitions.get(foreignKey);
    if (definition == null) {
      throw new IllegalArgumentException("Referenced entity definition not found for foreign key: " + foreignKey);
    }

    return definition;
  }

  @Override
  public boolean hasDenormalizedProperties() {
    return hasDenormalizedProperties;
  }

  @Override
  public boolean hasDenormalizedProperties(Attribute<Entity> entityAttribute) {
    return hasDenormalizedProperties && entityProperties.denormalizedProperties.containsKey(entityAttribute);
  }

  @Override
  public List<DenormalizedProperty<?>> getDenormalizedProperties(Attribute<Entity> entityAttribute) {
    return entityProperties.denormalizedProperties.get(entityAttribute);
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
  public ColorProvider getBackgroundColorProvider() {
    return backgroundColorProvider;
  }

  @Override
  public ColorProvider getForegroundColorProvider() {
    return foregroundColorProvider;
  }

  @Override
  public Entity entity() {
    return entity(null, null);
  }

  @Override
  public Entity entity(Key key) {
    return new DefaultEntity(this, key);
  }

  @Override
  public Entity entity(Map<Attribute<?>, Object> values, Map<Attribute<?>, Object> originalValues) {
    return new DefaultEntity(this, values, originalValues);
  }

  @Override
  public Entity entityWithDefaultValues() {
    Map<Attribute<?>, Object> values = new HashMap<>();
    getProperties().forEach(property -> values.put(property.getAttribute(), property.getDefaultValue()));

    return entity(values, null);
  }

  @Override
  public Key primaryKey(Integer value) {
    return createPrimaryKey(value);
  }

  @Override
  public Key primaryKey(Long value) {
    return createPrimaryKey(value);
  }

  /**
   * Returns true if an entity definition has been associated with the given foreign key.
   * @param foreignKey the foreign key
   * @return true if the referenced entity definition has been set for the given foreign key
   */
  boolean hasReferencedEntityDefinition(ForeignKey foreignKey) {
    return foreignEntityDefinitions.containsKey(foreignKey);
  }

  /**
   * Associates the given definition with the given foreign key.
   * @param foreignKey the foreign key attribute
   * @param definition the entity definition referenced by the given foreign key
   * @throws IllegalStateException in case the foreign definition has already been set
   * @throws IllegalArgumentException in case the definition does not match the foreign key
   */
  void setReferencedEntityDefinition(ForeignKey foreignKey, EntityDefinition definition) {
    requireNonNull(foreignKey, FOREIGN_KEY);
    requireNonNull(definition, "definition");
    ForeignKeyProperty foreignKeyProperty = getForeignKeyProperty(foreignKey);
    if (foreignEntityDefinitions.containsKey(foreignKey)) {
      throw new IllegalStateException("Foreign definition has already been set for " + foreignKey);
    }
    if (!foreignKeyProperty.getReferencedEntityType().equals(definition.getEntityType())) {
      throw new IllegalArgumentException("Definition for entity " + foreignKeyProperty.getReferencedEntityType() +
              " expected for " + foreignKey);
    }
    foreignEntityDefinitions.put(foreignKey, definition);
  }

  /**
   * @return a {@link Builder} for this definition instance
   */
  DefaultBuilder builder() {
    return new DefaultBuilder(this);
  }

  private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
    stream.defaultReadObject();
    defaultMethodHandles = new ConcurrentHashMap<>();
  }

  private Key createPrimaryKey(Object value) {
    if (!hasPrimaryKey()) {
      throw new IllegalArgumentException("Entity '" + entityType + "' has no primary key defined");
    }
    if (getPrimaryKeyAttributes().size() > 1) {
      throw new IllegalStateException(entityType + " has a composite primary key");
    }
    Attribute<Object> attribute = (Attribute<Object>) getPrimaryKeyAttributes().get(0);
    attribute.validateType(value);

    return new DefaultKey(this, attribute, value, true);
  }

  /**
   * @return a list of grouping columns separated with a comma, to serve as a group by clause,
   * null if no grouping properties are defined
   */
  private String initializeGroupByClause() {
    List<String> groupingColumnNames = entityProperties.columnProperties.stream()
            .filter(ColumnProperty::isGroupingColumn)
            .map(ColumnProperty::getColumnExpression)
            .collect(toList());
    if (groupingColumnNames.isEmpty()) {
      return null;
    }

    return String.join(", ", groupingColumnNames);
  }

  private void resolveEntityClassMethods() {
    if (!entityType.getEntityClass().equals(Entity.class)) {
      for (Method method : entityType.getEntityClass().getDeclaredMethods()) {
        if (method.isDefault()) {
          defaultMethodHandles.put(method.getName(), createDefaultMethodHandle(method));
        }
        else {
          getProperties().stream()
                  .filter(property -> isGetter(method, property))
                  .findFirst()
                  .ifPresent(property -> getters.put(method.getName(), property.getAttribute()));
          getProperties().stream()
                  .filter(property -> isSetter(method, property))
                  .findFirst()
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
  private static MethodHandle createDefaultMethodHandle(Method method) {
    try {
      Method privateLookupIn = MethodHandles.class.getMethod("privateLookupIn", Class.class, MethodHandles.Lookup.class);

      MethodHandles.Lookup lookup = (MethodHandles.Lookup) privateLookupIn.invoke(MethodHandles.class,
              method.getDeclaringClass(), MethodHandles.lookup());

			return lookup.findSpecial(method.getDeclaringClass(), method.getName(),
              MethodType.methodType(method.getReturnType(), method.getParameterTypes()), method.getDeclaringClass());
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static boolean isGetter(Method method, Property<?> property) {
    String beanProperty = property.getBeanProperty();
    if (beanProperty == null || method.getParameterCount() > 0) {
      return false;
    }

    String beanPropertyCamelCase = beanProperty.substring(0, 1).toUpperCase() + beanProperty.substring(1);
    String methodName = method.getName();
    Class<?> typeClass = getAttributeTypeClass(property.getAttribute());
    Class<?> methodReturnType = getMethodReturnType(method);

    return (methodReturnType.equals(typeClass) || method.getReturnType().equals(Optional.class))
            && (methodName.equals(beanProperty) || methodName.equals("get" + beanPropertyCamelCase) ||
            (methodName.equals("is" + beanPropertyCamelCase) && Boolean.class.equals(typeClass)));
  }

  private static Class<?> getMethodReturnType(Method method) {
    Class<?> returnType = method.getReturnType();
    if (returnType.isPrimitive()) {
      return Util.getPrimitiveBoxedType(returnType);
    }

    return returnType;
  }

  private static boolean isSetter(Method method, Property<?> property) {
    String beanProperty = property.getBeanProperty();
    if (beanProperty == null || method.getParameterCount() != 1) {
      return false;
    }

    String beanPropertyCamelCase = beanProperty.substring(0, 1).toUpperCase() + beanProperty.substring(1);
    String methodName = method.getName();
    Class<?> parameterType = getSetterParameterType(method);
    Class<?> attributeTypeClass = getAttributeTypeClass(property.getAttribute());

    return parameterType.equals(attributeTypeClass) && (methodName.equals(beanProperty) || methodName.equals("set" + beanPropertyCamelCase));
  }

  private static Class<?> getSetterParameterType(Method method) {
    Class<?> parameterType = method.getParameterTypes()[0];
    if (parameterType.isPrimitive()) {
      return Util.getPrimitiveBoxedType(parameterType);
    }

    return parameterType;
  }

  private static Class<?> getAttributeTypeClass(Attribute<?> attribute) {
    Class<?> typeClass = attribute.getTypeClass();
    if (attribute instanceof ForeignKey) {
      typeClass = ((ForeignKey) attribute).getReferencedEntityType().getEntityClass();
    }

    return typeClass;
  }

  private static boolean isWritable(ColumnProperty<?> property, boolean includePrimaryKeyProperties,
                                    boolean includeNonUpdatable) {
    return property.isInsertable() && (includeNonUpdatable || property.isUpdatable())
            && (includePrimaryKeyProperties || !property.isPrimaryKeyColumn());
  }

  private static final class EntityProperties implements Serializable {

    private static final long serialVersionUID = 1;

    private final EntityType entityType;

    private final Map<String, Attribute<?>> attributeMap;
    private final Map<Attribute<?>, Property<?>> propertyMap;
    private final List<Property<?>> properties;
    private final List<ColumnProperty<?>> columnProperties;
    private final List<ColumnProperty<?>> lazyLoadedBlobProperties;
    private final List<Attribute<?>> primaryKeyAttribues;
    private final List<ColumnProperty<?>> primaryKeyProperties;
    private final Map<Attribute<?>, ColumnProperty<?>> primaryKeyPropertyMap;
    private final List<ForeignKeyProperty> foreignKeyProperties;
    private final Map<ForeignKey, ForeignKeyProperty> foreignKeyPropertyMap;
    private final Map<Attribute<?>, List<ForeignKeyProperty>> columnPropertyForeignKeyProperties;
    private final Set<Attribute<?>> foreignKeyColumnAttributes = new HashSet<>();
    private final Map<Attribute<?>, Set<Attribute<?>>> derivedAttributes;
    private final List<TransientProperty<?>> transientProperties;
    private final Map<Attribute<Entity>, List<DenormalizedProperty<?>>> denormalizedProperties;
    private final List<Attribute<?>> defaultSelectAttributes;

    private final int serializationVersion;

    private EntityProperties(EntityType entityType, List<Property.Builder<?, ?>> propertyBuilders) {
      this.entityType = entityType;
      this.propertyMap = initializePropertyMap(propertyBuilders);
      this.attributeMap = initializeAttributeMap(propertyMap);
      this.properties = unmodifiableList(new ArrayList<>(propertyMap.values()));
      this.columnProperties = unmodifiableList(getColumnProperties());
      this.lazyLoadedBlobProperties = initializeLazyLoadedByteArrayProperties();
      this.primaryKeyProperties = unmodifiableList(getPrimaryKeyProperties());
      this.primaryKeyAttribues = unmodifiableList(getPrimaryKeyAttributes());
      this.primaryKeyPropertyMap = initializePrimaryKeyPropertyMap();
      this.foreignKeyProperties = unmodifiableList(getForeignKeyProperties());
      this.foreignKeyPropertyMap = initializeForeignKeyPropertyMap();
      this.columnPropertyForeignKeyProperties = initializeColumnPropertyForeignKeyProperties();
      this.derivedAttributes = initializeDerivedAttributes();
      this.transientProperties = unmodifiableList(getTransientProperties());
      this.denormalizedProperties = unmodifiableMap(getDenormalizedProperties());
      this.defaultSelectAttributes = unmodifiableList(getDefaultSelectAttributes());
      this.serializationVersion = createSerializationVersion();
    }

    private Map<Attribute<?>, Property<?>> initializePropertyMap(List<Property.Builder<?, ?>> builders) {
      Map<Attribute<?>, Property<?>> map = new HashMap<>(builders.size());
      for (Property.Builder<?, ?> builder : builders) {
        if (!(builder instanceof ForeignKeyProperty.Builder)) {
          validateAndAddProperty(builder.build(), map, entityType);
        }
      }
      validatePrimaryKeyProperties(map, entityType);

      initializeForeignKeyColumnProperties(builders.stream()
              .filter(ForeignKeyProperty.Builder.class::isInstance)
              .map(ForeignKeyProperty.Builder.class::cast)
              .collect(toList()), map);
      for (Property.Builder<?, ?> builder : builders) {
        if (builder instanceof ForeignKeyProperty.Builder) {
          validateAndAddProperty(builder.build(), map, entityType);
        }
      }
      Map<Attribute<?>, Property<?>> ordereredMap = new LinkedHashMap<>(builders.size());
      //retain the original attribute order
      for (Property.Builder<?, ?> builder : builders) {
        ordereredMap.put(builder.getAttribute(), map.get(builder.getAttribute()));
      }

      return unmodifiableMap(ordereredMap);
    }

    private Map<Attribute<?>, List<ForeignKeyProperty>> initializeColumnPropertyForeignKeyProperties() {
      Map<Attribute<?>, List<ForeignKeyProperty>> foreignKeyMap = new HashMap<>();
      foreignKeyProperties.forEach(foreignKeyProperty ->
              foreignKeyProperty.getReferences().forEach(reference ->
                      foreignKeyMap.computeIfAbsent(reference.getAttribute(),
                              columnAttribute -> new ArrayList<>()).add(foreignKeyProperty)));

      return foreignKeyMap;
    }

    private void initializeForeignKeyColumnProperties(List<ForeignKeyProperty.Builder> foreignKeyBuilders,
                                                      Map<Attribute<?>, Property<?>> propertyMap) {
      Map<ForeignKey, List<ColumnProperty<?>>> foreignKeyColumnProperties = foreignKeyBuilders.stream()
              .map(ForeignKeyProperty.Builder::getAttribute)
              .map(ForeignKey.class::cast)
              .collect(toMap(foreignKey -> foreignKey, foreignKey -> getForeignKeyColumnProperties(foreignKey, propertyMap)));
      foreignKeyColumnAttributes.addAll(foreignKeyColumnProperties.values().stream()
              .flatMap(columnProperties -> columnProperties.stream().map(Property::getAttribute))
              .collect(toSet()));
      foreignKeyBuilders.forEach(foreignKeyBuilder -> setForeignKeyNullable(foreignKeyBuilder, foreignKeyColumnProperties));
    }

    private Map<Attribute<?>, ColumnProperty<?>> initializePrimaryKeyPropertyMap() {
      return unmodifiableMap(primaryKeyProperties.stream()
              .collect(toMap(Property::getAttribute, property -> property)));
    }

    private List<ForeignKeyProperty> getForeignKeyProperties() {
      return properties.stream()
              .filter(ForeignKeyProperty.class::isInstance)
              .map(ForeignKeyProperty.class::cast)
              .collect(toList());
    }

    private List<ColumnProperty<?>> getColumnProperties() {
      return properties.stream()
              .filter(ColumnProperty.class::isInstance)
              .map(property -> (ColumnProperty<?>) property)
              .collect(toList());
    }

    private List<TransientProperty<?>> getTransientProperties() {
      return properties.stream()
              .filter(TransientProperty.class::isInstance)
              .map(property -> (TransientProperty<?>) property)
              .collect(toList());
    }

    private List<ColumnProperty<?>> initializeLazyLoadedByteArrayProperties() {
      return columnProperties.stream()
              .filter(property -> property.getAttribute().isByteArray())
              .filter(property -> !(property instanceof BlobProperty) || !((BlobProperty) property).isEagerlyLoaded())
              .collect(toList());
    }

    private Map<Attribute<Entity>, List<DenormalizedProperty<?>>> getDenormalizedProperties() {
      Map<Attribute<Entity>, List<DenormalizedProperty<?>>> denormalizedPropertyMap = new HashMap<>(properties.size());
      properties.stream()
              .filter(DenormalizedProperty.class::isInstance)
              .map(DenormalizedProperty.class::cast)
              .forEach(denormalizedProperty ->
                      denormalizedPropertyMap.computeIfAbsent(denormalizedProperty.getEntityAttribute(), attribute ->
                              new ArrayList<>()).add(denormalizedProperty));

      return denormalizedPropertyMap;
    }

    private List<Attribute<?>> getDefaultSelectAttributes() {
      List<Attribute<?>> selectableAttributes = columnProperties.stream()
              .filter(ColumnProperty::isSelectable)
              .filter(property -> !lazyLoadedBlobProperties.contains(property))
              .map(Property::getAttribute)
              .collect(toList());
      selectableAttributes.addAll(foreignKeyProperties.stream()
              .map(ForeignKeyProperty::getAttribute)
              .collect(toList()));

      return selectableAttributes;
    }

    private Map<Attribute<?>, Set<Attribute<?>>> initializeDerivedAttributes() {
      Map<Attribute<?>, Set<Attribute<?>>> derivedPropertyMap = new HashMap<>();
      properties.stream()
              .filter(DerivedProperty.class::isInstance)
              .map(DerivedProperty.class::cast)
              .forEach(derivedProperty -> {
                List<Attribute<?>> sourceAttributes = derivedProperty.getSourceAttributes();
                for (Attribute<?> sourceAttribute : sourceAttributes) {
                  derivedPropertyMap.computeIfAbsent(sourceAttribute, attribute -> new HashSet<>()).add(derivedProperty.getAttribute());
                }
              });

      return derivedPropertyMap;
    }

    private List<ColumnProperty<?>> getPrimaryKeyProperties() {
      return properties.stream()
              .filter(ColumnProperty.class::isInstance)
              .map(property -> (ColumnProperty<?>) property)
              .filter(ColumnProperty::isPrimaryKeyColumn)
              .sorted(comparingInt(ColumnProperty::getPrimaryKeyIndex))
              .collect(toList());
    }

    private List<Attribute<?>> getPrimaryKeyAttributes() {
      return primaryKeyProperties.stream()
              .map(Property::getAttribute)
              .collect(toList());
    }

    private int createSerializationVersion() {
      return propertyMap.values().stream()
              .filter(property -> !(property instanceof DerivedProperty))
              .map(Property::getAttribute)
              .map(attribute -> attribute.getName() + attribute.getTypeClass().getName())
              .collect(Collectors.joining())
              .hashCode();
    }

    private static Map<String, Attribute<?>> initializeAttributeMap(Map<Attribute<?>, Property<?>> properties) {
      Map<String, Attribute<?>> map = new HashMap<>();
      properties.values().forEach(property -> map.put(property.getAttribute().getName(), property.getAttribute()));

      return map;
    }

    private static void validateAndAddProperty(Property<?> property, Map<Attribute<?>, Property<?>> properties, EntityType entityType) {
      validate(property, properties, entityType);
      properties.put(property.getAttribute(), property);
    }

    private static void validatePrimaryKeyProperties(Map<Attribute<?>, Property<?>> properties, EntityType entityType) {
      Set<Integer> usedPrimaryKeyIndexes = new LinkedHashSet<>();
      for (Property<?> property : properties.values()) {
        if (property instanceof ColumnProperty && ((ColumnProperty<?>) property).isPrimaryKeyColumn()) {
          Integer index = ((ColumnProperty<?>) property).getPrimaryKeyIndex();
          if (usedPrimaryKeyIndexes.contains(index)) {
            throw new IllegalArgumentException("Primary key index " + index + " in property " + property + " has already been used");
          }
          usedPrimaryKeyIndexes.add(index);
        }
      }
      usedPrimaryKeyIndexes.stream()
              .min(Integer::compareTo)
              .ifPresent(minPrimaryKeyIndex -> {
                if (minPrimaryKeyIndex != 0) {
                  throw new IllegalArgumentException("Minimum primary key index is "
                          + minPrimaryKeyIndex + " for entity " + entityType + ", when it should be 0");
                }
              });
      usedPrimaryKeyIndexes.stream()
              .max(Integer::compareTo)
              .ifPresent(maxPrimaryKeyIndex -> {
                if (usedPrimaryKeyIndexes.size() != maxPrimaryKeyIndex + 1) {
                  throw new IllegalArgumentException("Expecting " + (maxPrimaryKeyIndex + 1)
                          + " primary key properties for entity " + entityType + ", but found only "
                          + usedPrimaryKeyIndexes.size() + " distinct primary key indexes " + usedPrimaryKeyIndexes + "");
                }
              });
    }

    private static void validate(Property<?> property, Map<Attribute<?>, Property<?>> properties, EntityType entityType) {
      if (!entityType.equals(property.getEntityType())) {
        throw new IllegalArgumentException("Attribute entityType (" +
                property.getEntityType() + ") in property " + property.getAttribute() +
                " does not match the definition entityType: " + entityType);
      }
      if (properties.containsKey(property.getAttribute())) {
        throw new IllegalArgumentException("Property " + property.getAttribute()
                + (property.getCaption() != null ? " (" + property.getCaption() + ")" : "")
                + " has already been defined as: " + properties.get(property.getAttribute()) + " in entity: " + entityType);
      }
    }

    private Map<ForeignKey, ForeignKeyProperty> initializeForeignKeyPropertyMap() {
      Map<ForeignKey, ForeignKeyProperty> foreignKeyMap = new LinkedHashMap<>(foreignKeyProperties.size());
      foreignKeyProperties.forEach(foreignKeyProperty ->
              foreignKeyMap.put(foreignKeyProperty.getAttribute(), foreignKeyProperty));

      return unmodifiableMap(foreignKeyMap);
    }

    private static List<ColumnProperty<?>> getForeignKeyColumnProperties(ForeignKey foreignKey, Map<Attribute<?>, Property<?>> propertyMap) {
      return foreignKey.getReferences().stream()
              .map(reference -> getForeignKeyColumnProperty(reference, propertyMap))
              .collect(toList());
    }

    private static ColumnProperty<?> getForeignKeyColumnProperty(ForeignKey.Reference<?> reference, Map<Attribute<?>, Property<?>> propertyMap) {
      ColumnProperty<?> columnProperty = (ColumnProperty<?>) propertyMap.get(reference.getAttribute());
      if (columnProperty == null) {
        throw new IllegalArgumentException("ColumnProperty based on attribute: " + reference.getAttribute()
                + " not found when initializing foreign key");
      }

      return columnProperty;
    }

    private static void setForeignKeyNullable(ForeignKeyProperty.Builder foreignKeyBuilder,
                                              Map<ForeignKey, List<ColumnProperty<?>>> foreignKeyColumnProperties) {
      //make foreign key properties nullable if and only if any of their constituent column properties are nullable
      foreignKeyBuilder.nullable(foreignKeyColumnProperties.get(foreignKeyBuilder.getAttribute())
              .stream()
              .anyMatch(Property::isNullable));
    }
  }

  static final class DefaultBuilder implements Builder {

    private final DefaultEntityDefinition definition;

    private DefaultBuilder(DefaultEntityDefinition definition) {
      this.definition = definition;
    }

    @Override
    public DefaultEntityDefinition get() {
      return definition;
    }

    @Override
    public Builder conditionProvider(ConditionType conditionType, ConditionProvider conditionProvider) {
      requireNonNull(conditionType, "conditionType");
      requireNonNull(conditionProvider, "conditionProvider");
      if (definition.conditionProviders == null) {
        definition.conditionProviders = new HashMap<>();
      }
      if (definition.conditionProviders.containsKey(conditionType)) {
        throw new IllegalStateException("ConditionProvider for condition type  " + conditionType + " has already been added");
      }
      definition.conditionProviders.put(conditionType, conditionProvider);
      return this;
    }

    @Override
    public Builder caption(String caption) {
      definition.caption = requireNonNull(caption, "caption");
      return this;
    }

    @Override
    public Builder captionResourceKey(String captionResourceKey) {
      if (definition.caption != null) {
        throw new IllegalStateException("Caption has already been set for entity: " + definition.entityType);
      }
      definition.captionResourceKey = requireNonNull(captionResourceKey, "captionResourceKey");
      return this;
    }

    @Override
    public Builder smallDataset() {
      definition.smallDataset = true;
      return this;
    }

    @Override
    public Builder staticData() {
      definition.staticData = true;
      return this;
    }

    @Override
    public Builder readOnly() {
      definition.readOnly = true;
      return this;
    }

    @Override
    public Builder keyGenerator(KeyGenerator keyGenerator) {
      if (!definition.hasPrimaryKey()) {
        throw new IllegalStateException("KeyGenerator can not be set for an entity without a primary key");
      }
      definition.keyGenerator = requireNonNull(keyGenerator, "keyGenerator");
      definition.keyGenerated = true;
      return this;
    }

    @Override
    public Builder orderBy(OrderBy orderBy) {
      requireNonNull(orderBy, "orderBy");
      if (definition.orderBy != null) {
        throw new IllegalStateException("Order by has already been set: " + definition.orderBy);
      }
      definition.orderBy = orderBy;
      return this;
    }

    @Override
    public Builder groupByClause(String groupByClause) {
      requireNonNull(groupByClause, "groupByClause");
      if (definition.groupByClause != null) {
        throw new IllegalStateException("Group by clause has already been set: " + definition.groupByClause);
      }
      definition.groupByClause = groupByClause;
      return this;
    }

    @Override
    public Builder havingClause(String havingClause) {
      requireNonNull(havingClause, "havingClause");
      if (definition.havingClause != null) {
        throw new IllegalStateException("Having clause has already been set: " + definition.havingClause);
      }
      definition.havingClause = havingClause;
      return this;
    }

    @Override
    public Builder selectTableName(String selectTableName) {
      definition.selectTableName = requireNonNull(selectTableName, "selectTableName");
      return this;
    }

    @Override
    public Builder selectQuery(SelectQuery selectQuery) {
      definition.selectQuery = requireNonNull(selectQuery, "selectQuery");
      return this;
    }

    @Override
    public Builder comparator(Comparator<Entity> comparator) {
      definition.comparator = requireNonNull(comparator, "comparator");
      return this;
    }

    @Override
    public Builder stringFactory(StringFactory.Builder builder) {
      return stringFactory(requireNonNull(builder, "builder").get());
    }

    @Override
    public Builder stringFactory(Function<Entity, String> stringFactory) {
      definition.stringFactory = requireNonNull(stringFactory, "stringFactory");
      return this;
    }

    @Override
    public Builder backgroundColorProvider(ColorProvider backgroundColorProvider) {
      definition.backgroundColorProvider = requireNonNull(backgroundColorProvider, "backgroundColorProvider");
      return this;
    }

    @Override
    public Builder foregroundColorProvider(ColorProvider foregroundColorProvider) {
      definition.foregroundColorProvider = requireNonNull(foregroundColorProvider, "foregroundColorProvider");
      return this;
    }

    @Override
    public Builder validator(EntityValidator validator) {
      definition.validator = requireNonNull(validator, "validator");
      return this;
    }
  }
}
