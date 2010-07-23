/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.model.Util;
import org.jminor.common.model.valuemap.ValueChangeMapImpl;
import org.jminor.common.model.valuemap.ValueMap;
import org.jminor.common.model.valuemap.ValueMapImpl;

import java.io.IOException;
import java.io.Serializable;
import java.sql.Timestamp;
import java.text.Format;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a row in a database table, providing access to the column values via the ValueMap interface.
 */
final class EntityImpl extends ValueChangeMapImpl<String, Object> implements Entity, Serializable, Comparable<Entity> {

  private static final long serialVersionUID = 1;

  /**
   * The entity ID
   */
  private String entityID;

  /**
   * Used to cache the return value of the frequently called toString(),
   * invalidated each time a property value changes
   */
  private transient String toString;

  /**
   * Caches the result of <code>getReferencedPrimaryKey</code> method
   */
  private transient Map<Property.ForeignKeyProperty, Key> referencedPrimaryKeysCache;

  /**
   * Keep a reference to this frequently referenced map
   */
  private transient Map<String, Property> properties;

  /**
   * The primary key of this entity
   */
  private transient Key primaryKey;

  /**
   * Instantiates a new Entity
   * @param entityID the ID of the entity type
   */
  EntityImpl(final String entityID) {
    this.entityID = entityID;
    properties = EntityRepository.getProperties(entityID);
  }

  /**
   * Instantiates a new Entity
   * @param primaryKey the primary key
   */
  EntityImpl(final Key primaryKey) {
    this(Util.rejectNullValue(primaryKey, "primaryKey").getEntityID());
    for (final Property.PrimaryKeyProperty property : primaryKey.getProperties()) {
      setValue(property, primaryKey.getValue(property.getPropertyID()));
    }
    this.primaryKey = primaryKey;
  }

  public String getEntityID() {
    return entityID;
  }

  public Key getPrimaryKey() {
    if (primaryKey == null) {
      primaryKey = new KeyImpl(entityID);
      for (final Property.PrimaryKeyProperty property : EntityRepository.getPrimaryKeyProperties(entityID)) {
        primaryKey.setValue(property.getPropertyID(), getValue(property));
      }
    }

    return primaryKey;
  }

  public Key getOriginalPrimaryKey() {
    final Key key = new KeyImpl(entityID);
    for (final Property.PrimaryKeyProperty property : EntityRepository.getPrimaryKeyProperties(entityID)) {
      key.setValue(property.getPropertyID(), getOriginalValue(property.getPropertyID()));
    }

    return key;
  }

  public boolean is(final String entityID) {
    return this.entityID.equals(entityID);
  }

  public Property getProperty(final String propertyID) {
    Util.rejectNullValue(propertyID, "propertyID");
    if (properties == null) {
      this.properties = EntityRepository.getProperties(entityID);
    }

    final Property property = properties.get(propertyID);
    if (property == null) {
      throw new RuntimeException("Property " + propertyID + " not found in entity: " + entityID);
    }

    return property;
  }

  /**
   * Returns true if one or more writable properties have been modified,
   * read only and non-updatable properties are excluded unless they
   * are transient.
   * @return true if one or more properties have been modified
   * since the entity was instantiated
   */
  @Override
  public boolean isModified() {
    return writablePropertiesModified();
  }

  @Override
  public Object setValue(final String key, final Object value) {
    return setValue(getProperty(key), value);
  }

  public Object setValue(final Property property, final Object value) {
    return setValue(property, value, true);
  }

  /**
   * Initializes the given value assuming it has no previously set value.
   * This method does not propagate foreign key values but does set denormalized values if any exist.
   * This method should be used with care, if at all.
   * @param key the ID of the property for which to initialize the value
   * @param value the value
   */
  @Override
  public void initializeValue(final String key, final Object value) {
    initializeValue(getProperty(key), value);
  }

  public void initializeValue(final Property property, final Object value) {
    Util.rejectNullValue(property, "property");
    super.initializeValue(property.getPropertyID(), value);
  }

  /**
   * @param key the ID of the property for which to retrieve the value
   * @return the value of the property identified by <code>propertyID</code>
   */
  @Override
  public Object getValue(final String key) {
    return getValue(getProperty(key));
  }

