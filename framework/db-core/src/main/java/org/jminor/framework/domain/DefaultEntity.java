/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.Util;
import org.jminor.common.db.valuemap.DefaultValueMap;
import org.jminor.common.db.valuemap.ValueMap;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Timestamp;
import java.text.Format;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a row in a database table, providing access to the column values via the {@link ValueMap} interface.
 */
final class DefaultEntity extends DefaultValueMap<Property, Object> implements Entity {

  private static final long serialVersionUID = 1;

  private static final String PROPERTY_PARAM = "property";
  private static final String PROPERTY_ID_PARAM = "propertyID";

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
   * The domain entities
   */
  private Entities entities;

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
   * @param definition the definition of the entity type
   */
  DefaultEntity(final Entities entities, final String entityID) {
    this(entities, entityID, null, null);
  }

  /**
   * Instantiates a new DefaultEntity
   * @param definition the definition of the entity type
   * @param key the primary key
   */
  DefaultEntity(final Entities entities, final String entityID, final Key key) {
    this(entities, entityID, null, null);
    final List<Property.ColumnProperty> properties = key.getProperties();
    for (int i = 0; i < properties.size(); i++) {
      final Property.ColumnProperty property = properties.get(i);
      put(property, key.get(property));
    }
    this.key = key;
  }

  /**
   * Instantiates a new DefaultEntity
   * @param definition the definition of the entity type
   * @param values the initial values
   */
  DefaultEntity(final Entities entities, final String entityID, final Map<Property, Object> values) {
    this(entities, entityID, values, null);
  }

  /**
   * Instantiates a new DefaultEntity based on the given values.
   * @param definition the definition of the entity type
   * @param values the initial values
   * @param originalValues the original values, may be null
   */
  DefaultEntity(final Entities entities, final String entityID, final Map<Property, Object> values,
                final Map<Property, Object> originalValues) {
    super(values, originalValues);
    this.entities = entities;
    this.definition = entities.getDefinition(entityID);
  }

