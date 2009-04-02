/*
 * Copyright (c) 2008, Bj�rn Darri Sigur�sson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.model.Event;
import org.jminor.common.model.PropertyChangeEvent;
import org.jminor.common.model.State;
import org.jminor.common.model.Util;
import org.jminor.framework.FrameworkSettings;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a row in a database table
 */
public final class Entity implements Externalizable, Comparable<Entity> {

  /**
   * A central repository containing information about loaded entity definitions
   */
  private static final EntityRepository repository = EntityRepository.get();

  /**
   * The primary key of this entity
   */
  private EntityKey primaryKey;

  /**
   * Holds the values of all properties except primary key properties
   */
  private Map<String, Object> propertyValues = new HashMap<String, Object>();

  /**
   * Holds the original value of properties which values have changed
   */
  private Map<String, Object> originalPropertyValues;

  /**
   * An event fired when a property changes, is null until initialized with
   * a call to setFirePropertyChangeEvents
   */
  private transient Event evtPropertyChanged;

  /**
   * A state indicating the modified status of this entity
   */
  private transient State stModified;

  /**
   * Used to cache the return value of the frequently called toString(),
   * invalidated each time a property changes
   */
  private transient String toString;

  /**
   * Used to cache this frequently referenced attribute
   * may not be necessary, performance wise
   */
  private transient boolean hasDenormalizedProperties;

  private transient Map<Property.EntityProperty, EntityKey> referencedKeysCache;

  private static boolean propertyDebug = (Boolean) FrameworkSettings.get().getProperty(FrameworkSettings.PROPERTY_DEBUG_OUTPUT);

  /** Not for general usage, required for serialization */
  public Entity() {}

  /**
   * Instantiates a new entity
   * @param entityID the ID of the entity type
   */
  public Entity(final String entityID) {
    this(new EntityKey(entityID));
  }

  /**
   * Instantiates a new entity
   * @param key the primary key
   */
  public Entity(final EntityKey key) {
    primaryKey = key;
    hasDenormalizedProperties = repository.hasDenormalizedProperties(primaryKey.entityID);
  }

  /**
   * @return the entity ID
   */
  public String getEntityID() {
    return primaryKey.entityID;
  }

  /**
   * @return the primary key of this entity
   */
  public EntityKey getPrimaryKey() {
    return primaryKey;
  }

  /**
   * @param entityID the entityID
   * @return true if this entity is of the given type
   */
  public boolean is(final String entityID) {
    return primaryKey.entityID.equals(entityID);
  }

  /**
   * Retrieves the property identified by propertyID from the entity repository
   * @param propertyID the ID of the property to retrieve
   * @return the property identified by propertyID
   */
  public Property getProperty(final String propertyID) {
    return repository.getProperty(primaryKey.entityID, propertyID);
  }

  /**
   * @return an Event which is fired each time a property value changes
   * @see PropertyChangeEvent
   */
  public Event getPropertyChangeEvent() {
    if (evtPropertyChanged == null)
      evtPropertyChanged = new Event(getEntityID() + ".evtPropertyChanged");

    return evtPropertyChanged;
  }

  /**
   * @return a State object indicating whether this entity has been
   * modified since it was instantiated
   */
  public State getModifiedState() {
    if (stModified == null)
      stModified = new State(isModified());

    return stModified;
  }

  /**
   * @return true if one or more properties have been modified
   * since the entity was instantiated
   */
  public boolean isModified() {
    return originalPropertyValues != null && originalPropertyValues.size() > 0;
  }

