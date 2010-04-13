/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.model.ValueMapModel;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.io.Serializable;
import java.sql.Timestamp;
import java.text.Collator;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a row in a database table, providing access to the column values via the ValueMap interface.
 */
public final class Entity extends ValueMapModel implements Serializable, Comparable<Entity> {

  private static final long serialVersionUID = 1;

  /**
   * The primary key of this entity
   */
  private final Key primaryKey;

  /**
   * Used to cache the return value of the frequently called toString(),
   * invalidated each time a property value changes
   */
  private transient String toString;

  /**
   * Caches the result of <code>getReferencedPrimaryKey</code> method
   */
  private transient Map<Property.ForeignKeyProperty, Key> referencedPrimaryKeysCache;

  private static Map<String, Proxy> proxies;
  private static Proxy defaultProxy = new Proxy();

  /**
   * Instantiates a new Entity
   * @param entityID the ID of the entity type
   */
  public Entity(final String entityID) {
    this(new Key(entityID));
  }

  /**
   * Instantiates a new Entity
   * @param primaryKey the primary key
   */
  public Entity(final Key primaryKey) {
    if (primaryKey == null)
      throw new IllegalArgumentException("Can not instantiate a Entity without a primary key");
    this.primaryKey = primaryKey;
    this.primaryKey.eventPropertyChanged().addListener(eventPropertyChanged());
  }

  /**
   * @return the entity ID
   */
  public String getEntityID() {
    return primaryKey.getEntityID();
  }

  /**
   * @return the primary key of this entity
   */
  public Key getPrimaryKey() {
    return primaryKey;
  }

  /**
   * @param entityID the entityID
   * @return true if this entity is of the given type
   */
  public boolean is(final String entityID) {
    return getEntityID().equals(entityID);
  }

  /**
   * Retrieves the property identified by propertyID from the entity repository
   * @param propertyID the ID of the property to retrieve
   * @return the property identified by propertyID
   */
  public Property getProperty(final String propertyID) {
    return EntityRepository.getProperty(getEntityID(), propertyID);
  }

  /**
   * Adds a Property.Listener, this listener will be notified each time a property value changes
   * @param propertyListener the Property.Listener
   * @see org.jminor.framework.domain.Property.Event
   */
  public void addPropertyListener(final Property.Listener propertyListener) {
    eventPropertyChanged().addListener(propertyListener);
  }

  /**
   * Removes the given PropertyListener
   * @param propertyListener the PropertyListener to remove
   */
  public void removePropertyListener(final Property.Listener propertyListener) {
    if (evtPropertyChanged != null)
      evtPropertyChanged.removeListener(propertyListener);
  }

  /**
   * @param propertyID the property ID
   * @return true if the property has been modified since the entity was instantiated
   */
  @Override
  public boolean isModified(final String propertyID) {
    return super.isModified(propertyID) || primaryKey.isModified(propertyID);
  }

  /**
   * @return true if one or more properties have been modified
   * since the entity was instantiated
   */
  @Override
  public boolean isModified() {
    return super.isModified() || primaryKey.isModified();
  }

  /**
   * Returns the value this property had when the entity was loaded
   * @param propertyID the property identifier
   * @return the original value of the property
   */
  @Override
  public Object getOriginalValue(String propertyID) {
    final Property property = getProperty(propertyID);
    if (property instanceof Property.PrimaryKeyProperty)
      return primaryKey.getOriginalValue(propertyID);

    return super.getOriginalValue(propertyID);
  }

  /**
   * Sets the value of the given property.
   * If <code>property</code> is an instance of Property.ForeignKeyProperty, denormalized values and
   * values comprising the foreign key are also set.
   * @param property the property
   * @param value the new value
   * @return the old value
   */
  public Object setValue(final Property property, final Object value) {
    return setValue(property.getPropertyID(), value);
  }

