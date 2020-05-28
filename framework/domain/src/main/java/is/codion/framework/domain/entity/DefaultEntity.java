/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.common.Util;
import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.Events;
import is.codion.framework.domain.property.Attribute;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static is.codion.framework.domain.entity.ValueChanges.valueChange;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;

/**
 * Represents a row in a database table.
 */
final class DefaultEntity implements Entity {

  private static final long serialVersionUID = 1;

  private static final String PROPERTY = "property";

  /**
   * Holds the values contained in this value map.
   */
  private Map<Property, Object> values;

  /**
   * Holds the original value for keys which values have changed since they were first set.
   */
  private Map<Property, Object> originalValues;

  /**
   * Used to cache the return value of the frequently called toString(),
   * invalidated each time a property value changes
   */
  private String toString;

  /**
   * Caches the result of {@link #getReferencedKey} method
   */
  private Map<Attribute<?>, Key> referencedKeyCache;

  /**
   * Keep a reference to this frequently referenced object
   */
  private EntityDefinition definition;

  /**
   * The primary key of this entity
   */
  private Key key;

  /**
   * Fired when a value changes, null until initialized by a call to {@link #getValueChangeEvent()}.
   */
  private Event<ValueChange> valueChangeEvent;

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
  DefaultEntity(final EntityDefinition definition, final Map<Property, Object> values, final Map<Property, Object> originalValues) {
    final Map<Property, Object> validatedValues = validateProperties(definition, values);
    this.values = validatedValues == null ? new HashMap<>() : validatedValues;
    this.originalValues = validateProperties(definition, originalValues);
    this.definition = definition;
  }