  /**
   * @param propertyID the property ID
   * @return true if the property has been modified since the entity was instantiated
   */
  public boolean isModified(final String propertyID) {
    return originalPropertyValues != null && originalPropertyValues.containsKey(propertyID);
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
   * @param validate set to true if basic type validation should be performed on the value
   */
  public void setValue(final String propertyID, final Object value, final boolean validate) {
    setValue(getProperty(propertyID), value, validate);
  }

  /**
   * Sets the value of the given property.
   * If <code>property</code> is an instance of Property.EntityProperty, denormalized values and
   * values comprising the foreign key are also set.
   * @param property the property
   * @param value the new value
   * @param validate set to true if basic type validation should be performed on the value
   */
  public void setValue(final Property property, final Object value, final boolean validate) {
    if (validate)
      EntityUtil.validateValue(property, value);

    final boolean primarKeyProperty = property instanceof Property.PrimaryKeyProperty;
    final boolean initialization = primarKeyProperty ? !primaryKey.keyValues.containsKey(property.propertyID)
            : !propertyValues.containsKey(property.propertyID);
    doSetValue(property, value, primarKeyProperty, initialization, true);
  }

  /**
   * Initializing a value has the same effect as using <code>setValue()</code> except for Property.EntityPropertys
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

    return EntityProxy.getEntityProxy(primaryKey.entityID).getValue(this, property);
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
   */
  public Entity getEntityValue(final String propertyID) {
    return (Entity) getValue(propertyID);
  }

  /**
   * @param propertyID the ID of the date property for which to retrieve the value
   * @return the value of the property identified by <code>propertyID</code>,
   * assuming it is a Timestamp
   */
  public Timestamp getDateValue(final String propertyID) {
    return (Timestamp) getValue(propertyID);
  }

  /**
   * @param propertyID the ID of the property for which to retrieve the value
   * @return the value of the property identified by <code>propertyID</code>,
   * assuming it is a String
   */
  public String getStringValue(final String propertyID) {
    return (String) getValue(propertyID);
  }

  /**
   * @param propertyID the ID of the property for which to retrieve the value
   * @return the value of the property identified by <code>propertyID</code>,
   * assuming it is an Integer
   */
  public Integer getIntValue(final String propertyID) {
    return (Integer) getValue(propertyID);
  }

  /**
   * @param propertyID the ID of the property for which to retrieve the value
   * @return the value of the property identified by <code>propertyID</code>,
   * assuming it is a Boolean
   */
  public Type.Boolean getBooleanValue(final String propertyID) {
    return (Type.Boolean) getValue(propertyID);
  }

  /**
   * @param propertyID the ID of the property for which to retrieve the value
   * @return the value of the property identified by <code>propertyID</code>,
   * assuming it is a Character
   */
  public Character getCharValue(final String propertyID) {
    return (Character) getValue(propertyID);
  }

  /**
   * @param propertyID the ID of the property for which to retrieve the value
   * @return the value of the property identified by <code>propertyID</code>,
   * assuming it is a Double
   */
  public Double getDoubleValue(final String propertyID) {
    return (Double) getValue(propertyID);
  }

  /**
   * @param propertyID the ID of the property for which to retrieve the value
   * @return a String representation of the value of the property identified by <code>propertyID</code>
   * @see org.jminor.framework.model.EntityProxy#getValueAsString(Entity, Property)
   */
  public String getValueAsString(final String propertyID) {
    return getValueAsString(getProperty(propertyID));
  }

  /**
   * @param property the property for which to retrieve the value
   * @return a String representation of the value of <code>property</code>
   * @see org.jminor.framework.model.EntityProxy#getValueAsString(Entity, Property)
   */
  public String getValueAsString(final Property property) {
    if (property instanceof Property.DenormalizedViewProperty)
      return getDenormalizedViewValueAsString((Property.DenormalizedViewProperty) property);

    return EntityProxy.getEntityProxy(primaryKey.entityID).getValueAsString(this, property);
  }

  /**
   * Returns the value to use when the property is shown in a table
   * @param propertyID the ID of the property for which to retrieve the value
   * @return the table representation of the value of the property identified by <code>propertyID</code>
   * @see org.jminor.framework.model.EntityProxy#getTableValue(Entity, Property)
   */
  public Object getTableValue(final String propertyID) {
    return getTableValue(getProperty(propertyID));
  }

  /**
   * Returns the value to use when the property is shown in a table
   * @param property the property for which to retrieve the value
   * @return the table representation of the value of <code>property</code>
   * @see org.jminor.framework.model.EntityProxy#getTableValue(Entity, Property)
   */
  public Object getTableValue(final Property property) {
    if (property instanceof Property.DenormalizedViewProperty)
      return getDenormalizedViewValue((Property.DenormalizedViewProperty) property);

    return EntityProxy.getEntityProxy(primaryKey.entityID).getTableValue(this, property);
  }

  /**
   * Returns a formatted date value
   * @param propertyID the ID of the property for which to retrieve a formatted value
   * @param dateFormat the DateFormat to use when formatting the value
   * @return a formatted date value
   */
  public String getDateStringValue(final String propertyID, final DateFormat dateFormat) {
    return dateFormat.format(getDateValue(propertyID));
  }

  /**
   * @return true if the this entity instance is null
   */
  public boolean isNull() {
    if (getPrimaryKey() == null)
      throw new RuntimeException("Can only tell if entity is null if it has a primary key");

    return getPrimaryKey().isNull();
  }

  /**
   * @param propertyID the property identifier
   * @return the original value of the property
   */
  public Object getOriginalValue(final String propertyID) {
    if (isModified(propertyID))
      return originalPropertyValues.get(propertyID);

    return getValue(propertyID);
  }

  /**
   * @param propertyID the property identifier
   * @return true if this entity contains a value for the property
   * N.B. does not include the primary key properties
   */
  public boolean hasValue(final String propertyID) {
    return propertyValues.containsKey(propertyID);
  }

  /**
   * @param propertyID the property identifier
   * @return the value of the property, bypassing the EntityProxy
   */
  public Object getRawValue(final String propertyID) {
    if (primaryKey.containsProperty(propertyID))
      return primaryKey.keyValues.get(propertyID);

    return propertyValues.get(propertyID);
  }

  /**
   * @param entity the entity to compare to
   * @return true if all property values are equal
   */
  public boolean propertyValuesEqual(final Entity entity) {
    if (!entity.primaryKey.equals(primaryKey))
      return false;

    for (final Map.Entry<String, Object> entry : entity.propertyValues.entrySet()) {
      if (propertyValues.containsKey(entry.getKey())) {
        if (!Util.equal(entry.getValue(), propertyValues.get(entry.getKey())))
          return false;
      }
      else
        return false;
    }

    return true;
  }

  /**
   * @param object the object to compare with
   * @return true if this entity is equal to object
   */
  public boolean equals(final Object object) {
    return object instanceof Entity && equals((Entity) object);
  }

  /**
   * @param entity the entity to compare with
   * @return true if the primary keys of <code>entity</code> is identical to the primary key of this Entity
   */
  public boolean equals(final Entity entity) {
    return entity.primaryKey.equals(primaryKey);
  }

  /**
   * @param propertyID the propertyID
   * @param value the value
   * @return true if <code>value</code> is equal to the value of the property identified by <code>propertyID</code>
   */
  public boolean isValueEqualTo(final String propertyID, final Object value) {
    return Util.equal(getValue(propertyID), value);
  }

  /**
   * @param entity the entity to compare with
   * @return the compare result from comparing <code>entity</code> with this Entity instance
   */
  public int compareTo(final Entity entity) {
    return EntityProxy.getEntityProxy(getEntityID()).compareTo(this, entity);
  }

  /**
   * Returns the hash code of the primary key
   */
  public int hashCode() {
    return primaryKey.hashCode();
  }

  /**
   * @return a string representation of this entity
   * @see EntityProxy#toString(Entity)
   */
  public String toString() {
    if (toString == null)
      toString = EntityProxy.getEntityProxy(getEntityID()).toString(this);

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
      throw new RuntimeException("Unable to copy entity: " + getEntityID() + ": " + e.getMessage());
    }
  }

