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
  DefaultEntity(final Definition definition) {
    this(definition, null, null);
  }

  /**
   * Instantiates a new DefaultEntity
   * @param definition the definition of the entity type
   * @param key the primary key
   */
  DefaultEntity(final Definition definition, final Key key) {
    this(definition);
    for (final Property.ColumnProperty property : key.getProperties()) {
      put(property, key.get(property));
    }
    this.key = key;
  }

  /**
   * Instantiates a new DefaultEntity
   * @param definition the definition of the entity type
   * @param values the initial values
   */
  DefaultEntity(final Definition definition, final Map<Property, Object> values) {
    this(definition, values, null);
  }

  /**
   * Instantiates a new DefaultEntity based on the given values.
   * @param definition the definition of the entity type
   * @param values the initial values
   * @param originalValues the original values, may be null
   */
  DefaultEntity(final Definition definition, final Map<Property, Object> values, final Map<Property, Object> originalValues) {
    super(values, originalValues);
    this.definition = definition;
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
    return put(property, value, validateType, DefaultEntityDefinition.getDefinitionMap());
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
        return new DefaultEntity(DefaultEntityDefinition.getDefinitionMap().get(referencedKey.getEntityID()), referencedKey);
      }
    }

    return value;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isLoaded(final String foreignKeyPropertyID) {
    return super.get(Entities.getForeignKeyProperty(getEntityID(), foreignKeyPropertyID)) != null;
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
    for (final Property.ColumnProperty primaryKeyProperty : definition.getPrimaryKeyProperties()) {
      remove(primaryKeyProperty);
      removeOriginalValue(primaryKeyProperty);
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
    for (final Property property : definition.getProperties().values()) {
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
    return new DefaultEntity(definition);
  }

  /** {@inheritDoc} */
  @Override
  public Key getReferencedKey(final Property.ForeignKeyProperty foreignKeyProperty) {
    Objects.requireNonNull(foreignKeyProperty, "foreignKeyProperty");
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
    for (final Property property : foreignKeyProperty.getReferenceProperties()) {
      if (isValueNull(property.getPropertyID())) {
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
      for (final Property referenceProperty : ((Property.ForeignKeyProperty) property).getReferenceProperties()) {
        remove(referenceProperty);
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

  private void propagateForeignKeyValues(final Property.ForeignKeyProperty foreignKeyProperty, final Entity newValue,
                                         final Map<String, Definition> entityDefinitions) {
    setForeignKeyValues(foreignKeyProperty, newValue, entityDefinitions);
    if (definition.hasDenormalizedProperties()) {
      setDenormalizedValues(foreignKeyProperty, newValue, entityDefinitions);
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
  private void setForeignKeyValues(final Property.ForeignKeyProperty foreignKeyProperty, final Entity referencedEntity,
                                   final Map<String, Definition> entityDefinitions) {
    referencedKeyCache = null;
    final Collection<Property.ColumnProperty> referenceEntityPKProperties =
            entityDefinitions.get(foreignKeyProperty.getReferencedEntityID()).getPrimaryKeyProperties();
    for (final Property.ColumnProperty primaryKeyProperty : referenceEntityPKProperties) {
      final Property referenceProperty = foreignKeyProperty.getReferenceProperties().get(primaryKeyProperty.getPrimaryKeyIndex());
      if (!(referenceProperty instanceof Property.MirrorProperty)) {
        final Object value;
        if (referencedEntity == null) {
          value = null;
        }
        else {
          value = referencedEntity.get(primaryKeyProperty);
        }
        put(referenceProperty, value, false, entityDefinitions);
      }
    }
  }

  /**
   * Sets the denormalized property values
   * @param foreignKeyProperty the foreign key property referring to the value source
   * @param referencedEntity the entity value owning the denormalized values
   * @param entityDefinitions a global entity definition map
   */
  private void setDenormalizedValues(final Property.ForeignKeyProperty foreignKeyProperty, final Entity referencedEntity,
                                     final Map<String, Definition> entityDefinitions) {
    final Collection<Property.DenormalizedProperty> denormalizedProperties =
            definition.getDenormalizedProperties(foreignKeyProperty.getPropertyID());
    if (denormalizedProperties != null) {
      for (final Property.DenormalizedProperty denormalizedProperty : denormalizedProperties) {
        final Object value;
        if (referencedEntity == null) {
          value = null;
        }
        else {
          value = referencedEntity.get(denormalizedProperty.getDenormalizedProperty());
        }
        put(denormalizedProperty, value, false, entityDefinitions);
      }
    }
  }

  private Object getDenormalizedViewValue(final Property.DenormalizedViewProperty denormalizedViewProperty) {
    final Entity valueOwner = (Entity) get(denormalizedViewProperty.getForeignKeyPropertyID());
    if (valueOwner == null) {
      return null;
    }

    return valueOwner.get(denormalizedViewProperty.getDenormalizedProperty());
  }

  private Key initializeReferencedKey(final Property.ForeignKeyProperty foreignKeyProperty) {
    if (foreignKeyProperty.isCompositeReference()) {
      final List<Property.ColumnProperty> referenceProperties = foreignKeyProperty.getReferenceProperties();
      final Map<Property.ColumnProperty, Object> values = new HashMap<>(referenceProperties.size());
      for (final Property.ColumnProperty referenceKeyProperty : referenceProperties) {
        final Object value = super.get(referenceKeyProperty);
        if (value == null) {
          return null;
        }
        else {
          values.put(foreignKeyProperty.getReferencedProperty(referenceKeyProperty), value);
        }
      }

      return new DefaultKey(DefaultEntityDefinition.getDefinitionMap().get(foreignKeyProperty.getReferencedEntityID()), values);
    }
    else {
      final Property referenceKeyProperty = foreignKeyProperty.getReferenceProperties().get(0);
      final Object value = super.get(referenceKeyProperty);
      if (value == null) {
        return null;
      }

      return new DefaultKey(DefaultEntityDefinition.getDefinitionMap().get(foreignKeyProperty.getReferencedEntityID()), value);
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
    final Map<Property.ColumnProperty, Object> values = new HashMap<>(primaryKeyProperties.size());
    for (final Property.ColumnProperty property : primaryKeyProperties) {
      values.put(property, originalValues ? getOriginal(property) : super.get(property));
    }

    return new DefaultKey(definition, values);
  }

  private Object getDerivedValue(final Property.DerivedProperty derivedProperty) {
    final Map<String, Object> values = new HashMap<>(derivedProperty.getSourcePropertyIDs().size());
    for (final String linkedPropertyID : derivedProperty.getSourcePropertyIDs()) {
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

  /**
   * Sets the property value and propagates foreign key values
   * @param property the property
   * @param value the value
   * @param validateType if true then type validation is performed
   * @param entityDefinitions a global entity definition map
   * @return the old value
   */
  private Object put(final Property property, final Object value, final boolean validateType,
                     final Map<String, Definition> entityDefinitions) {
    Objects.requireNonNull(property, PROPERTY_PARAM);
    validateValue(this, property, value);
    if (validateType) {
      validateType(property, value);
    }
    if (property instanceof Property.ColumnProperty && ((Property.ColumnProperty) property).isPrimaryKeyProperty()) {
      key = null;
    }
    toString = null;
    if (property instanceof Property.ForeignKeyProperty) {
      propagateForeignKeyValues((Property.ForeignKeyProperty) property, (Entity) value, entityDefinitions);
    }

    return super.put(property, prepareValue(property, value));
  }

  private void writeObject(final ObjectOutputStream stream) throws IOException {
    stream.writeObject(definition.getEntityID());
    final boolean isModified = isModified();
    stream.writeBoolean(isModified);
    for (final Property property : definition.getProperties().values()) {
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
    final String entityID = (String) stream.readObject();
    final boolean isModified = stream.readBoolean();
    definition = DefaultEntityDefinition.getDefinitionMap().get(entityID);
    if (definition == null) {
      throw new IllegalArgumentException("Undefined entity: " + entityID);
    }
    for (final Property property : definition.getProperties().values()) {
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

  private static void validateValue(final DefaultEntity entity, final Property property, final Object value) {
    if (property instanceof Property.DenormalizedViewProperty) {
      throw new IllegalArgumentException("Can not set the value of a denormalized view property");
    }
    if (property instanceof Property.DerivedProperty) {
      throw new IllegalArgumentException("Can not set the value of a derived property");
    }
    if (property instanceof Property.ValueListProperty && value != null && !((Property.ValueListProperty) property).isValid(value)) {
      throw new IllegalArgumentException("Invalid value list value: " + value + " for property " + property.getPropertyID());
    }
    if (value instanceof Entity && primaryKeysEqual(entity, (Entity) value)) {
      throw new IllegalArgumentException("Circular entity reference detected: " + entity + "->" + property.getPropertyID());
    }
  }

  private static boolean primaryKeysEqual(final DefaultEntity entity1, final Entity entity2) {
    if (entity1.getEntityID().equals(entity2.getEntityID())) {
      for (final Property.ColumnProperty property : entity1.definition.getPrimaryKeyProperties()) {
        if (!Objects.equals(entity1.get(property.getPropertyID()), entity2.get(property))) {
          return false;
        }
      }

      return true;
    }

    return false;
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

    /**
     * Instantiates a new DefaultKey for the given entity type, assuming it is a single value key
     * @param definition the entity definition
     * @param value the value
     * @throws IllegalArgumentException in case this key is a composite key
     */
    DefaultKey(final Definition definition, final Object value) {
      this(definition, createSingleValueMap(definition, value));
      if (compositeKey) {
        throw new IllegalArgumentException(definition.getEntityID() + " has a composite primary key");
      }
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
      return getProperties().get(0);
    }

    @Override
    public Object getFirstValue() {
      return values().iterator().next();
    }

    @Override
    public Object put(final String propertyID, final Object value) {
      return super.put(Entities.getColumnProperty(getEntityID(), propertyID), value);
    }

    @Override
    public Object get(final String propertyID) {
      return super.get(Entities.getColumnProperty(getEntityID(), propertyID));
    }

    @Override
    public String toString() {
      final StringBuilder stringBuilder = new StringBuilder();
      int i = 0;
      for (final Property.ColumnProperty property : getProperties()) {
        stringBuilder.append(property.getPropertyID()).append(":").append(super.get(property));
        if (i++ < getPropertyCount() - 1) {
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
      if (cachedHashCode == null) {
        return true;
      }

      for (final Object value : values()) {
        if (value != null) {
          return false;
        }
      }

      return true;
    }

    @Override
    public boolean isValueNull(final String propertyID) {
      return super.isValueNull(Entities.getColumnProperty(getEntityID(), propertyID));
    }

    @Override
    protected void handlePut(final Property.ColumnProperty key, final Object value, final Object previousValue,
                             final boolean initialization) {
      hashCodeDirty = true;
      if (singleIntegerKey) {
        if (!(value == null || value instanceof Integer)) {
          throw new IllegalArgumentException("Expecting a Integer value for Key: "
                  + definition.getEntityID() + ", " + key + ", got " + value + "; " + value.getClass());
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
        final Collection values = values();
        boolean nullValue = values.isEmpty();
        int hash = 0;
        if (!nullValue) {
          for (final Object value : values) {
            if (value != null) {
              hash = hash + value.hashCode();
              nullValue = false;
            }
          }
        }

        if (nullValue) {
          cachedHashCode = null;
        }
        else {
          cachedHashCode = hash;
        }
        hashCodeDirty = false;
      }
    }

    private void writeObject(final ObjectOutputStream stream) throws IOException {
      stream.writeObject(definition.getEntityID());
      for (final Property.ColumnProperty property : definition.getPrimaryKeyProperties()) {
        stream.writeObject(super.get(property));
      }
    }

    private void readObject(final ObjectInputStream stream) throws IOException, ClassNotFoundException {
      final String entityID = (String) stream.readObject();
      definition = DefaultEntityDefinition.getDefinitionMap().get(entityID);
      if (definition == null) {
        throw new IllegalArgumentException("Undefined entity: " + entityID);
      }
      final List<Property.ColumnProperty> properties = definition.getPrimaryKeyProperties();
      compositeKey = properties.size() > 1;
      singleIntegerKey = !compositeKey && properties.get(0).isInteger();
      for (final Property.ColumnProperty property : properties) {
        super.put(property, stream.readObject());
      }
    }

    private static Map<Property.ColumnProperty, Object> createSingleValueMap(final Definition definition, final Object value) {
      final Map<Property.ColumnProperty, Object> values = new HashMap<>(1);
      values.put(definition.getPrimaryKeyProperties().get(0), value);

      return values;
    }
  }
}
