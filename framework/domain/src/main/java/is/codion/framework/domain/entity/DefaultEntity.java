/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.common.Primitives;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.DerivedProperty;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.domain.property.ItemProperty;
import is.codion.framework.domain.property.Property;
import is.codion.framework.domain.property.TransientProperty;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static is.codion.framework.domain.entity.DefaultKey.serializerForDomain;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;

class DefaultEntity implements Entity, Serializable {

  private static final long serialVersionUID = 1;

  static final DefaultStringFactory DEFAULT_STRING_FACTORY = new DefaultStringFactory();

  /**
   * Keep a reference to this frequently referenced object
   */
  protected EntityDefinition definition;

  /**
   * Holds the values contained in this entity.
   */
  protected Map<Attribute<?>, Object> values;

  /**
   * Holds the original value for attributes which values have changed since they were first set.
   */
  protected Map<Attribute<?>, Object> originalValues;

  /**
   * Used to cache the return value of the frequently called toString(),
   * invalidated each time an attribute value changes
   */
  private String toString;

  /**
   * Caches the result of {@link #referencedKey} method
   */
  private Map<ForeignKey, Key> referencedKeyCache;

  /**
   * The primary key of this entity
   */
  private Key primaryKey;

  protected DefaultEntity() {}

  /**
   * Instantiates a new DefaultEntity
   * @param key the key
   */
  DefaultEntity(Key key) {
    this(requireNonNull(key).definition(), createValueMap(key), null);
    if (key.isPrimaryKey()) {
      this.primaryKey = key;
    }
  }

  /**
   * Instantiates a new DefaultEntity based on the given values.
   * @param definition the entity definition
   * @param values the initial values, may be null
   * @param originalValues the original values, may be null
   * @throws IllegalArgumentException in case any of the properties are not part of the entity.
   */
  DefaultEntity(EntityDefinition definition, Map<Attribute<?>, Object> values, Map<Attribute<?>, Object> originalValues) {
    this.values = validate(requireNonNull(definition), values == null ? new HashMap<>() : new HashMap<>(values));
    this.originalValues = validate(definition, originalValues == null ? null : new HashMap<>(originalValues));
    this.definition = definition;
  }

  @Override
  public final EntityType type() {
    return definition.type();
  }

  @Override
  public final EntityDefinition definition() {
    return definition;
  }

  @Override
  public final Key primaryKey() {
    if (primaryKey == null) {
      primaryKey = createPrimaryKey(false);
    }

    return primaryKey;
  }

  @Override
  public final Key originalPrimaryKey() {
    return createPrimaryKey(true);
  }

  @Override
  public final boolean isModified() {
    if (originalValues != null) {
      for (Attribute<?> attribute : originalValues.keySet()) {
        Property<?> property = definition.property(attribute);
        if (property instanceof ColumnProperty) {
          ColumnProperty<?> columnProperty = (ColumnProperty<?>) property;
          if (columnProperty.isInsertable() && columnProperty.isUpdatable()) {
            return true;
          }
        }
        if (property instanceof TransientProperty && ((TransientProperty<?>) property).modifiesEntity()) {
          return true;
        }
      }
    }

    return false;
  }

  @Override
  public final <T> T get(Attribute<T> attribute) {
    return get(definition.property(attribute));
  }

  @Override
  public final <T> Optional<T> optional(Attribute<T> attribute) {
    return Optional.ofNullable(get(attribute));
  }

  @Override
  public final <T> T original(Attribute<T> attribute) {
    return original(definition.property(attribute));
  }

  @Override
  public final boolean isNull(Attribute<?> attribute) {
    return isNull(definition.property(attribute));
  }

  @Override
  public final boolean isNotNull(Attribute<?> attribute) {
    return !isNull(attribute);
  }

  @Override
  public final boolean isModified(Attribute<?> attribute) {
    definition.property(attribute);
    return isModifiedInternal(attribute);
  }

  @Override
  public final boolean isNew() {
    return primaryKey().isNull() || originalPrimaryKey().isNull();
  }