  /**
   * @return a deep copy of this entity in its original state
   */
  public Entity getOriginalCopy() {
    final Entity ret = getCopy();
    if (originalPropertyValues != null) {
      for (final Map.Entry<String, Object> entry : originalPropertyValues.entrySet())
        propertyValues.put(entry.getKey(), EntityUtil.copyPropertyValue(entry.getValue()));
    }

    return ret;
  }

  /**
   * Makes this entity identical to <code>sourceEntity</code>.
   * Entity values, which are mutable, are copied with getCopy()
   * Original property values, if any are not deep-copied
   * @param sourceEntity the entity to copy
   */
  public void setAs(final Entity sourceEntity) {
    primaryKey.setValue(sourceEntity.getPrimaryKey());
    propertyValues.clear();
    if (originalPropertyValues != null)
      originalPropertyValues.clear();
    for (final Map.Entry<String, Object> entry : sourceEntity.propertyValues.entrySet()) {
      propertyValues.put(entry.getKey(), EntityUtil.copyPropertyValue(sourceEntity.propertyValues.get(entry.getKey())));
    }
    if (sourceEntity.originalPropertyValues != null && !sourceEntity.originalPropertyValues.isEmpty()) {
      if (originalPropertyValues == null)
        originalPropertyValues = new HashMap<String, Object>();
      originalPropertyValues.putAll(sourceEntity.originalPropertyValues);
    }
    if (evtPropertyChanged != null)
      for (final Property property : repository.getProperties(getEntityID(), true))
        firePropertyChangeEvent(property, getRawValue(property.propertyID), null, true);

    toString = sourceEntity.toString;
    if (stModified != null)
      stModified.setActive(isModified());
  }

