/*
 * Copyright (c) 2008 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.common.Primitives;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.DenormalizedProperty;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;

/**
 * Represents a row in a database table.
 */
final class DefaultEntity implements Entity, Serializable {

  private static final long serialVersionUID = 1;

  private static final String ATTRIBUTE = "attribute";

  /**
   * Holds the values contained in this entity.
   */
  private Map<Attribute<?>, Object> values;

  /**
   * Holds the original value for attributes which values have changed since they were first set.
   */
  private Map<Attribute<?>, Object> originalValues;

  /**
   * Used to cache the return value of the frequently called toString(),
   * invalidated each time an attribute value changes
   */
  private String toString;

  /**
   * Caches the result of {@link #getReferencedKey} method
   */
  private Map<ForeignKey, Key> referencedKeyCache;

  /**
   * Keep a reference to this frequently referenced object
   */
  private EntityDefinition definition;

  /**
   * The primary key of this entity
   */
  private Key primaryKey;

  /**
   * Instantiates a new DefaultEntity
   * @param key the key
   */
  DefaultEntity(Key key) {
    this(requireNonNull(key, "key").getDefinition(), createValueMap(key), null);
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
    this.values = validate(requireNonNull(definition, "definition"), values == null ? new HashMap<>() : new HashMap<>(values));
    this.originalValues = validate(definition, originalValues == null ? null : new HashMap<>(originalValues));
    this.definition = definition;
  }

  @Override
  public EntityType getEntityType() {
    return definition.getEntityType();
  }

  @Override
  public EntityDefinition getDefinition() {
    return definition;
  }

  @Override
  public Key getPrimaryKey() {
    if (primaryKey == null) {
      primaryKey = initializePrimaryKey(false);
    }

    return primaryKey;
  }

  @Override
  public Key getOriginalPrimaryKey() {
    return initializePrimaryKey(true);
  }

  @Override
  public boolean isModified() {
    return isModified(false);
  }

  @Override
  public <T> T put(Attribute<T> attribute, T value) {
    return put(definition.getProperty(attribute), value);
  }

  @Override
  public <T> T get(Attribute<T> attribute) {
    return get(definition.getProperty(attribute));
  }

  @Override
  public <T> Optional<T> getOptional(Attribute<T> attribute) {
    return Optional.ofNullable(get(attribute));
  }

  @Override
  public boolean isNull(Attribute<?> attribute) {
    return isNull(definition.getProperty(attribute));
  }

  @Override
  public boolean isNotNull(Attribute<?> attribute) {
    return !isNull(attribute);
  }

  @Override
  public boolean isModified(Attribute<?> attribute) {
    definition.getProperty(requireNonNull(attribute, ATTRIBUTE));
    return originalValues != null && originalValues.containsKey(attribute);
  }

  @Override
  public boolean isNew() {
    Key key = getPrimaryKey();
    Key originalKey = getOriginalPrimaryKey();

    return key.isNull() || originalKey.isNull();
  }

  @Override
  public Entity getForeignKey(ForeignKey foreignKey) {
    Entity value = (Entity) values.get(foreignKey);
    if (value == null) {//possibly not loaded
      Key referencedKey = getReferencedKey(foreignKey);
      if (referencedKey != null) {
        return new DefaultEntity(referencedKey);
      }
    }

    return value;
  }

  @Override
  public boolean isLoaded(ForeignKey foreignKey) {
    definition.getForeignKeyProperty(foreignKey);
    return values.get(foreignKey) != null;
  }

  @Override
  public <T> String toString(Attribute<T> attribute) {
    return toString(definition.getProperty(attribute));
  }

  @Override
  public <T> String toString(Property<T> property) {
    if (!getEntityType().equals(requireNonNull(property).getEntityType())) {
      throw new IllegalArgumentException("Property " + property + " is not part of entity " + getEntityType());
    }
    Attribute<T> attribute = property.getAttribute();
    if (attribute instanceof ForeignKey && !isLoaded(((ForeignKey) attribute))) {
      Key referencedKey = getReferencedKey((ForeignKey) attribute);
      if (referencedKey != null) {
        return referencedKey.toString();
      }
    }

    return property.toString(get(property));
  }