  /** {@inheritDoc} */
  @Override
  public String getEntityID() {
    return definition.getEntityID();
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
  public boolean is(final String entityID) {
    return definition.getEntityID().equals(entityID);
  }

  /** {@inheritDoc} */
  @Override
  public Property getProperty(final String propertyID) {
    Objects.requireNonNull(propertyID, PROPERTY_ID_PARAM);
    final Property property = definition.getProperties().get(propertyID);
    if (property == null) {
      throw new IllegalArgumentException("Property " + propertyID + " not found in entity: " + definition.getEntityID());
    }

    return property;
  }

  /**
   * Returns true if one or more writable properties have been modified, read only and non-updatable properties
   * are excluded unless they are transient.
   * @return true if one or more properties have been modified since the entity was instantiated
   */
  @Override
  public boolean isModified() {
    return super.isModified() && writablePropertiesModified();
  }

  /** {@inheritDoc} */
  @Override
  public Object put(final String propertyID, final Object value) {
    return put(getProperty(propertyID), value);
  }

  /** {@inheritDoc} */
  @Override
  public Object put(final Property property, final Object value) {
    return put(property, value, true);
  }

  /** {@inheritDoc} */
  @Override
  public Object put(final Property property, final Object value, final boolean validateType) {
    Objects.requireNonNull(property, PROPERTY_PARAM);
    validateValue(property, value);
    if (validateType) {
      validateType(property, value);
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
   * @param propertyID the ID of the property for which to retrieve the value
   * @return the value of the property identified by {@code propertyID}
   */
  @Override
  public Object get(final String propertyID) {
    return get(getProperty(propertyID));
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
    Objects.requireNonNull(property, PROPERTY_PARAM);
    if (property instanceof Property.DenormalizedViewProperty) {
      return getDenormalizedViewValue((Property.DenormalizedViewProperty) property);
    }
    if (property instanceof Property.DerivedProperty) {
      return getDerivedValue((Property.DerivedProperty) property);
    }

    return super.get(property);
  }

  /**
   * Returns true if the value associated with the given property is null. In case of foreign key properties
   * the value of the underlying reference property is checked.
   * @param propertyID the property ID
   * @return true if the value associated with the property is null
   */
  @Override
  public boolean isValueNull(final String propertyID) {
    return isValueNull(getProperty(propertyID));
  }

  /**
   * Returns true if the value associated with the given property is null. In case of foreign key properties
   * the value of the underlying reference property is checked.
   * @param property the property
   * @return true if the value associated with the property is null
   */
  @Override
  public boolean isValueNull(final Property property) {
    Objects.requireNonNull(property, PROPERTY_PARAM);
    if (property instanceof Property.ForeignKeyProperty) {
      return isForeignKeyNull((Property.ForeignKeyProperty) property);
    }

    return super.isValueNull(property);
  }

  /** {@inheritDoc} */
  @Override
  public boolean isModified(final String propertyID) {
    return isModified(getProperty(propertyID));
  }

  /** {@inheritDoc} */
  @Override
  public Entity getForeignKey(final String foreignKeyPropertyID) {
    final Property property = getProperty(foreignKeyPropertyID);
    if (property instanceof Property.ForeignKeyProperty) {
      return getForeignKey((Property.ForeignKeyProperty) property);
    }

    throw new IllegalArgumentException(foreignKeyPropertyID + " is not a foreign key property");
  }

  /** {@inheritDoc} */
  @Override
  public Entity getForeignKey(final Property.ForeignKeyProperty foreignKeyProperty) {
    final Entity value = (Entity) super.get(foreignKeyProperty);
    if (value == null) {//possibly not loaded
      final Entity.Key referencedKey = getReferencedKey(foreignKeyProperty);
      if (referencedKey != null) {
        return new DefaultEntity(entities, referencedKey.getEntityID(), referencedKey);
      }
    }

    return value;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isLoaded(final String foreignKeyPropertyID) {
    return super.get(entities.getForeignKeyProperty(getEntityID(), foreignKeyPropertyID)) != null;
  }

  /** {@inheritDoc} */
  @Override
  public Date getDate(final String propertyID) {
    return (Date) get(propertyID);
  }

  /** {@inheritDoc} */
  @Override
  public Timestamp getTimestamp(final String propertyID) {
    return (Timestamp) get(propertyID);
  }

  /** {@inheritDoc} */
  @Override
  public String getString(final String propertyID) {
    return (String) get(propertyID);
  }

  /** {@inheritDoc} */
  @Override
  public Integer getInteger(final String propertyID) {
    return (Integer) get(propertyID);
  }

  /** {@inheritDoc} */
  @Override
  public Boolean getBoolean(final String propertyID) {
    return (Boolean) get(propertyID);
  }

  /** {@inheritDoc} */
  @Override
  public Character getCharacter(final String propertyID) {
    return (Character) get(propertyID);
  }

  /** {@inheritDoc} */
  @Override
  public Double getDouble(final String propertyID) {
    return (Double) get(propertyID);
  }

  /** {@inheritDoc} */
  @Override
  public String getAsString(final Property property) {
    if (property instanceof Property.ValueListProperty) {
      return ((Property.ValueListProperty) property).getCaption(get(property));
    }
    if (property instanceof Property.ForeignKeyProperty && !isLoaded(property.getPropertyID())) {
      final Entity.Key referencedKey = getReferencedKey((Property.ForeignKeyProperty) property);
      if (referencedKey != null) {
        return referencedKey.toString();
      }
    }

    return getFormatted(property, property.getFormat());
  }

  /** {@inheritDoc} */
  @Override
  public String getFormatted(final String propertyID, final Format format) {
    return getFormatted(getProperty(propertyID), format);
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
   * @param propertyID the ID of the property for which to retrieve the value
   * @return a String representation of the value of the property identified by {@code propertyID}
   * @see #getFormatted(Property, java.text.Format)
   */
  @Override
  public String getAsString(final String propertyID) {
    return getAsString(getProperty(propertyID));
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
  public Object getOriginal(final String propertyID) {
    return getOriginal(getProperty(propertyID));
  }

  /** {@inheritDoc} */
  @Override
  public void save(final String propertyID) {
    save(getProperty(propertyID));
  }

  /** {@inheritDoc} */
  @Override
  public void revert(final String propertyID) {
    revert(getProperty(propertyID));
  }

  /** {@inheritDoc} */
  @Override
  public void remove(final String propertyID) {
    remove(getProperty(propertyID));
  }

  /** {@inheritDoc} */
  @Override
  public boolean valuesEqual(final Entity entity) {
    Objects.requireNonNull(entity, "entity");
    final List<Property> propertyList = definition.getPropertyList();
    for (int i = 0; i < propertyList.size(); i++) {
      final Property property = propertyList.get(i);
      if (property instanceof Property.ColumnProperty && !Objects.equals(get(property), entity.get(property))) {
        return false;
      }
    }

    return true;
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
   * @return a new Entity instance with the same entityID as this entity
   */
  @Override
  public Entity newInstance() {
    return new DefaultEntity(entities, definition.getEntityID());
  }

  /** {@inheritDoc} */
  @Override
  public Key getReferencedKey(final Property.ForeignKeyProperty foreignKeyProperty) {
    Objects.requireNonNull(foreignKeyProperty, "foreignKeyProperty");
    if (!Objects.equals(getEntityID(), foreignKeyProperty.getEntityID())) {
      throw new IllegalArgumentException("Foreign key property " + foreignKeyProperty
              + " is not part of entity: " + getEntityID());
    }
    final String propertyID = foreignKeyProperty.getPropertyID();
    Key referencedPrimaryKey = getCachedReferencedKey(propertyID);
    if (referencedPrimaryKey != null) {
      return referencedPrimaryKey;
    }

    referencedPrimaryKey = initializeReferencedKey(foreignKeyProperty);
    if (referencedPrimaryKey != null) {
      cacheReferencedKey(propertyID, referencedPrimaryKey);
    }

    return referencedPrimaryKey;
  }

  /** {@inheritDoc} */
  @Override
  public boolean containsKey(final String propertyID) {
    return containsKey(getProperty(propertyID));
  }

  /**
   * Returns true if any of the properties involved in the given foreign key are null
   * @param foreignKeyProperty the foreign key property
   * @return true if the foreign key is null
   */
  @Override
  public boolean isForeignKeyNull(final Property.ForeignKeyProperty foreignKeyProperty) {
    Objects.requireNonNull(foreignKeyProperty, "foreignKeyProperty");
    final List<Property.ColumnProperty> referenceProperties = foreignKeyProperty.getReferenceProperties();
    if (referenceProperties.size() == 1) {
      return isValueNull(referenceProperties.get(0));
    }
    final List<Property.ColumnProperty> foreignProperties = entities.getReferencedProperties(foreignKeyProperty);
    for (int i = 0; i < referenceProperties.size(); i++) {
      if (!foreignProperties.get(i).isNullable() && isValueNull(referenceProperties.get(i))) {
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
      final List<Property.ColumnProperty> referenceProperties = ((Property.ForeignKeyProperty) property).getReferenceProperties();
      for (int i = 0; i < referenceProperties.size(); i++) {
        remove(referenceProperties.get(i));
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  protected void handleValueChangedEventInitialized() {
    if (definition.hasDerivedProperties()) {
      addValueListener(valueChange -> {
        final Collection<Property.DerivedProperty> linkedProperties = definition.getDerivedProperties(valueChange.getKey().getPropertyID());
        for (final Property.DerivedProperty property : linkedProperties) {
          final Object linkedValue = get(property);
          notifyValueChange(property, linkedValue, linkedValue, false);
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
   * Example: EntityOne references EntityTwo via entityTwoID, after a call to this method the EntityOne.entityTwoID
   * property has the value of EntityTwos primary key property. If {@code referencedEntity} is null then
   * the corresponding reference values are set to null.
   * @param foreignKeyProperty the entity reference property
   * @param referencedEntity the referenced entity
   * @param entityDefinitions a global entity definition map
   */
  private void setForeignKeyValues(final Property.ForeignKeyProperty foreignKeyProperty, final Entity referencedEntity) {
    referencedKeyCache = null;
    final List<Property.ColumnProperty> referenceProperties = foreignKeyProperty.getReferenceProperties();
    final List<Property.ColumnProperty> foreignColumnProperties = entities.getReferencedProperties(foreignKeyProperty);
    if (referenceProperties.size() > 1) {
      setCompositeForeignKeyValues(referencedEntity, referenceProperties, foreignColumnProperties);
    }
    else {
      setSingleForeignKeyValue(referencedEntity, referenceProperties.get(0), foreignColumnProperties.get(0));
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
    if (!(foreignColumnProperty instanceof Property.MirrorProperty)) {
      put(referenceProperty, referencedEntity == null ? null : referencedEntity.get(foreignColumnProperty), false);
    }
  }

  /**
   * Sets the denormalized property values
   * @param foreignKeyProperty the foreign key property referring to the value source
   * @param referencedEntity the entity value owning the denormalized values
   * @param entityDefinitions a global entity definition map
   */
  private void setDenormalizedValues(final Property.ForeignKeyProperty foreignKeyProperty, final Entity referencedEntity) {
    final List<Property.DenormalizedProperty> denormalizedProperties =
            definition.getDenormalizedProperties(foreignKeyProperty.getPropertyID());
    if (denormalizedProperties != null) {
      for (int i = 0; i < denormalizedProperties.size(); i++) {
        final Property.DenormalizedProperty denormalizedProperty = denormalizedProperties.get(i);
        put(denormalizedProperty, referencedEntity == null ? null : referencedEntity.get(denormalizedProperty
                .getDenormalizedProperty()), false);
      }
    }
  }

  private Object getDenormalizedViewValue(final Property.DenormalizedViewProperty denormalizedViewProperty) {
    final Entity valueSource = (Entity) get(denormalizedViewProperty.getForeignKeyPropertyID());
    if (valueSource == null) {
      return null;
    }

    return valueSource.get(denormalizedViewProperty.getDenormalizedProperty());
  }

  /**
   * Creates the primary key referenced by the given foreign key
   * @param foreignKeyProperty the foreign key
   * @return the referenced primary key or null if a valid key can not be created (null values for non-nullable properties)
   */
  private Key initializeReferencedKey(final Property.ForeignKeyProperty foreignKeyProperty) {
    final List<Property.ColumnProperty> foreignColumnProperties = entities.getReferencedProperties(foreignKeyProperty);
    final List<Property.ColumnProperty> referenceProperties = foreignKeyProperty.getReferenceProperties();
    if (foreignKeyProperty.isCompositeReference()) {
      final Map<Property.ColumnProperty, Object> values = new HashMap<>(referenceProperties.size());
      for (int i = 0; i < referenceProperties.size(); i++) {
        final Property.ColumnProperty foreignColumnProperty = foreignColumnProperties.get(i);
        final Object value = super.get(referenceProperties.get(i));
        if (!foreignColumnProperty.isNullable() && value == null) {
          return null;
        }
        else {
          values.put(foreignColumnProperty, value);
        }
      }

      return new DefaultKey(entities, entities.getDefinition(foreignKeyProperty.getReferencedEntityID()), values);
    }
    else {
      final Object value = super.get(referenceProperties.get(0));
      if (!foreignColumnProperties.get(0).isNullable() && value == null) {
        return null;
      }

      return new DefaultKey(entities, entities.getDefinition(foreignKeyProperty.getReferencedEntityID()), value);
    }
  }

  private void cacheReferencedKey(final String fkPropertyID, final Key referencedPrimaryKey) {
    if (referencedKeyCache == null) {
      referencedKeyCache = new HashMap<>();
    }
    referencedKeyCache.put(fkPropertyID, referencedPrimaryKey);
  }

  private Key getCachedReferencedKey(final String fkPropertyID) {
    if (referencedKeyCache == null) {
      return null;
    }

    return referencedKeyCache.get(fkPropertyID);
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

      return new DefaultKey(entities, definition, values);
    }
    else {
      return new DefaultKey(entities, definition, originalValues ? getOriginal(primaryKeyProperties.get(0)) : super.get(primaryKeyProperties.get(0)));
    }
  }

  private Object getDerivedValue(final Property.DerivedProperty derivedProperty) {
    final Map<String, Object> values = new HashMap<>(derivedProperty.getSourcePropertyIDs().size());
    final List<String> sourcePropertyIDs = derivedProperty.getSourcePropertyIDs();
    for (int i = 0; i < sourcePropertyIDs.size(); i++) {
      final String linkedPropertyID = sourcePropertyIDs.get(i);
      values.put(linkedPropertyID, get(linkedPropertyID));
    }

    return derivedProperty.getValueProvider().getValue(values);
  }

  private boolean writablePropertiesModified() {
    for (final Property property : originalKeySet()) {
      if (property instanceof Property.ColumnProperty) {
        final Property.ColumnProperty columnProperty = (Property.ColumnProperty) property;
        if (!columnProperty.isReadOnly() && columnProperty.isUpdatable()) {
          return true;
        }
      }
      if (property instanceof Property.TransientProperty) {
        return true;
      }
    }

    return false;
  }

  private void writeObject(final ObjectOutputStream stream) throws IOException {
    stream.writeObject(definition.getDomainID());
    stream.writeObject(definition.getEntityID());
    final boolean isModified = isModified();
    stream.writeBoolean(isModified);
    final List<Property> propertyList = definition.getPropertyList();
    for (int i = 0; i < propertyList.size(); i++) {
      final Property property = propertyList.get(i);
      if (!(property instanceof Property.DerivedProperty) && !(property instanceof Property.DenormalizedViewProperty)) {
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

  private void readObject(final ObjectInputStream stream) throws IOException, ClassNotFoundException {
    final String domainID = (String) stream.readObject();
    final String entityID = (String) stream.readObject();
    final boolean isModified = stream.readBoolean();
    entities = Entities.getDomainEntities(domainID);
    definition = entities.getDefinition(entityID);
    if (definition == null) {
      throw new IllegalArgumentException("Undefined entity: " + entityID);
    }
    final List<Property> propertyList = definition.getPropertyList();
    for (int i = 0; i < propertyList.size(); i++) {
      final Property property = propertyList.get(i);
      if (!(property instanceof Property.DerivedProperty) && !(property instanceof Property.DenormalizedViewProperty)) {
        super.put(property, stream.readObject());
        if (isModified && stream.readBoolean()) {
          setOriginalValue(property, stream.readObject());
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

    return value;
  }

  /**
   * Performs a basic data validation of {@code value}, checking if the {@code value} data type is
   * consistent with the data type of this property.
   * @param value the value to validate
   * @param property the property
   * @throws IllegalArgumentException when the value type does not fit the property type
   */
  private static void validateType(final Property property, final Object value) {
    if (value == null) {
      return;
    }

    property.validateType(value);
    if (property instanceof Property.ForeignKeyProperty) {
      final String fkPropertyEntityID = ((Property.ForeignKeyProperty) property).getReferencedEntityID();
      final String actualEntityID = ((Entity) value).getEntityID();
      if (!Objects.equals(fkPropertyEntityID, actualEntityID)) {
        throw new IllegalArgumentException("Entity of type " + fkPropertyEntityID + " expected for property " + property + ", got: " + actualEntityID);
      }
    }
  }

  private static void validateValue(final Property property, final Object value) {
    if (property instanceof Property.DenormalizedViewProperty) {
      throw new IllegalArgumentException("Can not set the value of a denormalized view property");
    }
    if (property instanceof Property.DerivedProperty) {
      throw new IllegalArgumentException("Can not set the value of a derived property");
    }
    if (property instanceof Property.ValueListProperty && value != null && !((Property.ValueListProperty) property).isValid(value)) {
      throw new IllegalArgumentException("Invalid value list value: " + value + " for property " + property.getPropertyID());
    }
  }

  /**
   * A class representing column key objects for entities.
   */
  static final class DefaultKey extends DefaultValueMap<Property.ColumnProperty, Object> implements Entity.Key {

    private static final long serialVersionUID = 1;

    /**
     * The domain entities
     */
    private Entities entities;

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
    DefaultKey(final Entities entities, final Definition definition, final Object value) {
      this(entities, definition, createSingleValueMap(definition.getPrimaryKeyProperties().get(0), value));
      if (compositeKey) {
        throw new IllegalArgumentException(definition.getEntityID() + " has a composite primary key");
      }
    }

    /**
     * Instantiates a new Key for the given entity type
     * @param definition the entity definition
     */
    DefaultKey(final Entities entities, final Definition definition, final Map<Property.ColumnProperty, Object> values) {
      super(values, null);
      this.entities = entities;
      this.definition = definition;
      final List<Property.ColumnProperty> properties = definition.getPrimaryKeyProperties();
      this.compositeKey = properties.size() > 1;
      this.singleIntegerKey = !compositeKey && properties.get(0).isInteger();
    }

    @Override
    public String getEntityID() {
      return definition.getEntityID();
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
    public Object put(final String propertyID, final Object value) {
      return put(entities.getColumnProperty(getEntityID(), propertyID), value);
    }

    @Override
    public Object put(final Property.ColumnProperty property, final Object value) {
      property.validateType(value);

      return super.put(property, value);
    }

    @Override
    public Object get(final String propertyID) {
      return super.get(entities.getColumnProperty(getEntityID(), propertyID));
    }

    @Override
    public String toString() {
      final StringBuilder stringBuilder = new StringBuilder();
      final List<Property.ColumnProperty> primaryKeyProperties = definition.getPrimaryKeyProperties();
      for (int i = 0; i < primaryKeyProperties.size(); i++) {
        final Property.ColumnProperty property = primaryKeyProperties.get(i);
        stringBuilder.append(property.getPropertyID()).append(":").append(super.get(property));
        if (i < getPropertyCount() - 1) {
          stringBuilder.append(",");
        }
      }

      return stringBuilder.toString();
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
     * Key objects are equal if the entityIDs match as well as all property values.
     * @param obj the object to compare with
     * @return true if object is equal to this key
     */
    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj instanceof Key) {
        final String entityID = definition.getEntityID();
        final Key otherKey = (Key) obj;
        if (compositeKey) {
          return otherKey.isCompositeKey() && entityID.equals(otherKey.getEntityID()) && super.equals(otherKey);
        }
        if (singleIntegerKey) {
          return otherKey.isSingleIntegerKey() && isNull() == otherKey.isNull()
                  && hashCode() == otherKey.hashCode() && entityID.equals(otherKey.getEntityID());
        }
        //single non-integer key
        return !otherKey.isCompositeKey() && entityID.equals(otherKey.getEntityID()) && Objects.equals(getFirstValue(), otherKey.getFirstValue());
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
    public boolean isValueNull(final String propertyID) {
      return super.isValueNull(entities.getColumnProperty(getEntityID(), propertyID));
    }

    @Override
    protected void handlePut(final Property.ColumnProperty property, final Object value, final Object previousValue,
                             final boolean initialization) {
      hashCodeDirty = true;
      if (singleIntegerKey) {
        if (!(value == null || value instanceof Integer)) {
          throw new IllegalArgumentException("Expecting a Integer value for Key: "
                  + definition.getEntityID() + ", " + property + ", got " + value + "; " + value.getClass());
        }
        setHashCode((Integer) value);
      }
    }

    @Override
    protected void handleClear() {
      cachedHashCode = null;
      hashCodeDirty = false;
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
      stream.writeObject(definition.getDomainID());
      stream.writeObject(definition.getEntityID());
      final List<Property.ColumnProperty> primaryKeyProperties = definition.getPrimaryKeyProperties();
      for (int i = 0; i < primaryKeyProperties.size(); i++) {
        stream.writeObject(super.get(primaryKeyProperties.get(i)));
      }
    }

    private void readObject(final ObjectInputStream stream) throws IOException, ClassNotFoundException {
      final String domainID = (String) stream.readObject();
      final String entityID = (String) stream.readObject();
      entities = Entities.getDomainEntities(domainID);
      definition = entities.getDefinition(entityID);
      if (definition == null) {
        throw new IllegalArgumentException("Undefined entity: " + entityID);
      }
      final List<Property.ColumnProperty> properties = definition.getPrimaryKeyProperties();
      compositeKey = properties.size() > 1;
      singleIntegerKey = !compositeKey && properties.get(0).isInteger();
      for (int i = 0; i < properties.size(); i++) {
        super.put(properties.get(i), stream.readObject());
      }
    }

    private static Map<Property.ColumnProperty, Object> createSingleValueMap(final Property.ColumnProperty keyProperty, final Object value) {
      final Map<Property.ColumnProperty, Object> values = new HashMap<>(1);
      values.put(keyProperty, value);

      return values;
    }
  }
}