  @Override
  public String getEntityId() {
    return definition.getEntityId();
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
  public boolean is(final String entityId) {
    return definition.getEntityId().equals(entityId);
  }

  /**
   * Returns true if one or more writable properties have been modified, read only and non-updatable properties
   * are excluded unless they are transient.
   * @return true if one or more properties have been modified since the entity was instantiated
   */
  @Override
  public boolean isModified() {
    return isModifiedInternal(false);
  }

  @Override
  public <T> T put(final Attribute<T> propertyId, final T value) {
    return (T) put(definition.getProperty(propertyId), value);
  }

  @Override
  public Object put(final Property property, final Object value) {
    requireNonNull(property, PROPERTY);
    final Object newValue = validateAndPrepareForPut(property, value);
    final boolean initialization = !values.containsKey(property);
    final Object previousValue = values.put(property, newValue);
    if (!initialization && Objects.equals(previousValue, newValue)) {
      return newValue;
    }
    if (!initialization) {
      updateOriginalValue(property, newValue, previousValue);
    }
    onValuePut(property, newValue, previousValue);
    onValueChanged(property, newValue, previousValue, initialization);

    return previousValue;
  }

  /**
   * @param propertyId the id of the property for which to retrieve the value
   * @return the value of the property identified by {@code propertyId}
   */
  @Override
  public <T> T get(final Attribute<T> propertyId) {
    return (T) get(definition.getProperty(propertyId));
  }

  /**
   * Returns the value associated with the given property.
   * Foreign key values which have non-null references but have not been loaded are simply returned
   * as null, use {@link #getForeignKey(ForeignKeyProperty)} to get an empty entity instance
   * @param property the property for which to retrieve the value
   * @return the value associated with the given property.
   * @see #getForeignKey(ForeignKeyProperty)
   * @see #getDerivedValue(DerivedProperty)
   * @see #isLoaded(String)
   */
  @Override
  public Object get(final Property property) {
    requireNonNull(property, "property");
    if (property instanceof MirrorProperty) {
      return get(definition.getProperty(property.getPropertyId()));
    }
    if (property instanceof DerivedProperty) {
      return getDerivedValue((DerivedProperty) property);
    }

    return values.get(property);
  }

  @Override
  public boolean isNull(final Attribute<?> propertyId) {
    return isNull(definition.getProperty(propertyId));
  }

  @Override
  public boolean isNotNull(final Attribute<?> propertyId) {
    return !isNull(propertyId);
  }

  @Override
  public final boolean isNotNull(final Property property) {
    return !isNull(property);
  }

  /**
   * Returns true if the value associated with the given property is null. In case of foreign key properties
   * the value of the underlying reference property is checked.
   * @param property the property
   * @return true if the value associated with the property is null
   */
  @Override
  public boolean isNull(final Property property) {
    if (property instanceof ForeignKeyProperty) {
      return isForeignKeyNull((ForeignKeyProperty) property);
    }

    return get(property) == null;//todo values.get()?
  }

  @Override
  public boolean isModified(final Attribute<?> propertyId) {
    return isModified(definition.getProperty(propertyId));
  }

  @Override
  public final boolean isModified(final Property property) {
    requireNonNull(property, PROPERTY);
    return originalValues != null && originalValues.containsKey(property);
  }

  @Override
  public Entity getForeignKey(final Attribute<Entity> foreignKeyPropertyId) {
    return getForeignKey(definition.getForeignKeyProperty(foreignKeyPropertyId));
  }

  @Override
  public Entity getForeignKey(final ForeignKeyProperty foreignKeyProperty) {
    final Entity value = (Entity) values.get(foreignKeyProperty);
    if (value == null) {//possibly not loaded
      final Entity.Key referencedKey = getReferencedKey(foreignKeyProperty);
      if (referencedKey != null) {
        return new DefaultEntity(definition.getForeignDefinition(foreignKeyProperty.getPropertyId()), referencedKey);
      }
    }

    return value;
  }

  @Override
  public boolean isLoaded(final Attribute<?> foreignKeyPropertyId) {
    return values.get(definition.getForeignKeyProperty(foreignKeyPropertyId)) != null;
  }

  @Override
  public String getAsString(final Property property) {
    if (property instanceof ValueListProperty) {
      return ((ValueListProperty) property).getCaption(get(property));
    }
    if (property instanceof ForeignKeyProperty && !isLoaded(property.getPropertyId())) {
      final Entity.Key referencedKey = getReferencedKey((ForeignKeyProperty) property);
      if (referencedKey != null) {
        return referencedKey.toString();
      }
    }

    return property.formatValue(get(property));
  }

  @Override
  public String getAsString(final Attribute<?> propertyId) {
    return getAsString(definition.getProperty(propertyId));
  }

  @Override
  public void clearKeyValues() {
    final List<ColumnProperty> primaryKeyProperties = definition.getPrimaryKeyProperties();
    for (int i = 0; i < primaryKeyProperties.size(); i++) {
      remove(primaryKeyProperties.get(i));
    }
    this.key = null;
  }

  @Override
  public <T> T getOriginal(final Attribute<T> propertyId) {
    return (T) getOriginal(definition.getProperty(propertyId));
  }

  @Override
  public final Object getOriginal(final Property property) {
    if (isModified(property)) {
      return originalValues.get(property);
    }

    return get(property);
  }

  @Override
  public void save(final Attribute<?> propertyId) {
    save(definition.getProperty(propertyId));
  }

  @Override
  public final void save(final Property property) {
    requireNonNull(property, PROPERTY);
    removeOriginalValue(property);
  }

  @Override
  public final void saveAll() {
    originalValues = null;
  }

  @Override
  public void revert(final Attribute<?> propertyId) {
    revert(definition.getProperty(propertyId));
  }

  @Override
  public final void revert(final Property property) {
    if (isModified(property)) {
      put(property, getOriginal(property));
    }
  }

  @Override
  public void revertAll() {
    for (final Property property : keySet()) {
      revert(property);
    }
  }

  @Override
  public <T> T remove(final Attribute<T> propertyId) {
    return (T) remove(definition.getProperty(propertyId));
  }

  @Override
  public final Object remove(final Property property) {
    if (values.containsKey(requireNonNull(property, PROPERTY))) {
      final Object value = values.remove(property);
      removeOriginalValue(property);
      onValueChanged(property, null, value, false);

      return value;
    }

    return null;
  }

  @Override
  public final void setAs(final Entity entity) {
    if (entity == this) {
      return;
    }
    final Set<Property> affectedProperties = new HashSet<>(keySet());
    clear();
    if (entity != null) {
      final Collection<Property> sourceProperties = entity.keySet();
      affectedProperties.addAll(sourceProperties);
      for (final Property property : sourceProperties) {
        values.put(property, entity.get(property));
      }
      if (entity.isModified()) {
        originalValues = new HashMap<>();
        for (final Property property : entity.originalKeySet()) {
          originalValues.put(property, entity.getOriginal(property));
        }
      }
    }
    for (final Property property : affectedProperties) {
      onValueChanged(property, values.get(property), null, true);
    }
  }

  @Override
  public boolean valuesEqual(final Entity entity) {
    requireNonNull(entity, "entity");

    return definition.getColumnProperties().stream().allMatch(property -> {
      if (property.isBlob()) {
        return Arrays.equals((byte[]) get(property), (byte[]) entity.get(property));
      }

      return Objects.equals(get(property), entity.get(property));
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
  public Object getColor(final Property property) {
    return definition.getColorProvider().getColor(this, property);
  }

  @Override
  public Key getReferencedKey(final ForeignKeyProperty foreignKeyProperty) {
    requireNonNull(foreignKeyProperty, "foreignKeyProperty");
    if (!Objects.equals(getEntityId(), foreignKeyProperty.getEntityId())) {
      throw new IllegalArgumentException("Foreign key property " + foreignKeyProperty
              + " is not part of entity: " + getEntityId());
    }
    final Key cachedReferencedKey = getCachedReferencedKey(foreignKeyProperty.getPropertyId());
    if (cachedReferencedKey != null) {
      return cachedReferencedKey;
    }

    return initializeAndCacheReferencedKey(foreignKeyProperty);
  }

  @Override
  public boolean containsKey(final Attribute<?> propertyId) {
    return containsKey(definition.getProperty(propertyId));
  }

  @Override
  public final boolean containsKey(final Property property) {
    return values.containsKey(requireNonNull(property, PROPERTY));
  }

  @Override
  public final Set<Property> keySet() {
    return unmodifiableSet(values.keySet());
  }

  @Override
  public final Set<Property> originalKeySet() {
    if (originalValues == null) {
      return emptySet();
    }

    return unmodifiableSet(originalValues.keySet());
  }

  @Override
  public final int size() {
    return values.size();
  }

  @Override
  public boolean isForeignKeyNull(final ForeignKeyProperty foreignKeyProperty) {
    requireNonNull(foreignKeyProperty, "foreignKeyProperty");
    final List<ColumnProperty> properties = foreignKeyProperty.getColumnProperties();
    if (properties.size() == 1) {
      return isNull(properties.get(0));
    }
    final List<ColumnProperty> foreignProperties =
            definition.getForeignDefinition(foreignKeyProperty.getPropertyId()).getPrimaryKeyProperties();
    for (int i = 0; i < properties.size(); i++) {
      if (!foreignProperties.get(i).isNullable() && isNull(properties.get(i))) {
        return true;
      }
    }

    return false;
  }

  @Override
  public void addValueListener(final EventDataListener<ValueChange> valueListener) {
    getValueChangeEvent().addDataListener(valueListener);
  }

  @Override
  public void removeValueListener(final EventDataListener<ValueChange> valueListener) {
    if (valueChangeEvent != null) {
      valueChangeEvent.removeDataListener(valueListener);
    }
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

  private Object validateAndPrepareForPut(final Property property, final Object value) {
    if (property instanceof DerivedProperty) {
      throw new IllegalArgumentException("Can not set the value of a derived property");
    }
    if (property instanceof ValueListProperty && value != null && !((ValueListProperty) property).isValid(value)) {
      throw new IllegalArgumentException("Invalid value list value: " + value + " for property " + property.getPropertyId());
    }

    return property.prepareValue(property.validateType(value));
  }

  private void onValuePut(final Property property, final Object value, final Object previousValue) {
    if (property instanceof ColumnProperty) {
      final ColumnProperty columnProperty = (ColumnProperty) property;
      if (columnProperty.isPrimaryKeyProperty()) {
        key = null;
      }
      if (columnProperty.isForeignKeyProperty()) {
        removeInvalidForeignKeyValues(columnProperty, value);
      }
    }
    toString = null;
    if (property instanceof ForeignKeyProperty) {
      propagateForeignKeyValues((ForeignKeyProperty) property, (Entity) value);
    }
  }

  /**
   * Fires notifications for a value change for the given property as well as for properties derived from it.
   * @param property the property which value is changing
   * @param currentValue the new value
   * @param previousValue the previous value, if any
   * @param initialization true if the value is being initialized, that is, no previous value exists
   * @see #addValueListener(EventDataListener)
   */
  private void onValueChanged(final Property property, final Object currentValue, final Object previousValue, final boolean initialization) {
    if (valueChangeEvent != null) {
      valueChangeEvent.onEvent(valueChange(property, currentValue, previousValue, initialization));
      if (definition.hasDerivedProperties()) {
        final Collection<DerivedProperty> derivedProperties = definition.getDerivedProperties(property.getPropertyId());
        for (final DerivedProperty derivedProperty : derivedProperties) {
          final Object derivedValue = getDerivedValue(derivedProperty);
          valueChangeEvent.onEvent(valueChange(derivedProperty, derivedValue, derivedValue));
        }
      }
    }
  }

  private void propagateForeignKeyValues(final ForeignKeyProperty foreignKeyProperty, final Entity newValue) {
    setForeignKeyValues(foreignKeyProperty, newValue);
    if (definition.hasDenormalizedProperties()) {
      setDenormalizedValues(foreignKeyProperty, newValue);
    }
  }

  private void removeInvalidForeignKeyValues(final ColumnProperty columnProperty, final Object value) {
    final List<ForeignKeyProperty> propertyForeignKeyProperties =
            definition.getForeignKeyProperties(columnProperty.getPropertyId());
    for (final ForeignKeyProperty foreignKeyProperty : propertyForeignKeyProperties) {
      final Entity foreignKeyEntity = (Entity) get(foreignKeyProperty);
      if (foreignKeyEntity != null) {
        final Entity.Key referencedKey = foreignKeyEntity.getKey();
        final ColumnProperty keyProperty =
                referencedKey.getProperties().get(foreignKeyProperty.getColumnProperties().indexOf(columnProperty));
        //if the value isn't equal to the value in the foreign key,
        //that foreign key reference is invalid and is removed
        if (!Objects.equals(value, referencedKey.get(keyProperty))) {
          remove(foreignKeyProperty);
          removeCachedReferencedKey(foreignKeyProperty.getPropertyId());
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
    removeCachedReferencedKey(foreignKeyProperty.getPropertyId());
    final List<ColumnProperty> properties = foreignKeyProperty.getColumnProperties();
    final List<ColumnProperty> foreignProperties =
            definition.getForeignDefinition(foreignKeyProperty.getPropertyId()).getPrimaryKeyProperties();
    if (properties.size() > 1) {
      setCompositeForeignKeyValues(referencedEntity, properties, foreignProperties);
    }
    else {
      setSingleForeignKeyValue(referencedEntity, properties.get(0), foreignProperties.get(0));
    }
  }

  private void setCompositeForeignKeyValues(final Entity referencedEntity,
                                            final List<ColumnProperty> referenceProperties,
                                            final List<ColumnProperty> foreignColumnProperties) {
    for (int i = 0; i < referenceProperties.size(); i++) {
      setSingleForeignKeyValue(referencedEntity, referenceProperties.get(i), foreignColumnProperties.get(i));
    }
  }

  private void setSingleForeignKeyValue(final Entity referencedEntity,
                                        final ColumnProperty referenceProperty,
                                        final ColumnProperty foreignColumnProperty) {
    if (!(referenceProperty instanceof MirrorProperty)) {
      values.put(referenceProperty, referencedEntity == null ? null : referencedEntity.get(foreignColumnProperty));
    }
  }

  /**
   * Sets the denormalized property values
   * @param foreignKeyProperty the foreign key property from which to denormalize the values
   * @param referencedEntity the foreign key entity containing the values to denormalize
   */
  private void setDenormalizedValues(final ForeignKeyProperty foreignKeyProperty, final Entity referencedEntity) {
    final List<DenormalizedProperty> denormalizedProperties =
            definition.getDenormalizedProperties(foreignKeyProperty.getPropertyId());
    if (denormalizedProperties != null) {
      for (int i = 0; i < denormalizedProperties.size(); i++) {
        final DenormalizedProperty denormalizedProperty = denormalizedProperties.get(i);
        values.put(denormalizedProperty, referencedEntity == null ? null : referencedEntity.get(denormalizedProperty
                .getDenormalizedProperty()));
      }
    }
  }

  /**
   * Creates and caches the primary key referenced by the given foreign key
   * @param foreignKeyProperty the foreign key
   * @return the referenced primary key or null if a valid key can not be created (null values for non-nullable properties)
   */
  private Key initializeAndCacheReferencedKey(final ForeignKeyProperty foreignKeyProperty) {
    if (foreignKeyProperty.isCompositeKey()) {
      return initializeAndCacheCompositeReferenceKey(foreignKeyProperty);
    }

    return initializeAndCacheSingleReferenceKey(foreignKeyProperty);
  }

  private Key initializeAndCacheCompositeReferenceKey(final ForeignKeyProperty foreignKeyProperty) {
    final EntityDefinition foreignEntityDefinition = definition.getForeignDefinition(foreignKeyProperty.getPropertyId());
    if (!foreignEntityDefinition.hasPrimaryKey()) {
      throw new IllegalArgumentException("Entity '" + foreignEntityDefinition.getEntityId() + "' has no primary key defined");
    }
    final List<ColumnProperty> foreignProperties = foreignEntityDefinition.getPrimaryKeyProperties();
    final List<ColumnProperty> columnProperties = foreignKeyProperty.getColumnProperties();
    final Map<ColumnProperty, Object> keyValues = new HashMap<>(columnProperties.size());
    for (int i = 0; i < columnProperties.size(); i++) {
      ColumnProperty columnProperty = columnProperties.get(i);
      if (columnProperty instanceof MirrorProperty) {
        columnProperty = definition.getColumnProperty(columnProperty.getPropertyId());
      }
      final ColumnProperty foreignColumnProperty = foreignProperties.get(i);
      final Object value = values.get(columnProperty);
      if (value == null && !foreignColumnProperty.isNullable()) {
        return null;
      }
      keyValues.put(foreignColumnProperty, value);
    }

    return cacheReferencedKey(foreignKeyProperty.getPropertyId(), new DefaultEntityKey(foreignEntityDefinition, keyValues));
  }

  private Key initializeAndCacheSingleReferenceKey(final ForeignKeyProperty foreignKeyProperty) {
    final List<ColumnProperty> columnProperties = foreignKeyProperty.getColumnProperties();
    final Object value = values.get(columnProperties.get(0));
    if (value == null) {
      return null;
    }

    return cacheReferencedKey(foreignKeyProperty.getPropertyId(),
            new DefaultEntityKey(definition.getForeignDefinition(foreignKeyProperty.getPropertyId()), value));
  }

  private Key cacheReferencedKey(final Attribute<?> foreignKeyPropertyId, final Key referencedPrimaryKey) {
    if (referencedKeyCache == null) {
      referencedKeyCache = new HashMap<>();
    }
    referencedKeyCache.put(foreignKeyPropertyId, referencedPrimaryKey);

    return referencedPrimaryKey;
  }

  private Key getCachedReferencedKey(final Attribute<?> fkPropertyId) {
    if (referencedKeyCache == null) {
      return null;
    }

    return referencedKeyCache.get(fkPropertyId);
  }

  private void removeCachedReferencedKey(final Attribute<?> fkPropertyId) {
    if (referencedKeyCache != null) {
      referencedKeyCache.remove(fkPropertyId);
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
      return new DefaultEntityKey(definition);
    }
    final List<ColumnProperty> primaryKeyProperties = definition.getPrimaryKeyProperties();
    if (primaryKeyProperties.size() > 1) {
      final Map<ColumnProperty, Object> keyValues = new HashMap<>(primaryKeyProperties.size());
      for (int i = 0; i < primaryKeyProperties.size(); i++) {
        final ColumnProperty property = primaryKeyProperties.get(i);
        keyValues.put(property, originalValues ? getOriginal(property) : values.get(property));
      }

      return new DefaultEntityKey(definition, keyValues);
    }

    return new DefaultEntityKey(definition, originalValues ? getOriginal(primaryKeyProperties.get(0)) : values.get(primaryKeyProperties.get(0)));
  }

  private Object getDerivedValue(final DerivedProperty derivedProperty) {
    return derivedProperty.getValueProvider().getValue(getSourceValues(derivedProperty.getSourcePropertyIds()));
  }

  private Map<Attribute<?>, Object> getSourceValues(final List<Attribute<?>> sourcePropertyIds) {
    if (sourcePropertyIds.size() == 1) {
      final Attribute<?> sourcePropertyId = sourcePropertyIds.get(0);

      return singletonMap(sourcePropertyId, get(sourcePropertyId));
    }
    else {
      final Map<Attribute<?>, Object> values = new HashMap<>(sourcePropertyIds.size());
      for (int i = 0; i < sourcePropertyIds.size(); i++) {
        final Attribute<?> sourcePropertyId = sourcePropertyIds.get(i);
        values.put(sourcePropertyId, get(sourcePropertyId));
      }

      return values;
    }
  }

  private boolean isModifiedInternal(final boolean overrideModifiesEntity) {
    return !Util.nullOrEmpty(originalValues) && writablePropertiesModified(overrideModifiesEntity);
  }

  private boolean writablePropertiesModified(final boolean overrideModifiesEntity) {
    for (final Property property : originalKeySet()) {
      if (property instanceof ColumnProperty) {
        final ColumnProperty columnProperty = (ColumnProperty) property;
        if (columnProperty.isInsertable() && columnProperty.isUpdatable()) {
          return true;
        }
      }
      if (property instanceof TransientProperty) {
        return overrideModifiesEntity || ((TransientProperty) property).isModifiesEntity();
      }
    }

    return false;
  }

  private Event<ValueChange> getValueChangeEvent() {
    if (valueChangeEvent == null) {
      valueChangeEvent = Events.event();
    }

    return valueChangeEvent;
  }

  private void setOriginalValue(final Property property, final Object previousValue) {
    if (originalValues == null) {
      originalValues = new HashMap<>();
    }
    originalValues.put(property, previousValue);
  }

  private void removeOriginalValue(final Property property) {
    if (originalValues != null) {
      originalValues.remove(property);
      if (originalValues.isEmpty()) {
        originalValues = null;
      }
    }
  }

  private void updateOriginalValue(final Property property, final Object value, final Object previousValue) {
    final boolean modified = isModified(property);
    if (modified && Objects.equals(getOriginal(property), value)) {
      removeOriginalValue(property);//we're back to the original value
    }
    else if (!modified) {//only the first original value is kept
      setOriginalValue(property, previousValue);
    }
  }

  private void writeObject(final ObjectOutputStream stream) throws IOException {
    stream.writeObject(definition.getDomainId());
    stream.writeObject(definition.getEntityId());
    final boolean isModified = isModifiedInternal(true);
    stream.writeBoolean(isModified);
    final List<Property> properties = definition.getProperties();
    for (int i = 0; i < properties.size(); i++) {
      final Property property = properties.get(i);
      if (!(property instanceof DerivedProperty)) {
        final boolean containsValue = containsKey(property);
        stream.writeBoolean(containsValue);
        if (containsValue) {
          stream.writeObject(values.get(property));
          if (isModified) {
            final boolean valueModified = isModified(property);
            stream.writeBoolean(valueModified);
            if (valueModified) {
              stream.writeObject(getOriginal(property));
            }
          }
        }
      }
    }
  }

  private void readObject(final ObjectInputStream stream) throws IOException, ClassNotFoundException {
    final String domainId = (String) stream.readObject();
    final String entityId = (String) stream.readObject();
    final boolean isModified = stream.readBoolean();
    definition = DefaultEntities.getEntities(domainId).getDefinition(entityId);
    if (definition == null) {
      throw new IllegalArgumentException("Undefined entity: " + entityId);
    }
    values = new HashMap<>();
    final List<Property> properties = definition.getProperties();
    for (int i = 0; i < properties.size(); i++) {
      final Property property = properties.get(i);
      if (!(property instanceof DerivedProperty)) {
        final boolean containsValue = stream.readBoolean();
        if (containsValue) {
          values.put(property, property.validateType(stream.readObject()));
          if (isModified && stream.readBoolean()) {
            setOriginalValue(property, property.validateType(stream.readObject()));
          }
        }
      }
    }
  }

  private static Map<Property, Object> validateProperties(final EntityDefinition definition,
                                                          final Map<Property, Object> propertyValues) {
    requireNonNull(definition, "definition");
    if (propertyValues != null && !propertyValues.isEmpty()) {
      final Set<Property> propertySet = definition.getPropertySet();
      for (final Map.Entry<Property, Object> valueEntry : propertyValues.entrySet()) {
        final Property property = valueEntry.getKey();
        if (!property.getEntityId().equals(definition.getEntityId()) || !propertySet.contains(property)) {
          throw new IllegalArgumentException("Property " + property + " is not part of entity: " + definition.getEntityId());
        }
        property.validateType(valueEntry.getValue());
      }
    }

    return propertyValues;
  }

  private static Map<Property, Object> createValueMap(final Key key) {
    requireNonNull(key, "key");
    final List<ColumnProperty> properties = key.getProperties();
    final Map<Property, Object> values = new HashMap<>(properties.size());
    for (int i = 0; i < properties.size(); i++) {
      final ColumnProperty property = properties.get(i);
      values.put(property, key.get(property));
    }

    return values;
  }
}