  @Override
  public Entity clearPrimaryKey() {
    definition.getPrimaryKeyAttributes().forEach(this::remove);
    primaryKey = null;

    return this;
  }

  @Override
  public <T> T getOriginal(Attribute<T> attribute) {
    return getOriginal(definition.getProperty(attribute));
  }

  @Override
  public void save(Attribute<?> attribute) {
    removeOriginalValue(requireNonNull(attribute, ATTRIBUTE));
  }

  @Override
  public void saveAll() {
    originalValues = null;
  }

  @Override
  public void revert(Attribute<?> attribute) {
    if (isModified(attribute)) {
      Attribute<Object> objectAttribute = (Attribute<Object>) attribute;
      put(objectAttribute, getOriginal(objectAttribute));
    }
  }

  @Override
  public void revertAll() {
    for (Attribute<?> attribute : new ArrayList<>(values.keySet())) {
      revert(attribute);
    }
  }

  @Override
  public <T> T remove(Attribute<T> attribute) {
    if (values.containsKey(requireNonNull(attribute, ATTRIBUTE))) {
      T value = (T) values.remove(attribute);
      removeOriginalValue(attribute);
      if (definition.isForeignKeyAttribute(attribute)) {
        definition.getForeignKeyProperties(attribute).forEach(foreignKeyProperty -> remove(foreignKeyProperty.getAttribute()));
      }

      return value;
    }

    return null;
  }

  @Override
  public Map<Attribute<?>, Object> setAs(Entity entity) {
    if (entity == this) {
      return Collections.emptyMap();
    }
    if (entity != null && !definition.getEntityType().equals(entity.getEntityType())) {
      throw new IllegalArgumentException("Entity of type: " + definition.getEntityType() + " expected, got: " + entity.getEntityType());
    }
    Map<Property<?>, Object> previousValues = new HashMap<>();
    definition.getProperties().forEach(property -> previousValues.put(property, get(property)));
    clear();
    if (entity != null) {
      entity.entrySet().forEach(attributeValue -> values.put(attributeValue.getKey(), attributeValue.getValue()));
      if (entity.isModified()) {
        originalValues = new HashMap<>();
        entity.originalEntrySet().forEach(entry -> originalValues.put(entry.getKey(), entry.getValue()));
      }
    }
    Map<Attribute<?>, Object> affectedAttributes = new HashMap<>();
    previousValues.forEach((property, previousValue) -> {
      if (!Objects.equals(get(property), previousValue)) {
        affectedAttributes.put(property.getAttribute(), previousValue);
      }
    });

    return unmodifiableMap(affectedAttributes);
  }

  @Override
  public Entity copy() {
    Entity entity = new DefaultEntity(definition, null, null);
    entity.setAs(this);

    return entity;
  }

  @Override
  public Builder copyBuilder() {
    return new DefaultEntityBuilder(definition, values, originalValues);
  }

  @Override
  public Entity deepCopy() {
    Entity copy = copy();
    for (ForeignKey foreignKey : definition.getForeignKeys()) {
      Entity foreignKeyValue = copy.get(foreignKey);
      if (foreignKeyValue != null) {
        copy.put(foreignKey, foreignKeyValue.deepCopy());
      }
    }

    return copy;
  }

