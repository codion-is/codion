/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.common.Util;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.DenormalizedProperty;
import is.codion.framework.domain.property.DerivedProperty;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.domain.property.MirrorProperty;
import is.codion.framework.domain.property.Property;
import is.codion.framework.domain.property.TransientProperty;
import is.codion.framework.domain.property.ValueListProperty;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;

/**
 * Represents a row in a database table.
 */
final class DefaultEntity implements Entity {

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
   * invalidated each time a attribute value changes
   */
  private String toString;

  /**
   * Caches the result of {@link #getReferencedKey} method
   */
  private Map<Attribute<Entity>, Key> referencedKeyCache;

  /**
   * Keep a reference to this frequently referenced object
   */
  private EntityDefinition definition;

  /**
   * The primary key of this entity
   */
  private Key key;

  /**
   * Instantiates a new DefaultEntity
   * @param definition the entity definition
   * @param key the primary key
   */
  DefaultEntity(final EntityDefinition definition, final Key key) {
    this(definition, createValueMap(key), null);
    this.key = key;
  }

  /**
   * Instantiates a new DefaultEntity based on the given values.
   * @param definition the entity definition
   * @param values the initial values, may be null
   * @param originalValues the original values, may be null
   * @throws IllegalArgumentException in case any of the properties are not part of the entity.
   */
  DefaultEntity(final EntityDefinition definition, final Map<Attribute<?>, Object> values, final Map<Attribute<?>, Object> originalValues) {
    this.values = validatePropertiesAndValues(definition, values == null ? new HashMap<>() : values);
    this.originalValues = validatePropertiesAndValues(definition, originalValues);
    this.definition = definition;
  }

  @Override
  public EntityType getEntityType() {
    return definition.getEntityType();
  }

  @Override
  public Key getKey() {
    if (key == null) {
      key = initializeKey(false);
    }

    return key;
  }

  @Override
  public Key getOriginalKey() {
    return initializeKey(true);
  }

  @Override
  public boolean is(final EntityType entityType) {
    return definition.getEntityType().equals(entityType);
  }

  @Override
  public boolean isModified() {
    return isModifiedInternal(false);
  }

  @Override
  public <T> T put(final Attribute<T> attribute, final T value) {
    return putInternal(definition.getProperty(attribute), value);
  }

  @Override
  public <T> T get(final Attribute<T> attribute) {
    return get(definition.getProperty(attribute));
  }

  @Override
  public <T> boolean isNull(final Attribute<T> attribute) {
    return isNull(definition.getProperty(attribute));
  }

  @Override
  public <T> boolean isNotNull(final Attribute<T> attribute) {
    return !isNull(attribute);
  }

  @Override
  public final <T> boolean isModified(final Attribute<T> attribute) {
    requireNonNull(attribute, ATTRIBUTE);
    return originalValues != null && originalValues.containsKey(attribute);
  }

  @Override
  public Entity getForeignKey(final Attribute<Entity> entityAttribute) {
    final Entity value = (Entity) values.get(entityAttribute);
    if (value == null) {//possibly not loaded
      final Key referencedKey = getReferencedKey(entityAttribute);
      if (referencedKey != null) {
        return new DefaultEntity(definition.getForeignDefinition(entityAttribute), referencedKey);
      }
    }

    return value;
  }

  @Override
  public boolean isLoaded(final Attribute<Entity> foreignKeyAttribute) {
    return values.get(definition.getForeignKeyProperty(foreignKeyAttribute).getAttribute()) != null;
  }

  @Override
  public <T> String getAsString(final Attribute<T> attribute) {
    return getAsString(definition.getProperty(attribute));
  }

  @Override
  public void clearKeyValues() {
    final List<Attribute<?>> primaryKeyAttributes = definition.getPrimaryKeyAttributes();
    for (int i = 0; i < primaryKeyAttributes.size(); i++) {
      remove(primaryKeyAttributes.get(i));
    }
    this.key = null;
  }

  @Override
  public <T> T getOriginal(final Attribute<T> attribute) {
    return getOriginal(definition.getProperty(attribute));
  }

  @Override
  public <T> void save(final Attribute<T> attribute) {
    requireNonNull(attribute, ATTRIBUTE);
    removeOriginalValue(attribute);
  }

  @Override
  public final void saveAll() {
    originalValues = null;
  }