  /**
   * @param value if true this entity should start firing propertyChangeEvents, false if it should stop
   */
  public void setFirePropertyChangeEvents(final boolean value) {
    if (value) {
      if (evtPropertyChanged == null)
        evtPropertyChanged = new Event("Entity.evtPropertyChanged");
    }
    else
      evtPropertyChanged = null;
  }

  /**
   * Returns the primary key of the entity referenced by the given EntityProperty
   * @param property the entity reference property for which to retrieve the underlying EntityKey
   * @return the primary key of the underlying entity, null if no entity is referenced
   */
  public EntityKey getReferencedKey(final Property.EntityProperty property) {
    EntityKey key = referencedKeysCache == null ? null : referencedKeysCache.get(property);
    if (key != null)
      return key;

    for (int i = 0; i < property.referenceProperties.size(); i++) {
      final Property referenceKeyProperty = property.referenceProperties.get(i);
      final Object value = referenceKeyProperty instanceof Property.PrimaryKeyProperty
              ? primaryKey.getValue(referenceKeyProperty.propertyID)
              : propertyValues.get(referenceKeyProperty.propertyID);
      if (!EntityUtil.isValueNull(referenceKeyProperty.propertyType, value)) {
        if (key == null)
          (referencedKeysCache == null ? referencedKeysCache = new HashMap<Property.EntityProperty, EntityKey>()
                  : referencedKeysCache).put(property, key = new EntityKey(property.referenceEntityID));
        key.setValue(key.getProperties().get(i).propertyID, value);//check the index thing set EntityResultPacker.getReferenceEntity
      }
      else
        break;
    }

    return key;
  }

  /**
   * @param propertyID the property identifier
   * @return true if the value of the given property is null
   */
  public boolean isValueNull(final String propertyID) {
    final Property property = getProperty(propertyID);
    final Object value = property instanceof Property.TransientProperty ? getValue(propertyID) : getRawValue(propertyID);

    return EntityUtil.isValueNull(property.propertyType, value);
  }

  /** {@inheritDoc} */
  public void writeExternal(final ObjectOutput out) throws IOException {
    out.writeObject(primaryKey);
    out.writeObject(propertyValues);
    out.writeObject(originalPropertyValues);
  }