  public Object getValue(final Property property) {
    Util.rejectNullValue(property, "property");
    if (property instanceof Property.DenormalizedViewProperty) {
      return getDenormalizedViewValue((Property.DenormalizedViewProperty) property);
    }
    if (property instanceof Property.DerivedProperty) {
      return EntityRepository.getProxy(entityID).getDerivedValue(this, (Property.DerivedProperty) property);
    }

    if (containsValue(property.getPropertyID())) {
      return super.getValue(property.getPropertyID());
    }
    else {
      return property.getDefaultValue();
    }
  }

  @Override
  public Object removeValue(final String key) {
    final Property property = getProperty(key);
    if (property instanceof Property.ForeignKeyProperty) {
      for (final Property fkProperty : ((Property.ForeignKeyProperty) property).getReferenceProperties()) {
        removeValue(fkProperty.getPropertyID());
      }
    }

    return super.removeValue(key);
  }

  @Override
  public boolean isValueNull(final String key) {
    return isValueNull(getProperty(key));
  }

  public boolean isValueNull(final Property property) {
    return super.isValueNull(Util.rejectNullValue(property, "property").getPropertyID());
  }

  public Entity getForeignKeyValue(final String foreignKeyPropertyID) {
    final Property property = getProperty(foreignKeyPropertyID);
    if (property instanceof Property.ForeignKeyProperty) {
      return (Entity) getValue(property.getPropertyID());
    }

    throw new RuntimeException(foreignKeyPropertyID + " is not a foreign key property");
  }

  public boolean isLoaded(final String foreignKeyPropertyID) {
    return !isValueNull(foreignKeyPropertyID);
  }

  public Date getDateValue(final String propertyID) {
    return (Date) getValue(propertyID);
  }

  public Timestamp getTimestampValue(final String propertyID) {
    return (Timestamp) getValue(propertyID);
  }

  public String getStringValue(final String propertyID) {
    return (String) getValue(propertyID);
  }

  public Integer getIntValue(final String propertyID) {
    return (Integer) getValue(propertyID);
  }

  public Boolean getBooleanValue(final String propertyID) {
    return (Boolean) getValue(propertyID);
  }

  public Character getCharValue(final String propertyID) {
    return (Character) getValue(propertyID);
  }

  public Double getDoubleValue(final String propertyID) {
    return (Double) getValue(propertyID);
  }

  /**
   * @param key the ID of the property for which to retrieve the value
   * @return a String representation of the value of the property identified by <code>propertyID</code>
   * @see #getFormattedValue(Property, java.text.Format)
   */
  @Override
  public String getValueAsString(final String key) {
    return getValueAsString(getProperty(key));
  }

  public String getValueAsString(final Property property) {
    if (property instanceof Property.DenormalizedViewProperty) {
      return getDenormalizedViewValueFormatted((Property.DenormalizedViewProperty) property);
    }
    if (property instanceof Property.ValueListProperty) {
      return ((Property.ValueListProperty) property).getCaption(getValue(property));
    }

    return getFormattedValue(property);
  }

  public String getFormattedValue(final String propertyID) {
    return getFormattedValue(getProperty(propertyID));
  }

  public String getFormattedValue(final String propertyID, final Format format) {
    return getFormattedValue(getProperty(propertyID), format);
  }

  public String getFormattedValue(final Property property) {
    Util.rejectNullValue(property, "property");
    return getFormattedValue(property, property.getFormat());
  }

  public String getFormattedValue(final Property property, final Format format) {
    return EntityRepository.getProxy(entityID).getFormattedValue(this, property, format);
  }

  @Override
  public void clear() {
    primaryKey = null;
    super.clear();
  }

  public boolean isNull() {
    return getPrimaryKey().isNull();
  }