  @Override
  public Object setValue(final String key, final Object value) {
    final Property property = getProperty(key);
    if (property instanceof Property.PrimaryKeyProperty)
      return primaryKey.setValue(key, value);
    if (property instanceof Property.DenormalizedViewProperty)
      throw new IllegalArgumentException("Can not set the value of a denormalized property");
    if (value != null && value instanceof Entity && value.equals(this))
      throw new IllegalArgumentException("Circular entity reference detected: " + primaryKey + "->" + property.getPropertyID());

    validateType(property, value);

    toString = null;
    if (property instanceof Property.ForeignKeyProperty && (value == null || value instanceof Entity))
      propagateReferenceValues((Property.ForeignKeyProperty) property, (Entity) value);

    return super.setValue(key, value);
  }

  /**
   * @param property the property for which to retrieve the value
   * @return the value of the <code>property</code>
   */
  public Object getValue(final Property property) {
    return getValue(property.getPropertyID());//todo
  }

  /**
   * @param propertyID the ID of the property for which to retrieve the value
   * @return the value of the property identified by <code>propertyID</code>
   */
  @Override
  public Object getValue(final String propertyID) {
    final Property property = getProperty(propertyID);
    if (property instanceof Property.PrimaryKeyProperty)
      return primaryKey.getValue(propertyID);
    if (property instanceof Property.DenormalizedViewProperty)
      return getDenormalizedViewValue((Property.DenormalizedViewProperty) property);

    return getProxy(getEntityID()).getValue(this, property);
  }

  /**
   * @param propertyID the ID of the property for which to retrieve the value
   * @return the value of the property identified by <code>propertyID</code>,
   * assuming it is an Entity
   * @throws ClassCastException if the value is not an Entity instance
   */
  public Entity getEntityValue(final String propertyID) {
    return (Entity) getValue(propertyID);
  }

  /**
   * @param propertyID the ID of the date property for which to retrieve the value
   * @return the value of the property identified by <code>propertyID</code>,
   * assuming it is a Date
   * @throws ClassCastException if the value is not a Date instance
   */
  public Date getDateValue(final String propertyID) {
    return (Date) getValue(propertyID);
  }

  /**
   * @param propertyID the ID of the date property for which to retrieve the value
   * @return the value of the property identified by <code>propertyID</code>,
   * assuming it is a Timestamp
   * @throws ClassCastException if the value is not a Timestamp instance
   */
  public Timestamp getTimestampValue(final String propertyID) {
    return (Timestamp) getValue(propertyID);
  }

  /**
   * @param propertyID the ID of the property for which to retrieve the value
   * @return the value of the property identified by <code>propertyID</code>,
   * assuming it is a String
   * @throws ClassCastException if the value is not a String instance
   */
  public String getStringValue(final String propertyID) {
    return (String) getValue(propertyID);
  }

  /**
   * @param propertyID the ID of the property for which to retrieve the value
   * @return the value of the property identified by <code>propertyID</code>,
   * assuming it is an Integer
   * @throws ClassCastException if the value is not a Integer instance
   */
  public Integer getIntValue(final String propertyID) {
    return (Integer) getValue(propertyID);
  }

  /**
   * @param propertyID the ID of the property for which to retrieve the value
   * @return the value of the property identified by <code>propertyID</code>,
   * assuming it is a Boolean
   * @throws ClassCastException if the value is not a Boolean instance
   */
  public Boolean getBooleanValue(final String propertyID) {
    return (Boolean) getValue(propertyID);
  }

  /**
   * @param propertyID the ID of the property for which to retrieve the value
   * @return the value of the property identified by <code>propertyID</code>,
   * assuming it is a Character
   * @throws ClassCastException if the value is not a Character instance
   */
  public Character getCharValue(final String propertyID) {
    return (Character) getValue(propertyID);
  }

  /**
   * @param propertyID the ID of the property for which to retrieve the value
   * @return the value of the property identified by <code>propertyID</code>,
   * assuming it is a Double
   * @throws ClassCastException if the value is not a Double instance
   */
  public Double getDoubleValue(final String propertyID) {
    return (Double) getValue(propertyID);
  }