  @Override
  public <T> void revert(final Attribute<T> attribute) {
    if (isModified(attribute)) {
      put(attribute, getOriginal(attribute));
    }
  }

  @Override
  public void revertAll() {
    for (final Attribute<?> attribute : values.keySet()) {
      revert(attribute);
    }
  }

  @Override
  public <T> T remove(final Attribute<T> attribute) {
    if (values.containsKey(requireNonNull(attribute, ATTRIBUTE))) {
      final T value = (T) values.remove(attribute);
      removeOriginalValue(attribute);

      return value;
    }

    return null;
  }

  @Override
  public final Collection<Attribute<?>> setAs(final Entity entity) {
    if (entity == this) {
      return Collections.emptyList();
    }
    if (entity != null && !definition.getEntityType().equals(entity.getEntityType())) {
      throw new IllegalArgumentException("Entity of type: " + definition.getEntityType() + " expected, got: " + entity.getEntityType());
    }
    final Set<Attribute<?>> affectedAttributes = new HashSet<>(values.keySet());
    clear();
    if (entity != null) {
      for (final Map.Entry<Attribute<?>, Object> entry : entity.entrySet()) {
        values.put(entry.getKey(), entry.getValue());
        affectedAttributes.add(entry.getKey());
      }
      if (entity.isModified()) {
        originalValues = new HashMap<>();
        for (final Map.Entry<Attribute<?>, Object> entry : entity.originalEntrySet()) {
          originalValues.put(entry.getKey(), entry.getValue());
        }
      }
    }

    return affectedAttributes;
  }

  @Override
  public boolean valuesEqual(final Entity entity) {
    requireNonNull(entity, "entity");

    return definition.getColumnProperties().stream().allMatch(property -> {
      if (property.getAttribute().isBlob()) {
        return Arrays.equals((byte[]) get(property), (byte[]) entity.get(property.getAttribute()));
      }

      return Objects.equals(get(property), entity.get(property.getAttribute()));
    });
  }

  /**
   * @param obj the object to compare with
   * @return true if the given object is an Entity and its primary key is equal to this ones
   */
  @Override
  public boolean equals(final Object obj) {
    return this == obj || obj instanceof Entity && getKey().equals(((Entity) obj).getKey());
  }

  /**
   * @param entity the entity to compare with
   * @return the compare result from comparing {@code entity} with this Entity instance
   * @see EntityDefinition.Builder#comparator(java.util.Comparator)
   */
  @Override
  public int compareTo(final Entity entity) {
    return definition.getComparator().compare(this, entity);
  }

  /**
   * Returns the hash code of the primary key
   */
  @Override
  public int hashCode() {
    return getKey().hashCode();
  }

  /**
   * @return a string representation of this entity
   * @see EntityDefinition.Builder#stringProvider(java.util.function.Function)
   * @see EntityDefinition#getStringProvider()
   */
  @Override
  public String toString() {
    if (toString == null) {
      toString = definition.getStringProvider().apply(this);
    }

    return toString;
  }

  @Override
  public Key getReferencedKey(final Attribute<Entity> foreignKeyAttribute) {
    requireNonNull(foreignKeyAttribute, "foreignKeyAttribute");
    final ForeignKeyProperty foreignKeyProperty = definition.getForeignKeyProperty(foreignKeyAttribute);
    final Key cachedReferencedKey = getCachedReferencedKey(foreignKeyProperty.getAttribute());
    if (cachedReferencedKey != null) {
      return cachedReferencedKey;
    }

    return initializeAndCacheReferencedKey(foreignKeyProperty);
  }

