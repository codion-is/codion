/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.Util;
import org.jminor.common.db.valuemap.DefaultValueMap;
import org.jminor.common.db.valuemap.ValueMap;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.text.Format;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAccessor;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Collections.singletonMap;
import static java.util.Objects.requireNonNull;

/**
 * Represents a row in a database table, providing access to the column values via the {@link ValueMap} interface.
 */
final class DefaultEntity extends DefaultValueMap<Property, Object> implements Entity {

  private static final long serialVersionUID = 1;

  private static final String PROPERTY_PARAM = "property";
  private static final String PROPERTY_ID_PARAM = "propertyId";

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
   * The domain model defining this entity
   */
  private Domain domain;

  /**
   * Keep a reference to this frequently referenced object
   */
  private Definition definition;

  /**
   * The primary key of this entity
   */
  private Key key;

  /**
   * Instantiates a new DefaultEntity
   * @param domain the domain
   */
  DefaultEntity(final Domain domain, final String entityId) {
    this(domain, entityId, null, null);
  }

  /**
   * Instantiates a new DefaultEntity
   * @param domain the domain
   * @param key the primary key
   */
  DefaultEntity(final Domain domain, final Key key) {
    this(domain, requireNonNull(key, "key").getEntityId(), createValueMap(key));
    this.key = key;
  }

  /**
   * Instantiates a new DefaultEntity
   * @param domain the domain model
   * @param entityId the entity id
   * @param values the initial values
   */
  DefaultEntity(final Domain domain, final String entityId, final Map<Property, Object> values) {
    this(domain, entityId, values, null);
  }