  /**
   * @param propertyID the ID of the property for which to retrieve the value
   * @return a String representation of the value of the property identified by <code>propertyID</code>
   * @see org.jminor.framework.domain.Entity.Proxy#getValueAsString(Entity, Property)
   */
  public String getValueAsString(final String propertyID) {
    return getValueAsString(getProperty(propertyID));
  }

  /**
   * @param property the property for which to retrieve the value
   * @return a String representation of the value of <code>property</code>
   * @see org.jminor.framework.domain.Entity.Proxy#getValueAsString(Entity, Property)
   */
  public String getValueAsString(final Property property) {
    if (property instanceof Property.DenormalizedViewProperty)
      return getDenormalizedViewValueAsString((Property.DenormalizedViewProperty) property);

    return getProxy(getEntityID()).getValueAsString(this, property);
  }

  /**
   * Returns the value to use when the property is shown in a table
   * @param propertyID the ID of the property for which to retrieve the value
   * @return the table representation of the value of the property identified by <code>propertyID</code>
   * @see org.jminor.framework.domain.Entity.Proxy#getTableValue(Entity, Property)
   */
  public Object getTableValue(final String propertyID) {
    return getTableValue(getProperty(propertyID));
  }

  /**
   * Returns the value to use when the property is shown in a table
   * @param property the property for which to retrieve the value
   * @return the table representation of the value of <code>property</code>
   * @see org.jminor.framework.domain.Entity.Proxy#getTableValue(Entity, Property)
   */
  public Object getTableValue(final Property property) {
    if (property instanceof Property.DenormalizedViewProperty)
      return getDenormalizedViewValue((Property.DenormalizedViewProperty) property);

    return getProxy(getEntityID()).getTableValue(this, property);
  }

  /**
   * Returns a date value formatted with <code>dateFormat</code>, in case the value is null
   * and empty string is returned
   * @param propertyID the ID of the property for which to retrieve a formatted value
   * @param dateFormat the DateFormat to use when formatting the value
   * @return a formatted date value, an empty string in case of a null value
   */
  public String getFormattedDate(final String propertyID, final DateFormat dateFormat) {
    final Date value = getDateValue(propertyID);
    return value == null ? "" : dateFormat.format(value);
  }

  /**
   * @return true if the this entity instance has a null primary key
   */
  public boolean isNull() {
    return getPrimaryKey().isNull();
  }

  /**
   * @param propertyID the property identifier
   * @return the value of the property, bypassing the Entity.Proxy
   */
  public Object getRawValue(final String propertyID) {
    if (primaryKey.containsProperty(propertyID))
      return primaryKey.getValue(propertyID);

    return super.getValue(propertyID);
  }

  /**
   * @param entity the entity to compare to
   * @return true if all property values are equal
   */
  public boolean propertyValuesEqual(final Entity entity) {
    return entity.primaryKey.equals(primaryKey) && super.equals(entity);
  }

  /**
   * @param object the object to compare with
   * @return true if the given object is an Entity and it´s primary key is equal to this ones
   */
  @Override
  public boolean equals(final Object object) {
    return this == object || object instanceof Entity && ((Entity) object).primaryKey.equals(primaryKey);
  }

  /**
   * @param entity the entity to compare with
   * @return the compare result from comparing <code>entity</code> with this Entity instance
   */
  public int compareTo(final Entity entity) {
    return getProxy(getEntityID()).compareTo(this, entity);
  }

  /**
   * Returns the hash code of the primary key
   */
  @Override
  public int hashCode() {
    return primaryKey.hashCode();
  }

  /**
   * @return a string representation of this entity
   * @see org.jminor.framework.domain.Entity.Proxy#toString(Entity)
   */
  @Override
  public String toString() {
    if (toString == null)
      toString = getProxy(getEntityID()).toString(this);

    return toString;
  }

  /**
   * @return a deep copy of this Entity
   */
  public Entity getCopy() {
    final Entity copy = new Entity(getEntityID());
    copy.setAs(this);

    return copy;
  }