  @Override
  public <T> boolean containsKey(final Attribute<T> attribute) {
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
  public boolean isForeignKeyNull(final Attribute<Entity> foreignKeyAttribute) {
    requireNonNull(foreignKeyAttribute, "foreignKeyAttribute");
    final ForeignKeyProperty foreignKeyProperty = definition.getForeignKeyProperty(foreignKeyAttribute);
    final List<Attribute<?>> attributes = foreignKeyProperty.getColumnAttributes();
    if (attributes.size() == 1) {
      return isNull(attributes.get(0));
    }
    final List<ColumnProperty<?>> foreignProperties =
            definition.getForeignDefinition(foreignKeyProperty.getAttribute()).getPrimaryKeyProperties();
    for (int i = 0; i < attributes.size(); i++) {
      if (!foreignProperties.get(i).isNullable() && isNull(attributes.get(i))) {
        return true;
      }
    }

    return false;
  }

  private void clear() {
    values.clear();
    if (originalValues != null) {
      originalValues = null;
    }
    key = null;
    referencedKeyCache = null;
    toString = null;
  }

  private <T> T get(final Property<T> property) {
    requireNonNull(property, "property");
    if (property instanceof MirrorProperty) {
      return get(definition.getProperty(property.getAttribute()).getAttribute());
    }
    if (property instanceof DerivedProperty) {
      return getDerivedValue((DerivedProperty<T>) property);
    }

    return (T) values.get(property.getAttribute());
  }

  private <T> T getOriginal(final Property<T> property) {
    if (isModified(property.getAttribute())) {
      return (T) originalValues.get(property.getAttribute());
    }

    return get(property);
  }

  private <T> String getAsString(final Property<T> property) {
    if (property instanceof ValueListProperty) {
      return ((ValueListProperty<T>) property).getCaption(get(property));
    }
    if (property instanceof ForeignKeyProperty && !isLoaded(((ForeignKeyProperty) property).getAttribute())) {
      final Key referencedKey = getReferencedKey(((ForeignKeyProperty) property).getAttribute());
      if (referencedKey != null) {
        return referencedKey.toString();
      }
    }

    return property.formatValue(get(property));
  }

  private <T> T putInternal(final Property<T> property, final T value) {
    requireNonNull(property, ATTRIBUTE);
    final T newValue = validateAndPrepareForPut(property, value);
    final boolean initialization = !values.containsKey(property.getAttribute());
    final T previousValue = (T) values.put(property.getAttribute(), newValue);
    if (!initialization && Objects.equals(previousValue, newValue)) {
      return newValue;
    }
    if (!initialization) {
      updateOriginalValue(property.getAttribute(), newValue, previousValue);
    }
    if (property instanceof ColumnProperty) {
      final ColumnProperty<T> columnProperty = (ColumnProperty<T>) property;
      if (columnProperty.isPrimaryKeyColumn()) {
        key = null;
      }
      if (columnProperty.isForeignKeyColumn()) {
        removeInvalidForeignKeyValues(columnProperty.getAttribute(), newValue);
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
  private <T> boolean isNull(final Property<T> property) {
    if (property instanceof ForeignKeyProperty) {
      return isForeignKeyNull(((ForeignKeyProperty) property).getAttribute());
    }

    return get(property) == null;
  }

  private <T> T validateAndPrepareForPut(final Property<T> property, final T value) {
    if (property instanceof DerivedProperty) {
      throw new IllegalArgumentException("Can not set the value of a derived property");
    }
    if (property instanceof ValueListProperty && value != null && !((ValueListProperty<T>) property).isValid(value)) {
      throw new IllegalArgumentException("Invalid value list value: " + value + " for property " + property.getAttribute());
    }
    if (value != null && property instanceof ForeignKeyProperty) {
      validateForeignKeyValue((ForeignKeyProperty) property, (Entity) value);
    }

    return property.prepareValue(property.getAttribute().validateType(value));
  }

  private void validateForeignKeyValue(final ForeignKeyProperty property, final Entity value) {
    final Entity entity = value;
    final EntityType referencedEntityType = property.getReferencedEntityType();
    if (!Objects.equals(referencedEntityType, entity.getEntityType())) {
      throw new IllegalArgumentException("Entity of type " + referencedEntityType +
              " expected for property " + this + ", got: " + entity.getEntityType());
    }
  }

  private void propagateForeignKeyValues(final ForeignKeyProperty foreignKeyProperty, final Entity newValue) {
    setForeignKeyValues(foreignKeyProperty, newValue);
    if (definition.hasDenormalizedProperties()) {
      setDenormalizedValues(foreignKeyProperty, newValue);
    }
  }

  private <T> void removeInvalidForeignKeyValues(final Attribute<T> attribute, final T value) {
    final List<ForeignKeyProperty> propertyForeignKeyProperties = definition.getForeignKeyProperties(attribute);
    for (final ForeignKeyProperty foreignKeyProperty : propertyForeignKeyProperties) {
      final Entity foreignKeyEntity = get(foreignKeyProperty);
      if (foreignKeyEntity != null) {
        final Key referencedKey = foreignKeyEntity.getKey();
        final Attribute<T> keyAttribute = (Attribute<T>) referencedKey.getAttributes()
                .get(foreignKeyProperty.getColumnAttributes().indexOf(attribute));
        //if the value isn't equal to the value in the foreign key,
        //that foreign key reference is invalid and is removed
        if (!Objects.equals(value, referencedKey.get(keyAttribute))) {
          remove(foreignKeyProperty.getAttribute());
          removeCachedReferencedKey(foreignKeyProperty.getAttribute());
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
  private void setForeignKeyValues(final ForeignKeyProperty foreignKeyProperty, final Entity referencedEntity) {
    removeCachedReferencedKey(foreignKeyProperty.getAttribute());
    final List<ColumnProperty<?>> properties = foreignKeyProperty.getColumnProperties();
    final List<Attribute<?>> foreignAttributes =
            definition.getForeignDefinition(foreignKeyProperty.getAttribute()).getPrimaryKeyAttributes();
    if (properties.size() > 1) {
      setCompositeForeignKeyValues(referencedEntity, properties, foreignAttributes);
    }
    else {
      setSingleForeignKeyValue(referencedEntity, properties.get(0), foreignAttributes.get(0));
    }
  }

  private void setCompositeForeignKeyValues(final Entity referencedEntity,
                                            final List<ColumnProperty<?>> referenceProperties,
                                            final List<Attribute<?>> foreignColumnAttributes) {
    for (int i = 0; i < referenceProperties.size(); i++) {
      setSingleForeignKeyValue(referencedEntity, referenceProperties.get(i), foreignColumnAttributes.get(i));
    }
  }

  private void setSingleForeignKeyValue(final Entity referencedEntity,
                                        final ColumnProperty<?> referenceProperty,
                                        final Attribute<?> foreignColumnAttribute) {
    if (!(referenceProperty instanceof MirrorProperty)) {
      putInternal((Property<Object>) referenceProperty, referencedEntity == null ? null : referencedEntity.get(foreignColumnAttribute));
    }
  }

  /**
   * Sets the denormalized property values
   * @param foreignKeyProperty the foreign key property from which to denormalize the values
   * @param referencedEntity the foreign key entity containing the values to denormalize
   */
  private void setDenormalizedValues(final ForeignKeyProperty foreignKeyProperty, final Entity referencedEntity) {
    final List<DenormalizedProperty<?>> denormalizedProperties =
            definition.getDenormalizedProperties(foreignKeyProperty.getAttribute());
    if (denormalizedProperties != null) {
      for (int i = 0; i < denormalizedProperties.size(); i++) {
        final DenormalizedProperty<Object> denormalizedProperty = (DenormalizedProperty<Object>) denormalizedProperties.get(i);
        putInternal(denormalizedProperty, referencedEntity == null ? null :
                referencedEntity.get(denormalizedProperty.getDenormalizedAttribute()));
      }
    }
  }

  /**
   * Creates and caches the primary key referenced by the given foreign key
   * @param foreignKeyProperty the foreign key
   * @return the referenced primary key or null if a valid key can not be created (null values for non-nullable properties)
   */
  private Key initializeAndCacheReferencedKey(final ForeignKeyProperty foreignKeyProperty) {
    if (foreignKeyProperty.getColumnAttributes().size() > 1) {
      return initializeAndCacheCompositeReferenceKey(foreignKeyProperty);
    }

    return initializeAndCacheSingleReferenceKey(foreignKeyProperty);
  }

  private Key initializeAndCacheCompositeReferenceKey(final ForeignKeyProperty foreignKeyProperty) {
    final EntityDefinition foreignEntityDefinition = definition.getForeignDefinition(foreignKeyProperty.getAttribute());
    if (!foreignEntityDefinition.hasPrimaryKey()) {
      throw new IllegalArgumentException("Entity '" + foreignEntityDefinition.getEntityType() + "' has no primary key defined");
    }
    final List<ColumnProperty<?>> foreignProperties = foreignEntityDefinition.getPrimaryKeyProperties();
    final List<ColumnProperty<?>> columnProperties = foreignKeyProperty.getColumnProperties();
    final Map<Attribute<?>, Object> keyValues = new HashMap<>(columnProperties.size());
    for (int i = 0; i < columnProperties.size(); i++) {
      ColumnProperty<?> columnProperty = columnProperties.get(i);
      if (columnProperty instanceof MirrorProperty) {
        columnProperty = definition.getColumnProperty(columnProperty.getAttribute());
      }
      final ColumnProperty<?> foreignColumnProperty = foreignProperties.get(i);
      final Object value = values.get(columnProperty.getAttribute());
      if (value == null && !foreignColumnProperty.isNullable()) {
        return null;
      }
      keyValues.put(foreignColumnProperty.getAttribute(), value);
    }

    return cacheReferencedKey(foreignKeyProperty.getAttribute(), new DefaultKey(foreignEntityDefinition, keyValues));
  }

  private Key initializeAndCacheSingleReferenceKey(final ForeignKeyProperty foreignKeyProperty) {
    final List<Attribute<?>> columnAttributes = foreignKeyProperty.getColumnAttributes();
    final Object value = values.get(columnAttributes.get(0));
    if (value == null) {
      return null;
    }

    return cacheReferencedKey(foreignKeyProperty.getAttribute(),
            new DefaultKey(definition.getForeignDefinition(foreignKeyProperty.getAttribute()), value));
  }

  private Key cacheReferencedKey(final Attribute<Entity> foreignKeyAttribute, final Key referencedPrimaryKey) {
    if (referencedKeyCache == null) {
      referencedKeyCache = new HashMap<>();
    }
    referencedKeyCache.put(foreignKeyAttribute, referencedPrimaryKey);

    return referencedPrimaryKey;
  }

  private Key getCachedReferencedKey(final Attribute<Entity> foreignKeyAttribute) {
    if (referencedKeyCache == null) {
      return null;
    }

    return referencedKeyCache.get(foreignKeyAttribute);
  }

  private void removeCachedReferencedKey(final Attribute<Entity> foreignKeyAttribute) {
    if (referencedKeyCache != null) {
      referencedKeyCache.remove(foreignKeyAttribute);
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
  private Key initializeKey(final boolean originalValues) {
    if (!definition.hasPrimaryKey()) {
      return new DefaultKey(definition);
    }
    final List<Attribute<?>> primaryKeyAttributes = definition.getPrimaryKeyAttributes();
    if (primaryKeyAttributes.size() > 1) {
      final Map<Attribute<?>, Object> keyValues = new HashMap<>(primaryKeyAttributes.size());
      for (int i = 0; i < primaryKeyAttributes.size(); i++) {
        final Attribute<?> attribute = primaryKeyAttributes.get(i);
        keyValues.put(attribute, originalValues ? getOriginal(attribute) : values.get(attribute));
      }

      return new DefaultKey(definition, keyValues);
    }

    return new DefaultKey(definition, originalValues ? getOriginal(primaryKeyAttributes.get(0)) : values.get(primaryKeyAttributes.get(0)));
  }

  private <T> T getDerivedValue(final DerivedProperty<T> derivedProperty) {
    return derivedProperty.getValueProvider().get(getSourceValues(derivedProperty));
  }

  private DerivedProperty.SourceValues getSourceValues(final DerivedProperty<?> derivedProperty) {
    final List<Attribute<?>> sourceAttributes = derivedProperty.getSourceAttributes();
    if (sourceAttributes.size() == 1) {
      final Attribute<?> sourceAttribute = sourceAttributes.get(0);

      return new DefaultSourceValues(derivedProperty.getAttribute(), singletonMap(sourceAttribute, get(sourceAttribute)));
    }
    else {
      final Map<Attribute<?>, Object> valueMap = new HashMap<>(sourceAttributes.size());
      for (int i = 0; i < sourceAttributes.size(); i++) {
        final Attribute<?> sourceAttribute = sourceAttributes.get(i);
        valueMap.put(sourceAttribute, get(sourceAttribute));
      }

      return new DefaultSourceValues(derivedProperty.getAttribute(), valueMap);
    }
  }

  private boolean isModifiedInternal(final boolean overrideModifiesEntity) {
    return !Util.nullOrEmpty(originalValues) && writablePropertiesModified(overrideModifiesEntity);
  }

  private boolean writablePropertiesModified(final boolean overrideModifiesEntity) {
    if (originalValues != null) {
      for (final Attribute<?> attribute : originalValues.keySet()) {
        final Property<?> property = definition.getProperty(attribute);
        if (property instanceof ColumnProperty) {
          final ColumnProperty<?> columnProperty = (ColumnProperty<?>) property;
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

  private <T> void setOriginalValue(final Attribute<T> attribute, final Object originalValue) {
    if (originalValues == null) {
      originalValues = new HashMap<>();
    }
    originalValues.put(attribute, originalValue);
  }

  private <T> void removeOriginalValue(final Attribute<T> attribute) {
    if (originalValues != null) {
      originalValues.remove(attribute);
      if (originalValues.isEmpty()) {
        originalValues = null;
      }
    }
  }

  private <T> void updateOriginalValue(final Attribute<T> attribute, final T value, final T previousValue) {
    final boolean modified = isModified(attribute);
    if (modified && Objects.equals(getOriginal(attribute), value)) {
      removeOriginalValue(attribute);//we're back to the original value
    }
    else if (!modified) {//only the first original value is kept
      setOriginalValue(attribute, previousValue);
    }
  }

  private void writeObject(final ObjectOutputStream stream) throws IOException {
    stream.writeObject(definition.getDomainName());
    stream.writeObject(definition.getEntityType().getName());
    final boolean isModified = isModifiedInternal(true);
    stream.writeBoolean(isModified);
    final List<Property<?>> properties = definition.getProperties();
    for (int i = 0; i < properties.size(); i++) {
      final Property<?> property = properties.get(i);
      if (!(property instanceof DerivedProperty)) {
        final Attribute<?> attribute = property.getAttribute();
        final boolean containsValue = values.containsKey(attribute);
        stream.writeBoolean(containsValue);
        if (containsValue) {
          stream.writeObject(values.get(attribute));
          if (isModified) {
            final boolean valueModified = originalValues != null && originalValues.containsKey(attribute);
            stream.writeBoolean(valueModified);
            if (valueModified) {
              stream.writeObject(originalValues.get(attribute));
            }
          }
        }
      }
    }
  }

  private void readObject(final ObjectInputStream stream) throws IOException, ClassNotFoundException {
    final String domainName = (String) stream.readObject();
    final EntityType entityType = EntityType.entityType((String) stream.readObject());
    final boolean isModified = stream.readBoolean();
    definition = DefaultEntities.getEntities(domainName).getDefinition(entityType);
    if (definition == null) {
      throw new IllegalArgumentException("Undefined entity: " + entityType);
    }
    values = new HashMap<>();
    final List<Property<?>> properties = definition.getProperties();
    for (int i = 0; i < properties.size(); i++) {
      final Property<Object> property = (Property<Object>) properties.get(i);
      if (!(property instanceof DerivedProperty)) {
        final boolean containsValue = stream.readBoolean();
        if (containsValue) {
          final Attribute<Object> attribute = property.getAttribute();
          values.put(attribute, attribute.validateType(stream.readObject()));
          if (isModified && stream.readBoolean()) {
            setOriginalValue(attribute, attribute.validateType(stream.readObject()));
          }
        }
      }
    }
  }

  private static Map<Attribute<?>, Object> validatePropertiesAndValues(final EntityDefinition definition,
                                                                       final Map<Attribute<?>, Object> propertyValues) {
    requireNonNull(definition, "definition");
    if (propertyValues != null && !propertyValues.isEmpty()) {
      for (final Map.Entry<Attribute<?>, Object> valueEntry : propertyValues.entrySet()) {
        final Property<Object> property = definition.getProperty((Attribute<Object>) valueEntry.getKey());
        property.getAttribute().validateType(valueEntry.getValue());
      }
    }

    return propertyValues;
  }

  private static Map<Attribute<?>, Object> createValueMap(final Key key) {
    requireNonNull(key, "key");
    final List<Attribute<?>> attributes = key.getAttributes();
    final Map<Attribute<?>, Object> values = new HashMap<>(attributes.size());
    for (int i = 0; i < attributes.size(); i++) {
      final Attribute<?> attribute = attributes.get(i);
      values.put(attribute, key.get(attribute));
    }

    return values;
  }

  private static final class DefaultSourceValues implements DerivedProperty.SourceValues {

    private final Attribute<?> derivedAttribute;
    private final Map<Attribute<?>, Object> values;

    private DefaultSourceValues(final Attribute<?> derivedAttribute, final Map<Attribute<?>, Object> values) {
      this.derivedAttribute = derivedAttribute;
      this.values = values;
    }

    @Override
    public <T> T get(final Attribute<T> attribute) {
      if (!values.containsKey(attribute)) {
        throw new IllegalArgumentException("Attribute " + attribute +
                " is not specified as a source attribute for derived property: " + derivedAttribute);
      }

      return (T) values.get(attribute);
    }
  }
}
