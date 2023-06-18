/*
 * Copyright (c) 2009 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.common.Primitives;
import is.codion.common.Text;
import is.codion.framework.domain.entity.query.SelectQuery;
import is.codion.framework.domain.property.BlobProperty;
import is.codion.framework.domain.property.ColumnProperty;
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

import static is.codion.common.NullOrEmpty.nullOrEmpty;
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
  private final String caption;

  /**
   * The resource bundle key specifying the caption
   */
  private final String captionResourceKey;

  /**
   * The caption from the resource bundle, if any
   */
  private transient String resourceCaption;

  /**
   * The entity description, if any
   */
  private final String description;

  /**
   * Holds the order by clause
   */
  private final OrderBy orderBy;

  /**
   * If true then it should not be possible to insert, update or delete entities of this type
   */
  private final boolean readOnly;

  /**
   * A somewhat subjective indicator, useful in deciding if all entities of this type
   * would fit in, say, a combo box
   */
  private final boolean smallDataset;

  /**
   * Another somewhat subjective indicator, indicating if the data in the underlying table can be regarded as static,
   * this is useful in deciding how often to refresh, say, a combo box based on the entity
   */
  private final boolean staticData;

  /**
   * True if a key generator has been set for this entity type
   */
  private final boolean keyGenerated;

  /**
   * True if optimistic locking should be used during updates
   */
  private final transient boolean optimisticLockingEnabled;

  /**
   * The {@link Function} to use when toString() is called for this entity type
   */
  private final Function<Entity, String> stringFactory;

  /**
   * Provides the background color
   */
  private final ColorProvider backgroundColorProvider;

  /**
   * Provides the color
   */
  private final ColorProvider foregroundColorProvider;

  /**
   * The comparator
   */
  private final Comparator<Entity> comparator;

  /**
   * The validator
   */
  private final EntityValidator validator;

  /**
   * The name of the underlying table
   */
  private final transient String tableName;

  /**
   * The table (view, query) from which to select the entity
   * Used if it differs from the one used for inserts, updates and deletes
   */
  private final transient String selectTableName;

  /**
   * Holds the group by clause
   */
  private final transient String groupByClause;

  /**
   * The primary key value generator
   */
  private final transient KeyGenerator keyGenerator;

  /**
   * Provides a custom sql query used when selecting entities of this type
   */
  private final transient SelectQuery selectQuery;

  /**
   * The {@link ConditionProvider}s mapped to their respective conditionIds
   */
  private final transient Map<ConditionType, ConditionProvider> conditionProviders;

  /**
   * Maps the definition of a referenced entity to its foreign key attribute.
   */
  private final Map<Attribute<?>, EntityDefinition> foreignEntityDefinitions = new HashMap<>();

  /**
   * The properties associated with this entity.
   */
  private final EntityProperties entityProperties;

  private DefaultEntityDefinition(DefaultBuilder builder) {
    this.domainName = builder.properties.entityType.domainName();
    this.entityType = builder.properties.entityType;
    this.caption = builder.caption;
    this.captionResourceKey = builder.captionResourceKey;
    this.description = builder.description;
    this.orderBy = builder.orderBy;
    this.readOnly = builder.readOnly;
    this.smallDataset = builder.smallDataset;
    this.staticData = builder.staticData;
    this.keyGenerator = builder.keyGenerator;
    this.keyGenerated = builder.keyGenerated;
    this.optimisticLockingEnabled = builder.optimisticLockingEnabled;
    this.stringFactory = builder.stringFactory;
    this.backgroundColorProvider = builder.backgroundColorProvider;
    this.foregroundColorProvider = builder.foregroundColorProvider;
    this.comparator = builder.comparator;
    this.validator = builder.validator;
    this.tableName = builder.tableName;
    this.selectTableName = builder.selectTableName;
    this.selectQuery = builder.selectQuery;
    this.conditionProviders = builder.conditionProviders == null ? null : new HashMap<>(builder.conditionProviders);
    this.entityProperties = builder.properties;
    this.groupByClause = createGroupByClause();
    resolveEntityClassMethods();
  }

  @Override
  public EntityType type() {
    return entityType;
  }

  @Override
  public Attribute<?> getterAttribute(Method method) {
    return getters.get(requireNonNull(method, METHOD).getName());
  }

  @Override
  public Attribute<?> setterAttribute(Method method) {
    return setters.get(requireNonNull(method, METHOD).getName());
  }

  @Override
  public MethodHandle defaultMethodHandle(Method method) {
    return defaultMethodHandles.computeIfAbsent(requireNonNull(method, METHOD).getName(),
            methodName -> createDefaultMethodHandle(method));
  }

  @Override
  public int serializationVersion() {
    return entityProperties.serializationVersion;
  }

  @Override
  public String tableName() {
    return tableName;
  }

  @Override
  public ConditionProvider conditionProvider(ConditionType conditionType) {
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
  public String domainName() {
    return domainName;
  }

  @Override
  public String caption() {
    if (entityType.resourceBundleName() != null) {
      if (resourceCaption == null) {
        ResourceBundle bundle = ResourceBundle.getBundle(entityType.resourceBundleName());
        resourceCaption = bundle.containsKey(captionResourceKey) ? bundle.getString(captionResourceKey) : "";
      }

      if (!resourceCaption.isEmpty()) {
        return resourceCaption;
      }
    }

    return caption == null ? entityType.name() : caption;
  }

  @Override
  public String description() {
    return description;
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
  public boolean isOptimisticLockingEnabled() {
    return optimisticLockingEnabled;
  }

  @Override
  public KeyGenerator keyGenerator() {
    return keyGenerator;
  }

  @Override
  public boolean isKeyGenerated() {
    return keyGenerated;
  }

  @Override
  public OrderBy orderBy() {
    return orderBy;
  }

  @Override
  public String groupByClause() {
    return groupByClause;
  }

  @Override
  public String selectTableName() {
    return selectTableName == null ? tableName : selectTableName;
  }

  @Override
  public SelectQuery selectQuery() {
    return selectQuery;
  }

  @Override
  public Function<Entity, String> stringFactory() {
    return stringFactory;
  }

  @Override
  public Comparator<Entity> comparator() {
    return comparator;
  }

  @Override
  public boolean containsAttribute(Attribute<?> attribute) {
    return entityProperties.propertyMap.containsKey(requireNonNull(attribute));
  }

  @Override
  public <T> Attribute<T> attribute(String attributeName) {
    return (Attribute<T>) entityProperties.attributeMap.get(requireNonNull(attributeName));
  }

  @Override
  public Collection<Attribute<String>> searchAttributes() {
    return entityProperties.columnProperties.stream()
            .filter(ColumnProperty::isSearchProperty)
            .map(property -> ((ColumnProperty<String>) property).attribute())
            .collect(toList());
  }

  @Override
  public Collection<Attribute<?>> defaultSelectAttributes() {
    return entityProperties.defaultSelectAttributes;
  }

  @Override
  public <T> ColumnProperty<T> columnProperty(Attribute<T> attribute) {
    Property<T> property = property(attribute);
    if (!(property instanceof ColumnProperty)) {
      throw new IllegalArgumentException("Property based on " + attribute + " is not a ColumnProperty");
    }

    return (ColumnProperty<T>) property;
  }

  @Override
  public <T> Property<T> property(Attribute<T> attribute) {
    Property<T> property = (Property<T>) entityProperties.propertyMap.get(requireNonNull(attribute, ATTRIBUTE));
    if (property == null) {
      throw new IllegalArgumentException("Attribute " + attribute + " not found in entity: " + entityType);
    }

    return property;
  }

  @Override
  public <T> ColumnProperty<T> primaryKeyProperty(Attribute<T> attribute) {
    ColumnProperty<T> property = (ColumnProperty<T>) entityProperties.primaryKeyPropertyMap.get(requireNonNull(attribute, ATTRIBUTE));
    if (property == null) {
      throw new IllegalArgumentException("Primary key property based on " + attribute + " not found in entity: " + entityType);
    }

    return property;
  }

  @Override
  public Collection<Property<?>> properties(Collection<Attribute<?>> attributes) {
    return requireNonNull(attributes, ATTRIBUTES).stream()
            .map(this::property)
            .collect(toList());
  }

  @Override
  public List<ColumnProperty<?>> columnProperties(List<Attribute<?>> attributes) {
    List<ColumnProperty<?>> propertyList = new ArrayList<>(requireNonNull(attributes, ATTRIBUTES).size());
    for (int i = 0; i < attributes.size(); i++) {
      propertyList.add(columnProperty(attributes.get(i)));
    }

    return propertyList;
  }

  @Override
  public List<ColumnProperty<?>> writableColumnProperties(boolean includePrimaryKeyProperties,
                                                          boolean includeNonUpdatable) {
    return entityProperties.columnProperties.stream()
            .filter(property -> isWritable(property, includePrimaryKeyProperties, includeNonUpdatable))
            .collect(toList());
  }

  @Override
  public Collection<Property<?>> updatableProperties() {
    List<ColumnProperty<?>> writableColumnProperties = writableColumnProperties(!isKeyGenerated(), false);
    writableColumnProperties.removeIf(property -> isForeignKeyAttribute(property.attribute()));
    List<Property<?>> updatable = new ArrayList<>(writableColumnProperties);
    for (ForeignKeyProperty foreignKeyProperty : entityProperties.foreignKeyProperties) {
      if (isUpdatable(foreignKeyProperty.attribute())) {
        updatable.add(foreignKeyProperty);
      }
    }

    return updatable;
  }

  @Override
  public boolean isUpdatable(ForeignKey foreignKey) {
    return requireNonNull(foreignKey, FOREIGN_KEY).references().stream()
            .map(reference -> columnProperty(reference.attribute()))
            .allMatch(ColumnProperty::isUpdatable);
  }

  @Override
  public boolean isForeignKeyAttribute(Attribute<?> attribute) {
    return entityProperties.foreignKeyColumnAttributes.contains(requireNonNull(attribute, ATTRIBUTE));
  }

  @Override
  public Collection<ForeignKey> foreignKeys(EntityType referencedEntityType) {
    requireNonNull(referencedEntityType, "referencedEntityType");
    return foreignKeys().stream()
            .filter(foreignKey -> foreignKey.referencedType().equals(referencedEntityType))
            .collect(toList());
  }

  @Override
  public ForeignKeyProperty foreignKeyProperty(ForeignKey foreignKey) {
    ForeignKeyProperty property = entityProperties.foreignKeyPropertyMap.get(requireNonNull(foreignKey, FOREIGN_KEY));
    if (property == null) {
      throw new IllegalArgumentException("Foreign key: " + foreignKey + " not found in entity of type: " + entityType);
    }

    return property;
  }

  @Override
  public <T> Collection<ForeignKeyProperty> foreignKeyProperties(Attribute<T> columnAttribute) {
    return entityProperties.columnPropertyForeignKeyProperties
            .getOrDefault(requireNonNull(columnAttribute, "columnAttribute"), emptyList());
  }

  @Override
  public List<Property<?>> properties() {
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
    return entityProperties.derivedAttributes.containsKey(requireNonNull(attribute, ATTRIBUTE));
  }

  @Override
  public <T> Collection<Attribute<?>> derivedAttributes(Attribute<T> attribute) {
    return entityProperties.derivedAttributes.getOrDefault(requireNonNull(attribute, ATTRIBUTE), emptySet());
  }

  @Override
  public List<Attribute<?>> primaryKeyAttributes() {
    return entityProperties.primaryKeyAttribues;
  }

  @Override
  public List<ColumnProperty<?>> primaryKeyProperties() {
    return entityProperties.primaryKeyProperties;
  }

  @Override
  public List<Property<?>> visibleProperties() {
    return entityProperties.properties.stream()
            .filter(property -> !property.isHidden())
            .collect(toList());
  }

  @Override
  public List<ColumnProperty<?>> columnProperties() {
    return entityProperties.columnProperties;
  }

  @Override
  public List<ColumnProperty<?>> lazyLoadedBlobProperties() {
    return entityProperties.lazyLoadedBlobProperties;
  }

  @Override
  public List<TransientProperty<?>> transientProperties() {
    return entityProperties.transientProperties;
  }

  @Override
  public List<ForeignKeyProperty> foreignKeyProperties() {
    return entityProperties.foreignKeyProperties;
  }

  @Override
  public Collection<ForeignKey> foreignKeys() {
    return entityProperties.foreignKeyPropertyMap.keySet();
  }

  @Override
  public EntityDefinition referencedDefinition(ForeignKey foreignKey) {
    EntityDefinition definition = foreignEntityDefinitions.get(requireNonNull(foreignKey, FOREIGN_KEY));
    if (definition == null) {
      throw new IllegalArgumentException("Referenced entity definition not found for foreign key: " + foreignKey);
    }

    return definition;
  }

  @Override
  public String toString() {
    return entityType.name();
  }

  @Override
  public EntityValidator validator() {
    return validator;
  }

  @Override
  public ColorProvider backgroundColorProvider() {
    return backgroundColorProvider;
  }

  @Override
  public ColorProvider foregroundColorProvider() {
    return foregroundColorProvider;
  }

  @Override
  public Entity entity() {
    return entity(null);
  }

  @Override
  public Entity entity(Map<Attribute<?>, Object> values) {
    return entity(values, null);
  }

  @Override
  public Entity entity(Map<Attribute<?>, Object> values, Map<Attribute<?>, Object> originalValues) {
    return new DefaultEntity(this, values, originalValues);
  }

  @Override
  public <T> Key primaryKey(T value) {
    if (!hasPrimaryKey()) {
      throw new IllegalArgumentException("Entity '" + entityType + "' has no primary key");
    }
    if (primaryKeyAttributes().size() > 1) {
      throw new IllegalStateException(entityType + " has a composite primary key");
    }
    Attribute<T> attribute = (Attribute<T>) primaryKeyAttributes().get(0);
    attribute.validateType(value);

    return new DefaultKey(this, attribute, value, true);
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
    ForeignKeyProperty foreignKeyProperty = foreignKeyProperty(foreignKey);
    if (foreignEntityDefinitions.containsKey(foreignKey)) {
      throw new IllegalStateException("Foreign definition has already been set for " + foreignKey);
    }
    if (!foreignKeyProperty.referencedType().equals(definition.type())) {
      throw new IllegalArgumentException("Definition for entity " + foreignKeyProperty.referencedType() +
              " expected for " + foreignKey);
    }
    foreignEntityDefinitions.put(foreignKey, definition);
  }

  private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
    stream.defaultReadObject();
    defaultMethodHandles = new ConcurrentHashMap<>();
  }

  /**
   * @return a group by clause based on grouping column properties, null if no grouping properties are defined
   */
  private String createGroupByClause() {
    List<String> groupingColumnNames = entityProperties.columnProperties.stream()
            .filter(ColumnProperty::isGroupingColumn)
            .map(ColumnProperty::columnExpression)
            .collect(toList());
    if (groupingColumnNames.isEmpty()) {
      return null;
    }

    return String.join(", ", groupingColumnNames);
  }

  private void resolveEntityClassMethods() {
    if (!entityType.entityClass().equals(Entity.class)) {
      for (Method method : entityType.entityClass().getDeclaredMethods()) {
        if (method.isDefault()) {
          defaultMethodHandles.put(method.getName(), createDefaultMethodHandle(method));
        }
        else {
          properties().stream()
                  .filter(property -> isGetter(method, property))
                  .findFirst()
                  .ifPresent(property -> getters.put(method.getName(), property.attribute()));
          properties().stream()
                  .filter(property -> isSetter(method, property))
                  .findFirst()
                  .ifPresent(property -> setters.put(method.getName(), property.attribute()));
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
    String beanProperty = property.beanProperty();
    if (beanProperty == null || method.getParameterCount() > 0) {
      return false;
    }

    String beanPropertyCamelCase = beanProperty.substring(0, 1).toUpperCase() + beanProperty.substring(1);
    String methodName = method.getName();
    Class<?> attributeValueClass = attributeValueClass(property.attribute());
    Class<?> methodReturnType = methodReturnType(method);

    return returnsAttributeValueClassOrOptional(methodReturnType, attributeValueClass) &&
            (isBeanOrPropertyGetter(methodName, beanProperty, beanPropertyCamelCase) ||
                    isBooleanGetter(methodName, beanPropertyCamelCase, attributeValueClass));
  }

  private static boolean returnsAttributeValueClassOrOptional(Class<?> methodReturnType, Class<?> attributeValueClass) {
    return methodReturnType.equals(attributeValueClass) || methodReturnType.equals(Optional.class);
  }

  private static boolean isBeanOrPropertyGetter(String methodName, String beanProperty, String beanPropertyCamelCase) {
    return methodName.equals(beanProperty) || methodName.equals("get" + beanPropertyCamelCase);
  }

  private static boolean isBooleanGetter(String methodName, String beanPropertyCamelCase, Class<?> attributeValueClass) {
    return methodName.equals("is" + beanPropertyCamelCase) && Boolean.class.equals(attributeValueClass);
  }

  private static Class<?> methodReturnType(Method method) {
    Class<?> returnType = method.getReturnType();
    if (returnType.isPrimitive()) {
      return Primitives.boxedType(returnType);
    }

    return returnType;
  }

  private static boolean isSetter(Method method, Property<?> property) {
    String beanProperty = property.beanProperty();
    if (beanProperty == null || method.getParameterCount() != 1 || method.isVarArgs()) {
      return false;
    }

    String beanPropertyCamelCase = beanProperty.substring(0, 1).toUpperCase() + beanProperty.substring(1);
    String methodName = method.getName();
    Class<?> parameterType = setterParameterType(method);
    Class<?> attributeValueClass = attributeValueClass(property.attribute());

    return parameterType.equals(attributeValueClass) && (methodName.equals(beanProperty) || methodName.equals("set" + beanPropertyCamelCase));
  }

  private static Class<?> setterParameterType(Method method) {
    Class<?> parameterType = method.getParameterTypes()[0];
    if (parameterType.isPrimitive()) {
      return Primitives.boxedType(parameterType);
    }

    return parameterType;
  }

  private static Class<?> attributeValueClass(Attribute<?> attribute) {
    Class<?> valueClass = attribute.valueClass();
    if (attribute instanceof ForeignKey) {
      valueClass = ((ForeignKey) attribute).referencedType().entityClass();
    }

    return valueClass;
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
    private final Map<Attribute<?>, Collection<ForeignKeyProperty>> columnPropertyForeignKeyProperties;
    private final Set<Attribute<?>> foreignKeyColumnAttributes = new HashSet<>();
    private final Map<Attribute<?>, Set<Attribute<?>>> derivedAttributes;
    private final List<TransientProperty<?>> transientProperties;
    private final List<Attribute<?>> defaultSelectAttributes;

    private final int serializationVersion;

    private EntityProperties(List<Property.Builder<?, ?>> propertyBuilders) {
      if (requireNonNull(propertyBuilders, "propertyBuilders").isEmpty()) {
        throw new IllegalArgumentException("One of more properties must be specified for an entity");
      }
      this.entityType = propertyBuilders.get(0).attribute().entityType();
      this.propertyMap = unmodifiableMap(propertyMap(propertyBuilders));
      this.attributeMap = unmodifiableMap(attributeMap(propertyMap));
      this.properties = unmodifiableList(new ArrayList<>(propertyMap.values()));
      this.columnProperties = unmodifiableList(columnProperties());
      this.lazyLoadedBlobProperties = unmodifiableList(lazyLoadedByteArrayProperties());
      this.primaryKeyProperties = unmodifiableList(primaryKeyProperties());
      this.primaryKeyAttribues = unmodifiableList(primaryKeyAttributes());
      this.primaryKeyPropertyMap = unmodifiableMap(primaryKeyPropertyMap());
      this.foreignKeyProperties = unmodifiableList(foreignKeyProperties());
      this.foreignKeyPropertyMap = unmodifiableMap(foreignKeyPropertyMap());
      this.columnPropertyForeignKeyProperties = unmodifiableMap(columnPropertyForeignKeyProperties());
      this.derivedAttributes = unmodifiableMap(derivedAttributes());
      this.transientProperties = unmodifiableList(transientProperties());
      this.defaultSelectAttributes = unmodifiableList(defaultSelectAttributes());
      this.serializationVersion = createSerializationVersion();
    }

    private Map<Attribute<?>, Property<?>> propertyMap(List<Property.Builder<?, ?>> builders) {
      Map<Attribute<?>, Property<?>> map = new HashMap<>(builders.size());
      for (Property.Builder<?, ?> builder : builders) {
        if (!(builder instanceof ForeignKeyProperty.Builder)) {
          validateAndAddProperty(builder.build(), map, entityType);
        }
      }
      validatePrimaryKeyProperties(map, entityType);

      configureForeignKeyColumnProperties(builders.stream()
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
        ordereredMap.put(builder.attribute(), map.get(builder.attribute()));
      }

      return ordereredMap;
    }

    private Map<Attribute<?>, Collection<ForeignKeyProperty>> columnPropertyForeignKeyProperties() {
      Map<Attribute<?>, Collection<ForeignKeyProperty>> foreignKeyMap = new HashMap<>();
      foreignKeyProperties.forEach(foreignKeyProperty ->
              foreignKeyProperty.references().forEach(reference ->
                      foreignKeyMap.computeIfAbsent(reference.attribute(),
                              columnAttribute -> new ArrayList<>()).add(foreignKeyProperty)));

      return foreignKeyMap;
    }

    private void configureForeignKeyColumnProperties(List<ForeignKeyProperty.Builder> foreignKeyBuilders,
                                                     Map<Attribute<?>, Property<?>> propertyMap) {
      Map<ForeignKey, List<ColumnProperty<?>>> foreignKeyColumnProperties = foreignKeyBuilders.stream()
              .map(ForeignKeyProperty.Builder::attribute)
              .map(ForeignKey.class::cast)
              .collect(toMap(Function.identity(), foreignKey -> foreignKeyColumnProperties(foreignKey, propertyMap)));
      foreignKeyColumnAttributes.addAll(foreignKeyColumnProperties.values().stream()
              .flatMap(properties -> properties.stream().map(Property::attribute))
              .collect(toSet()));
      foreignKeyBuilders.forEach(foreignKeyBuilder -> setForeignKeyNullable(foreignKeyBuilder, foreignKeyColumnProperties));
    }

    private Map<Attribute<?>, ColumnProperty<?>> primaryKeyPropertyMap() {
      return primaryKeyProperties.stream()
              .collect(toMap(Property::attribute, Function.identity()));
    }

    private List<ForeignKeyProperty> foreignKeyProperties() {
      return properties.stream()
              .filter(ForeignKeyProperty.class::isInstance)
              .map(ForeignKeyProperty.class::cast)
              .collect(toList());
    }

    private List<ColumnProperty<?>> columnProperties() {
      return properties.stream()
              .filter(ColumnProperty.class::isInstance)
              .map(property -> (ColumnProperty<?>) property)
              .collect(toList());
    }

    private List<TransientProperty<?>> transientProperties() {
      return properties.stream()
              .filter(TransientProperty.class::isInstance)
              .map(property -> (TransientProperty<?>) property)
              .collect(toList());
    }

    private List<ColumnProperty<?>> lazyLoadedByteArrayProperties() {
      return columnProperties.stream()
              .filter(property -> property.attribute().isByteArray())
              .filter(property -> !(property instanceof BlobProperty) || !((BlobProperty) property).isEagerlyLoaded())
              .collect(toList());
    }

    private List<Attribute<?>> defaultSelectAttributes() {
      List<Attribute<?>> selectableAttributes = columnProperties.stream()
              .filter(ColumnProperty::isSelectable)
              .filter(property -> !lazyLoadedBlobProperties.contains(property))
              .map(Property::attribute)
              .collect(toList());
      selectableAttributes.addAll(foreignKeyProperties.stream()
              .map(ForeignKeyProperty::attribute)
              .collect(toList()));

      return selectableAttributes;
    }

    private Map<Attribute<?>, Set<Attribute<?>>> derivedAttributes() {
      Map<Attribute<?>, Set<Attribute<?>>> derivedPropertyMap = new HashMap<>();
      properties.stream()
              .filter(DerivedProperty.class::isInstance)
              .map(DerivedProperty.class::cast)
              .forEach(derivedProperty -> {
                List<Attribute<?>> sourceAttributes = derivedProperty.sourceAttributes();
                for (Attribute<?> sourceAttribute : sourceAttributes) {
                  derivedPropertyMap.computeIfAbsent(sourceAttribute, attribute -> new HashSet<>()).add(derivedProperty.attribute());
                }
              });

      return derivedPropertyMap;
    }

    private List<ColumnProperty<?>> primaryKeyProperties() {
      return properties.stream()
              .filter(ColumnProperty.class::isInstance)
              .map(property -> (ColumnProperty<?>) property)
              .filter(ColumnProperty::isPrimaryKeyColumn)
              .sorted(comparingInt(ColumnProperty::primaryKeyIndex))
              .collect(toList());
    }

    private List<Attribute<?>> primaryKeyAttributes() {
      return primaryKeyProperties.stream()
              .map(Property::attribute)
              .collect(toList());
    }

    private int createSerializationVersion() {
      return propertyMap.values().stream()
              .filter(property -> !property.isDerived())
              .map(Property::attribute)
              .map(attribute -> attribute.name() + attribute.valueClass().getName())
              .collect(Collectors.joining())
              .hashCode();
    }

    private static Map<String, Attribute<?>> attributeMap(Map<Attribute<?>, Property<?>> properties) {
      return properties.keySet().stream()
              .collect(Collectors.toMap(Attribute::name, Function.identity()));
    }

    private static void validateAndAddProperty(Property<?> property, Map<Attribute<?>, Property<?>> properties, EntityType entityType) {
      validate(property, properties, entityType);
      properties.put(property.attribute(), property);
    }

    private static void validatePrimaryKeyProperties(Map<Attribute<?>, Property<?>> properties, EntityType entityType) {
      Set<Integer> usedPrimaryKeyIndexes = new LinkedHashSet<>();
      for (Property<?> property : properties.values()) {
        if (property instanceof ColumnProperty && ((ColumnProperty<?>) property).isPrimaryKeyColumn()) {
          Integer index = ((ColumnProperty<?>) property).primaryKeyIndex();
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
                          + usedPrimaryKeyIndexes.size() + " distinct primary key indexes " + usedPrimaryKeyIndexes);
                }
              });
    }

    private static void validate(Property<?> property, Map<Attribute<?>, Property<?>> properties, EntityType entityType) {
      if (!entityType.equals(property.entityType())) {
        throw new IllegalArgumentException("Attribute entityType (" +
                property.entityType() + ") in property " + property.attribute() +
                " does not match the definition entityType: " + entityType);
      }
      if (properties.containsKey(property.attribute())) {
        throw new IllegalArgumentException("Property " + property.attribute()
                + (property.caption() != null ? " (" + property.caption() + ")" : "")
                + " has already been defined as: " + properties.get(property.attribute()) + " in entity: " + entityType);
      }
    }

    private Map<ForeignKey, ForeignKeyProperty> foreignKeyPropertyMap() {
      return foreignKeyProperties.stream()
              .collect(Collectors.toMap(ForeignKeyProperty::attribute, Function.identity()));
    }

    private static List<ColumnProperty<?>> foreignKeyColumnProperties(ForeignKey foreignKey, Map<Attribute<?>, Property<?>> propertyMap) {
      return foreignKey.references().stream()
              .map(reference -> foreignKeyColumnProperty(reference, propertyMap))
              .collect(toList());
    }

    private static ColumnProperty<?> foreignKeyColumnProperty(ForeignKey.Reference<?> reference, Map<Attribute<?>, Property<?>> propertyMap) {
      ColumnProperty<?> columnProperty = (ColumnProperty<?>) propertyMap.get(reference.attribute());
      if (columnProperty == null) {
        throw new IllegalArgumentException("ColumnProperty based on attribute: " + reference.attribute()
                + " not found when initializing foreign key");
      }

      return columnProperty;
    }

    private static void setForeignKeyNullable(ForeignKeyProperty.Builder foreignKeyBuilder,
                                              Map<ForeignKey, List<ColumnProperty<?>>> foreignKeyColumnProperties) {
      //make foreign key properties nullable if and only if any of their constituent column properties are nullable
      foreignKeyBuilder.nullable(foreignKeyColumnProperties.get(foreignKeyBuilder.attribute()).stream()
              .anyMatch(Property::isNullable));
    }
  }

  static final class DefaultBuilder implements Builder {

    private final EntityProperties properties;

    private String tableName;
    private Map<ConditionType, ConditionProvider> conditionProviders;
    private String caption;
    private String captionResourceKey;
    private String description;
    private boolean smallDataset;
    private boolean staticData;
    private boolean readOnly;
    private KeyGenerator keyGenerator = DefaultEntity.DEFAULT_KEY_GENERATOR;
    private boolean keyGenerated;
    private boolean optimisticLockingEnabled = true;
    private OrderBy orderBy;
    private String selectTableName;
    private SelectQuery selectQuery;
    private Function<Entity, String> stringFactory = DefaultEntity.DEFAULT_STRING_FACTORY;
    private ColorProvider backgroundColorProvider = new NullColorProvider();
    private ColorProvider foregroundColorProvider = new NullColorProvider();
    private Comparator<Entity> comparator = Text.spaceAwareCollator();
    private EntityValidator validator = new DefaultEntityValidator();

    DefaultBuilder(List<Property.Builder<?, ?>> propertyBuilders) {
      this.properties = new EntityProperties(propertyBuilders);
      this.tableName = properties.entityType.name();
      this.captionResourceKey = properties.entityType.name();
    }

    @Override
    public Builder tableName(String tableName) {
      if (nullOrEmpty(tableName)) {
        throw new IllegalArgumentException("Table name must be non-empty");
      }
      this.tableName = tableName;
      return this;
    }

    @Override
    public Builder conditionProvider(ConditionType conditionType, ConditionProvider conditionProvider) {
      requireNonNull(conditionType, "conditionType");
      requireNonNull(conditionProvider, "conditionProvider");
      if (this.conditionProviders == null) {
        this.conditionProviders = new HashMap<>();
      }
      if (this.conditionProviders.containsKey(conditionType)) {
        throw new IllegalStateException("ConditionProvider for condition type  " + conditionType + " has already been added");
      }
      this.conditionProviders.put(conditionType, conditionProvider);
      return this;
    }

    @Override
    public Builder caption(String caption) {
      this.caption = requireNonNull(caption, "caption");
      return this;
    }

    @Override
    public Builder captionResourceKey(String captionResourceKey) {
      if (this.caption != null) {
        throw new IllegalStateException("Caption has already been set for entity: " + properties.entityType);
      }
      this.captionResourceKey = requireNonNull(captionResourceKey, "captionResourceKey");
      return this;
    }

    @Override
    public Builder description(String description) {
      this.description = description;
      return this;
    }

    @Override
    public Builder smallDataset(boolean smallDataset) {
      this.smallDataset = smallDataset;
      return this;
    }

    @Override
    public Builder staticData(boolean staticData) {
      this.staticData = staticData;
      return this;
    }

    @Override
    public Builder readOnly(boolean readOnly) {
      this.readOnly = readOnly;
      return this;
    }

    @Override
    public Builder optimisticLockingEnabled(boolean optimisticLockingEnabled) {
      this.optimisticLockingEnabled = optimisticLockingEnabled;
      return this;
    }

    @Override
    public Builder keyGenerator(KeyGenerator keyGenerator) {
      if (properties.primaryKeyProperties.isEmpty()) {
        throw new IllegalStateException("KeyGenerator can not be set for an entity without a primary key: " + properties.entityType);
      }
      this.keyGenerator = requireNonNull(keyGenerator, "keyGenerator");
      this.keyGenerated = true;
      return this;
    }

    @Override
    public Builder orderBy(OrderBy orderBy) {
      this.orderBy = requireNonNull(orderBy, "orderBy");
      return this;
    }

    @Override
    public Builder selectTableName(String selectTableName) {
      this.selectTableName = requireNonNull(selectTableName, "selectTableName");
      return this;
    }

    @Override
    public Builder selectQuery(SelectQuery selectQuery) {
      this.selectQuery = requireNonNull(selectQuery, "selectQuery");
      return this;
    }

    @Override
    public Builder comparator(Comparator<Entity> comparator) {
      this.comparator = requireNonNull(comparator, "comparator");
      return this;
    }

    @Override
    public Builder stringFactory(Attribute<?> attribute) {
      return stringFactory(StringFactory.builder()
              .value(attribute)
              .build());
    }

    @Override
    public Builder stringFactory(Function<Entity, String> stringFactory) {
      this.stringFactory = requireNonNull(stringFactory, "stringFactory");
      return this;
    }

    @Override
    public Builder backgroundColorProvider(ColorProvider backgroundColorProvider) {
      this.backgroundColorProvider = requireNonNull(backgroundColorProvider, "backgroundColorProvider");
      return this;
    }

    @Override
    public Builder foregroundColorProvider(ColorProvider foregroundColorProvider) {
      this.foregroundColorProvider = requireNonNull(foregroundColorProvider, "foregroundColorProvider");
      return this;
    }

    @Override
    public Builder validator(EntityValidator validator) {
      this.validator = requireNonNull(validator, "validator");
      return this;
    }

    @Override
    public EntityDefinition build() {
      return new DefaultEntityDefinition(this);
    }
  }
}
