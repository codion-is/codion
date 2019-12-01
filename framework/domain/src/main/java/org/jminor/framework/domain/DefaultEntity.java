/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.Util;
import org.jminor.common.db.valuemap.DefaultValueMap;
import org.jminor.common.db.valuemap.ValueMap;
import org.jminor.framework.domain.property.ColumnProperty;
import org.jminor.framework.domain.property.DenormalizedProperty;
import org.jminor.framework.domain.property.DerivedProperty;
import org.jminor.framework.domain.property.ForeignKeyProperty;
import org.jminor.framework.domain.property.MirrorProperty;
import org.jminor.framework.domain.property.Property;
import org.jminor.framework.domain.property.TransientProperty;
import org.jminor.framework.domain.property.ValueListProperty;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.text.Format;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static java.util.Collections.singletonMap;
import static java.util.Objects.requireNonNull;

/**
 * Represents a row in a database table, providing access to the column values via the {@link ValueMap} interface.
 */
final class DefaultEntity extends DefaultValueMap<Property, Object> implements Entity {

  private static final long serialVersionUID = 1;

  private static final String PROPERTY_PARAM = "property";

  /**
   * Used to cache the return value of the frequently called toString(),
   * invalidated each time a property value changes
   */
  private String toString;

  /**
   * Caches the result of {@code getReferencedKey} method
   */
  private Map<String, Key> referencedKeyCache;

  /**
   * Provides access to domain entity definitions
   */
  private EntityDefinition.Provider definitionProvider;

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
   * @param definitionProvider the domain
   * @param key the primary key
   */
  DefaultEntity(final EntityDefinition.Provider definitionProvider, final Key key) {
    this(definitionProvider, requireNonNull(key, "key").getEntityId(), createValueMap(key), null);
    this.key = key;
  }

  /**
   * Instantiates a new DefaultEntity based on the given values.
   * @param definitionProvider the domain model
   * @param values the initial values, may be null
   * @param originalValues the original values, may be null
   */
  DefaultEntity(final EntityDefinition.Provider definitionProvider, final String entityId,
                final Map<Property, Object> values, final Map<Property, Object> originalValues) {
    this(requireNonNull(definitionProvider, "definitionProvider"),
            definitionProvider.getDefinition(entityId), values, originalValues);
  }

  /**
   * Instantiates a new DefaultEntity based on the given values.
   * @param definitionProvider the domain model
   * @param values the initial values, may be null
   * @param originalValues the original values, may be null
   */
  DefaultEntity(final EntityDefinition.Provider definitionProvider, final EntityDefinition definition,
                final Map<Property, Object> values, final Map<Property, Object> originalValues) {
    super(values, originalValues);
    this.definitionProvider = requireNonNull(definitionProvider, "definitionProvider");
    this.definition = requireNonNull(definition, "definition");
    validateProperties(definition, values);
    validateProperties(definition, originalValues);
  }

  /** {@inheritDoc} */
  @Override
  public String getEntityId() {
    return definition.getEntityId();
  }

  /** {@inheritDoc} */
  @Override
  public Key getKey() {
    if (key == null) {
      key = initializeKey(false);
    }

    return key;
  }

  /** {@inheritDoc} */
  @Override
  public Key getOriginalKey() {
    return initializeKey(true);
  }

  /** {@inheritDoc} */
  @Override
  public boolean is(final String entityId) {
    return definition.getEntityId().equals(entityId);
  }

  /** {@inheritDoc} */
  @Override
  public List<Property> getProperties() {
    return definition.getProperties();
  }

  /** {@inheritDoc} */
  @Override
  public List<ColumnProperty> getPrimaryKeyProperties() {
    return definition.getPrimaryKeyProperties();
  }