  /**
   * @return a deep copy of this entity in its original state
   */
  public Entity getOriginalCopy() {
    final Entity copy = getCopy();
    if (originalValues != null)
      for (final Map.Entry<String, Object> entry : originalValues.entrySet())
        copy.values.put(entry.getKey(), copyPropertyValue(entry.getValue()));

    return copy;
  }

  /**
   * Makes this entity identical to <code>sourceEntity</code>.
   * Reference entity values, which are mutable, are deep copied with getCopy()
   * Original property values, if any are not deep-copied
   * @param sourceEntity the entity to copy
   */
  public void setAs(final Entity sourceEntity) {
    primaryKey.setValue(sourceEntity.getPrimaryKey());
    values.clear();
    if (originalValues != null)
      originalValues.clear();
    for (final Map.Entry<String, Object> entry : sourceEntity.values.entrySet())
      values.put(entry.getKey(), copyPropertyValue(sourceEntity.values.get(entry.getKey())));

    if (sourceEntity.originalValues != null && !sourceEntity.originalValues.isEmpty()) {
      if (originalValues == null)
        originalValues = new HashMap<String, Object>();
      originalValues.putAll(sourceEntity.originalValues);
    }
    if (eventPropertyChanged() != null)
      for (final Property property : EntityRepository.getProperties(getEntityID(), true))
        eventPropertyChanged().fire(getValueChangeEvent(property.getPropertyID(), getRawValue(property.getPropertyID()), null, true));

    toString = sourceEntity.toString;
    if (stModified != null)
      stModified.setActive(isModified());
  }

  /**
   * Returns the primary key of the entity referenced by the given ForeignKeyProperty
   * @param foreignKeyProperty the foreign key property for which to retrieve the underlying EntityKey
   * @return the primary key of the underlying entity, null if no entity is referenced
   */
  public Key getReferencedPrimaryKey(final Property.ForeignKeyProperty foreignKeyProperty) {
    Key primaryKey = referencedPrimaryKeysCache == null ? null : referencedPrimaryKeysCache.get(foreignKeyProperty);
    if (primaryKey != null)
      return primaryKey;

    for (int i = 0; i < foreignKeyProperty.getReferenceProperties().size(); i++) {
      final Property referenceKeyProperty = foreignKeyProperty.getReferenceProperties().get(i);
      final Object value = referenceKeyProperty instanceof Property.PrimaryKeyProperty
              ? this.primaryKey.getValue(referenceKeyProperty.getPropertyID())
              : super.getValue(referenceKeyProperty.getPropertyID());
      if (!isValueNull(referenceKeyProperty.getPropertyType(), value)) {
        if (primaryKey == null)
          (referencedPrimaryKeysCache == null ? referencedPrimaryKeysCache = new HashMap<Property.ForeignKeyProperty, Key>()
                  : referencedPrimaryKeysCache).put(foreignKeyProperty, primaryKey = new Key(foreignKeyProperty.getReferencedEntityID()));
        primaryKey.setValue(primaryKey.getProperties().get(i).getPropertyID(), value);
      }
      else
        break;
    }

    return primaryKey;
  }

  /**
   * @param propertyID the property identifier
   * @return true if the value of the given property is null
   */
  @Override
  public boolean isValueNull(final String propertyID) {
    final Property property = getProperty(propertyID);
    final Object value = property instanceof Property.TransientProperty ? getValue(propertyID) : getRawValue(propertyID);

    return isValueNull(property.getPropertyType(), value);
  }

  /**
   * True if the given objects are equal. Both objects being null results in true.
   * @param type the type
   * @param one the first object
   * @param two the second object
   * @return true if the given objects are equal
   */
  public static boolean isEqual(final Type type, final Object one, final Object two) {
    final boolean oneNull = isValueNull(type, one);
    final boolean twoNull = isValueNull(type, two);

    return oneNull && twoNull || !(oneNull ^ twoNull) && one.equals(two);
  }

