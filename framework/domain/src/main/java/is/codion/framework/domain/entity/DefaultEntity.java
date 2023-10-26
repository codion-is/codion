/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.common.Primitives;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.DerivedAttribute;
import is.codion.framework.domain.entity.attribute.DerivedAttributeDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.attribute.ForeignKeyDefinition;
import is.codion.framework.domain.entity.attribute.ItemColumnDefinition;
import is.codion.framework.domain.entity.attribute.TransientAttributeDefinition;

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
import java.util.function.Predicate;

import static is.codion.framework.domain.entity.DefaultKey.serializerForDomain;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;

class DefaultEntity implements Entity, Serializable {

  private static final long serialVersionUID = 1;

  static final DefaultKeyGenerator DEFAULT_KEY_GENERATOR = new DefaultKeyGenerator();
  static final DefaultStringFactory DEFAULT_STRING_FACTORY = new DefaultStringFactory();
  static final NullColorProvider NULL_COLOR_PROVIDER = new NullColorProvider();
  static final EntityValidator DEFAULT_VALIDATOR = new DefaultEntityValidator();
  static final Predicate<Entity> DEFAULT_EXISTS = new DefaultEntityExists();

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
    this(requireNonNull(key).entityDefinition(), createValueMap(key), null);
    if (key.primaryKey()) {
      this.primaryKey = key;
    }
  }

  /**
   * Instantiates a new DefaultEntity based on the given values.
   * @param definition the entity definition
   * @param values the initial values, may be null
   * @param originalValues the original values, may be null
   * @throws IllegalArgumentException in case any of the attributes are not part of the entity.
   */
  DefaultEntity(EntityDefinition definition, Map<Attribute<?>, Object> values, Map<Attribute<?>, Object> originalValues) {
    this.values = validateTypes(requireNonNull(definition), values == null ? new HashMap<>() : new HashMap<>(values));
    this.originalValues = validateTypes(definition, originalValues == null ? null : new HashMap<>(originalValues));
    this.definition = definition;
  }

  @Override
  public final EntityType entityType() {
    return definition.entityType();
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
  public final boolean modified() {
    if (originalValues != null) {
      for (Attribute<?> attribute : originalValues.keySet()) {
        AttributeDefinition<?> attributeDefinition = definition.attributes().definition(attribute);
        if (attributeDefinition instanceof ColumnDefinition) {
          ColumnDefinition<?> columnDefinition = (ColumnDefinition<?>) attributeDefinition;
          if (columnDefinition.insertable() && columnDefinition.updatable()) {
            return true;
          }
        }
        if (attributeDefinition instanceof TransientAttributeDefinition && ((TransientAttributeDefinition<?>) attributeDefinition).modifiesEntity()) {
          return true;
        }
      }
    }

    return false;
  }

  @Override
  public final <T> T get(Attribute<T> attribute) {
    return get(definition.attributes().definition(attribute));
  }

  @Override
  public final <T> Optional<T> optional(Attribute<T> attribute) {
    return Optional.ofNullable(get(attribute));
  }

  @Override
  public final <T> T original(Attribute<T> attribute) {
    return original(definition.attributes().definition(attribute));
  }

  @Override
  public final boolean isNull(Attribute<?> attribute) {
    return isNull(definition.attributes().definition(attribute));
  }

  @Override
  public final boolean isNotNull(Attribute<?> attribute) {
    return !isNull(attribute);
  }

  @Override
  public final boolean modified(Attribute<?> attribute) {
    definition.attributes().definition(attribute);
    return isModifiedInternal(attribute);
  }

  @Override
  public final boolean exists() {
    return definition.exists().test(this);
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
  public final boolean loaded(ForeignKey foreignKey) {
    definition.foreignKeys().definition(foreignKey);
    return values.get(foreignKey) != null;
  }

  @Override
  public final <T> String string(Attribute<T> attribute) {
    AttributeDefinition<T> attributeDefinition = definition.attributes().definition(attribute);
    if (attribute instanceof ForeignKey && values.get(attribute) == null) {
      Key referencedKey = referencedKey((ForeignKey) attribute);
      if (referencedKey != null) {
        return referencedKey.toString();
      }
    }

    return attributeDefinition.string(get(attributeDefinition));
  }

  @Override
  public <T> T put(Attribute<T> attribute, T value) {
    return put(definition.attributes().definition(attribute), value);
  }

  @Override
  public Entity clearPrimaryKey() {
    definition.primaryKey().columns().forEach(this::remove);
    primaryKey = null;

    return this;
  }

  @Override
  public void save(Attribute<?> attribute) {
    removeOriginalValue(requireNonNull(attribute));
  }

  @Override
  public void save() {
    originalValues = null;
  }

  @Override
  public void revert(Attribute<?> attribute) {
    AttributeDefinition<?> attributeDefinition = definition.attributes().definition(attribute);
    if (isModifiedInternal(attribute)) {
      put((AttributeDefinition<Object>) attributeDefinition, original(attributeDefinition));
    }
  }

  @Override
  public void revert() {
    if (originalValues != null) {
      for (Attribute<?> attribute : new ArrayList<>(originalValues.keySet())) {
        revert(attribute);
      }
    }
  }

  @Override
  public <T> T remove(Attribute<T> attribute) {
    AttributeDefinition<T> attributeDefinition = definition.attributes().definition(attribute);
    T value = null;
    if (values.containsKey(attribute)) {
      value = (T) values.remove(attribute);
      removeOriginalValue(attribute);
      if (attributeDefinition instanceof ColumnDefinition && ((ColumnDefinition<?>) attributeDefinition).primaryKey()) {
        primaryKey = null;
      }
      if (attribute instanceof Column) {
        definition.foreignKeys().definitions((Column<?>) attribute).forEach(foreignKey -> remove(foreignKey.attribute()));
      }
    }

    return value;
  }

  @Override
  public Map<Attribute<?>, Object> set(Entity entity) {
    if (entity == this) {
      return emptyMap();
    }
    if (entity != null && !definition.entityType().equals(entity.entityType())) {
      throw new IllegalArgumentException("Entity of type: " + definition.entityType() + " expected, got: " + entity.entityType());
    }
    Map<Attribute<?>, Object> previousValues = new HashMap<>();
    definition.attributes().definitions().forEach(attributeDefinition -> previousValues.put(attributeDefinition.attribute(), get(attributeDefinition)));
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
    previousValues.forEach((attribute, previousValue) -> {
      if (!Objects.equals(get(attribute), previousValue)) {
        affectedAttributes.put(attribute, previousValue);
      }
    });

    return unmodifiableMap(affectedAttributes);
  }

  @Override
  public final Entity copy() {
    Entity entity = new DefaultEntity(definition, null, null);
    entity.set(this);

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
    if (!mutable()) {
      return this;
    }

    return new ImmutableEntity(this);
  }

  @Override
  public final boolean mutable() {
    return !(this instanceof ImmutableEntity);
  }

  @Override
  public final <T extends Entity> T castTo(Class<T> entityClass) {
    requireNonNull(entityClass);
    if (entityClass.isAssignableFrom(getClass())) {
      // no wrapping required
      return (T) this;
    }
    if (!entityType().entityClass().equals(entityClass)) {
      throw new IllegalArgumentException("entityClass " + entityType().entityClass() + " expected, got: " + entityClass);
    }

    return (T) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class[] {entityClass}, new EntityInvoker(this));
  }

  @Override
  public final boolean columnValuesEqual(Entity entity) {
    if (!definition.entityType().equals(requireNonNull(entity).entityType())) {
      throw new IllegalArgumentException("Entity of type " + definition.entityType() +
              " expected, got: " + entity.entityType());
    }

    return definition.columns().definitions().stream()
            .allMatch(column -> valueEqual(entity, column));
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
    definition.foreignKeys().definition(foreignKey);
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

  private <T> T get(AttributeDefinition<T> attributeDefinition) {
    if (attributeDefinition.derived()) {
      return derivedValue((DerivedAttributeDefinition<T>) attributeDefinition, false);
    }

    return (T) values.get(attributeDefinition.attribute());
  }

  private <T> T original(AttributeDefinition<T> attributeDefinition) {
    if (attributeDefinition.derived()) {
      return derivedValue((DerivedAttributeDefinition<T>) attributeDefinition, true);
    }
    if (isModifiedInternal(attributeDefinition.attribute())) {
      return (T) originalValues.get(attributeDefinition.attribute());
    }

    return get(attributeDefinition);
  }

  private <T> T put(AttributeDefinition<T> attributeDefinition, T value) {
    T newValue = validateAndPrepareValue(attributeDefinition, value);
    Attribute<T> attribute = attributeDefinition.attribute();
    boolean initialization = !values.containsKey(attribute);
    T previousValue = (T) values.put(attribute, newValue);
    if (!initialization && Objects.equals(previousValue, newValue)) {
      return newValue;
    }
    if (!initialization) {
      updateOriginalValue(attribute, newValue, previousValue);
    }
    if (attributeDefinition instanceof ColumnDefinition) {
      if (((ColumnDefinition<?>) attributeDefinition).primaryKey()) {
        primaryKey = null;
      }
      Column<T> column = (Column<T>) attribute;
      if (definition.foreignKeys().foreignKeyColumn(column)) {
        removeInvalidForeignKeyValues(column, newValue);
      }
    }
    toString = null;
    if (attributeDefinition instanceof ForeignKeyDefinition) {
      setForeignKeyValues((ForeignKeyDefinition) attributeDefinition, (Entity) newValue);
    }

    return previousValue;
  }

  private <T> boolean isNull(AttributeDefinition<T> attributeDefinition) {
    if (attributeDefinition instanceof ForeignKeyDefinition) {
      return isReferenceNull(((ForeignKeyDefinition) attributeDefinition).attribute());
    }

    return get(attributeDefinition) == null;
  }

  private boolean isReferenceNull(ForeignKey foreignKey) {
    List<ForeignKey.Reference<?>> references = foreignKey.references();
    if (references.size() == 1) {
      return isNull(references.get(0).column());
    }
    EntityDefinition referencedEntity = definition.foreignKeys().referencedBy(foreignKey);
    for (int i = 0; i < references.size(); i++) {
      ForeignKey.Reference<?> reference = references.get(i);
      ColumnDefinition<?> referencedColumn = referencedEntity.columns().definition(reference.referencedColumn());
      if (!referencedColumn.nullable() && isNull(reference.column())) {
        return true;
      }
    }

    return false;
  }

  private <T> T validateAndPrepareValue(AttributeDefinition<T> attributeDefinition, T value) {
    if (attributeDefinition.derived()) {
      throw new IllegalArgumentException("Can not set the value of a derived attribute");
    }
    if (attributeDefinition instanceof ItemColumnDefinition && value != null && !((ItemColumnDefinition<T>) attributeDefinition).valid(value)) {
      throw new IllegalArgumentException("Invalid item value: " + value + " for attribute " + attributeDefinition.attribute());
    }
    if (value != null && attributeDefinition instanceof ForeignKeyDefinition) {
      validateForeignKeyValue((ForeignKeyDefinition) attributeDefinition, (Entity) value);
    }

    return attributeDefinition.prepareValue(attributeDefinition.attribute().type().validateType(value));
  }

  private void validateForeignKeyValue(ForeignKeyDefinition foreignKeyDefinition, Entity foreignKeyValue) {
    EntityType referencedType = foreignKeyDefinition.attribute().referencedType();
    if (!Objects.equals(referencedType, foreignKeyValue.entityType())) {
      throw new IllegalArgumentException("Entity of type " + referencedType +
              " expected for foreign key " + foreignKeyDefinition + ", got: " + foreignKeyValue.entityType());
    }
    for (ForeignKey.Reference<?> reference : foreignKeyDefinition.references()) {
      throwIfModifiesReadOnlyReference(foreignKeyDefinition, foreignKeyValue, reference);
    }
  }

  private void throwIfModifiesReadOnlyReference(ForeignKeyDefinition foreignKeyDefinition, Entity foreignKeyValue,
                                                ForeignKey.Reference<?> reference) {
    boolean readOnlyReference = foreignKeyDefinition.readOnly(reference.column());
    if (readOnlyReference) {
      boolean containsValue = contains(reference.column());
      if (containsValue) {
        Object currentReferenceValue = get(reference.column());
        Object newReferenceValue = foreignKeyValue.get(reference.referencedColumn());
        if (!Objects.equals(currentReferenceValue, newReferenceValue)) {
          throw new IllegalArgumentException("Foreign key " + foreignKeyDefinition + " is not allowed to modify read-only reference: " +
                  reference.column() + " from " + currentReferenceValue + " to " + newReferenceValue);
        }
      }
    }
  }

  private <T> void removeInvalidForeignKeyValues(Column<T> column, T value) {
    for (ForeignKeyDefinition foreignKeyDefinition : definition.foreignKeys().definitions(column)) {
      Entity foreignKeyEntity = get(foreignKeyDefinition);
      if (foreignKeyEntity != null) {
        ForeignKey foreignKey = foreignKeyDefinition.attribute();
        //if the value isn't equal to the value in the foreign key,
        //that foreign key reference is invalid and is removed
        if (!Objects.equals(value, foreignKeyEntity.get(foreignKey.reference(column).referencedColumn()))) {
          remove(foreignKey);
          removeCachedReferencedKey(foreignKey);
        }
      }
    }
  }

  /**
   * Sets the values of the columns used in the reference to the corresponding values found in {@code referencedEntity}.
   * Example: EntityOne references EntityTwo via entityTwoId, after a call to this method the EntityOne.entityTwoId
   * attribute has the value of EntityTwos primary key attribute. If {@code referencedEntity} is null then
   * the corresponding reference values are set to null.
   * @param foreignKeyDefinition the foreign key definition
   * @param referencedEntity the referenced entity
   */
  private void setForeignKeyValues(ForeignKeyDefinition foreignKeyDefinition, Entity referencedEntity) {
    removeCachedReferencedKey(foreignKeyDefinition.attribute());
    List<ForeignKey.Reference<?>> references = foreignKeyDefinition.references();
    for (int i = 0; i < references.size(); i++) {
      ForeignKey.Reference<?> reference = references.get(i);
      if (!foreignKeyDefinition.readOnly(reference.column())) {
        AttributeDefinition<Object> columnDefinition = definition.columns().definition((Column<Object>) reference.column());
        put(columnDefinition, referencedEntity == null ? null : referencedEntity.get(reference.referencedColumn()));
      }
    }
  }

  /**
   * Creates and caches the key referenced by the given foreign key
   * @param foreignKey the foreign key
   * @return the referenced key or null if a valid key can not be created (null values for non-nullable columns)
   */
  private Key createAndCacheReferencedKey(ForeignKey foreignKey) {
    EntityDefinition referencedEntity = definition.foreignKeys().referencedBy(foreignKey);
    List<ForeignKey.Reference<?>> references = foreignKey.references();
    if (references.size() > 1) {
      return createAndCacheCompositeReferenceKey(foreignKey, references, referencedEntity);
    }

    return createAndCacheSingleReferenceKey(foreignKey, references.get(0), referencedEntity);
  }

  private Key createAndCacheCompositeReferenceKey(ForeignKey foreignKey,
                                                  List<ForeignKey.Reference<?>> references,
                                                  EntityDefinition referencedEntity) {
    Map<Column<?>, Object> keyValues = new HashMap<>(references.size());
    for (int i = 0; i < references.size(); i++) {
      ForeignKey.Reference<?> reference = references.get(i);
      ColumnDefinition<?> referencedColumn = referencedEntity.columns().definition(reference.referencedColumn());
      Object value = values.get(reference.column());
      if (value == null && !referencedColumn.nullable()) {
        return null;
      }
      keyValues.put(reference.referencedColumn(), value);
    }
    Set<Column<?>> referencedColumns = keyValues.keySet();
    List<Column<?>> primaryKeyColumns = referencedEntity.primaryKey().columns();
    boolean isPrimaryKey = referencedColumns.size() == primaryKeyColumns.size() && referencedColumns.containsAll(primaryKeyColumns);

    return cacheReferencedKey(foreignKey, new DefaultKey(referencedEntity, keyValues, isPrimaryKey));
  }

  private Key createAndCacheSingleReferenceKey(ForeignKey foreignKey,
                                               ForeignKey.Reference<?> reference,
                                               EntityDefinition referencedEntityDefinition) {
    Object value = values.get(reference.column());
    if (value == null) {
      return null;
    }

    List<Column<?>> primaryKeyColumns = referencedEntityDefinition.primaryKey().columns();
    boolean isPrimaryKey = primaryKeyColumns.size() == 1 && reference.referencedColumn().equals(primaryKeyColumns.get(0));

    return cacheReferencedKey(foreignKey,
            new DefaultKey(definition.foreignKeys().referencedBy(foreignKey),
                    reference.referencedColumn(), value, isPrimaryKey));
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
   * @param originalValues if true then the original values of the columns involved are used
   * @return a Key based on the values in this Entity instance
   */
  private Key createPrimaryKey(boolean originalValues) {
    if (definition.primaryKey().columns().isEmpty()) {
      return new DefaultKey(definition, emptyList(), true);
    }
    List<Column<?>> primaryKeyColumns = definition.primaryKey().columns();
    if (primaryKeyColumns.size() == 1) {
      return createSingleColumnPrimaryKey(primaryKeyColumns.get(0), originalValues);
    }

    return createMultiColumnPrimaryKey(primaryKeyColumns, originalValues);
  }

  private DefaultKey createSingleColumnPrimaryKey(Column<?> column, boolean originalValues) {
    return new DefaultKey(definition, column, originalValues ? original(column) : values.get(column), true);
  }

  private DefaultKey createMultiColumnPrimaryKey(List<Column<?>> primaryKeyColumn, boolean originalValues) {
    Map<Column<?>, Object> keyValues = new HashMap<>(primaryKeyColumn.size());
    for (int i = 0; i < primaryKeyColumn.size(); i++) {
      Column<?> column = primaryKeyColumn.get(i);
      keyValues.put(column, originalValues ? original(column) : values.get(column));
    }

    return new DefaultKey(definition, keyValues, true);
  }

  private <T> T derivedValue(DerivedAttributeDefinition<T> derivedDefinition, boolean originalValue) {
    return derivedDefinition.valueProvider().get(createSourceValues(derivedDefinition, originalValue));
  }

  private DerivedAttribute.SourceValues createSourceValues(DerivedAttributeDefinition<?> derivedDefinition, boolean originalValue) {
    List<Attribute<?>> sourceAttributes = derivedDefinition.sourceAttributes();
    if (sourceAttributes.size() == 1) {
      return new DefaultSourceValues(derivedDefinition.attribute(), createSingleAttributeSourceValueMap(sourceAttributes.get(0), originalValue));
    }

    return new DefaultSourceValues(derivedDefinition.attribute(), createMultiAttributeSourceValueMap(sourceAttributes, originalValue));
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

  private boolean valueEqual(Entity entity, AttributeDefinition<?> attributeDefinition) {
    Attribute<?> attribute = attributeDefinition.attribute();
    if (contains(attribute) != entity.contains(attribute)) {
      return false;
    }
    if (attribute.type().isByteArray()) {
      return Arrays.equals((byte[]) get(attributeDefinition), (byte[]) entity.get(attribute));
    }

    return Objects.equals(get(attributeDefinition), entity.get(attribute));
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
    for (ForeignKey foreignKey : definition.foreignKeys().get()) {
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
    stream.writeObject(definition.entityType().domainType().name());
    EntitySerializer.serialize(this, stream);
  }

  private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
    serializerForDomain((String) stream.readObject()).deserialize(this, stream);
  }

  private static Map<Attribute<?>, Object> validateTypes(EntityDefinition definition, Map<Attribute<?>, Object> values) {
    if (values != null && !values.isEmpty()) {
      for (Map.Entry<Attribute<?>, Object> valueEntry : values.entrySet()) {
        definition.attributes().definition((Attribute<Object>) valueEntry.getKey()).attribute().type().validateType(valueEntry.getValue());
      }
    }

    return values;
  }

  private static Map<Attribute<?>, Object> createValueMap(Key key) {
    Collection<Column<?>> columns = key.columns();
    Map<Attribute<?>, Object> values = new HashMap<>(columns.size());
    for (Column<?> column : columns) {
      values.put(column, key.get(column));
    }

    return values;
  }

  private static final class DefaultSourceValues implements DerivedAttribute.SourceValues {

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
                " is not specified as a source attribute for derived attribute: " + derivedAttribute);
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

        value = entityValue.castTo(entityValue.entityType().entityClass());
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