  @Override
  public <T extends Entity> T castTo(Class<T> entityClass) {
    requireNonNull(entityClass, "entityClass");
    if (entityClass.isAssignableFrom(getClass())) {
      // no wrapping required
      return (T) this;
    }
    if (!getEntityType().getEntityClass().equals(entityClass)) {
      throw new IllegalArgumentException("entityClass " + getEntityType().getEntityClass() + " expected, got: " + entityClass);
    }

    return (T) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class[] {entityClass}, new EntityInvoker(this));
  }

  @Override
  public boolean columnValuesEqual(Entity entity) {
    if (!definition.getEntityType().equals(requireNonNull(entity, "entity").getEntityType())) {
      throw new IllegalArgumentException("Entity of type " + definition.getEntityType() +
              " expected, got: " + entity.getEntityType());
    }

    return definition.getColumnProperties().stream()
            .allMatch(property -> valueEqual(entity, property));
  }

  /**
   * @param obj the object to compare with
   * @return true if the given object is an Entity and its primary key is equal to this ones
   */
  @Override
  public boolean equals(Object obj) {
    return this == obj || obj instanceof Entity && getPrimaryKey().equals(((Entity) obj).getPrimaryKey());
  }

  /**
   * @param entity the entity to compare with
   * @return the compare result from comparing {@code entity} with this Entity instance
   * @see EntityDefinition.Builder#comparator(java.util.Comparator)
   */
  @Override
  public int compareTo(Entity entity) {
    return definition.getComparator().compare(this, entity);
  }

  /**
   * Returns the hash code of the primary key
   */
  @Override
  public int hashCode() {
    return getPrimaryKey().hashCode();
  }

  /**
   * @return a string representation of this entity
   * @see EntityDefinition.Builder#stringFactory(java.util.function.Function)
   * @see EntityDefinition#getStringFactory()
   */
  @Override
  public String toString() {
    if (toString == null) {
      toString = definition.getStringFactory().apply(this);
    }

    return toString;
  }

  @Override
  public Key getReferencedKey(ForeignKey foreignKey) {
    definition.getForeignKeyProperty(foreignKey);
    Key cachedReferencedKey = getCachedReferencedKey(foreignKey);
    if (cachedReferencedKey != null) {
      return cachedReferencedKey;
    }

    return initializeAndCacheReferencedKey(foreignKey);
  }

  @Override
  public boolean contains(Attribute<?> attribute) {
    return values.containsKey(requireNonNull(attribute, ATTRIBUTE));
  }

  @Override
  public Set<Map.Entry<Attribute<?>, Object>> entrySet() {
    return unmodifiableSet(values.entrySet());
  }

  @Override
  public Set<Map.Entry<Attribute<?>, Object>> originalEntrySet() {
    if (originalValues == null) {
      return emptySet();
    }

    return unmodifiableSet(originalValues.entrySet());
  }

  @Override
  public boolean isForeignKeyNull(ForeignKey foreignKey) {
    List<ForeignKey.Reference<?>> references = foreignKey.getReferences();
    if (references.size() == 1) {
      return isNull(references.get(0).getAttribute());
    }
    EntityDefinition referencedDefinition = definition.getReferencedEntityDefinition(foreignKey);
    for (int i = 0; i < references.size(); i++) {
      ForeignKey.Reference<?> reference = references.get(i);
      ColumnProperty<?> referencedProperty = referencedDefinition.getColumnProperty(reference.getReferencedAttribute());
      if (!referencedProperty.isNullable() && isNull(reference.getAttribute())) {
        return true;
      }
    }

    return false;
  }

  private void clear() {
    values.clear();
    originalValues = null;
    primaryKey = null;
    referencedKeyCache = null;
    toString = null;
  }

  private <T> T get(Property<T> property) {
    if (property instanceof DerivedProperty) {
      return getDerivedValue((DerivedProperty<T>) property, false);
    }

    return (T) values.get(property.getAttribute());
  }

  private <T> T getOriginal(Property<T> property) {
    if (property instanceof DerivedProperty) {
      return getDerivedValue((DerivedProperty<T>) property, true);
    }
    if (isModified(property.getAttribute())) {
      return (T) originalValues.get(property.getAttribute());
    }

    return get(property);
  }

  private <T> T put(Property<T> property, T value) {
    T newValue = validateAndPrepareForPut(property, value);
    Attribute<T> attribute = property.getAttribute();
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
      propagateForeignKeyValues((ForeignKeyProperty) property, (Entity) newValue);
    }

    return previousValue;
  }

  /**
   * Returns true if the value associated with the given property is null. In case of foreign key properties
   * the value of the underlying reference property is checked.
   * @param property the property
   * @return true if the value associated with the property is null
   */
  private <T> boolean isNull(Property<T> property) {
    if (property instanceof ForeignKeyProperty) {
      return isForeignKeyNull(((ForeignKeyProperty) property).getAttribute());
    }

    return get(property) == null;
  }

  private <T> T validateAndPrepareForPut(Property<T> property, T value) {
    if (property instanceof DerivedProperty) {
      throw new IllegalArgumentException("Can not set the value of a derived property");
    }
    if (property instanceof ItemProperty && value != null && !((ItemProperty<T>) property).isValid(value)) {
      throw new IllegalArgumentException("Invalid item value: " + value + " for property " + property.getAttribute());
    }
    if (value != null && property instanceof ForeignKeyProperty) {
      validateForeignKeyValue((ForeignKeyProperty) property, (Entity) value);
    }

    return property.prepareValue(property.getAttribute().validateType(value));
  }

  private void validateForeignKeyValue(ForeignKeyProperty property, Entity foreignKeyValue) {
    EntityType referencedEntityType = property.getReferencedEntityType();
    if (!Objects.equals(referencedEntityType, foreignKeyValue.getEntityType())) {
      throw new IllegalArgumentException("Entity of type " + referencedEntityType +
              " expected for property " + property + ", got: " + foreignKeyValue.getEntityType());
    }
    property.getReferences().forEach(reference -> throwIfModifiesReadOnlyReference(property, foreignKeyValue, reference));
  }

  private void throwIfModifiesReadOnlyReference(ForeignKeyProperty property, Entity foreignKeyValue,
                                                ForeignKey.Reference<?> reference) {
    boolean readOnlyReference = property.isReadOnly(reference.getAttribute());
    if (readOnlyReference) {
      boolean containsValue = contains(reference.getAttribute());
      if (containsValue) {
        Object currentReferenceValue = get(reference.getAttribute());
        Object newReferenceValue = foreignKeyValue.get(reference.getReferencedAttribute());
        if (!Objects.equals(currentReferenceValue, newReferenceValue)) {
          throw new IllegalArgumentException("Foreign key " + property + " is not allowed to modify read-only reference: " +
                  reference.getAttribute() + " from " + currentReferenceValue + " to " + newReferenceValue);
        }
      }
    }
  }

  private void propagateForeignKeyValues(ForeignKeyProperty foreignKeyProperty, Entity newValue) {
    setForeignKeyValues(foreignKeyProperty, newValue);
    if (definition.hasDenormalizedProperties()) {
      setDenormalizedValues(foreignKeyProperty, newValue);
    }
  }

  private <T> void removeInvalidForeignKeyValues(Attribute<T> attribute, T value) {
    for (ForeignKeyProperty foreignKeyProperty : definition.getForeignKeyProperties(attribute)) {
      Entity foreignKeyEntity = get(foreignKeyProperty);
      if (foreignKeyEntity != null) {
        ForeignKey foreignKey = foreignKeyProperty.getAttribute();
        //if the value isn't equal to the value in the foreign key,
        //that foreign key reference is invalid and is removed
        if (!Objects.equals(value, foreignKeyEntity.get(foreignKey.getReference(attribute).getReferencedAttribute()))) {
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
    removeCachedReferencedKey(foreignKeyProperty.getAttribute());
    List<ForeignKey.Reference<?>> references = foreignKeyProperty.getReferences();
    for (int i = 0; i < references.size(); i++) {
      ForeignKey.Reference<?> reference = references.get(i);
      if (!foreignKeyProperty.isReadOnly(reference.getAttribute())) {
        Property<Object> columnProperty = definition.getColumnProperty((Attribute<Object>) reference.getAttribute());
        put(columnProperty, referencedEntity == null ? null : referencedEntity.get(reference.getReferencedAttribute()));
      }
    }
  }

  /**
   * Sets the denormalized property values
   * @param foreignKeyProperty the foreign key property from which to denormalize the values
   * @param referencedEntity the foreign key entity containing the values to denormalize
   */
  private void setDenormalizedValues(ForeignKeyProperty foreignKeyProperty, Entity referencedEntity) {
    List<DenormalizedProperty<?>> denormalizedProperties =
            definition.getDenormalizedProperties(foreignKeyProperty.getAttribute());
    if (denormalizedProperties != null) {
      for (int i = 0; i < denormalizedProperties.size(); i++) {
        DenormalizedProperty<Object> denormalizedProperty = (DenormalizedProperty<Object>) denormalizedProperties.get(i);
        put(denormalizedProperty, referencedEntity == null ? null :
                referencedEntity.get(denormalizedProperty.getDenormalizedAttribute()));
      }
    }
  }

  /**
   * Creates and caches the primary key referenced by the given foreign key
   * @param foreignKey the foreign key
   * @return the referenced primary key or null if a valid key can not be created (null values for non-nullable properties)
   */
  private Key initializeAndCacheReferencedKey(ForeignKey foreignKey) {
    EntityDefinition referencedEntityDefinition = definition.getReferencedEntityDefinition(foreignKey);
    List<ForeignKey.Reference<?>> references = foreignKey.getReferences();
    if (references.size() > 1) {
      return initializeAndCacheCompositeReferenceKey(foreignKey, references, referencedEntityDefinition);
    }

    return initializeAndCacheSingleReferenceKey(foreignKey, references.get(0), referencedEntityDefinition);
  }

  private Key initializeAndCacheCompositeReferenceKey(ForeignKey foreignKey,
                                                      List<ForeignKey.Reference<?>> references,
                                                      EntityDefinition referencedEntityDefinition) {
    Map<Attribute<?>, Object> keyValues = new HashMap<>(references.size());
    for (int i = 0; i < references.size(); i++) {
      ForeignKey.Reference<?> reference = references.get(i);
      ColumnProperty<?> referencedProperty = referencedEntityDefinition.getColumnProperty(reference.getReferencedAttribute());
      Object value = values.get(reference.getAttribute());
      if (value == null && !referencedProperty.isNullable()) {
        return null;
      }
      keyValues.put(reference.getReferencedAttribute(), value);
    }
    Set<Attribute<?>> referencedAttributes = keyValues.keySet();
    List<Attribute<?>> primaryKeyAttributes = referencedEntityDefinition.getPrimaryKeyAttributes();
    boolean isPrimaryKey = referencedAttributes.size() == primaryKeyAttributes.size() && referencedAttributes.containsAll(primaryKeyAttributes);

    return cacheReferencedKey(foreignKey, new DefaultKey(referencedEntityDefinition, keyValues, isPrimaryKey));
  }

  private Key initializeAndCacheSingleReferenceKey(ForeignKey foreignKey,
                                                   ForeignKey.Reference<?> reference,
                                                   EntityDefinition referencedEntityDefinition) {
    Object value = values.get(reference.getAttribute());
    if (value == null) {
      return null;
    }

    boolean isPrimaryKey = reference.getReferencedAttribute().equals(referencedEntityDefinition.getPrimaryKeyAttributes().get(0));

    return cacheReferencedKey(foreignKey,
            new DefaultKey(definition.getReferencedEntityDefinition(foreignKey),
                    reference.getReferencedAttribute(), value, isPrimaryKey));
  }

  private Key cacheReferencedKey(ForeignKey foreignKey, Key referencedPrimaryKey) {
    if (referencedKeyCache == null) {
      referencedKeyCache = new HashMap<>();
    }
    referencedKeyCache.put(foreignKey, referencedPrimaryKey);

    return referencedPrimaryKey;
  }

  private Key getCachedReferencedKey(ForeignKey foreignKey) {
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
   * Initializes a Key for this Entity instance
   * @param originalValues if true then the original values of the properties involved are used
   * @return a Key based on the values in this Entity instance
   */
  private Key initializePrimaryKey(boolean originalValues) {
    if (!definition.hasPrimaryKey()) {
      return new DefaultKey(definition, emptyList(), true);
    }
    List<Attribute<?>> primaryKeyAttributes = definition.getPrimaryKeyAttributes();
    if (primaryKeyAttributes.size() > 1) {
      Map<Attribute<?>, Object> keyValues = new HashMap<>(primaryKeyAttributes.size());
      for (int i = 0; i < primaryKeyAttributes.size(); i++) {
        Attribute<?> attribute = primaryKeyAttributes.get(i);
        keyValues.put(attribute, originalValues ? getOriginal(attribute) : values.get(attribute));
      }

      return new DefaultKey(definition, keyValues, true);
    }

    Attribute<?> attribute = primaryKeyAttributes.get(0);

    return new DefaultKey(definition, attribute, originalValues ? getOriginal(attribute) : values.get(attribute), true);
  }

  private <T> T getDerivedValue(DerivedProperty<T> derivedProperty, boolean originalValue) {
    return derivedProperty.getValueProvider().get(getSourceValues(derivedProperty, originalValue));
  }

  private DerivedProperty.SourceValues getSourceValues(DerivedProperty<?> derivedProperty, boolean originalValues) {
    List<Attribute<?>> sourceAttributes = derivedProperty.getSourceAttributes();
    if (sourceAttributes.size() == 1) {
      Attribute<?> sourceAttribute = sourceAttributes.get(0);

      return new DefaultSourceValues(derivedProperty.getAttribute(),
              singletonMap(sourceAttribute, originalValues ? getOriginal(sourceAttribute) : get(sourceAttribute)));
    }
    else {
      Map<Attribute<?>, Object> valueMap = new HashMap<>(sourceAttributes.size());
      for (int i = 0; i < sourceAttributes.size(); i++) {
        Attribute<?> sourceAttribute = sourceAttributes.get(i);
        valueMap.put(sourceAttribute, originalValues ? getOriginal(sourceAttribute) : get(sourceAttribute));
      }

      return new DefaultSourceValues(derivedProperty.getAttribute(), valueMap);
    }
  }

  private boolean isModified(boolean overrideModifiesEntity) {
    if (originalValues != null && !originalValues.isEmpty()) {
      for (Attribute<?> attribute : originalValues.keySet()) {
        Property<?> property = definition.getProperty(attribute);
        if (property instanceof ColumnProperty) {
          ColumnProperty<?> columnProperty = (ColumnProperty<?>) property;
          if (columnProperty.isInsertable() && columnProperty.isUpdatable()) {
            return true;
          }
        }
        if (property instanceof TransientProperty) {
          return overrideModifiesEntity || ((TransientProperty<?>) property).isModifiesEntity();
        }
      }
    }

    return false;
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
    boolean modified = isModified(attribute);
    if (modified && Objects.equals(getOriginal(attribute), value)) {
      removeOriginalValue(attribute);//we're back to the original value
    }
    else if (!modified) {//only the first original value is kept
      setOriginalValue(attribute, previousValue);
    }
  }

  private boolean valueEqual(Entity entity, ColumnProperty<?> property) {
    Attribute<?> attribute = property.getAttribute();
    if (contains(attribute) != entity.contains(attribute)) {
      return false;
    }
    if (attribute.isByteArray()) {
      return Arrays.equals((byte[]) get(property), (byte[]) entity.get(attribute));
    }

    return Objects.equals(get(property), entity.get(attribute));
  }

  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.writeObject(definition.getDomainName());
    stream.writeObject(definition.getEntityType().getName());
    stream.writeInt(definition.getSerializationVersion());
    boolean isModified = isModified(true);
    stream.writeBoolean(isModified);
    List<Property<?>> properties = definition.getProperties();
    for (int i = 0; i < properties.size(); i++) {
      Property<?> property = properties.get(i);
      if (!(property instanceof DerivedProperty)) {
        Attribute<?> attribute = property.getAttribute();
        boolean containsValue = values.containsKey(attribute);
        stream.writeBoolean(containsValue);
        if (containsValue) {
          stream.writeObject(values.get(attribute));
          if (isModified) {
            boolean valueModified = originalValues.containsKey(attribute);
            stream.writeBoolean(valueModified);
            if (valueModified) {
              stream.writeObject(originalValues.get(attribute));
            }
          }
        }
      }
    }
  }

  private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
    Entities entities = DefaultEntities.getEntities((String) stream.readObject());
    definition = entities.getDefinition((String) stream.readObject());
    if (definition.getSerializationVersion() != stream.readInt()) {
      throw new IllegalArgumentException("Entity type '" + definition.getEntityType() + "' can not be deserialized due to version difference");
    }
    boolean isModified = stream.readBoolean();
    values = new HashMap<>();
    List<Property<?>> properties = definition.getProperties();
    for (int i = 0; i < properties.size(); i++) {
      Property<Object> property = (Property<Object>) properties.get(i);
      if (!(property instanceof DerivedProperty)) {
        boolean containsValue = stream.readBoolean();
        if (containsValue) {
          Attribute<Object> attribute = property.getAttribute();
          values.put(attribute, attribute.validateType(stream.readObject()));
          if (isModified && stream.readBoolean()) {
            setOriginalValue(attribute, attribute.validateType(stream.readObject()));
          }
        }
      }
    }
  }

  private static Map<Attribute<?>, Object> validate(EntityDefinition definition, Map<Attribute<?>, Object> values) {
    if (values != null && !values.isEmpty()) {
      for (Map.Entry<Attribute<?>, Object> valueEntry : values.entrySet()) {
        definition.getProperty((Attribute<Object>) valueEntry.getKey()).getAttribute().validateType(valueEntry.getValue());
      }
    }

    return values;
  }

  private static Map<Attribute<?>, Object> createValueMap(Key key) {
    Collection<Attribute<?>> attributes = key.getAttributes();
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
        Attribute<?> attribute = entity.definition.getGetterAttribute(method);
        if (attribute != null) {
          return getValue(attribute, method.getReturnType());
        }
      }
      else if (method.getParameterCount() == 1) {
        Attribute<?> attribute = entity.definition.getSetterAttribute(method);
        if (attribute != null) {
          return setValue(attribute, args[0]);
        }
      }
      if (method.isDefault()) {
        return entity.definition.getDefaultMethodHandle(method).bindTo(proxy).invokeWithArguments(args);
      }

      return method.invoke(entity, args);
    }

    private Object getValue(Attribute<?> attribute, Class<?> getterReturnType) {
      Object value;
      if (attribute instanceof ForeignKey) {
        value = entity.getForeignKey((ForeignKey) attribute);
      }
      else {
        value = entity.get(attribute);
      }
      if (value instanceof Entity) {
        Entity entityValue = (Entity) value;

        value = entityValue.castTo(entityValue.getEntityType().getEntityClass());
      }
      if (getterReturnType.equals(Optional.class)) {
        return Optional.ofNullable(value);
      }
      if (value == null && getterReturnType.isPrimitive()) {
        return Primitives.getDefaultValue(getterReturnType);
      }

      return value;
    }

    private Object setValue(Attribute<?> attribute, Object value) {
      entity.put((Attribute<Object>) attribute, value);

      return null;
    }
  }
}