  /**
   * Returns true if <code>value</code> represents a null value for the given property type.
   * An empty string is regarded as null.
   * @param propertyType the property type
   * @param value the value to check
   * @return true if <code>value</code> represents null
   */
  public static boolean isValueNull(final Type propertyType, final Object value) {
    if (value == null)
      return true;

    if (propertyType == Type.STRING)
      return ((String) value).length() == 0;
    if (propertyType == Type.ENTITY) {
      final Entity.Key key = value instanceof Entity ? ((Entity) value).getPrimaryKey() : (Entity.Key) value;
      return key.isNull();
    }

    return false;
  }

  /**
   * Returns a copy of the given value.
   * If the value is an entity it is deep copied.
   * @param value the value to copy
   * @return a copy of <code>value</code>
   */
  public static Object copyPropertyValue(final Object value) {
    return value instanceof Entity ? ((Entity)value).getCopy() : value;
  }

  /**
   * @param entities the entities to check, assumes they are all of the same type
   * @return true if any of the given entities has a modified primary key property
   */
  public static boolean isPrimaryKeyModified(final Collection<Entity> entities) {
    if (entities == null || entities.size() == 0)
      return false;

    for (final Property.PrimaryKeyProperty property :
            EntityRepository.getPrimaryKeyProperties(entities.iterator().next().getEntityID())) {
      for (final Entity entity : entities)
        if (entity.isModified(property.getPropertyID()))
          return true;
    }

    return false;
  }

  /**
   * Sets the global default static proxy instance
   * @param proxy sets the default Entity.Proxy instance used if no entity specific one is specified
   * @see org.jminor.framework.domain.Entity.Proxy
   */
  public static void setDefaultProxy(final Proxy proxy) {
    defaultProxy = proxy;
  }

  /**
   * Sets a entity specific proxy instance
   * @param entityID the ID of the entity for which this proxy instance is used
   * @param entityProxy the proxy instance to link to the given entity ID
   * @see org.jminor.framework.domain.Entity.Proxy
   */
  public static void setProxy(final String entityID, final Proxy entityProxy) {
    if (proxies == null)
      proxies = new HashMap<String, Proxy>();

    if (proxies.containsKey(entityID))
      throw new RuntimeException("Proxy already defined for: " + entityID);

    proxies.put(entityID, entityProxy);
  }

  /**
   * Returns the proxy instance assigned to the given entity ID or the default proxy if none has been assigned
   * @param entityID the entity ID for which to retrieve the proxy
   * @return the proxy instance assigned to the given entity ID
   * @see org.jminor.framework.domain.Entity.Proxy
   */
  public static Proxy getProxy(final String entityID) {
    if (proxies != null && proxies.containsKey(entityID))
      return proxies.get(entityID);

    return defaultProxy;
  }

  static Entity initializeEntity(final String entityID, final Map<String, Object> values, final Map<String, Object> originalValues) {
    final Entity entity = new Entity(entityID);
    for (final Map.Entry<String, Object> entry : values.entrySet()) {
      entity.setValue(entry.getKey(), entry.getValue());
    }
    if (originalValues != null) {
      entity.originalValues = new HashMap<String, Object>(originalValues);
      entity.originalValues.putAll(originalValues);
    }

    return entity;
  }

  @Override
  public ActionEvent getValueChangeEvent(final String key, final Object newValue, final Object oldValue,
                                            final boolean initialization) {
    return initValueChangeEvent(this, getEntityID(), getProperty(key), newValue, oldValue, initialization);
  }

  static ActionEvent initValueChangeEvent(final Object source, final String entityID, final Property property, Object newValue, Object oldValue, boolean initialization) {
    return new Property.Event(source, entityID, property, newValue, oldValue, true, initialization);
  }

  private void propagateReferenceValues(final Property.ForeignKeyProperty foreignKeyProperty, final Entity newValue) {
    referencedPrimaryKeysCache = null;
    setForeignKeyValues(foreignKeyProperty, newValue);
    if (EntityRepository.hasDenormalizedProperties(getEntityID())) {
      final Collection<Property.DenormalizedProperty> denormalizedProperties =
              EntityRepository.getDenormalizedProperties(getEntityID(), foreignKeyProperty.getPropertyID());
      setDenormalizedValues(newValue, denormalizedProperties);
    }
  }