  /** {@inheritDoc} */
  @SuppressWarnings({"unchecked"})
  public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
    primaryKey = (EntityKey) in.readObject();
    propertyValues = (Map<String, Object>) in.readObject();
    originalPropertyValues = (Map<String, Object>) in.readObject();
    hasDenormalizedProperties = repository.hasDenormalizedProperties(primaryKey.entityID);
  }

  /**
   * Performs the actual value setting, minding all the stuff that needs minding here
   * @param property the property
   * @param newValue the new value
   * @param primaryKeyProperty true if the property is part of the primary key
   * @param initialization true if the property value is being initialized, if true it is assumed
   * that this Entity instance does not contain a value for the property, thus the modified state
   * can be safely disregarded during the value setting
   * @param propagateReferenceValues if set to true then both reference key values and
   * denormalized values are set in case <code>property</code> is a Property.EntityProperty.
   * @throws IllegalArgumentException in case <code>newValue</code> is the entity itself, preveting circular references
   */
  private void doSetValue(final Property property, final Object newValue, final boolean primaryKeyProperty,
                          final boolean initialization, boolean propagateReferenceValues) {
    if (property instanceof Property.DenormalizedViewProperty)
      throw new IllegalArgumentException("Can not set the value of a denormalized property");
    if (newValue != null && newValue instanceof Entity && newValue.equals(this))
      throw new IllegalArgumentException("Circular entity reference detected: " + primaryKey + "->" + property.propertyID);

    //invalidate the toString cache
    toString = null;

    if (propagateReferenceValues && property instanceof Property.EntityProperty && (newValue == null || newValue instanceof Entity))
      propagateReferenceValues((Property.EntityProperty) property, (Entity) newValue);

    final Object oldValue = initialization ? null :
            primaryKeyProperty ? primaryKey.keyValues.get(property.propertyID) : propertyValues.get(property.propertyID);
    if (primaryKeyProperty)
      primaryKey.setValue(property.propertyID, newValue);
    else
      propertyValues.put(property.propertyID, newValue);

    if (!initialization)
      updateModifiedState(property.propertyID, property.propertyType, newValue, oldValue);

    if (evtPropertyChanged != null && !EntityUtil.equal(property.propertyType, newValue, oldValue))
      firePropertyChangeEvent(property, newValue, oldValue, initialization);
  }

  private void propagateReferenceValues(final Property.EntityProperty property, final Entity newValue) {
    referencedKeysCache = null;
    setReferenceKeyValues(property, newValue);
    if (hasDenormalizedProperties) {
      final Collection<Property.DenormalizedProperty> properties =
              repository.getDenormalizedProperties(primaryKey.entityID, property.referenceEntityID);
      setDenormalizedValues(property, newValue, properties);
    }
  }

  /**
   * Sets the actual reference id property values
   * @param property the entity reference property
   * @param entity the entity value
   */
  private void setReferenceKeyValues(final Property.EntityProperty property, final Entity entity) {
    final Collection<Property.PrimaryKeyProperty> referenceEntityPKProperties =
            entity != null ? entity.primaryKey.properties
                    : repository.getPrimaryKeyProperties(property.referenceEntityID);
    for (final Property.PrimaryKeyProperty primaryKeyProperty : referenceEntityPKProperties) {
      final Property referenceProperty = property.referenceProperties.get(primaryKeyProperty.primaryKeyIndex);
      if (!(referenceProperty instanceof Property.MirrorProperty)) {
        final boolean isPrimaryKeyProperty = referenceProperty instanceof Property.PrimaryKeyProperty;
        final boolean initialization = isPrimaryKeyProperty ? !primaryKey.keyValues.containsKey(referenceProperty.propertyID)
            : !propertyValues.containsKey(referenceProperty.propertyID);
        doSetValue(referenceProperty, entity != null ? entity.getRawValue(primaryKeyProperty.propertyID) : null,
                isPrimaryKeyProperty, initialization, true);
      }
    }
  }

  /**
   * Sets the denormalized property values
   * @param property the entity reference property
   * @param entity the entity value
   * @param denormalizedProperties the denormalized properties
   */
  private void setDenormalizedValues(final Property.EntityProperty property, final Entity entity,
                                     final Collection<Property.DenormalizedProperty> denormalizedProperties) {
    if (denormalizedProperties != null) {
      for (final Property.DenormalizedProperty denormalizedProperty : denormalizedProperties) {
        doSetValue(denormalizedProperty,
                entity == null ? null : entity.getRawValue(denormalizedProperty.valueSourceProperty.propertyID),
                false, !propertyValues.containsKey(property.propertyID), true);
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
  private void updateModifiedState(final String propertyID, final Type type, final Object newValue, final Object oldValue) {
    if (originalPropertyValues != null && originalPropertyValues.containsKey(propertyID)) {
      if (EntityUtil.equal(type, originalPropertyValues.get(propertyID), newValue)) {
        originalPropertyValues.remove(propertyID);//we're back to the original value
        if (propertyDebug)
          System.out.println(propertyID + " reverted back to original value " + newValue);
      }
    }
    else if (!EntityUtil.equal(type, oldValue, newValue)) {
      (originalPropertyValues == null ?
              (originalPropertyValues = new HashMap<String, Object>()) : originalPropertyValues).put(propertyID, oldValue);
    }

    if (stModified != null)
      stModified.setActive(isModified());
  }

  /**
   * Fires evtPropertyChanged
   * @param property the property
   * @param newValue the new value
   * @param oldValue the old value
   * @param initialization true if the property is being set for the first time
   */
  private void firePropertyChangeEvent(final Property property, final Object newValue, final Object oldValue,
                                       final boolean initialization) {
    if (propertyDebug)
      System.out.println(EntityUtil.getPropertyChangeDebugString(getEntityID(), property, oldValue, newValue, initialization));

    evtPropertyChanged.fire(new PropertyChangeEvent(property, newValue, oldValue, true, initialization));
  }

  private Object getDenormalizedViewValue(final Property.DenormalizedViewProperty denormalizedViewProperty) {
    final Entity valueOwner = getEntityValue(denormalizedViewProperty.referencePropertyID);

    return valueOwner != null ? valueOwner.getValue(denormalizedViewProperty.denormalizedProperty) : null;
  }

  private String getDenormalizedViewValueAsString(final Property.DenormalizedViewProperty denormalizedViewProperty) {
    final Entity valueOwner = getEntityValue(denormalizedViewProperty.referencePropertyID);

    return valueOwner != null ? valueOwner.getValueAsString(denormalizedViewProperty.denormalizedProperty) : null;
  }
}