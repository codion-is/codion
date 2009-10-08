/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.model.Event;
import org.jminor.common.model.State;
import org.jminor.common.model.Util;

import java.awt.Color;
import java.io.Serializable;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a row in a database table
 */
public final class Entity implements Serializable, Comparable<Entity> {

  private static final long serialVersionUID = 1;

  /**
   * The primary key of this entity
   */
  private final Key primaryKey;

  /**
   * Holds the values of all properties except primary key properties
   */
  private final Map<String, Object> values = new HashMap<String, Object>();

  /**
   * Holds the original value of properties which values have changed,
   * including primary key properties
   */
  private Map<String, Object> originalValues;

  /**
   * An event fired when a property changes
   * @see #addPropertyListener(org.jminor.framework.domain.Property.Listener)
   * @see #removePropertyListener(org.jminor.framework.domain.Property.Listener)
   */
  private transient Event evtPropertyChanged;

  /**
   * A state indicating the modified status of this entity
   */
  private transient State stModified;

  /**
   * Used to cache the return value of the frequently called toString(),
   * invalidated each time a property value changes
   */
  private transient String toString;

  /**
   * Used to cache this frequently referenced attribute
   * may not be necessary, performance wise
   */
  private transient boolean hasDenormalizedProperties;

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
    hasDenormalizedProperties = EntityRepository.hasDenormalizedProperties(getEntityID());
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
   * Adds a PropertyListener, this listener will be notified each time a property value changes
   * @param propertyListener the PropertyListener
   */
  public void addPropertyListener(final Property.Listener propertyListener) {
    if (evtPropertyChanged == null)
      evtPropertyChanged = new Event();
    evtPropertyChanged.addListener(propertyListener);
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
   * @return a State object indicating whether this entity has been
   * modified since it was instantiated
   */
  public State getModifiedState() {
    if (stModified == null)
      stModified = new State(isModified());

    return stModified.getLinkedState();
  }

  /**
   * @return true if one or more properties have been modified
   * since the entity was instantiated
   */
  public boolean isModified() {
    return originalValues != null && originalValues.size() > 0;
  }

  /**
   * @param propertyID the property ID
   * @return true if the property has been modified since the entity was instantiated
   */
  public boolean isModified(final String propertyID) {
    return originalValues != null && originalValues.containsKey(propertyID);
  }

  /**
   * Sets the value of the given property
   * @param propertyID the ID of the property
   * @param value the new value
   */
  public void setValue(final String propertyID, final Object value) {
    setValue(propertyID, value, true);
  }

  /**
   * Sets the value of the given property
   * @param propertyID the ID of the property
   * @param value the new value
   * @param validateType set to true if basic type validation should be performed on the value
   */
  public void setValue(final String propertyID, final Object value, final boolean validateType) {
    setValue(getProperty(propertyID), value, validateType);
  }

  /**
   * Sets the value of the given property.
   * If <code>property</code> is an instance of Property.ForeignKeyProperty, denormalized values and
   * values comprising the foreign key are also set.
   * @param property the property
   * @param value the new value
   * @param validateType set to true if basic type validation should be performed on the value
   */
  public void setValue(final Property property, final Object value, final boolean validateType) {
    if (validateType)
      validateType(property, value);

    final boolean isPrimarKeyProperty = property instanceof Property.PrimaryKeyProperty;
    final boolean initialization = isPrimarKeyProperty ? !primaryKey.hasValue(property.getPropertyID())
            : !values.containsKey(property.getPropertyID());
    doSetValue(property, value, isPrimarKeyProperty, initialization, true);
  }

  /**
   * Initializing a value has the same effect as using <code>setValue()</code> except for Property.ForeignKeyPropertys
   * for which neither denormalized (Property.DenormalizedProperty) values nor the reference key values are set.
   * It is also assumed that the Entity does not contain a value for the property, so the modified state is
   * ignored during the value setting
   * Use with care.
   * @param property the property to initialize
   * @param value the initial value
   */
  public void initializeValue(final Property property, final Object value) {
    doSetValue(property, value, property instanceof Property.PrimaryKeyProperty, true, false);
  }

  /**
   * @param property the property for which to retrieve the value
   * @return the value of the <code>property</code>
   */
  public Object getValue(final Property property) {
    if (property instanceof Property.DenormalizedViewProperty)
      return getDenormalizedViewValue((Property.DenormalizedViewProperty) property);

    return getProxy(getEntityID()).getValue(this, property);
  }

  /**
   * @param propertyID the ID of the property for which to retrieve the value
   * @return the value of the property identified by <code>propertyID</code>
   */
  public Object getValue(final String propertyID) {
    return getValue(getProperty(propertyID));
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
   * Returns a date value formatted with <code>dateFormat</code>
   * @param propertyID the ID of the property for which to retrieve a formatted value
   * @param dateFormat the DateFormat to use when formatting the value
   * @return a formatted date value
   */
  public String getDateStringValue(final String propertyID, final DateFormat dateFormat) {
    return dateFormat.format(getDateValue(propertyID));
  }

  /**
   * @return true if the this entity instance has a null primary key
   */
  public boolean isNull() {
    return getPrimaryKey().isNull();
  }

  /**
   * Returns the value this property had when the entity was loaded
   * @param propertyID the property identifier
   * @return the original value of the property
   */
  public Object getOriginalValue(final String propertyID) {
    if (isModified(propertyID))
      return originalValues.get(propertyID);

    return getValue(propertyID);
  }

  /**
   * @param propertyID the property identifier
   * @return true if this entity contains a value for the property
   * N.B. does not include the primary key properties
   */
  public boolean hasValue(final String propertyID) {
    return values.containsKey(propertyID);
  }

  /**
   * @param propertyID the property identifier
   * @return the value of the property, bypassing the Entity.Proxy
   */
  public Object getRawValue(final String propertyID) {
    if (primaryKey.containsProperty(propertyID))
      return primaryKey.getValue(propertyID);

    return values.get(propertyID);
  }

  /**
   * @param entity the entity to compare to
   * @return true if all property values are equal
   */
  public boolean propertyValuesEqual(final Entity entity) {
    if (!entity.primaryKey.equals(primaryKey))
      return false;

    for (final Map.Entry<String, Object> entry : entity.values.entrySet()) {
      if (values.containsKey(entry.getKey())) {
        if (!Util.equal(entry.getValue(), values.get(entry.getKey())))
          return false;
      }
      else
        return false;
    }

    return true;
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
    try {
      final Entity ret = new Entity(getEntityID());
      ret.setAs(this);

      return ret;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException("Unable to copy entity: " + getEntityID() + ": " + e.getMessage(), e);
    }
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
    if (evtPropertyChanged != null)
      for (final Property property : EntityRepository.getProperties(getEntityID(), true))
        evtPropertyChanged.fire(new Property.Event(this, getEntityID(), property, getRawValue(property.getPropertyID()),
                null, true, true));

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

    for (int i = 0; i < foreignKeyProperty.referenceProperties.size(); i++) {
      final Property referenceKeyProperty = foreignKeyProperty.referenceProperties.get(i);
      final Object value = referenceKeyProperty instanceof Property.PrimaryKeyProperty
              ? this.primaryKey.getValue(referenceKeyProperty.getPropertyID())
              : values.get(referenceKeyProperty.getPropertyID());
      if (!isValueNull(referenceKeyProperty.getPropertyType(), value)) {
        if (primaryKey == null)
          (referencedPrimaryKeysCache == null ? referencedPrimaryKeysCache = new HashMap<Property.ForeignKeyProperty, Key>()
                  : referencedPrimaryKeysCache).put(foreignKeyProperty, primaryKey = new Key(foreignKeyProperty.referenceEntityID));
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
   * Returns true if <code>value</code> represents a null value for the given property type
   * @param propertyType the property type
   * @param value the value to check
   * @return true if <code>value</code> represents null
   */
  public static boolean isValueNull(final Type propertyType, final Object value) {
    return value == null || propertyType == Type.ENTITY
            && (value instanceof Entity ? ((Entity) value).isNull() : ((Key) value).isNull());
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
   * @param proxy sets the default EntityProxy instance used if no entity specific one is specified
   */
  public static void setDefaultProxy(final Proxy proxy) {
    defaultProxy = proxy;
  }

  public static void setProxy(final String entityID, final Proxy entityProxy) {
    if (proxies == null)
      proxies = new HashMap<String, Proxy>();

    proxies.put(entityID, entityProxy);
  }

  public static Proxy getProxy(final String entityID) {
    if (proxies != null && proxies.containsKey(entityID))
      return proxies.get(entityID);

    return defaultProxy;
  }

  /**
   * Performs the actual value setting, minding all the stuff that needs minding here
   * @param property the property
   * @param newValue the new value
   * @param isPrimaryKeyProperty true if the property is part of the primary key
   * @param initialization true if the property value is being initialized, if true it is assumed
   * that this Entity instance does not contain a value for the property, thus the modified state
   * can be safely disregarded during the value setting
   * @param propagateReferenceValues if set to true then both reference key values and
   * denormalized values are set in case <code>property</code> is a Property.ForeignKeyProperty.
   * @throws IllegalArgumentException in case <code>newValue</code> is the entity itself, preventing circular references
   */
  private void doSetValue(final Property property, final Object newValue, final boolean isPrimaryKeyProperty,
                          final boolean initialization, boolean propagateReferenceValues) {
    if (property instanceof Property.DenormalizedViewProperty)
      throw new IllegalArgumentException("Can not set the value of a denormalized property");
    if (newValue != null && newValue instanceof Entity && newValue.equals(this))
      throw new IllegalArgumentException("Circular entity reference detected: " + primaryKey + "->" + property.getPropertyID());

    //invalidate the toString cache
    toString = null;

    if (propagateReferenceValues && property instanceof Property.ForeignKeyProperty && (newValue == null || newValue instanceof Entity))
      propagateReferenceValues((Property.ForeignKeyProperty) property, (Entity) newValue);

    final Object oldValue = initialization ? null :
            isPrimaryKeyProperty ? primaryKey.getValue(property.getPropertyID()) : values.get(property.getPropertyID());
    if (isPrimaryKeyProperty)
      primaryKey.setValue(property.getPropertyID(), newValue);
    else
      values.put(property.getPropertyID(), newValue);

    if (!initialization)
      handleValueChange(property.getPropertyID(), property.getPropertyType(), newValue, oldValue);

    if (evtPropertyChanged != null && !isEqual(property.getPropertyType(), newValue, oldValue))
      evtPropertyChanged.fire(new Property.Event(this, getEntityID(), property, newValue, oldValue, true, initialization));
  }

  private void propagateReferenceValues(final Property.ForeignKeyProperty foreignKeyProperty, final Entity newValue) {
    referencedPrimaryKeysCache = null;
    setForeignKeyValues(foreignKeyProperty, newValue);
    if (hasDenormalizedProperties) {
      final Collection<Property.DenormalizedProperty> denormalizedProperties =
              EntityRepository.getDenormalizedProperties(getEntityID(), foreignKeyProperty.getPropertyID());
      setDenormalizedValues(foreignKeyProperty, newValue, denormalizedProperties);
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
                    : EntityRepository.getPrimaryKeyProperties(foreignKeyProperty.referenceEntityID);
    for (final Property.PrimaryKeyProperty primaryKeyProperty : referenceEntityPKProperties) {
      final Property referenceProperty = foreignKeyProperty.referenceProperties.get(primaryKeyProperty.getIndex());
      if (!(referenceProperty instanceof Property.MirrorProperty)) {
        final boolean isPrimaryKeyProperty = referenceProperty instanceof Property.PrimaryKeyProperty;
        final boolean initialization = isPrimaryKeyProperty ? !primaryKey.containsProperty(referenceProperty.getPropertyID())
                : !values.containsKey(referenceProperty.getPropertyID());
        doSetValue(referenceProperty, referencedEntity != null ? referencedEntity.getRawValue(primaryKeyProperty.getPropertyID()) : null,
                isPrimaryKeyProperty, initialization, true);
      }
    }
  }

  /**
   * Sets the denormalized property values
   * @param foreignKeyProperty the foreign key property
   * @param entity the entity value
   * @param denormalizedProperties the denormalized properties
   */
  private void setDenormalizedValues(final Property.ForeignKeyProperty foreignKeyProperty, final Entity entity,
                                     final Collection<Property.DenormalizedProperty> denormalizedProperties) {
    if (denormalizedProperties != null) {
      for (final Property.DenormalizedProperty denormalizedProperty : denormalizedProperties) {
        doSetValue(denormalizedProperty,
                entity == null ? null : entity.getRawValue(denormalizedProperty.denormalizedProperty.getPropertyID()),
                false, !values.containsKey(foreignKeyProperty.getPropertyID()), true);
      }
    }
  }

  /**
   * Updates the modified state of this entity according to the current property change
   * @param propertyID the property identifier
   * @param type the property type
   * @param newValue the new value
   * @param oldValue the previous value
   */
  private void handleValueChange(final String propertyID, final Type type, final Object newValue, final Object oldValue) {
    if (originalValues != null && originalValues.containsKey(propertyID)) {
      if (isEqual(type, originalValues.get(propertyID), newValue))
        originalValues.remove(propertyID);//we're back to the original value
    }
    else if (!isEqual(type, oldValue, newValue))
      (originalValues == null ? (originalValues = new HashMap<String, Object>()) : originalValues).put(propertyID, oldValue);

    if (stModified != null)
      stModified.setActive(isModified());
  }

  private Object getDenormalizedViewValue(final Property.DenormalizedViewProperty denormalizedViewProperty) {
    final Entity valueOwner = getEntityValue(denormalizedViewProperty.foreignKeyPropertyID);

    return valueOwner != null ? valueOwner.getValue(denormalizedViewProperty.denormalizedProperty) : null;
  }

  private String getDenormalizedViewValueAsString(final Property.DenormalizedViewProperty denormalizedViewProperty) {
    final Entity valueOwner = getEntityValue(denormalizedViewProperty.foreignKeyPropertyID);

    return valueOwner != null ? valueOwner.getValueAsString(denormalizedViewProperty.denormalizedProperty) : null;
  }

  /**
   * Performes a basic data validation of <code>value</code>, checking if the <code>value</code> data type is
   * consistent with the data type of this property, returns the value
   * @param value the value to validate
   * @param property the property
   * @return the value to validate
   * @throws IllegalArgumentException when the value is not of the same type as the propertyValue
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
  public static class Key implements Serializable {

    private static final long serialVersionUID = 1;

    /**
     * the entity ID
     */
    private final String entityID;

    /**
     * Contains the values of this key mapped to their respective propertyIDs
     */
    private final Map<String, Object> values;

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
      if (entityID == null)
        throw new IllegalArgumentException("Key can not be instantiated without an entityID");
      this.entityID = entityID;
      this.properties = EntityRepository.getPrimaryKeyProperties(entityID);
      this.values = new HashMap<String, Object>(properties.size());
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
        if (property.getPropertyID().equals(propertyID))
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
     * @return true if this key contains a value for propertyID
     */
    public boolean hasValue(final String propertyID) {
      return values.containsKey(propertyID);
    }

    /**
     * @param propertyID the property identifier
     * @return true if this key contains a property with the given identifier
     */
    public boolean containsProperty(final String propertyID) {
      for (final Property.PrimaryKeyProperty property : getProperties())
        if (property.getPropertyID().equals(propertyID))
          return true;

      return false;
    }

    /**
     * @param propertyID the property identifier
     * @return the value of the property identified by propertyID
     */
    public Object getValue(final String propertyID) {
      return values.get(propertyID);
    }

    /**
     * @return a string representation of this key
     */
    @Override
    public String toString() {
      final StringBuilder ret = new StringBuilder();
      int i = 0;
      for (final Property.PrimaryKeyProperty property : getProperties()) {
        ret.append(property.getPropertyID()).append("=").append(getValue(property.getPropertyID()));
        if (i++ < getPropertyCount()-1)
          ret.append(", ");
      }

      return ret.toString();
    }

    /**
     * @return an identical deep copy of this entity key
     */
    public Key copy() {
      final Key ret = new Key(entityID);
      ret.setValue(this);

      return ret;
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
     * @param key the key to compare with
     * @return true if key is equal to this key
     */
    public boolean equals(final Key key) {
      return key.entityID.equals(entityID) && hashCode() == key.hashCode();
    }

    /**
     * @return a hash code based on the values of this key, unique among entities of the same type
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
        if (isValueNull(property.getPropertyType(), values.get(property.getPropertyID())))
          return true;

      return false;
    }

    public static List<Key> copyEntityKeys(final List<Key> entityKeys) {
      final List<Key> ret = new ArrayList<Key>(entityKeys.size());
      for (final Key key : entityKeys)
        ret.add(key.copy());

      return ret;
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
        for (final Property.PrimaryKeyProperty property : getProperties())
          values.put(property.getPropertyID(), copyPropertyValue(key.getValue(property.getPropertyID())));

        hashCode = key.hashCode;
        hashCodeDirty = key.hashCodeDirty;
      }
    }

    /**
     * Sets the value of the property identified by propertyID to newValue
     * @param propertyID the property identifier
     * @param newValue the new value
     */
    void setValue(final String propertyID, final Object newValue) {
      values.put(propertyID, newValue);
      hashCodeDirty = true;
      if (isSingleIntegerKey()) {
        if (!(newValue == null || newValue instanceof Integer))
          throw new IllegalArgumentException("Expecting a Integer value for Key: " + entityID + ", "
                  + propertyID + ", got " + newValue + "; " + newValue.getClass());
        hashCode = newValue == null ? -Integer.MAX_VALUE : (Integer) newValue;
        hashCodeDirty = false;
      }
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

    public Object getValue(final Entity entity, final Property property) {
      if (property instanceof Property.DenormalizedViewProperty)
        throw new IllegalArgumentException("Entity.Proxy.getValue does not handle denormalized view properties (Property.DenormalizedViewProperty)");
      else if (property instanceof Property.PrimaryKeyProperty)
        return entity.getPrimaryKey().getValue(property.getPropertyID());
      else if (entity.hasValue(property.getPropertyID()))
        return entity.getRawValue(property.getPropertyID());
      else
        return property.getDefaultValue();
    }

    public int compareTo(final Entity entity, final Entity entityToCompare) {
      return entity.toString().compareTo(entityToCompare.toString());
    }

    public String toString(final Entity entity) {
      return entity.getEntityID() + ": " + entity.getPrimaryKey().toString();
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