  /**
   * Sets the values of the properties used in the reference to the corresponding values found in <code>referencedEntity</code>.
   * Example: EntityOne references EntityTwo via entityTwoID, after a call to this method the EntityOne.entityTwoID
   * property has the value of EntityTwo's primary key property. If <code>referencedEntity</code> is null then
   * the corresponding reference values are set to null.
   * @param foreignKeyProperty the entity reference property
   * @param referencedEntity the referenced entity
   */
  private void setForeignKeyValues(final Property.ForeignKeyProperty foreignKeyProperty, final Entity referencedEntity) {
    final Collection<Property.PrimaryKeyProperty> referenceEntityPKProperties =
            referencedEntity != null ? referencedEntity.primaryKey.getProperties()
                    : EntityRepository.getPrimaryKeyProperties(foreignKeyProperty.getReferencedEntityID());
    for (final Property.PrimaryKeyProperty primaryKeyProperty : referenceEntityPKProperties) {
      final Property referenceProperty = foreignKeyProperty.getReferenceProperties().get(primaryKeyProperty.getIndex());
      if (!(referenceProperty instanceof Property.MirrorProperty))
        setValue(referenceProperty.getPropertyID(), referencedEntity != null ? referencedEntity.getRawValue(primaryKeyProperty.getPropertyID()) : null);
    }
  }

  /**
   * Sets the denormalized property values
   * @param entity the entity value owning the denormalized values
   * @param denormalizedProperties the denormalized properties
   */
  private void setDenormalizedValues(final Entity entity, final Collection<Property.DenormalizedProperty> denormalizedProperties) {
    if (denormalizedProperties != null) {
      for (final Property.DenormalizedProperty denormalizedProperty : denormalizedProperties) {
        setValue(denormalizedProperty.getPropertyID(),
                entity == null ? null : entity.getRawValue(denormalizedProperty.getDenormalizedProperty().getPropertyID()));
      }
    }
  }

  private Object getDenormalizedViewValue(final Property.DenormalizedViewProperty denormalizedViewProperty) {
    final Entity valueOwner = getEntityValue(denormalizedViewProperty.getForeignKeyPropertyID());

    return valueOwner != null ? valueOwner.getValue(denormalizedViewProperty.getDenormalizedProperty()) : null;
  }