  public boolean propertyValuesEqual(final Entity entity) {
    Util.rejectNullValue(entity, "entity");
    for (final Property property : EntityRepository.getColumnProperties(entity.getEntityID(), true, true, true, false)) {
      if (!Util.equal(getValue(property), entity.getValue(property))) {
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
    return this == obj || obj instanceof Entity && ((Entity) obj).getPrimaryKey().equals(getPrimaryKey());
  }

  /**
   * @param o the entity to compare with
   * @return the compare result from comparing <code>entity</code> with this Entity instance
   */
  public int compareTo(final Entity o) {
    return EntityRepository.getProxy(entityID).compareTo(this, o);
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
   * @see org.jminor.framework.domain.EntityRepository.Proxy#toString(Entity)
   */
  @Override
  public String toString() {
    if (toString == null) {
      toString = EntityRepository.getProxy(entityID).toString(this);
    }

    return toString;
  }

  /**
   * @return a new Entity instance with the same entityID as this entity
   */
  @Override
  public Entity getInstance() {
    return new EntityImpl(entityID);
  }

  /**
   * Makes this entity identical to <code>sourceEntity</code>, assuming it
   * is a Entity instance.
   * Reference entity values, which are mutable, are deep copied with getCopy()
   * @param sourceMap the Entity to copy
   * @throws IllegalArgumentException in case <code>sourceEntity</code> is not Entity instance.
   */
  @Override
  public void setAs(final ValueMap<String, Object> sourceMap) {
    super.setAs(sourceMap);
    toString = null;
    if (sourceMap instanceof Entity) {
      toString = sourceMap.toString();
    }
  }

  public Key getReferencedPrimaryKey(final Property.ForeignKeyProperty foreignKeyProperty) {
    Util.rejectNullValue(foreignKeyProperty, "foreignKeyProperty");
    Key referencedPrimaryKey = getCachedReferenceKey(foreignKeyProperty);
    if (referencedPrimaryKey != null) {
      return referencedPrimaryKey;
    }

    if (foreignKeyProperty.isCompositeReference()) {
      referencedPrimaryKey = initializeCompositeKey(foreignKeyProperty);
    }
    else {
      referencedPrimaryKey = initializeSingleValueKey(foreignKeyProperty);
    }

    cacheReferencedKey(foreignKeyProperty, referencedPrimaryKey);

    return referencedPrimaryKey;
  }

  public boolean containsValue(final Property property) {
    return containsValue(Util.rejectNullValue(property, "property").getPropertyID());
  }

  /**
   * Returns true if one or more of the properties involved in the given foreign key is null
   * @param foreignKeyProperty the foreign key property
   * @return true if the foreign key is null
   */
  public boolean isForeignKeyNull(final Property.ForeignKeyProperty foreignKeyProperty) {
    Util.rejectNullValue(foreignKeyProperty, "foreignKeyProperty");
    for (final Property property : foreignKeyProperty.getReferenceProperties()) {
      if (isValueNull(property.getPropertyID())) {
        return true;
      }
    }

    return false;
  }

  /**
   * Initializes a new Entity based on the given values.
   * @param entityID the entityID
   * @param values the values
   * @param originalValues the original values
   * @return an initialized Entity
   */
  static Entity entityInstance(final String entityID, final Map<String, Object> values, final Map<String, Object> originalValues) {
    Util.rejectNullValue(values, "values");
    final EntityImpl entity = new EntityImpl(entityID);
    for (final Map.Entry<String, Object> entry : values.entrySet()) {
      entity.setValue(entry.getKey(), entry.getValue());
    }
    if (originalValues != null) {
      for (final Map.Entry<String, Object> entry : originalValues.entrySet()) {
        entity.setOriginalValue(entry.getKey(), originalValues.get(entry.getKey()));
      }
    }

    return entity;
  }

  @Override
  protected void notifyValueChange(final String key, final Object value, final Object oldValue, final boolean initialization) {
    if (EntityRepository.hasLinkedProperties(entityID, key)) {
      final Collection<String> linkedPropertyIDs = EntityRepository.getLinkedPropertyIDs(entityID, key);
      for (final String propertyID : linkedPropertyIDs) {
        final Object linkedValue = getValue(propertyID);
        super.notifyValueChange(propertyID, linkedValue, linkedValue, false);
      }
    }
    super.notifyValueChange(key, value, oldValue, initialization);
  }

  private void propagateForeignKeyValues(final Property.ForeignKeyProperty foreignKeyProperty, final Entity newValue,
                                         final boolean initialization) {
    setForeignKeyValues(foreignKeyProperty, newValue, initialization);
    if (EntityRepository.hasDenormalizedProperties(entityID)) {
      setDenormalizedValues(foreignKeyProperty, newValue, initialization);
    }
  }

  /**
   * Sets the values of the properties used in the reference to the corresponding values found in <code>referencedEntity</code>.
   * Example: EntityOne references EntityTwo via entityTwoID, after a call to this method the EntityOne.entityTwoID
   * property has the value of EntityTwo's primary key property. If <code>referencedEntity</code> is null then
   * the corresponding reference values are set to null.
   * @param foreignKeyProperty the entity reference property
   * @param referencedEntity the referenced entity
   * @param initialization true if the values are being initialized
   */
  private void setForeignKeyValues(final Property.ForeignKeyProperty foreignKeyProperty, final Entity referencedEntity,
                                   final boolean initialization) {
    referencedPrimaryKeysCache = null;
    final Collection<Property.PrimaryKeyProperty> referenceEntityPKProperties =
            EntityRepository.getPrimaryKeyProperties(foreignKeyProperty.getReferencedEntityID());
    for (final Property.PrimaryKeyProperty primaryKeyProperty : referenceEntityPKProperties) {
      final Property referenceProperty = foreignKeyProperty.getReferenceProperties().get(primaryKeyProperty.getIndex());
      if (!(referenceProperty instanceof Property.MirrorProperty)) {
        final Object value;
        if (referencedEntity == null) {
          value = null;
        }
        else {
          value = referencedEntity.getValue(primaryKeyProperty);
        }
        if (initialization) {
          initializeValue(referenceProperty, value);
        }
        else {
          setValue(referenceProperty, value, false);
        }
      }
    }
  }

  /**
   * Sets the denormalized property values
   * @param foreignKeyProperty the foreign key property referring to the value source
   * @param referencedEntity the entity value owning the denormalized values
   * @param initialization true if the values are being initialized
   */
  private void setDenormalizedValues(Property.ForeignKeyProperty foreignKeyProperty, final Entity referencedEntity,
                                     final boolean initialization) {
    final Collection<Property.DenormalizedProperty> denormalizedProperties =
            EntityRepository.getDenormalizedProperties(entityID, foreignKeyProperty.getPropertyID());
    if (denormalizedProperties != null) {
      for (final Property.DenormalizedProperty denormalizedProperty : denormalizedProperties) {
        final Object value;
        if (referencedEntity == null) {
          value = null;
        }
        else {
          value = referencedEntity.getValue(denormalizedProperty.getDenormalizedProperty());
        }
        if (initialization) {
          initializeValue(denormalizedProperty, value);
        }
        else {
          setValue(denormalizedProperty, value, false);
        }
      }
    }
  }

  private Object getDenormalizedViewValue(final Property.DenormalizedViewProperty denormalizedViewProperty) {
    final Entity valueOwner = (Entity) getValue(denormalizedViewProperty.getForeignKeyPropertyID());
    if (valueOwner == null) {
      return null;
    }

    return valueOwner.getValue(denormalizedViewProperty.getDenormalizedProperty().getPropertyID());
  }

  private String getDenormalizedViewValueFormatted(final Property.DenormalizedViewProperty denormalizedViewProperty) {
    final Entity valueOwner = (Entity) getValue(denormalizedViewProperty.getForeignKeyPropertyID());
    if (valueOwner == null) {
      return null;
    }

    return valueOwner.getFormattedValue(denormalizedViewProperty.getDenormalizedProperty());
  }

  private Key initializeSingleValueKey(final Property.ForeignKeyProperty foreignKeyProperty) {
    final Property referenceKeyProperty = foreignKeyProperty.getReferenceProperties().get(0);
    final Object value = getValue(referenceKeyProperty);
    if (value == null) {
      return null;
    }

    return new KeyImpl(foreignKeyProperty.getReferencedEntityID(), value);
  }

  private Key initializeCompositeKey(final Property.ForeignKeyProperty foreignKeyProperty) {
    final Key key = new KeyImpl(foreignKeyProperty.getReferencedEntityID());
    for (final Property referenceKeyProperty : foreignKeyProperty.getReferenceProperties()) {
      final Object value = getValue(referenceKeyProperty);
      if (value == null) {
        return null;
      }
      else {
        key.setValue(foreignKeyProperty.getReferencedPropertyID(referenceKeyProperty), value);
      }
    }

    return key;
  }

  private boolean writablePropertiesModified() {
    for (final String propertyID : getOriginalValueKeys()) {
      final Property property = getProperty(propertyID);
      if (property instanceof Property.PrimaryKeyProperty || property instanceof Property.TransientProperty) {
        return true;
      }
      else if (property instanceof Property.ColumnProperty && ((Property.ColumnProperty) property).isUpdatable() && !property.isReadOnly()) {
        return true;
      }
    }

    return false;
  }

  private void cacheReferencedKey(final Property.ForeignKeyProperty foreignKeyProperty, final Key referencedPrimaryKey) {
    if (referencedPrimaryKey != null) {
      if (referencedPrimaryKeysCache == null) {
        referencedPrimaryKeysCache = new HashMap<Property.ForeignKeyProperty, Key>();
      }
      referencedPrimaryKeysCache.put(foreignKeyProperty, referencedPrimaryKey);
    }
  }

  private Key getCachedReferenceKey(final Property.ForeignKeyProperty foreignKeyProperty) {
    if (referencedPrimaryKeysCache == null) {
      return null;
    }

    return referencedPrimaryKeysCache.get(foreignKeyProperty);
  }

  private void writeObject(final java.io.ObjectOutputStream out) throws IOException {
    out.writeObject(entityID);
    for (final Property property : properties.values()) {
      if (!(property instanceof Property.DenormalizedViewProperty)) {
        out.writeObject(getValue(property));
      }
    }
    out.writeObject(getOriginalValues());
  }

  private void readObject(final java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
    entityID = (String) in.readObject();
    properties = EntityRepository.getProperties(entityID);
    for (final Property property : properties.values()) {
      if (!(property instanceof Property.DenormalizedViewProperty)) {
        setValue(property, in.readObject());
      }
    }
    final Map<String, Object> originalValues = (Map<String, Object>) in.readObject();
    if (originalValues != null) {
      for (final String propertyID : originalValues.keySet()) {
        setOriginalValue(propertyID, originalValues.get(propertyID));
      }
    }
  }

  /**
   * Sets the property value
   * @param property the property
   * @param value the value
   * @param validateType if true then type validation is performed
   * @return the old value
   */
  private Object setValue(final Property property, final Object value, final boolean validateType) {
    Util.rejectNullValue(property, "property");
    if (property instanceof Property.PrimaryKeyProperty) {
      this.primaryKey = null;
    }
    if (property instanceof Property.DenormalizedViewProperty) {
      throw new IllegalArgumentException("Can not set the value of a denormalized view property");
    }
    if (property instanceof Property.DerivedProperty) {
      throw new IllegalArgumentException("Can not set the value of a derived property");
    }
    if (property instanceof Property.ValueListProperty && value != null && !((Property.ValueListProperty) property).isValid(value)) {
      throw new IllegalArgumentException("Invalid value list value: " + value + " for property " + property.getPropertyID());
    }
    if (value instanceof Entity && value.equals(this)) {
      throw new IllegalArgumentException("Circular entity reference detected: " + this + "->" + property.getPropertyID());
    }

    if (validateType) {
      validateType(property, value);
    }

    toString = null;
    if (property instanceof Property.ForeignKeyProperty && (value == null || value instanceof Entity)) {
      propagateForeignKeyValues((Property.ForeignKeyProperty) property, (Entity) value, false);
    }

    return super.setValue(property.getPropertyID(), value);
  }

  /**
   * Performs a basic data validation of <code>value</code>, checking if the <code>value</code> data type is
   * consistent with the data type of this property, returns the value.
   * For foreign key properties this method also checks if the value entityID fits the key.
   * @param value the value to validate
   * @param property the property
   * @return the value to validate
   * @throws IllegalArgumentException when the value type does not fit the property type
   */
  private static Object validateType(final Property property, final Object value) {
    if (value == null) {
      return value;
    }

    final Class type = Util.getTypeClass(property.getType());
    if (!type.equals(value.getClass()) && !type.isAssignableFrom(value.getClass())) {
      throw new IllegalArgumentException("Value of type " + type + " expected for property " + property + ", got: " + value.getClass());
    }

    return value;
  }

  /**
   * A class representing column key objects for entities, contains the values for those columns.
   */
  static final class KeyImpl extends ValueMapImpl<String, Object> implements Entity.Key, Serializable {

    private static final long serialVersionUID = 1;

    private static final int INTEGER_NULL_VALUE = Integer.MAX_VALUE;

    /**
     * the entity ID
     */
    private final String entityID;

    /**
     * true if this key consists of a single integer value
     */
    private final boolean singleIntegerKey;

    /**
     * Caching the hash code
     */
    private int hashCode = INTEGER_NULL_VALUE;

    /**
     * True if the value of a key property has changed, thereby invalidating the cached hash code value
     */
    private boolean hashCodeDirty = true;

    /**
     * Caching this extremely frequently referenced attribute
     */
    private transient List<Property.PrimaryKeyProperty> properties;

    /**
     * Instantiates a new Key for the given entity type
     * @param entityID the entity ID
     */
    KeyImpl(final String entityID) {
      super(1);
      this.entityID = entityID;
      this.properties = EntityRepository.getPrimaryKeyProperties(entityID);
      this.singleIntegerKey = properties.size() == 1 && properties.get(0).isInteger();
    }

    /**
     * Instantiates a new Key for the given entity type, assuming it is a single value key
     * @param entityID the entity ID
     * @param value the value
     * @throws RuntimeException in case this key is a multi value key
     */
    KeyImpl(final String entityID, final Object value) {
      this(entityID);
      if (isCompositeKey()) {
        throw new RuntimeException("Not a single value key");
      }

      final Property property = properties.get(0);
      setValue(property.getPropertyID(), value);
      if (singleIntegerKey) {
        setHashcode(value);
      }
    }

    public String getEntityID() {
      return entityID;
    }

    public List<Property.PrimaryKeyProperty> getProperties() {
      if (properties == null) {
        properties = EntityRepository.getPrimaryKeyProperties(entityID);
      }

      return properties;
    }

    public boolean isCompositeKey() {
      return getPropertyCount() > 1;
    }

    public Property.PrimaryKeyProperty getFirstKeyProperty() {
      if (getPropertyCount() == 0) {
        throw new RuntimeException("No properties defined for primary key");
      }

      return getProperties().get(0);
    }

    public Object getFirstKeyValue() {
      return getValue(getFirstKeyProperty().getPropertyID());
    }

    @Override
    public Object setValue(final String key, final Object value) {
      Util.rejectNullValue(key, "key");
      hashCodeDirty = true;
      if (singleIntegerKey) {
        if (!(value == null || value instanceof Integer)) {
          throw new IllegalArgumentException("Expecting a Integer value for Key: " + entityID + ", "
                  + key + ", got " + value + "; " + value.getClass());
        }
        setHashcode(value);
      }

      return super.setValue(key, value);
    }

    @Override
    public void clear() {
      super.clear();
      hashCode = INTEGER_NULL_VALUE;
      hashCodeDirty = false;
    }

    /**
     * @return a string representation of this key
     */
    @Override
    public String toString() {
      final StringBuilder stringBuilder = new StringBuilder();
      int i = 0;
      for (final Property.PrimaryKeyProperty property : getProperties()) {
        stringBuilder.append(property.getPropertyID()).append(":").append(getValue(property.getPropertyID()));
        if (i++ < getPropertyCount() - 1) {
          stringBuilder.append(",");
        }
      }

      return stringBuilder.toString();
    }

    /**
     * @return an Key instance with the entityID of this key
     */
    @Override
    public Key getInstance() {
      return new KeyImpl(entityID);
    }

    public int getPropertyCount() {
      if (singleIntegerKey) {
        return 1;
      }

      return getProperties().size();
    }

    public boolean isSingleIntegerKey() {
      return singleIntegerKey;
    }

    /**
     * Key objects are equal if the entityIDs match as well as all property values.
     * @param obj the object to compare with
     * @return true if object is equal to this key
     */
    @SuppressWarnings({"SimplifiableIfStatement"})
    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj instanceof Key) {
        final Key key = (Key) obj;
        if (singleIntegerKey) {
          return key.isSingleIntegerKey() && hashCode() == key.hashCode() && entityID.equals(key.getEntityID());
        }
        else {
          return !key.isSingleIntegerKey() && entityID.equals(key.getEntityID()) && super.equals(key);
        }
      }

      return false;
    }

    /**
     * @return a hash code based on the values of this key, for single integer keys
     * the hash code is simply the key value.
     */
    @Override
    public int hashCode() {
      if (hashCodeDirty) {
        final Collection values = getValues();
        boolean nullValue = values.isEmpty();
        int hash = 0;
        if (!nullValue) {
          for (final Object value : values) {
            if (value != null) {
              hash = hash + value.hashCode();
            }
            else {
              nullValue = true;
              break;
            }
          }
        }

        if (nullValue) {
          hashCode = INTEGER_NULL_VALUE;
        }
        else {
          hashCode = hash;
        }
        hashCodeDirty = false;
      }

      return hashCode;
    }

    public boolean isNull() {
      if (singleIntegerKey) {
        return hashCode() == INTEGER_NULL_VALUE;
      }

      if (hashCode() == INTEGER_NULL_VALUE) {
        return true;
      }

      for (final Object value : getValues()) {
        if (value == null) {
          return true;
        }
      }

      return false;
    }

    private void setHashcode(final Object value) {
      if (value == null) {
        hashCode = INTEGER_NULL_VALUE;
      }
      else {
        hashCode = (Integer) value;
      }
      hashCodeDirty = false;
    }
  }
}