  /**
   * Instantiates a new DefaultEntity based on the given values.
   * @param domain the domain model
   * @param values the initial values
   * @param originalValues the original values, may be null
   */
  DefaultEntity(final Domain domain, final String entityId, final Map<Property, Object> values,
                final Map<Property, Object> originalValues) {
    super(values, originalValues);
    this.domain = requireNonNull(domain, "domain");
    this.definition = domain.getDefinition(entityId);
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
  public Property getProperty(final String propertyId) {
    requireNonNull(propertyId, PROPERTY_ID_PARAM);
    final Property property = definition.getPropertyMap().get(propertyId);
    if (property == null) {
      throw new IllegalArgumentException("Property " + propertyId + " not found in entity: " + definition.getEntityId());
    }

    return property;
  }

  /** {@inheritDoc} */
  @Override
  public List<Property> getProperties() {
    return definition.getProperties();
  }

  /** {@inheritDoc} */
  @Override
  public List<Property.ColumnProperty> getPrimaryKeyProperties() {
    return definition.getPrimaryKeyProperties();
  }

  /** {@inheritDoc} */
  @Override
  public KeyGenerator.Type getKeyGeneratorType() {
    return definition.getKeyGeneratorType();
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
    return put(getProperty(propertyId), value);
  }

  /** {@inheritDoc} */
  @Override
  public Object put(final Property property, final Object value) {
    return put(property, value, true);
  }

  /** {@inheritDoc} */
  @Override
  public Object put(final Property property, final Object value, final boolean validateType) {
    requireNonNull(property, PROPERTY_PARAM);
    validateValue(property, value);
    if (validateType && value != null) {
      property.validateType(value);
    }
    if (property instanceof Property.ColumnProperty && ((Property.ColumnProperty) property).isPrimaryKeyProperty()) {
      key = null;
    }
    toString = null;
    if (property instanceof Property.ForeignKeyProperty) {
      propagateForeignKeyValues((Property.ForeignKeyProperty) property, (Entity) value);
    }

    return super.put(property, prepareValue(property, value));
  }

  /**
   * @param propertyId the ID of the property for which to retrieve the value
   * @return the value of the property identified by {@code propertyId}
   */
  @Override
  public Object get(final String propertyId) {
    return get(getProperty(propertyId));
  }

  /**
   * Returns the value associated with the given property.
   * Foreign key values which have non-null references but have not been loaded are simply returned
   * as null, use {@link #getForeignKey(Property.ForeignKeyProperty)} (org.jminor.framework.domain.Property.ForeignKeyProperty)}
   * to get an empty entity instance
   * @param property the property for which to retrieve the value
   * @return the value associated with the given property.
   * @see #getForeignKeyValue(org.jminor.framework.domain.Property.ForeignKeyProperty)
   * @see #isLoaded(String)
   */
  @Override
  public Object get(final Property property) {
    requireNonNull(property, PROPERTY_PARAM);
    if (property instanceof Property.DerivedProperty) {
      return getDerivedValue((Property.DerivedProperty) property);
    }

    return super.get(property);
  }

  /** {@inheritDoc} */
  @Override
  public boolean isNull(final String propertyId) {
    return isNull(getProperty(propertyId));
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
    if (property instanceof Property.ForeignKeyProperty) {
      return isForeignKeyNull((Property.ForeignKeyProperty) property);
    }

    return super.isNull(property);
  }

  /** {@inheritDoc} */
  @Override
  public boolean isModified(final String propertyId) {
    return isModified(getProperty(propertyId));
  }

  /** {@inheritDoc} */
  @Override
  public Entity getForeignKey(final String foreignKeyPropertyId) {
    final Property property = getProperty(foreignKeyPropertyId);
    if (property instanceof Property.ForeignKeyProperty) {
      return getForeignKey((Property.ForeignKeyProperty) property);
    }

    throw new IllegalArgumentException(foreignKeyPropertyId + " is not a foreign key property");
  }

  /** {@inheritDoc} */
  @Override
  public Entity getForeignKey(final Property.ForeignKeyProperty foreignKeyProperty) {
    final Entity value = (Entity) super.get(foreignKeyProperty);
    if (value == null) {//possibly not loaded
      final Entity.Key referencedKey = getReferencedKey(foreignKeyProperty);
      if (referencedKey != null) {
        return new DefaultEntity(domain, referencedKey);
      }
    }

    return value;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isLoaded(final String foreignKeyPropertyId) {
    return super.get(domain.getDefinition(getEntityId()).getForeignKeyProperty(foreignKeyPropertyId)) != null;
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
  public String getAsString(final Property property) {
    if (property instanceof Property.ValueListProperty) {
      return ((Property.ValueListProperty) property).getCaption(get(property));
    }
    if (property instanceof Property.ForeignKeyProperty && !isLoaded(property.getPropertyId())) {
      final Entity.Key referencedKey = getReferencedKey((Property.ForeignKeyProperty) property);
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
    return getFormatted(getProperty(propertyId), format);
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
    return getAsString(getProperty(propertyId));
  }

  /** {@inheritDoc} */
  @Override
  public boolean isKeyNull() {
    return getKey().isNull();
  }

  /** {@inheritDoc} */
  @Override
  public void clearKeyValues() {
    final List<Property.ColumnProperty> primaryKeyProperties = definition.getPrimaryKeyProperties();
    for (int i = 0; i < primaryKeyProperties.size(); i++) {
      remove(primaryKeyProperties.get(i));
    }
    this.key = null;
  }

  /** {@inheritDoc} */
  @Override
  public Object getOriginal(final String propertyId) {
    return getOriginal(getProperty(propertyId));
  }

  /** {@inheritDoc} */
  @Override
  public void save(final String propertyId) {
    save(getProperty(propertyId));
  }

  /** {@inheritDoc} */
  @Override
  public void revert(final String propertyId) {
    revert(getProperty(propertyId));
  }

  /** {@inheritDoc} */
  @Override
  public Object remove(final String propertyId) {
    return remove(getProperty(propertyId));
  }

  /** {@inheritDoc} */
  @Override
  public boolean valuesEqual(final Entity entity) {
    requireNonNull(entity, "entity");

    return definition.getColumnProperties().stream().allMatch(property -> Objects.equals(get(property), entity.get(property)));
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
   * @see Definition#setComparator(java.util.Comparator)
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
   * @see Definition#setStringProvider(Entity.ToString)
   * @see Definition#toString(Entity)
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
  public Object getBackgroundColor(final Property property) {
    return definition.getBackgroundColor(this, property);
  }

  /**
   * @return a new Entity instance with the same entityId as this entity
   */
  @Override
  public Entity newInstance() {
    return new DefaultEntity(domain, definition.getEntityId());
  }

  /** {@inheritDoc} */
  @Override
  public Key getReferencedKey(final Property.ForeignKeyProperty foreignKeyProperty) {
    requireNonNull(foreignKeyProperty, "foreignKeyProperty");
    if (!Objects.equals(getEntityId(), foreignKeyProperty.getEntityId())) {
      throw new IllegalArgumentException("Foreign key property " + foreignKeyProperty
              + " is not part of entity: " + getEntityId());
    }
    final String propertyId = foreignKeyProperty.getPropertyId();
    Key referencedPrimaryKey = getCachedReferencedKey(propertyId);
    if (referencedPrimaryKey != null) {
      return referencedPrimaryKey;
    }

    referencedPrimaryKey = initializeReferencedKey(foreignKeyProperty);
    if (referencedPrimaryKey != null) {
      cacheReferencedKey(propertyId, referencedPrimaryKey);
    }

    return referencedPrimaryKey;
  }

  /** {@inheritDoc} */
  @Override
  public boolean containsKey(final String propertyId) {
    return containsKey(getProperty(propertyId));
  }

  /**
   * Returns true if any of the non-nullable properties involved in the given foreign key are null
   * @param foreignKeyProperty the foreign key property
   * @return true if the foreign key is null
   */
  @Override
  public boolean isForeignKeyNull(final Property.ForeignKeyProperty foreignKeyProperty) {
    requireNonNull(foreignKeyProperty, "foreignKeyProperty");
    final List<Property.ColumnProperty> properties = foreignKeyProperty.getProperties();
    if (properties.size() == 1) {
      return isNull(properties.get(0));
    }
    final List<Property.ColumnProperty> foreignProperties =
            domain.getDefinition(foreignKeyProperty.getForeignEntityId()).getPrimaryKeyProperties();
    for (int i = 0; i < properties.size(); i++) {
      if (!foreignProperties.get(i).isNullable() && isNull(properties.get(i))) {
        return true;
      }
    }

    return false;
  }

  /** {@inheritDoc} */
  @Override
  protected Object copy(final Object value) {
    if (value instanceof Entity) {
      return ((Entity) value).getCopy();
    }

    return super.copy(value);
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
  protected void handleRemove(final Property property, final Object value) {
    if (property instanceof Property.ForeignKeyProperty) {
      final List<Property.ColumnProperty> properties = ((Property.ForeignKeyProperty) property).getProperties();
      for (int i = 0; i < properties.size(); i++) {
        remove(properties.get(i));
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  protected void handleValueChangedEventInitialized() {
    if (definition.hasDerivedProperties()) {
      addValueListener(valueChange -> {
        final Collection<Property.DerivedProperty> derivedProperties = definition.getDerivedProperties(valueChange.getKey().getPropertyId());
        for (final Property.DerivedProperty derivedProperty : derivedProperties) {
          final Object derivedValue = getDerivedValue(derivedProperty);
          notifyValueChange(derivedProperty, derivedValue, derivedValue, false);
        }
      });
    }
  }

  private void propagateForeignKeyValues(final Property.ForeignKeyProperty foreignKeyProperty, final Entity newValue) {
    setForeignKeyValues(foreignKeyProperty, newValue);
    if (definition.hasDenormalizedProperties()) {
      setDenormalizedValues(foreignKeyProperty, newValue);
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
  private void setForeignKeyValues(final Property.ForeignKeyProperty foreignKeyProperty, final Entity referencedEntity) {
    referencedKeyCache = null;
    final List<Property.ColumnProperty> properties = foreignKeyProperty.getProperties();
    final List<Property.ColumnProperty> foreignProperties =
            domain.getDefinition(foreignKeyProperty.getForeignEntityId()).getPrimaryKeyProperties();
    if (properties.size() > 1) {
      setCompositeForeignKeyValues(referencedEntity, properties, foreignProperties);
    }
    else {
      setSingleForeignKeyValue(referencedEntity, properties.get(0), foreignProperties.get(0));
    }
  }

  private void setCompositeForeignKeyValues(final Entity referencedEntity,
                                            final List<Property.ColumnProperty> referenceProperties,
                                            final List<Property.ColumnProperty> foreignColumnProperties) {
    for (int i = 0; i < referenceProperties.size(); i++) {
      setSingleForeignKeyValue(referencedEntity, referenceProperties.get(i), foreignColumnProperties.get(i));
    }
  }

  private void setSingleForeignKeyValue(final Entity referencedEntity,
                                        final Property.ColumnProperty referenceProperty,
                                        final Property.ColumnProperty foreignColumnProperty) {
    if (!(referenceProperty instanceof Property.MirrorProperty)) {
      put(referenceProperty, referencedEntity == null ? null : referencedEntity.get(foreignColumnProperty), false);
    }
  }

  /**
   * Sets the denormalized property values
   * @param foreignKeyProperty the foreign key property from which to denormalize the values
   * @param referencedEntity the foreign key entity containing the values to denormalize
   */
  private void setDenormalizedValues(final Property.ForeignKeyProperty foreignKeyProperty, final Entity referencedEntity) {
    final List<Property.DenormalizedProperty> denormalizedProperties =
            definition.getDenormalizedProperties(foreignKeyProperty.getPropertyId());
    if (denormalizedProperties != null) {
      for (int i = 0; i < denormalizedProperties.size(); i++) {
        final Property.DenormalizedProperty denormalizedProperty = denormalizedProperties.get(i);
        put(denormalizedProperty, referencedEntity == null ? null : referencedEntity.get(denormalizedProperty
                .getDenormalizedProperty()), false);
      }
    }
  }

  /**
   * Creates the primary key referenced by the given foreign key
   * @param foreignKeyProperty the foreign key
   * @return the referenced primary key or null if a valid key can not be created (null values for non-nullable properties)
   */
  private Key initializeReferencedKey(final Property.ForeignKeyProperty foreignKeyProperty) {
    final List<Property.ColumnProperty> properties = foreignKeyProperty.getProperties();
    final Definition entityDefinition = domain.getDefinition(foreignKeyProperty.getForeignEntityId());
    if (foreignKeyProperty.isCompositeKey()) {
      final List<Property.ColumnProperty> foreignProperties = entityDefinition.getPrimaryKeyProperties();
      final Map<Property.ColumnProperty, Object> values = new HashMap<>(properties.size());
      for (int i = 0; i < properties.size(); i++) {
        final Property.ColumnProperty foreignProperty = foreignProperties.get(i);
        final Object value = super.get(properties.get(i));
        if (!foreignProperty.isNullable() && value == null) {
          return null;
        }
        else {
          values.put(foreignProperty, value);
        }
      }

      return new DefaultKey(entityDefinition, values);
    }
    else {
      final Object value = super.get(properties.get(0));
      if (value == null) {
        return null;
      }

      return new DefaultKey(entityDefinition, value);
    }
  }

  private void cacheReferencedKey(final String fkPropertyId, final Key referencedPrimaryKey) {
    if (referencedKeyCache == null) {
      referencedKeyCache = new HashMap<>();
    }
    referencedKeyCache.put(fkPropertyId, referencedPrimaryKey);
  }

  private Key getCachedReferencedKey(final String fkPropertyId) {
    if (referencedKeyCache == null) {
      return null;
    }

    return referencedKeyCache.get(fkPropertyId);
  }

  /**
   * Initializes a Key for this Entity instance
   * @param originalValues if true then the original values of the properties involved are used
   * @return a Key based on the values in this Entity instance
   */
  private Key initializeKey(final boolean originalValues) {
    final List<Property.ColumnProperty> primaryKeyProperties = definition.getPrimaryKeyProperties();
    if (primaryKeyProperties.size() > 1) {
      final Map<Property.ColumnProperty, Object> values = new HashMap<>(primaryKeyProperties.size());
      for (int i = 0; i < primaryKeyProperties.size(); i++) {
        final Property.ColumnProperty property = primaryKeyProperties.get(i);
        values.put(property, originalValues ? getOriginal(property) : super.get(property));
      }

      return new DefaultKey(definition, values);
    }
    else {
      return new DefaultKey(definition, originalValues ? getOriginal(primaryKeyProperties.get(0)) : super.get(primaryKeyProperties.get(0)));
    }
  }

  private Object getDerivedValue(final Property.DerivedProperty derivedProperty) {
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
      if (property instanceof Property.ColumnProperty) {
        final Property.ColumnProperty columnProperty = (Property.ColumnProperty) property;
        if (!columnProperty.isReadOnly() && columnProperty.isUpdatable()) {
          return true;
        }
      }
      if (property instanceof Property.TransientProperty) {
        return overrideModifiesEntity || ((Property.TransientProperty) property).isModifiesEntity();
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
      if (!(property instanceof Property.DerivedProperty)) {
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
    domain = Domain.getDomain(domainId);
    definition = domain.getDefinition(entityId);
    if (definition == null) {
      throw new IllegalArgumentException("Undefined entity: " + entityId);
    }
    final List<Property> properties = definition.getProperties();
    for (int i = 0; i < properties.size(); i++) {
      final Property property = properties.get(i);
      if (!(property instanceof Property.DerivedProperty)) {
        final boolean containsValue = stream.readBoolean();
        if (containsValue) {
          final Object value = stream.readObject();
          property.validateType(value);
          super.put(property, value);
          if (isModified && stream.readBoolean()) {
            setOriginalValue(property, stream.readObject());
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

  private static void validateValue(final Property property, final Object value) {
    if (property instanceof Property.DerivedProperty) {
      throw new IllegalArgumentException("Can not set the value of a derived property");
    }
    if (property instanceof Property.ValueListProperty && value != null && !((Property.ValueListProperty) property).isValid(value)) {
      throw new IllegalArgumentException("Invalid value list value: " + value + " for property " + property.getPropertyId());
    }
  }

  private static Map<Property, Object> createValueMap(final Key key) {
    final List<Property.ColumnProperty> properties = key.getProperties();
    final Map<Property, Object> values = new HashMap<>(properties.size());
    for (int i = 0; i < properties.size(); i++) {
      final Property.ColumnProperty property = properties.get(i);
      values.put(property, key.get(property));
    }

    return values;
  }

  /**
   * A class representing column key objects for entities.
   */
  static final class DefaultKey extends DefaultValueMap<Property.ColumnProperty, Object> implements Entity.Key {

    private static final long serialVersionUID = 1;

    /**
     * true if this key consists of a single integer value
     */
    private boolean singleIntegerKey;

    /**
     * true if this key consists of multiple properties
     */
    private boolean compositeKey;

    /**
     * Caching the hash code
     */
    private Integer cachedHashCode = null;

    /**
     * True if the value of a key property has changed, thereby invalidating the cached hash code value
     */
    private boolean hashCodeDirty = true;

    /**
     * Caching this extremely frequently referenced object
     */
    private Definition definition;

    /**
     * Instantiates a new DefaultKey for the given entity type, assuming it is a single value key
     * @param definition the entity definition
     * @param value the value
     * @throws IllegalArgumentException in case this key is a composite key
     */
    DefaultKey(final Definition definition, final Object value) {
      this(definition, createSingleValueMap(definition.getPrimaryKeyProperties().get(0), value));
      if (compositeKey) {
        throw new IllegalArgumentException(definition.getEntityId() + " has a composite primary key");
      }
    }

    /**
     * Instantiates a new Key for the given entity type
     * @param definition the entity definition
     */
    DefaultKey(final Definition definition, final Map<Property.ColumnProperty, Object> values) {
      super(values, null);
      this.definition = definition;
      final List<Property.ColumnProperty> properties = definition.getPrimaryKeyProperties();
      this.compositeKey = properties.size() > 1;
      this.singleIntegerKey = !compositeKey && properties.get(0).isInteger();
    }

    @Override
    public String getEntityId() {
      return definition.getEntityId();
    }

    @Override
    public List<Property.ColumnProperty> getProperties() {
      return definition.getPrimaryKeyProperties();
    }

    @Override
    public Property.ColumnProperty getFirstProperty() {
      return definition.getPrimaryKeyProperties().get(0);
    }

    @Override
    public Object getFirstValue() {
      return super.get(getFirstProperty());
    }

    @Override
    public Object put(final String propertyId, final Object value) {
      return put(getPrimaryKeyProperty(propertyId), value);
    }

    @Override
    public Object put(final Property.ColumnProperty property, final Object value) {
      property.validateType(value);

      return super.put(property, value);
    }

    @Override
    public Object get(final String propertyId) {
      return super.get(getPrimaryKeyProperty(propertyId));
    }

    @Override
    public String toString() {
      final StringBuilder stringBuilder = new StringBuilder();
      final List<Property.ColumnProperty> primaryKeyProperties = definition.getPrimaryKeyProperties();
      for (int i = 0; i < primaryKeyProperties.size(); i++) {
        final Property.ColumnProperty property = primaryKeyProperties.get(i);
        stringBuilder.append(property.getPropertyId()).append(":").append(super.get(property));
        if (i < getPropertyCount() - 1) {
          stringBuilder.append(",");
        }
      }

      return stringBuilder.toString();
    }

    @Override
    public Entity.Key newInstance() {
      return new DefaultKey(definition, (Map<Property.ColumnProperty, Object>) null);
    }

    @Override
    public int getPropertyCount() {
      if (compositeKey) {
        return getProperties().size();
      }

      return 1;
    }

    @Override
    public boolean isSingleIntegerKey() {
      return singleIntegerKey;
    }

    @Override
    public boolean isCompositeKey() {
      return compositeKey;
    }

    /**
     * Key objects are equal if the entityIds match as well as all property values.
     * @param obj the object to compare with
     * @return true if object is equal to this key
     */
    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj instanceof Key) {
        final String entityId = definition.getEntityId();
        final Key otherKey = (Key) obj;
        if (compositeKey) {
          return otherKey.isCompositeKey() && entityId.equals(otherKey.getEntityId()) && super.equals(otherKey);
        }
        if (singleIntegerKey) {
          return otherKey.isSingleIntegerKey() && isNull() == otherKey.isNull()
                  && hashCode() == otherKey.hashCode() && entityId.equals(otherKey.getEntityId());
        }
        //single non-integer key
        return !otherKey.isCompositeKey() && entityId.equals(otherKey.getEntityId()) && Objects.equals(getFirstValue(), otherKey.getFirstValue());
      }

      return false;
    }

    /**
     * @return a hash code based on the values of this key, for single integer keys the hash code is simply the key value.
     */
    @Override
    public int hashCode() {
      updateHashCode();

      return cachedHashCode == null ? 0 : cachedHashCode;
    }

    @Override
    public boolean isNull() {
      updateHashCode();

      return cachedHashCode == null;
    }

    @Override
    public boolean isNull(final String propertyId) {
      return super.isNull(definition.getPrimaryKeyPropertyMap().get(propertyId));
    }

    @Override
    public boolean isNotNull(final String propertyId) {
      return !isNull(propertyId);
    }

    @Override
    protected void handlePut(final Property.ColumnProperty property, final Object value, final Object previousValue,
                             final boolean initialization) {
      hashCodeDirty = true;
      if (singleIntegerKey) {
        if (!(value == null || value instanceof Integer)) {
          throw new IllegalArgumentException("Expecting a Integer value for Key: "
                  + definition.getEntityId() + ", " + property + ", got " + value + "; " + value.getClass());
        }
        setHashCode((Integer) value);
      }
    }

    @Override
    protected void handleClear() {
      cachedHashCode = null;
      hashCodeDirty = false;
    }

    private Property.ColumnProperty getPrimaryKeyProperty(final String propertyId) {
      final Property.ColumnProperty property = definition.getPrimaryKeyPropertyMap().get(propertyId);
      if (property == null) {
        throw new IllegalArgumentException("Primary key property " + propertyId + " not found in entity: " + definition.getEntityId());
      }

      return property;
    }

    private void setHashCode(final Integer value) {
      cachedHashCode = value;
      hashCodeDirty = false;
    }

    /**
     * Updates the cached hashCode in case it is dirty
     */
    private void updateHashCode() {
      if (hashCodeDirty) {
        cachedHashCode = computeHashCode();
        hashCodeDirty = false;
      }
    }

    private Integer computeHashCode() {
      if (size() == 0) {
        return null;
      }
      if (isCompositeKey()) {
        return computeCompositeHashCode();
      }

      return computeSingleHashCode();
    }

    private Integer computeCompositeHashCode() {
      int hash = 0;
      final List<Property.ColumnProperty> primaryKeyProperties = definition.getPrimaryKeyProperties();
      for (int i = 0; i < primaryKeyProperties.size(); i++) {
        final Property.ColumnProperty property = primaryKeyProperties.get(i);
        final Object value = super.get(property);
        if (!property.isNullable() && value == null) {
          return null;
        }
        if (value != null) {
          hash = hash + value.hashCode();
        }
      }

      return hash;
    }

    private Integer computeSingleHashCode() {
      final Property.ColumnProperty property = getFirstProperty();
      final Object value = super.get(property);
      if (value == null) {
        return null;
      }
      else if (singleIntegerKey) {
        return (Integer) value;
      }

      return value.hashCode();
    }

    private void writeObject(final ObjectOutputStream stream) throws IOException {
      stream.writeObject(definition.getDomainId());
      stream.writeObject(definition.getEntityId());
      final List<Property.ColumnProperty> primaryKeyProperties = definition.getPrimaryKeyProperties();
      for (int i = 0; i < primaryKeyProperties.size(); i++) {
        stream.writeObject(super.get(primaryKeyProperties.get(i)));
      }
    }

    private void readObject(final ObjectInputStream stream) throws IOException, ClassNotFoundException {
      final String domainId = (String) stream.readObject();
      final String entityId = (String) stream.readObject();
      definition = Domain.getDomain(domainId).getDefinition(entityId);
      if (definition == null) {
        throw new IllegalArgumentException("Undefined entity: " + entityId);
      }
      final List<Property.ColumnProperty> properties = definition.getPrimaryKeyProperties();
      compositeKey = properties.size() > 1;
      singleIntegerKey = !compositeKey && properties.get(0).isInteger();
      for (int i = 0; i < properties.size(); i++) {
        put(properties.get(i), stream.readObject());
      }
    }

    private static Map<Property.ColumnProperty, Object> createSingleValueMap(final Property.ColumnProperty keyProperty, final Object value) {
      final Map<Property.ColumnProperty, Object> values = new HashMap<>(1);
      values.put(keyProperty, value);

      return values;
    }
  }
}