  private String getDenormalizedViewValueAsString(final Property.DenormalizedViewProperty denormalizedViewProperty) {
    final Entity valueOwner = getEntityValue(denormalizedViewProperty.getForeignKeyPropertyID());

    return valueOwner != null ? valueOwner.getValueAsString(denormalizedViewProperty.getDenormalizedProperty()) : null;
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
  private static Object validateType(final Property property, final Object value) throws IllegalArgumentException {
    if (value == null)
      return value;

    final String propertyID = property.getPropertyID();
    switch (property.getPropertyType()) {
      case INT:
        if (!(value instanceof Integer))
          throw new IllegalArgumentException("Integer value expected for property: " + propertyID + " (" + value.getClass() + ")");
        break;
      case DOUBLE:
        if (!(value instanceof Double))
          throw new IllegalArgumentException("Double value expected for property: " + propertyID + " (" + value.getClass() + ")");
        break;
      case BOOLEAN:
        if (!(value instanceof Boolean))
          throw new IllegalArgumentException("Boolean value expected for property: " + propertyID + " (" + value.getClass() + ")");
        break;
      case TIMESTAMP:
        if (!(value instanceof Timestamp))
          throw new IllegalArgumentException("Timestamp value expected for property: " + propertyID + " (" + value.getClass() + ")");
        break;
      case DATE:
        if (!(value instanceof Date))
          throw new IllegalArgumentException("Date value expected for property: " + propertyID + " (" + value.getClass() + ")");
        break;
      case ENTITY:
        if (!(value instanceof Entity))
          throw new IllegalArgumentException("Entity value expected for property: " + propertyID + " (" + value.getClass() + ")");
        final String requiredEntityID = ((Property.ForeignKeyProperty) property).getReferencedEntityID();
        if (!(((Entity) value).is(requiredEntityID)))
          throw new IllegalArgumentException("Entity of type " + requiredEntityID + " required, got " + ((Entity) value).getEntityID());
        break;
      case CHAR:
        if (!(value instanceof Character))
          throw new IllegalArgumentException("Character value expected for property: " + propertyID + " (" + value.getClass() + ")");
        break;
      case STRING:
        if (!(value instanceof String))
          throw new IllegalArgumentException("String value expected for property: " + propertyID + " (" + value.getClass() + ")");
        break;
    }

    return value;
  }

  /**
   * A class representing column key objects for entities, contains the values for those columns
   */
  public static class Key extends ValueMapModel implements Serializable {

    private static final long serialVersionUID = 1;

    /**
     * the entity ID
     */
    private final String entityID;

    /**
     * Caching the hash code
     */
    private int hashCode = -Integer.MAX_VALUE;

    /**
     * True if the value of a key property has changed, thereby invalidating the cached hash code value
     */
    private boolean hashCodeDirty = true;

    /**
     * Caching this extremely frequently referenced attribute
     */
    private transient List<Property.PrimaryKeyProperty> properties;

    /**
     * Instantiates a new EntityKey for the given entity type
     * @param entityID the entity ID
     */
    public Key(final String entityID) {
      super(1);
      if (entityID == null)
        throw new IllegalArgumentException("Key can not be instantiated without an entityID");
      this.entityID = entityID;
      this.properties = EntityRepository.getPrimaryKeyProperties(entityID);
    }

    /**
     * @return the entity ID
     */
    public String getEntityID() {
      return entityID;
    }

    /**
     * @return a List containing the properties comprising this key
     */
    public List<Property.PrimaryKeyProperty> getProperties() {
      if (properties == null)
        properties = EntityRepository.getPrimaryKeyProperties(entityID);

      return properties;
    }

    /**
     * @param propertyID the property identifier
     * @return the key property identified by propertyID, null if the property is not found
     */
    public Property.PrimaryKeyProperty getProperty(final String propertyID) {
      for (final Property.PrimaryKeyProperty property : getProperties())
        if (property.is(propertyID))
          return property;

      return null;
    }

    /**
     * @return the first key property
     */
    public Property getFirstKeyProperty() {
      return getProperties().size() > 0 ? getProperties().get(0) : null;
    }

    /**
     * @return the first value contained in this key, useful for single property keys
     */
    public Object getFirstKeyValue() {
      return values.values().iterator().next();
    }

    /**
     * @param propertyID the property identifier
     * @return true if this key contains a property with the given identifier
     */
    public boolean containsProperty(final String propertyID) {
      for (final Property.PrimaryKeyProperty property : getProperties())
        if (property.is(propertyID))
          return true;

      return false;
    }

    @Override
    public Object setValue(final String propertyID, final Object newValue) {
      hashCodeDirty = true;
      if (isSingleIntegerKey()) {
        if (!(newValue == null || newValue instanceof Integer))
          throw new IllegalArgumentException("Expecting a Integer value for Key: " + entityID + ", "
                  + propertyID + ", got " + newValue + "; " + newValue.getClass());
        hashCode = newValue == null ? -Integer.MAX_VALUE : (Integer) newValue;
        hashCodeDirty = false;
      }

      return super.setValue(propertyID, newValue);
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
        if (i++ < getPropertyCount() - 1)
          stringBuilder.append(",");
      }

      return stringBuilder.toString();
    }

    /**
     * @return an identical deep copy of this entity key
     */
    public Key copy() {
      final Key copy = new Key(entityID);
      copy.setValue(this);

      return copy;
    }

    /**
     * @return the number of properties comprising this key
     */
    public int getPropertyCount() {
      return getProperties().size();
    }

    /**
     * @param object the object to compare with
     * @return true if object is equal to this key
     */
    @Override
    public boolean equals(final Object object) {
      return this == object || object instanceof Key && ((Key) object).entityID.equals(entityID)
              && object.hashCode() == hashCode();
    }

    /**
     * @return a hash code based on the values of this key
     */
    @Override
    public int hashCode() {
      if (hashCodeDirty) {
        int hash = 0;
        for (final Object value : values.values())
          hash = hash + (value == null ? 0 : value.hashCode());

        hashCode = hash == 0 ? -Integer.MAX_VALUE : hash;//in case all values were null
        hashCodeDirty = false;
      }

      return hashCode;
    }

    /**
     * @return true if one of the properties has a null value
     */
    public boolean isNull() {
      if (isSingleIntegerKey())
        return hashCode() == -Integer.MAX_VALUE;

      if (hashCode() == -Integer.MAX_VALUE)
        return true;

      for (final Property property : getProperties())
        if (isValueNull(property.getPropertyID()))
          return true;

      return false;
    }

    @Override
    public boolean isValueNull(final String propertyID) {
      return Entity.isValueNull(getProperty(propertyID).getPropertyType(), getValue(propertyID));
    }

    public static List<Key> copy(final List<Key> entityKeys) {
      final List<Key> copies = new ArrayList<Key>(entityKeys.size());
      for (final Key key : entityKeys)
        copies.add(key.copy());

      return copies;
    }

    /**
     * Copies the values from key to this entity key
     * @param key the key to copy
     */
    void setValue(final Key key) {
      values.clear();
      hashCode = -Integer.MAX_VALUE;
      hashCodeDirty = true;
      if (key != null) {
        if (originalValues != null)
          originalValues.clear();
        values.clear();
        for (final Property.PrimaryKeyProperty property : getProperties()) {
          final Object value = copyPropertyValue(key.getValue(property.getPropertyID()));
          values.put(property.getPropertyID(), value);
          eventPropertyChanged().fire(initValueChangeEvent(this, getEntityID(), property, value, null, true));
        }
        if (stModified != null)
          stModified.setActive(false);
        hashCode = key.hashCode;
        hashCodeDirty = key.hashCodeDirty;
      }
    }

    @Override
    public ActionEvent getValueChangeEvent(final String key, final Object newValue, final Object oldValue,
                                              final boolean initialization) {
      return initValueChangeEvent(this, getEntityID(), getProperty(key), newValue, oldValue, initialization);
    }

    /**
     * @return true if this is a single integer column key
     */
    private boolean isSingleIntegerKey() {
      return getPropertyCount() == 1 && getFirstKeyProperty().getPropertyType() == Type.INT;
    }
  }