  @Override
  public final Entity referencedEntity(ForeignKey foreignKey) {
    Entity value = (Entity) values.get(foreignKey);
    if (value == null) {//possibly not loaded
      Key referencedKey = referencedKey(foreignKey);
      if (referencedKey != null) {
        return new DefaultEntity(referencedKey);
      }
    }

    return value;
  }

  @Override
  public final boolean isLoaded(ForeignKey foreignKey) {
    definition.foreignKeyProperty(foreignKey);
    return values.get(foreignKey) != null;
  }

  @Override
  public final <T> String toString(Attribute<T> attribute) {
    Property<T> property = definition.property(attribute);
    if (attribute instanceof ForeignKey && values.get(attribute) == null) {
      Key referencedKey = referencedKey((ForeignKey) attribute);
      if (referencedKey != null) {
        return referencedKey.toString();
      }
    }

    return property.toString(get(property));
  }

  @Override
  public <T> T put(Attribute<T> attribute, T value) {
    return put(definition.property(attribute), value);
  }

  @Override
  public Entity clearPrimaryKey() {
    definition.primaryKeyAttributes().forEach(this::remove);
    primaryKey = null;

    return this;
  }

  @Override
  public void save(Attribute<?> attribute) {
    removeOriginalValue(requireNonNull(attribute));
  }

  @Override
  public void saveAll() {
    originalValues = null;
  }

  @Override
  public void revert(Attribute<?> attribute) {
    Property<?> property = definition.property(attribute);
    if (isModifiedInternal(attribute)) {
      put((Property<Object>) property, original(property));
    }
  }

  @Override
  public void revertAll() {
    if (originalValues != null) {
      for (Attribute<?> attribute : new ArrayList<>(originalValues.keySet())) {
        revert(attribute);
      }
    }
  }

  @Override
  public <T> T remove(Attribute<T> attribute) {
    definition.property(attribute);
    T value = null;
    if (values.containsKey(attribute)) {
      value = (T) values.remove(attribute);
      removeOriginalValue(attribute);
      definition.foreignKeyProperties(attribute).forEach(foreignKeyProperty -> remove(foreignKeyProperty.attribute()));
    }

    return value;
  }

  @Override
  public Map<Attribute<?>, Object> setAs(Entity entity) {
    if (entity == this) {
      return emptyMap();
    }
    if (entity != null && !definition.type().equals(entity.type())) {
      throw new IllegalArgumentException("Entity of type: " + definition.type() + " expected, got: " + entity.type());
    }
    Map<Property<?>, Object> previousValues = new HashMap<>();
    definition.properties().forEach(property -> previousValues.put(property, get(property)));
    clear();
    if (entity != null) {
      entity.entrySet().forEach(attributeValue -> values.put(attributeValue.getKey(), attributeValue.getValue()));
      Set<Map.Entry<Attribute<?>, Object>> originalEntrySet = entity.originalEntrySet();
      if (!originalEntrySet.isEmpty()) {
        originalValues = new HashMap<>();
        originalEntrySet.forEach(entry -> originalValues.put(entry.getKey(), entry.getValue()));
      }
    }
    Map<Attribute<?>, Object> affectedAttributes = new HashMap<>();
    previousValues.forEach((property, previousValue) -> {
      if (!Objects.equals(get(property), previousValue)) {
        affectedAttributes.put(property.attribute(), previousValue);
      }
    });

    return unmodifiableMap(affectedAttributes);
  }

  @Override
  public final Entity copy() {
    Entity entity = new DefaultEntity(definition, null, null);
    entity.setAs(this);

    return entity;
  }

  @Override
  public final Builder copyBuilder() {
    return new DefaultEntityBuilder(definition, values, originalValues);
  }

  @Override
  public final Entity deepCopy() {
    return deepCopy(new HashMap<>());
  }

  @Override
  public final Entity immutable() {
    if (isImmutable()) {
      return this;
    }

    return new ImmutableEntity(this);
  }

  @Override
  public final boolean isImmutable() {
    return this instanceof ImmutableEntity;
  }