  /** {@inheritDoc} */
  @Override
  public boolean isKeyGenerated() {
    return definition.isKeyGenerated();
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

  /** {@inheritDoc} */
  @Override
  public Object put(final String propertyId, final Object value) {
    return super.put(definition.getProperty(propertyId), value);
  }

  /**
   * @param propertyId the ID of the property for which to retrieve the value
   * @return the value of the property identified by {@code propertyId}
   */
  @Override
  public Object get(final String propertyId) {
    return get(definition.getProperty(propertyId));
  }

  /**
   * Returns the value associated with the given property.
   * Foreign key values which have non-null references but have not been loaded are simply returned
   * as null, use {@link #getForeignKey(ForeignKeyProperty)} (org.jminor.framework.domain.property.Property.ForeignKeyProperty)}
   * to get an empty entity instance
   * @param property the property for which to retrieve the value
   * @return the value associated with the given property.
   * @see #getForeignKeyValue(ForeignKeyProperty)
   * @see #isLoaded(String)
   */
  @Override
  public Object get(final Property property) {
    requireNonNull(property, PROPERTY_PARAM);
    if (property instanceof DerivedProperty) {
      return getDerivedValue((DerivedProperty) property);
    }

    return super.get(property);
  }

  /** {@inheritDoc} */
  @Override
  public boolean isNull(final String propertyId) {
    return isNull(definition.getProperty(propertyId));
  }

  /** {@inheritDoc} */
  @Override
  public boolean isNotNull(final String propertyId) {
    return !isNull(propertyId);
  }

  /**
   * Returns true if the value associated with the given property is null. In case of foreign key properties
   * the value of the underlying reference property is checked.
   * @param property the property
   * @return true if the value associated with the property is null
   */
  @Override
  public boolean isNull(final Property property) {
    requireNonNull(property, PROPERTY_PARAM);
    if (property instanceof ForeignKeyProperty) {
      return isForeignKeyNull((ForeignKeyProperty) property);
    }

    return super.isNull(property);
  }

  /** {@inheritDoc} */
  @Override
  public boolean isModified(final String propertyId) {
    return isModified(definition.getProperty(propertyId));
  }

  /** {@inheritDoc} */
  @Override
  public Entity getForeignKey(final String foreignKeyPropertyId) {
    return getForeignKey(definition.getForeignKeyProperty(foreignKeyPropertyId));
  }

  /** {@inheritDoc} */
  @Override
  public Entity getForeignKey(final ForeignKeyProperty foreignKeyProperty) {
    final Entity value = (Entity) super.get(foreignKeyProperty);
    if (value == null) {//possibly not loaded
      final Entity.Key referencedKey = getReferencedKey(foreignKeyProperty);
      if (referencedKey != null) {
        return new DefaultEntity(definitionProvider, referencedKey);
      }
    }

    return value;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isLoaded(final String foreignKeyPropertyId) {
    return super.get(definition.getForeignKeyProperty(foreignKeyPropertyId)) != null;
  }

  /** {@inheritDoc} */
  @Override
  public LocalTime getTime(final String propertyId) {
    return (LocalTime) get(propertyId);
  }

  /** {@inheritDoc} */
  @Override
  public LocalDate getDate(final String propertyId) {
    return (LocalDate) get(propertyId);
  }

  /** {@inheritDoc} */
  @Override
  public LocalDateTime getTimestamp(final String propertyId) {
    return (LocalDateTime) get(propertyId);
  }

  /** {@inheritDoc} */
  @Override
  public String getString(final String propertyId) {
    return (String) get(propertyId);
  }

  /** {@inheritDoc} */
  @Override
  public Integer getInteger(final String propertyId) {
    return (Integer) get(propertyId);
  }

  /** {@inheritDoc} */
  @Override
  public Long getLong(final String propertyId) {
    return (Long) get(propertyId);
  }

  /** {@inheritDoc} */
  @Override
  public Boolean getBoolean(final String propertyId) {
    return (Boolean) get(propertyId);
  }

  /** {@inheritDoc} */
  @Override
  public Character getCharacter(final String propertyId) {
    return (Character) get(propertyId);
  }

  /** {@inheritDoc} */
  @Override
  public Double getDouble(final String propertyId) {
    return (Double) get(propertyId);
  }

  /** {@inheritDoc} */
  @Override
  public BigDecimal getBigDecimal(final String propertyId) {
    return (BigDecimal) get(propertyId);
  }

  /** {@inheritDoc} */
  @Override
  public byte[] getBlob(final String propertyId) {
    return (byte[]) get(propertyId);
  }

  /** {@inheritDoc} */
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
    if (property.isTemporal()) {
      final TemporalAccessor value = (TemporalAccessor) get(property);

      return value == null ? "" : property.getDateTimeFormatter().format(value);
    }

    return getFormatted(property, property.getFormat());
  }

  /** {@inheritDoc} */
  @Override
  public String getFormatted(final String propertyId, final Format format) {
    return getFormatted(definition.getProperty(propertyId), format);
  }

  /** {@inheritDoc} */
  @Override
  public String getFormatted(final Property property, final Format format) {
    final Object value = get(property);
    if (value == null) {
      return "";
    }

    if (format == null) {
      return value.toString();
    }

    return format.format(value);
  }

  /**
   * @param propertyId the ID of the property for which to retrieve the value
   * @return a String representation of the value of the property identified by {@code propertyId}
   * @see #getFormatted(Property, java.text.Format)
   */
  @Override
  public String getAsString(final String propertyId) {
    return getAsString(definition.getProperty(propertyId));
  }

  /** {@inheritDoc} */
  @Override
  public boolean isKeyNull() {
    return getKey().isNull();
  }

  /** {@inheritDoc} */
  @Override
  public void clearKeyValues() {
    final List<ColumnProperty> primaryKeyProperties = definition.getPrimaryKeyProperties();
    for (int i = 0; i < primaryKeyProperties.size(); i++) {
      remove(primaryKeyProperties.get(i));
    }
    this.key = null;
  }

  /** {@inheritDoc} */
  @Override
  public Object getOriginal(final String propertyId) {
    return getOriginal(definition.getProperty(propertyId));
  }

  /** {@inheritDoc} */
  @Override
  public void save(final String propertyId) {
    save(definition.getProperty(propertyId));
  }

  /** {@inheritDoc} */
  @Override
  public void revert(final String propertyId) {
    revert(definition.getProperty(propertyId));
  }

  /** {@inheritDoc} */
  @Override
  public Object remove(final String propertyId) {
    return remove(definition.getProperty(propertyId));
  }

  /** {@inheritDoc} */
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
   * @return true if the given object is an Entity and it´s primary key is equal to this ones
   */
  @Override
  public boolean equals(final Object obj) {
    return this == obj || obj instanceof Entity && getKey().equals(((Entity) obj).getKey());
  }

  /**
   * @param entity the entity to compare with
   * @return the compare result from comparing {@code entity} with this Entity instance
   * @see EntityDefinition.Builder#setComparator(java.util.Comparator)
   */
  @Override
  public int compareTo(final Entity entity) {
    return definition.compareTo(this, entity);
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
   * @see EntityDefinition.Builder#setStringProvider(Entity.ToString)
   * @see EntityDefinition#toString(Entity)
   */
  @Override
  public String toString() {
    if (toString == null) {
      toString = definition.toString(this);
    }

    return toString;
  }

  /** {@inheritDoc} */
  @Override
  public Object getColor(final Property property) {
    return definition.getColor(this, property);
  }

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
  @Override
  public boolean containsKey(final String propertyId) {
    return containsKey(definition.getProperty(propertyId));
  }

  /**
   * Returns true if any of the non-nullable properties involved in the given foreign key are null
   * @param foreignKeyProperty the foreign key property
   * @return true if the foreign key is null
   */
  @Override
  public boolean isForeignKeyNull(final ForeignKeyProperty foreignKeyProperty) {
    requireNonNull(foreignKeyProperty, "foreignKeyProperty");
    final List<ColumnProperty> properties = foreignKeyProperty.getColumnProperties();
    if (properties.size() == 1) {
      return isNull(properties.get(0));
    }
    final List<ColumnProperty> foreignProperties =
            definitionProvider.getDefinition(foreignKeyProperty.getForeignEntityId()).getPrimaryKeyProperties();
    for (int i = 0; i < properties.size(); i++) {
      if (!foreignProperties.get(i).isNullable() && isNull(properties.get(i))) {
        return true;
      }
    }

    return false;
  }

  /** {@inheritDoc} */
  @Override
  protected void handleClear() {
    key = null;
    referencedKeyCache = null;
    toString = null;
  }

  /** {@inheritDoc} */
  @Override
  protected void handleValueChangedEventInitialized() {
    if (definition.hasDerivedProperties()) {
      addValueListener(valueChange -> {
        final Collection<DerivedProperty> derivedProperties = definition.getDerivedProperties(valueChange.getKey().getPropertyId());
        for (final DerivedProperty derivedProperty : derivedProperties) {
          final Object derivedValue = getDerivedValue(derivedProperty);
          notifyValueChange(derivedProperty, derivedValue, derivedValue, false);
        }
      });
    }
  }

  /** {@inheritDoc} */
  @Override
  protected void handlePut(final Property property, final Object value, final Object previousValue,
                           final boolean initialization) {
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

  /** {@inheritDoc} */
  @Override
  protected Object validateAndPrepare(final Property property, final Object value) {
    if (property instanceof DerivedProperty) {
      throw new IllegalArgumentException("Can not set the value of a derived property");
    }
    if (property instanceof ValueListProperty && value != null && !((ValueListProperty) property).isValid(value)) {
      throw new IllegalArgumentException("Invalid value list value: " + value + " for property " + property.getPropertyId());
    }

    return prepareValue(property, property.validateType(value));
  }

  private void propagateForeignKeyValues(final ForeignKeyProperty foreignKeyProperty, final Entity newValue) {
    setForeignKeyValues(foreignKeyProperty, newValue);
    if (definition.hasDenormalizedProperties()) {
      setDenormalizedValues(foreignKeyProperty, newValue);
    }
  }

  private void removeInvalidForeignKeyValues(final ColumnProperty columnProperty, final Object value) {
    final List<ForeignKeyProperty> propertyForeignKeyProperties = definition.getForeignKeyProperties(columnProperty.getPropertyId());
    for (final ForeignKeyProperty foreignKeyProperty : propertyForeignKeyProperties) {
      final Entity foreignKeyValue = (Entity) get(foreignKeyProperty);
      if (foreignKeyValue != null) {
        final Entity.Key referencedKey = foreignKeyValue.getKey();
        final ColumnProperty keyProperty = referencedKey.getProperties().get(foreignKeyProperty.getColumnProperties().indexOf(columnProperty));
        //if the value isn't equal to the value in the foreign key, that foreign key reference is invalid and is removed
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
   * @param entityDefinitions a global entity definition map
   */
  private void setForeignKeyValues(final ForeignKeyProperty foreignKeyProperty, final Entity referencedEntity) {
    removeCachedReferencedKey(foreignKeyProperty.getPropertyId());
    final List<ColumnProperty> properties = foreignKeyProperty.getColumnProperties();
    final List<ColumnProperty> foreignProperties =
            definitionProvider.getDefinition(foreignKeyProperty.getForeignEntityId()).getPrimaryKeyProperties();
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
      super.put(referenceProperty, referencedEntity == null ? null : referencedEntity.get(foreignColumnProperty));
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
        super.put(denormalizedProperty, referencedEntity == null ? null : referencedEntity.get(denormalizedProperty
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
    final EntityDefinition foreignEntityDefinition = definitionProvider.getDefinition(foreignKeyProperty.getForeignEntityId());
    final List<ColumnProperty> foreignProperties = foreignEntityDefinition.getPrimaryKeyProperties();
    final List<ColumnProperty> columnProperties = foreignKeyProperty.getColumnProperties();
    final Map<ColumnProperty, Object> values = new HashMap<>(columnProperties.size());
    for (int i = 0; i < columnProperties.size(); i++) {
      final ColumnProperty foreignColumnProperty = foreignProperties.get(i);
      final Object value = super.get(columnProperties.get(i));
      if (value == null && !foreignColumnProperty.isNullable()) {
        return null;
      }
      values.put(foreignColumnProperty, value);
    }

    return cacheReferencedKey(foreignKeyProperty.getPropertyId(), new DefaultEntityKey(foreignEntityDefinition, values));
  }

  private Key initializeAndCacheSingleReferenceKey(final ForeignKeyProperty foreignKeyProperty) {
    final List<ColumnProperty> columnProperties = foreignKeyProperty.getColumnProperties();
    final Object value = super.get(columnProperties.get(0));
    if (value == null) {
      return null;
    }

    return cacheReferencedKey(foreignKeyProperty.getPropertyId(),
            new DefaultEntityKey(definitionProvider.getDefinition(foreignKeyProperty.getForeignEntityId()), value));
  }

  private Key cacheReferencedKey(final String fkPropertyId, final Key referencedPrimaryKey) {
    if (referencedKeyCache == null) {
      referencedKeyCache = new HashMap<>();
    }
    referencedKeyCache.put(fkPropertyId, referencedPrimaryKey);

    return referencedPrimaryKey;
  }

  private Key getCachedReferencedKey(final String fkPropertyId) {
    if (referencedKeyCache == null) {
      return null;
    }

    return referencedKeyCache.get(fkPropertyId);
  }

  private void removeCachedReferencedKey(final String fkPropertyId) {
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
    final List<ColumnProperty> primaryKeyProperties = definition.getPrimaryKeyProperties();
    if (primaryKeyProperties.size() > 1) {
      final Map<ColumnProperty, Object> values = new HashMap<>(primaryKeyProperties.size());
      for (int i = 0; i < primaryKeyProperties.size(); i++) {
        final ColumnProperty property = primaryKeyProperties.get(i);
        values.put(property, originalValues ? getOriginal(property) : super.get(property));
      }

      return new DefaultEntityKey(definition, values);
    }
    else {
      return new DefaultEntityKey(definition, originalValues ? getOriginal(primaryKeyProperties.get(0)) : super.get(primaryKeyProperties.get(0)));
    }
  }

  private Object getDerivedValue(final DerivedProperty derivedProperty) {
    return derivedProperty.getValueProvider().getValue(getSourceValues(derivedProperty.getSourcePropertyIds()));
  }

  private Map<String, Object> getSourceValues(final List<String> sourcePropertyIds) {
    if (sourcePropertyIds.size() == 1) {
      final String sourcePropertyId = sourcePropertyIds.get(0);

      return singletonMap(sourcePropertyId, get(sourcePropertyId));
    }
    else {
      final Map<String, Object> values = new HashMap<>(sourcePropertyIds.size());
      for (int i = 0; i < sourcePropertyIds.size(); i++) {
        final String sourcePropertyId = sourcePropertyIds.get(i);
        values.put(sourcePropertyId, get(sourcePropertyId));
      }

      return values;
    }
  }

  private boolean isModifiedInternal(final boolean overrideModifiesEntity) {
    return super.isModified() && writablePropertiesModified(overrideModifiesEntity);
  }

  private boolean writablePropertiesModified(final boolean overrideModifiesEntity) {
    for (final Property property : originalKeySet()) {
      if (property instanceof ColumnProperty) {
        final ColumnProperty columnProperty = (ColumnProperty) property;
        if (!columnProperty.isReadOnly() && columnProperty.isUpdatable()) {
          return true;
        }
      }
      if (property instanceof TransientProperty) {
        return overrideModifiesEntity || ((TransientProperty) property).isModifiesEntity();
      }
    }

    return false;
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
          stream.writeObject(super.get(property));
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
    definitionProvider = Domain.getDomain(domainId);
    definition = definitionProvider.getDefinition(entityId);
    if (definition == null) {
      throw new IllegalArgumentException("Undefined entity: " + entityId);
    }
    final List<Property> properties = definition.getProperties();
    for (int i = 0; i < properties.size(); i++) {
      final Property property = properties.get(i);
      if (!(property instanceof DerivedProperty)) {
        final boolean containsValue = stream.readBoolean();
        if (containsValue) {
          super.put(property, property.validateType(stream.readObject()));
          if (isModified && stream.readBoolean()) {
            setOriginalValue(property, property.validateType(stream.readObject()));
          }
        }
      }
    }
  }

  /**
   * Prepares the value according to the property configuration, such as rounding
   * to the correct number of fraction digits in case of doubles
   * @param property the property
   * @param value the value to prepare
   * @return the prepared value
   */
  private static Object prepareValue(final Property property, final Object value) {
    if (value != null && property.isDouble()) {
      return Util.roundDouble((Double) value, property.getMaximumFractionDigits());
    }
    if (value != null && property.isBigDecimal()) {
      return ((BigDecimal) value).setScale(property.getMaximumFractionDigits(),
              Property.BIG_DECIMAL_ROUNDING_MODE.get()).stripTrailingZeros();
    }

    return value;
  }

  private static void validateProperties(final EntityDefinition definition, final Map<Property, Object> propertyValues) {
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
  }

  private static Map<Property, Object> createValueMap(final Key key) {
    final List<ColumnProperty> properties = key.getProperties();
    final Map<Property, Object> values = new HashMap<>(properties.size());
    for (int i = 0; i < properties.size(); i++) {
      final ColumnProperty property = properties.get(i);
      values.put(property, key.get(property));
    }

    return values;
  }
}