  /**
   * Acts as a proxy for retrieving values from Entity objects, allowing for plugged
   * in entity specific functionality, such as providing toString() and compareTo() implementations
   */
  public static class Proxy {
    protected final Collator collator = Collator.getInstance();

    public Object getValue(final Entity entity, final Property property) {
      if (property instanceof Property.DenormalizedViewProperty)
        throw new IllegalArgumentException("Entity.Proxy.getValue does not handle denormalized view properties (Property.DenormalizedViewProperty)");
      else if (property instanceof Property.PrimaryKeyProperty)
        return entity.getPrimaryKey().getValue(property.getPropertyID());
      else if (entity.containsValue(property.getPropertyID()))
        return entity.getRawValue(property.getPropertyID());
      else
        return property.getDefaultValue();
    }

    public int compareTo(final Entity entity, final Entity entityToCompare) {
      return collator.compare(entity.toString(), entityToCompare.toString());
    }

    public String toString(final Entity entity) {
      final String entityID = entity.getEntityID();
      final ToString stringProvider = EntityRepository.getStringProvider(entityID);

      return stringProvider == null ? new StringBuilder(entityID).append(": ").append(entity.getPrimaryKey()).toString() : stringProvider.toString(entity);
    }

    public String getValueAsString(final Entity entity, final Property property) {
      return entity.isValueNull(property.getPropertyID()) ? "" : getValue(entity, property).toString();
    }

    public Object getTableValue(final Entity entity, final Property property) {
      return getValue(entity, property);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public Color getBackgroundColor(final Entity entity) {
      return null;
    }
  }

}