  @Override
  public final <T extends Entity> T castTo(Class<T> entityClass) {
    requireNonNull(entityClass);
    if (entityClass.isAssignableFrom(getClass())) {
      // no wrapping required
      return (T) this;
    }
    if (!type().entityClass().equals(entityClass)) {
      throw new IllegalArgumentException("entityClass " + type().entityClass() + " expected, got: " + entityClass);
    }

    return (T) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class[] {entityClass}, new EntityInvoker(this));
  }

  @Override
  public final boolean columnValuesEqual(Entity entity) {
    if (!definition.type().equals(requireNonNull(entity).type())) {
      throw new IllegalArgumentException("Entity of type " + definition.type() +
              " expected, got: " + entity.type());
    }

    return definition.columnProperties().stream()
            .allMatch(property -> valueEqual(entity, property));
  }

  /**
   * @param obj the object to compare with
   * @return true if the given object is an Entity and its primary key is equal to this ones
   */
  @Override
  public final boolean equals(Object obj) {
    return this == obj || obj instanceof Entity && primaryKey().equals(((Entity) obj).primaryKey());
  }

  /**
   * @param entity the entity to compare with
   * @return the compare result from comparing {@code entity} with this Entity instance
   * @see EntityDefinition.Builder#comparator(java.util.Comparator)
   */
  @Override
  public final int compareTo(Entity entity) {
    return definition.comparator().compare(this, entity);
  }

  /**
   * Returns the hash code of the primary key
   */
  @Override
  public final int hashCode() {
    return primaryKey().hashCode();
  }

  /**
   * Returns a String representation of this entity
   * Note that if the this entitys {@link StringFactory} returns null for some reason,
   * the default String factory is used instead.
   * @return a string representation of this entity
   * @see EntityDefinition.Builder#stringFactory(java.util.function.Function)
   * @see EntityDefinition#stringFactory()
   */
  @Override
  public final String toString() {
    if (toString == null) {
      toString = createToString();
    }

    return toString;
  }

  @Override
  public final Key referencedKey(ForeignKey foreignKey) {
    definition.foreignKeyProperty(foreignKey);
    Key cachedReferencedKey = cachedReferencedKey(foreignKey);
    if (cachedReferencedKey != null) {
      return cachedReferencedKey;
    }

    return createAndCacheReferencedKey(foreignKey);
  }

  @Override
  public final boolean contains(Attribute<?> attribute) {
    return values.containsKey(requireNonNull(attribute));
  }

  @Override
  public final Set<Map.Entry<Attribute<?>, Object>> entrySet() {
    return unmodifiableSet(values.entrySet());
  }

  @Override
  public final Set<Map.Entry<Attribute<?>, Object>> originalEntrySet() {
    if (originalValues == null) {
      return emptySet();
    }

    return unmodifiableSet(originalValues.entrySet());
  }

  private String createToString() {
    String string = definition.stringFactory().apply(this);
    if (string == null) {
      return DEFAULT_STRING_FACTORY.apply(this);
    }

    return string;
  }

  private void clear() {
    values.clear();
    originalValues = null;
    primaryKey = null;
    referencedKeyCache = null;
    toString = null;
  }

  private <T> T get(Property<T> property) {
    if (property.isDerived()) {
      return derivedValue((DerivedProperty<T>) property, false);
    }

    return (T) values.get(property.attribute());
  }

  private <T> T original(Property<T> property) {
    if (property.isDerived()) {
      return derivedValue((DerivedProperty<T>) property, true);
    }
    if (isModifiedInternal(property.attribute())) {
      return (T) originalValues.get(property.attribute());
    }

    return get(property);
  }

  private <T> T put(Property<T> property, T value) {
    T newValue = validateAndPrepareForPut(property, value);
    Attribute<T> attribute = property.attribute();
    boolean initialization = !values.containsKey(attribute);
    T previousValue = (T) values.put(attribute, newValue);
    if (!initialization && Objects.equals(previousValue, newValue)) {
      return newValue;
    }
    if (!initialization) {
      updateOriginalValue(attribute, newValue, previousValue);
    }
    if (property instanceof ColumnProperty) {
      if (((ColumnProperty<?>) property).isPrimaryKeyColumn()) {
        primaryKey = null;
      }
      if (definition.isForeignKeyAttribute(attribute)) {
        removeInvalidForeignKeyValues(attribute, newValue);
      }
    }
    toString = null;
    if (property instanceof ForeignKeyProperty) {
      setForeignKeyValues((ForeignKeyProperty) property, (Entity) newValue);
    }

    return previousValue;
  }

  private <T> boolean isNull(Property<T> property) {
    if (property instanceof ForeignKeyProperty) {
      return isReferenceNull(((ForeignKeyProperty) property).attribute());
    }

    return get(property) == null;
  }

  private boolean isReferenceNull(ForeignKey foreignKey) {
    List<ForeignKey.Reference<?>> references = foreignKey.references();
    if (references.size() == 1) {
      return isNull(references.get(0).attribute());
    }
    EntityDefinition referencedDefinition = definition.referencedEntityDefinition(foreignKey);
    for (int i = 0; i < references.size(); i++) {
      ForeignKey.Reference<?> reference = references.get(i);
      ColumnProperty<?> referencedProperty = referencedDefinition.columnProperty(reference.referencedAttribute());
      if (!referencedProperty.isNullable() && isNull(reference.attribute())) {
        return true;
      }
    }

    return false;
  }

  private <T> T validateAndPrepareForPut(Property<T> property, T value) {
    if (property.isDerived()) {
      throw new IllegalArgumentException("Can not set the value of a derived property");
    }
    if (property instanceof ItemProperty && value != null && !((ItemProperty<T>) property).isValid(value)) {
      throw new IllegalArgumentException("Invalid item value: " + value + " for property " + property.attribute());
    }
    if (value != null && property instanceof ForeignKeyProperty) {
      validateForeignKeyValue((ForeignKeyProperty) property, (Entity) value);
    }

    return property.prepareValue(property.attribute().validateType(value));
  }

  private void validateForeignKeyValue(ForeignKeyProperty property, Entity foreignKeyValue) {
    EntityType referencedType = property.referencedType();
    if (!Objects.equals(referencedType, foreignKeyValue.type())) {
      throw new IllegalArgumentException("Entity of type " + referencedType +
              " expected for property " + property + ", got: " + foreignKeyValue.type());
    }
    property.references().forEach(reference -> throwIfModifiesReadOnlyReference(property, foreignKeyValue, reference));
  }

  private void throwIfModifiesReadOnlyReference(ForeignKeyProperty property, Entity foreignKeyValue,
                                                ForeignKey.Reference<?> reference) {
    boolean readOnlyReference = property.isReadOnly(reference.attribute());
    if (readOnlyReference) {
      boolean containsValue = contains(reference.attribute());
      if (containsValue) {
        Object currentReferenceValue = get(reference.attribute());
        Object newReferenceValue = foreignKeyValue.get(reference.referencedAttribute());
        if (!Objects.equals(currentReferenceValue, newReferenceValue)) {
          throw new IllegalArgumentException("Foreign key " + property + " is not allowed to modify read-only reference: " +
                  reference.attribute() + " from " + currentReferenceValue + " to " + newReferenceValue);
        }
      }
    }
  }

  private <T> void removeInvalidForeignKeyValues(Attribute<T> attribute, T value) {
    for (ForeignKeyProperty foreignKeyProperty : definition.foreignKeyProperties(attribute)) {
      Entity foreignKeyEntity = get(foreignKeyProperty);
      if (foreignKeyEntity != null) {
        ForeignKey foreignKey = foreignKeyProperty.attribute();
        //if the value isn't equal to the value in the foreign key,
        //that foreign key reference is invalid and is removed
        if (!Objects.equals(value, foreignKeyEntity.get(foreignKey.reference(attribute).referencedAttribute()))) {
          remove(foreignKey);
          removeCachedReferencedKey(foreignKey);
        }
      }
    }
  }

  /**
   * Sets the values of the properties used in the reference to the corresponding values found in {@code referencedEntity}.
   * Example: EntityOne references EntityTwo via entityTwoId, after a call to this method the EntityOne.entityTwoId
   * property has the value of EntityTwos primary key property. If {@code referencedEntity} is null then
   * the corresponding reference values are set to null.
   * @param foreignKeyProperty the entity reference property
   * @param referencedEntity the referenced entity
   */
  private void setForeignKeyValues(ForeignKeyProperty foreignKeyProperty, Entity referencedEntity) {
    removeCachedReferencedKey(foreignKeyProperty.attribute());
    List<ForeignKey.Reference<?>> references = foreignKeyProperty.references();
    for (int i = 0; i < references.size(); i++) {
      ForeignKey.Reference<?> reference = references.get(i);
      if (!foreignKeyProperty.isReadOnly(reference.attribute())) {
        Property<Object> columnProperty = definition.columnProperty((Attribute<Object>) reference.attribute());
        put(columnProperty, referencedEntity == null ? null : referencedEntity.get(reference.referencedAttribute()));
      }
    }
  }

  /**
   * Creates and caches the key referenced by the given foreign key
   * @param foreignKey the foreign key
   * @return the referenced key or null if a valid key can not be created (null values for non-nullable properties)
   */
  private Key createAndCacheReferencedKey(ForeignKey foreignKey) {
    EntityDefinition referencedEntityDefinition = definition.referencedEntityDefinition(foreignKey);
    List<ForeignKey.Reference<?>> references = foreignKey.references();
    if (references.size() > 1) {
      return createAndCacheCompositeReferenceKey(foreignKey, references, referencedEntityDefinition);
    }

    return createAndCacheSingleReferenceKey(foreignKey, references.get(0), referencedEntityDefinition);
  }

  private Key createAndCacheCompositeReferenceKey(ForeignKey foreignKey,
                                                  List<ForeignKey.Reference<?>> references,
                                                  EntityDefinition referencedEntityDefinition) {
    Map<Attribute<?>, Object> keyValues = new HashMap<>(references.size());
    for (int i = 0; i < references.size(); i++) {
      ForeignKey.Reference<?> reference = references.get(i);
      ColumnProperty<?> referencedProperty = referencedEntityDefinition.columnProperty(reference.referencedAttribute());
      Object value = values.get(reference.attribute());
      if (value == null && !referencedProperty.isNullable()) {
        return null;
      }
      keyValues.put(reference.referencedAttribute(), value);
    }
    Set<Attribute<?>> referencedAttributes = keyValues.keySet();
    List<Attribute<?>> primaryKeyAttributes = referencedEntityDefinition.primaryKeyAttributes();
    boolean isPrimaryKey = referencedAttributes.size() == primaryKeyAttributes.size() && referencedAttributes.containsAll(primaryKeyAttributes);

    return cacheReferencedKey(foreignKey, new DefaultKey(referencedEntityDefinition, keyValues, isPrimaryKey));
  }

  private Key createAndCacheSingleReferenceKey(ForeignKey foreignKey,
                                               ForeignKey.Reference<?> reference,
                                               EntityDefinition referencedEntityDefinition) {
    Object value = values.get(reference.attribute());
    if (value == null) {
      return null;
    }

    boolean isPrimaryKey = reference.referencedAttribute().equals(referencedEntityDefinition.primaryKeyAttributes().get(0));

    return cacheReferencedKey(foreignKey,
            new DefaultKey(definition.referencedEntityDefinition(foreignKey),
                    reference.referencedAttribute(), value, isPrimaryKey));
  }

  private Key cacheReferencedKey(ForeignKey foreignKey, Key referencedKey) {
    if (referencedKeyCache == null) {
      referencedKeyCache = new HashMap<>();
    }
    referencedKeyCache.put(foreignKey, referencedKey);

    return referencedKey;
  }

  private Key cachedReferencedKey(ForeignKey foreignKey) {
    if (referencedKeyCache == null) {
      return null;
    }

    return referencedKeyCache.get(foreignKey);
  }

  private void removeCachedReferencedKey(ForeignKey foreignKey) {
    if (referencedKeyCache != null) {
      referencedKeyCache.remove(foreignKey);
      if (referencedKeyCache.isEmpty()) {
        referencedKeyCache = null;
      }
    }
  }

  /**
   * Creates a Key for this Entity instance
   * @param originalValues if true then the original values of the properties involved are used
   * @return a Key based on the values in this Entity instance
   */
  private Key createPrimaryKey(boolean originalValues) {
    if (!definition.hasPrimaryKey()) {
      return new DefaultKey(definition, emptyList(), true);
    }
    List<Attribute<?>> primaryKeyAttributes = definition.primaryKeyAttributes();
    if (primaryKeyAttributes.size() == 1) {
      return createSingleAttributePrimaryKey(primaryKeyAttributes.get(0), originalValues);
    }

    return createMultiAttributePrimaryKey(primaryKeyAttributes, originalValues);
  }

  private DefaultKey createSingleAttributePrimaryKey(Attribute<?> attribute, boolean originalValues) {
    return new DefaultKey(definition, attribute, originalValues ? original(attribute) : values.get(attribute), true);
  }

  private DefaultKey createMultiAttributePrimaryKey(List<Attribute<?>> primaryKeyAttributes, boolean originalValues) {
    Map<Attribute<?>, Object> keyValues = new HashMap<>(primaryKeyAttributes.size());
    for (int i = 0; i < primaryKeyAttributes.size(); i++) {
      Attribute<?> attribute = primaryKeyAttributes.get(i);
      keyValues.put(attribute, originalValues ? original(attribute) : values.get(attribute));
    }

    return new DefaultKey(definition, keyValues, true);
  }

  private <T> T derivedValue(DerivedProperty<T> derivedProperty, boolean originalValue) {
    return derivedProperty.valueProvider().get(createSourceValues(derivedProperty, originalValue));
  }

  private DerivedProperty.SourceValues createSourceValues(DerivedProperty<?> derivedProperty, boolean originalValue) {
    List<Attribute<?>> sourceAttributes = derivedProperty.sourceAttributes();
    if (sourceAttributes.size() == 1) {
      return new DefaultSourceValues(derivedProperty.attribute(), createSingleAttributeSourceValueMap(sourceAttributes.get(0), originalValue));
    }

    return new DefaultSourceValues(derivedProperty.attribute(), createMultiAttributeSourceValueMap(sourceAttributes, originalValue));
  }

  private Map<Attribute<?>, Object> createSingleAttributeSourceValueMap(Attribute<?> sourceAttribute, boolean originalValue) {
    return singletonMap(sourceAttribute, originalValue ? original(sourceAttribute) : get(sourceAttribute));
  }

  private Map<Attribute<?>, Object> createMultiAttributeSourceValueMap(List<Attribute<?>> sourceAttributes, boolean originalValue) {
    Map<Attribute<?>, Object> valueMap = new HashMap<>(sourceAttributes.size());
    for (int i = 0; i < sourceAttributes.size(); i++) {
      Attribute<?> sourceAttribute = sourceAttributes.get(i);
      valueMap.put(sourceAttribute, originalValue ? original(sourceAttribute) : get(sourceAttribute));
    }

    return valueMap;
  }

  private <T> void setOriginalValue(Attribute<T> attribute, T originalValue) {
    if (originalValues == null) {
      originalValues = new HashMap<>();
    }
    originalValues.put(attribute, originalValue);
  }

  private <T> void removeOriginalValue(Attribute<T> attribute) {
    if (originalValues != null) {
      originalValues.remove(attribute);
      if (originalValues.isEmpty()) {
        originalValues = null;
      }
    }
  }

  private <T> void updateOriginalValue(Attribute<T> attribute, T value, T previousValue) {
    boolean modified = isModifiedInternal(attribute);
    if (modified && Objects.equals(originalValues.get(attribute), value)) {
      removeOriginalValue(attribute);//we're back to the original value
    }
    else if (!modified) {//only the first original value is kept
      setOriginalValue(attribute, previousValue);
    }
  }

  private boolean valueEqual(Entity entity, ColumnProperty<?> property) {
    Attribute<?> attribute = property.attribute();
    if (contains(attribute) != entity.contains(attribute)) {
      return false;
    }
    if (attribute.isByteArray()) {
      return Arrays.equals((byte[]) get(property), (byte[]) entity.get(attribute));
    }

    return Objects.equals(get(property), entity.get(attribute));
  }

  private boolean isModifiedInternal(Attribute<?> attribute) {
    return originalValues != null && originalValues.containsKey(attribute);
  }

  /**
   * Watching out for cyclical foreign key values
   * @param copiedEntities entities that have already been copied
   * @return a deep copy of this entity
   */
  private Entity deepCopy(Map<Key, Entity> copiedEntities) {
    Entity copy = copy();
    copiedEntities.put(copy.primaryKey(), copy);
    for (ForeignKey foreignKey : definition.foreignKeys()) {
      Entity foreignKeyValue = copy.get(foreignKey);
      if (foreignKeyValue instanceof DefaultEntity) {//instead of null check, since we cast
        Entity copiedForeignKeyValue = copiedEntities.get(foreignKeyValue.primaryKey());
        if (copiedForeignKeyValue == null) {
          copiedForeignKeyValue = ((DefaultEntity) foreignKeyValue).deepCopy(copiedEntities);
          copiedEntities.put(copiedForeignKeyValue.primaryKey(), copiedForeignKeyValue);
        }
        copy.put(foreignKey, copiedForeignKeyValue);
      }
    }

    return copy;
  }

  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.writeObject(definition.domainName());
    serializerForDomain(definition.domainName()).serialize(this, stream);
  }

  private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
    serializerForDomain((String) stream.readObject()).deserialize(this, stream);
  }

  private static Map<Attribute<?>, Object> validate(EntityDefinition definition, Map<Attribute<?>, Object> values) {
    if (values != null && !values.isEmpty()) {
      for (Map.Entry<Attribute<?>, Object> valueEntry : values.entrySet()) {
        definition.property((Attribute<Object>) valueEntry.getKey()).attribute().validateType(valueEntry.getValue());
      }
    }

    return values;
  }

  private static Map<Attribute<?>, Object> createValueMap(Key key) {
    Collection<Attribute<?>> attributes = key.attributes();
    Map<Attribute<?>, Object> values = new HashMap<>(attributes.size());
    for (Attribute<?> attribute : attributes) {
      values.put(attribute, key.get(attribute));
    }

    return values;
  }

  private static final class DefaultSourceValues implements DerivedProperty.SourceValues {

    private final Attribute<?> derivedAttribute;
    private final Map<Attribute<?>, Object> values;

    private DefaultSourceValues(Attribute<?> derivedAttribute, Map<Attribute<?>, Object> values) {
      this.derivedAttribute = derivedAttribute;
      this.values = values;
    }

    @Override
    public <T> T get(Attribute<T> attribute) {
      if (!values.containsKey(attribute)) {
        throw new IllegalArgumentException("Attribute " + attribute +
                " is not specified as a source attribute for derived property: " + derivedAttribute);
      }

      return (T) values.get(attribute);
    }
  }

  private static final class EntityInvoker implements InvocationHandler, Serializable {

    private static final long serialVersionUID = 1;

    private static final String CAST_TO = "castTo";

    private final DefaultEntity entity;

    private EntityInvoker(DefaultEntity entity) {
      this.entity = entity;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      if (CAST_TO.equals(method.getName())) {
        //prevent double wrapping
        return proxy;
      }
      if (method.getParameterCount() == 0) {
        Attribute<?> attribute = entity.definition.getterAttribute(method);
        if (attribute != null) {
          return getValue(attribute, method.getReturnType());
        }
      }
      else if (method.getParameterCount() == 1) {
        Attribute<?> attribute = entity.definition.setterAttribute(method);
        if (attribute != null) {
          return setValue(attribute, args[0]);
        }
      }
      if (method.isDefault()) {
        return entity.definition.defaultMethodHandle(method).bindTo(proxy).invokeWithArguments(args);
      }

      return method.invoke(entity, args);
    }

    private Object getValue(Attribute<?> attribute, Class<?> getterReturnType) {
      Object value;
      if (attribute instanceof ForeignKey) {
        value = entity.referencedEntity((ForeignKey) attribute);
      }
      else {
        value = entity.get(attribute);
      }
      if (value instanceof Entity) {
        Entity entityValue = (Entity) value;

        value = entityValue.castTo(entityValue.type().entityClass());
      }
      if (getterReturnType.equals(Optional.class)) {
        return Optional.ofNullable(value);
      }
      if (value == null && getterReturnType.isPrimitive()) {
        return Primitives.defaultValue(getterReturnType);
      }

      return value;
    }

    private Object setValue(Attribute<?> attribute, Object value) {
      entity.put((Attribute<Object>) attribute, value);

      return null;
    }
  }
}
