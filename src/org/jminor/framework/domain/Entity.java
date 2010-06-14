/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.model.Event;
import org.jminor.common.model.Util;
import org.jminor.common.model.valuemap.ValueChangeMap;
import org.jminor.common.model.valuemap.ValueChangeMapImpl;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.sql.Timestamp;
import java.text.Collator;
import java.text.Format;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a row in a database table, providing access to the column values via the ValueMap interface.
 */
public final class Entity extends ValueChangeMapImpl<String, Object> implements Serializable, Comparable<Entity> {

  private static final long serialVersionUID = 1;

  /**
   * The primary key of this entity
   */
  private final Key primaryKey;

  /**
   * The foreign key values referenced by this entity
   */
  private final ValueChangeMap<String, Entity> foreignKeyValues = new ValueChangeMapImpl<String, Entity>() {
    @Override
    public String getMapTypeID() {
      return getEntityID();
    }
  };

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

  private static Map<String, Proxy> proxies;
  private static final Proxy defaultProxy = new Proxy();

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
    Util.rejectNullValue(primaryKey);
    this.primaryKey = primaryKey;
  }

  /**
   * @return the entity ID
   */
  public String getEntityID() {
    return primaryKey.getEntityID();
  }

  @Override
  public String getMapTypeID() {
    return getEntityID();
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
    if (properties == null) {
      properties = EntityRepository.getProperties(getEntityID());
    }

    final Property property = properties.get(propertyID);
    if (property == null) {
      throw new RuntimeException("Property " + propertyID + " not found in entity: " + getEntityID());
    }

    return property;
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
   * Returns true if one or more writable properties have been modified,
   * read only and non-updatable properties are excluded unless they
   * are transient.
   * @return true if one or more properties have been modified
   * since the entity was instantiated
   */
  @Override
  public boolean isModified() {
    return writablePropertiesModified() || primaryKey.isModified();
  }

  /**
   * Returns the value this property had when the entity was loaded
   * @param propertyID the property identifier
   * @return the original value of the property
   */
  @Override
  public Object getOriginalValue(final String propertyID) {
    final Property property = getProperty(propertyID);
    if (property instanceof Property.PrimaryKeyProperty) {
      return primaryKey.getOriginalValue(propertyID);
    }
    if (property instanceof Property.ForeignKeyProperty) {
      return foreignKeyValues.getOriginalValue(propertyID);
    }

    return super.getOriginalValue(propertyID);
  }

  @Override
  public Object setValue(final String propertyID, final Object value) {
    return setValue(getProperty(propertyID), value);
  }

  public Object setValue(final Property property, final Object value) {
    return setValue(property, value, true);
  }

  /**
   * Sets the property value
   * @param property the property
   * @param value the value
   * @param validateType if true then type validation is performed
   * @return the old value
   */
  public Object setValue(final Property property, final Object value, final boolean validateType) {
    if (property instanceof Property.PrimaryKeyProperty) {
      return primaryKey.setValue(property.getPropertyID(), value);
    }
    if (property instanceof Property.DenormalizedViewProperty) {
      throw new IllegalArgumentException("Can not set the value of a denormalized view property");
    }
    if (property instanceof Property.DerivedProperty) {
      throw new IllegalArgumentException("Can not set the value of a derived property");
    }
    if (property instanceof Property.ValueListProperty && value != null && !((Property.ValueListProperty) property).isValid(value)) {
      throw new IllegalArgumentException("Invalid value list value: " + value + " for property ");
    }
    if (value instanceof Entity && value.equals(this)) {
      throw new IllegalArgumentException("Circular entity reference detected: " + primaryKey + "->" + property.getPropertyID());
    }

    if (validateType) {
      validateType(property, value);
    }

    toString = null;
    if (property instanceof Property.ForeignKeyProperty && (value == null || value instanceof Entity)) {
      propagateReferenceValues((Property.ForeignKeyProperty) property, (Entity) value, false);
      return foreignKeyValues.setValue(property.getPropertyID(), (Entity) value);
    }
    else {
      return super.setValue(property.getPropertyID(), value);
    }
  }

  /**
   * Initializes the given value assuming it has no previously set value.
   * This method does not propagate foreign key values but does set denormalized values if any exist.
   * This method should be used with care, if at all.
   * @param propertyID the ID of the property for which to initialize the value
   * @param value the value
   */
  @Override
  public void initializeValue(final String propertyID, final Object value) {
    initializeValue(getProperty(propertyID), value);
  }

  /**
   * Initializes the given value assuming it has no previously set value.
   * This method does not propagate foreign key values nor set denormalized values.
   * This method should be used with care, if at all.
   * @param property the property for which to initialize the value
   * @param value the value
   */
  public void initializeValue(final Property property, final Object value) {
    if (property instanceof Property.PrimaryKeyProperty) {
      primaryKey.initializeValue(property.getPropertyID(), value);
    }
    else if (property instanceof Property.ForeignKeyProperty) {
      foreignKeyValues.initializeValue(property.getPropertyID(), (Entity) value);
    }
    else {
      super.initializeValue(property.getPropertyID(), value);
    }
  }

  /**
   * @param propertyID the ID of the property for which to retrieve the value
   * @return the value of the property identified by <code>propertyID</code>
   */
  @Override
  public Object getValue(final String propertyID) {
    return getValue(getProperty(propertyID));
  }

  /**
   * @param property the property for which to retrieve the value
   * @return the value of the given property
   */
  public Object getValue(final Property property) {
    if (property instanceof Property.PrimaryKeyProperty) {
      return primaryKey.getValue(property.getPropertyID());
    }
    if (property instanceof Property.ForeignKeyProperty) {
      return foreignKeyValues.getValue(property.getPropertyID());
    }
    if (property instanceof Property.DenormalizedViewProperty) {
      return getDenormalizedViewValue((Property.DenormalizedViewProperty) property);
    }
    if (property instanceof Property.DerivedProperty) {
      return getProxy(getEntityID()).getDerivedValue(this, (Property.DerivedProperty) property);
    }

    if (containsValue(property.getPropertyID())) {
      return super.getValue(property.getPropertyID());
    }
    else {
      return property.getDefaultValue();
    }
  }

  @Override
  public Collection<Object> getValues() {
    final Collection<Object> values = new ArrayList<Object>(primaryKey.getValues());
    values.addAll(foreignKeyValues.getValues());
    values.addAll(super.getValues());

    return values;
  }

  @Override
  public Object removeValue(final String propertyID) {
    final Property property = getProperty(propertyID);
    if (property instanceof Property.PrimaryKeyProperty) {
      return primaryKey.removeValue(propertyID);
    }
    if (property instanceof Property.ForeignKeyProperty) {
      for (final Property fkProperty : ((Property.ForeignKeyProperty) property).getReferenceProperties()) {
        removeValue(fkProperty.getPropertyID());
      }
      return foreignKeyValues.removeValue(propertyID);
    }

    return super.removeValue(propertyID);
  }

  @Override
  public boolean isValueNull(final String propertyID) {
    return isValueNull(getProperty(propertyID));
  }

  public boolean isValueNull(final Property property) {
    if (property instanceof Property.PrimaryKeyProperty) {
      return primaryKey.isValueNull(property.getPropertyID());
    }
    if (property instanceof Property.ForeignKeyProperty) {
      return foreignKeyValues.isValueNull(property.getPropertyID());
    }

    return super.isValueNull(property.getPropertyID());
  }

  /**
   * @param foreignKeyPropertyID the ID of the property for which to retrieve the value
   * @return the value of the property identified by <code>propertyID</code>,
   * assuming it is an Entity
   * @throws RuntimeException if the property is not a foreign key property
   */
  public Entity getForeignKeyValue(final String foreignKeyPropertyID) {
    final Property property = getProperty(foreignKeyPropertyID);
    if (property instanceof Property.ForeignKeyProperty) {
      return foreignKeyValues.getValue(property.getPropertyID());
    }

    throw new RuntimeException(foreignKeyPropertyID + " is not a foreign key property");
  }

  /**
   * Returns true if the entity referenced via the given foreign key property has been loaded
   * @param foreignKeyPropertyID the property id
   * @return true if the reference entity has been loaded
   */
  public boolean isLoaded(final String foreignKeyPropertyID) {
    return !foreignKeyValues.isValueNull(foreignKeyPropertyID);
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
   * @see #getFormattedValue(Property, java.text.Format)
   */
  public String getValueAsString(final String propertyID) {
    return getValueAsString(getProperty(propertyID));
  }

  /**
   * This method returns a String representation of the value associated with the given property,
   * if the property has a format it is used.
   * @param property the property for which to retrieve the value
   * @return a String representation of the value of <code>property</code>
   * @see #getFormattedValue(Property, java.text.Format)
   */
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
    return getFormattedValue(property, property.getFormat());
  }

  public String getFormattedValue(final Property property, final Format format) {
    return getProxy(getEntityID()).getFormattedValue(this, property, format);
  }

  @Override
  public void revertValue(final String key) {
    final Property property = getProperty(key);
    if (property instanceof Property.PrimaryKeyProperty) {
      primaryKey.revertValue(key);
    }
    else if (property instanceof Property.ForeignKeyProperty) {
      foreignKeyValues.revertValue(key);
    }
    else {
      super.revertValue(key);
    }
  }

  @Override
  public void revertAll() {
    primaryKey.revertAll();
    foreignKeyValues.revertAll();
    super.revertAll();
  }

  @Override
  public void saveValue(final String key) {
    final Property property = getProperty(key);
    if (property instanceof Property.PrimaryKeyProperty) {
      primaryKey.saveValue(key);
    }
    else if (property instanceof Property.ForeignKeyProperty) {
      foreignKeyValues.saveValue(key);
    }
    else {
      super.saveValue(key);
    }
  }

  @Override
  public void saveAll() {
    primaryKey.saveAll();
    foreignKeyValues.saveAll();
    super.revertAll();
  }

  @Override
  public void clear() {
    primaryKey.clear();
    foreignKeyValues.clear();
    super.clear();
  }

  /**
   * @return true if the this entity instance has a null primary key
   */
  public boolean isNull() {
    return getPrimaryKey().isNull();
  }

  /**
   * @param entity the entity to compare to
   * @return true if all property values are equal
   */
  public boolean propertyValuesEqual(final Entity entity) {
    return entity.primaryKey.equals(primaryKey) && super.equals(entity);
  }

  /**
   * Returns true if the value of the property identified by <code>propertyID</code>
   * is equal to <code>value</code>
   * @param propertyID the property ID
   * @param value the value
   * @return true if the given value is equal to the value identified by the given property ID
   */
  public boolean valueEquals(final String propertyID, final Object value) {
    return valuesEqual(getValue(propertyID), value);
  }

  @Override
  protected boolean valuesEqual(final Object valueOne, final Object valueTwo) {
    if (valueOne instanceof Entity && valueTwo instanceof Entity) {
      return ((Entity) valueOne).getPrimaryKey().equals(((Entity) valueTwo).getPrimaryKey());
    }
    return super.valuesEqual(valueOne, valueTwo);
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
    if (toString == null) {
      toString = getProxy(getEntityID()).toString(this);
    }

    return toString;
  }

  /**
   * @return a new Entity instance with the same entityID as this entity
   */
  @Override
  public ValueChangeMap<String, Object> getInstance() {
    return new Entity(getEntityID());
  }

  /**
   * Makes this entity identical to <code>sourceEntity</code>, assuming it
   * is a Entity instance.
   * Reference entity values, which are mutable, are deep copied with getCopy()
   * @param sourceEntity the Entity to copy
   * @throws IllegalArgumentException in case <code>sourceEntity</code> is not Entity instance.
   */
  @Override
  public void setAs(final ValueChangeMap<String, Object> sourceEntity) {
    if (sourceEntity != null && !(sourceEntity instanceof Entity)) {
      throw new IllegalArgumentException("Not a Entity instance: " + sourceEntity);
    }

    super.setAs(sourceEntity);
    final Entity entity = (Entity) sourceEntity;
    primaryKey.setAs(entity == null ? null : entity.getPrimaryKey());
    foreignKeyValues.setAs(entity == null ? null : entity.foreignKeyValues);
    toString = sourceEntity == null ? null : sourceEntity.toString();
  }

  /**
   * Returns the primary key of the entity referenced by the given ForeignKeyProperty,
   * if the reference is null this method returns null.
   * @param foreignKeyProperty the foreign key property for which to retrieve the underlying EntityKey
   * @return the primary key of the underlying entity, null if no entity is referenced
   */
  public Key getReferencedPrimaryKey(final Property.ForeignKeyProperty foreignKeyProperty) {
    Key primaryKey = referencedPrimaryKeysCache == null ? null : referencedPrimaryKeysCache.get(foreignKeyProperty);
    if (primaryKey != null) {
      return primaryKey;
    }

    primaryKey = foreignKeyProperty.getReferenceProperties().size() == 1 ?
            initializeSinglePropertyKey(foreignKeyProperty) : initializeMultiPropertyKey(foreignKeyProperty);

    if (primaryKey != null) {
      if (referencedPrimaryKeysCache == null) {
        referencedPrimaryKeysCache = new HashMap<Property.ForeignKeyProperty, Key>();
      }
      referencedPrimaryKeysCache.put(foreignKeyProperty, primaryKey);
    }

    return primaryKey;
  }

  @Override
  public void addValueListener(final ActionListener valueListener) {
    primaryKey.addValueListener(valueListener);
    foreignKeyValues.addValueListener(valueListener);
    super.addValueListener(valueListener);
  }

  @Override
  public void removeValueListener(final ActionListener valueListener) {
    primaryKey.removeValueListener(valueListener);
    foreignKeyValues.removeValueListener(valueListener);
    super.removeValueListener(valueListener);
  }

  @Override
  public boolean containsValue(final String propertyID) {
    return containsValue(getProperty(propertyID));
  }

  public boolean containsValue(final Property property) {
    if (property instanceof Property.PrimaryKeyProperty) {
      return primaryKey.containsValue(property.getPropertyID());
    }
    if (property instanceof Property.ForeignKeyProperty) {
      return foreignKeyValues.containsValue(property.getPropertyID());
    }

    return super.containsValue(property.getPropertyID());
  }

  /**
   * True if the given objects are equal. Both objects being null results in true.
   * @param one the first object
   * @param two the second object
   * @return true if the given objects are equal
   */
  public static boolean isEqual(final Object one, final Object two) {
    final boolean oneNull = one == null;
    final boolean twoNull = two == null;

    return oneNull && twoNull || !(oneNull ^ twoNull) && one.equals(two);
  }

  /**
   * Returns true if one or more of the properties involved in the given foreign key is null
   * @param foreignKeyProperty the foreign key property
   * @return true if the foreign key is null
   */
  public boolean isForeignKeyNull(final Property.ForeignKeyProperty foreignKeyProperty) {
    for (final Property property : foreignKeyProperty.getReferenceProperties()) {
      if (isValueNull(property.getPropertyID())) {
        return true;
      }
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
    return value instanceof Entity ? ((Entity) value).getCopy() : value;
  }

  /**
   * @param entities the entities to check
   * @return true if any of the given entities has a modified primary key
   */
  public static boolean isPrimaryKeyModified(final Collection<Entity> entities) {
    if (entities == null || entities.size() == 0) {
      return false;
    }

    for (final Entity entity : entities) {
      if (entity.primaryKey.isModified()) {
        return true;
      }
    }

    return false;
  }

  /**
   * Sets a entity specific proxy instance
   * @param entityID the ID of the entity for which this proxy instance is used
   * @param entityProxy the proxy instance to link to the given entity ID
   * @see org.jminor.framework.domain.Entity.Proxy
   */
  public static void setProxy(final String entityID, final Proxy entityProxy) {
    if (proxies == null) {
      proxies = new HashMap<String, Proxy>();
    }

    if (proxies.containsKey(entityID)) {
      throw new RuntimeException("Proxy already set for: " + entityID);
    }

    proxies.put(entityID, entityProxy);
  }

  /**
   * Returns the proxy instance assigned to the given entity ID or the default proxy if none has been assigned
   * @param entityID the entity ID for which to retrieve the proxy
   * @return the proxy instance assigned to the given entity ID
   * @see org.jminor.framework.domain.Entity.Proxy
   */
  public static Proxy getProxy(final String entityID) {
    if (proxies != null && proxies.containsKey(entityID)) {
      return proxies.get(entityID);
    }

    return defaultProxy;
  }

  /**
   * Initializes a new Entity based on the given values.
   * @param entityID the entityID
   * @param values the values
   * @param originalValues the original values
   * @return an initialized Entity
   */
  public static Entity initialize(final String entityID, final Map<String, Object> values, final Map<String, Object> originalValues) {
    final Entity entity = new Entity(entityID);
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
  protected Event initializeValueChangedEvent() {
    final Event event = super.initializeValueChangedEvent();
    primaryKey.addValueListener(event);

    return event;
  }

  @Override
  protected void notifyValueChange(final String key, final Object value, final Object oldValue, final boolean initialization) {
    if (EntityRepository.hasLinkedDerivedProperties(getEntityID(), key)) {
      final Collection<String> linkedPropertyIDs = EntityRepository.getLinkedDerivedPropertyIDs(getEntityID(), key);
      for (final String propertyID : linkedPropertyIDs) {
        super.notifyValueChange(propertyID, getValue(propertyID), null, false);
      }
    }
    super.notifyValueChange(key, value, oldValue, initialization);
  }

  private void propagateReferenceValues(final Property.ForeignKeyProperty foreignKeyProperty, final Entity newValue,
                                        final boolean initialization) {
    referencedPrimaryKeysCache = null;
    setForeignKeyValues(foreignKeyProperty, newValue, initialization);
    if (EntityRepository.hasDenormalizedProperties(getEntityID())) {
      setDenormalizedValues(newValue, foreignKeyProperty, initialization);
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
    final Collection<Property.PrimaryKeyProperty> referenceEntityPKProperties =
            referencedEntity != null ? referencedEntity.primaryKey.getProperties()
                    : EntityRepository.getPrimaryKeyProperties(foreignKeyProperty.getReferencedEntityID());
    for (final Property.PrimaryKeyProperty primaryKeyProperty : referenceEntityPKProperties) {
      final Property referenceProperty = foreignKeyProperty.getReferenceProperties().get(primaryKeyProperty.getIndex());
      if (!(referenceProperty instanceof Property.MirrorProperty)) {
        final Object value = referencedEntity != null ? referencedEntity.getValue(primaryKeyProperty) : null;
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
   * @param entity the entity value owning the denormalized values
   * @param foreignKeyProperty the foreign key property referring to the value source
   * @param initialization true if the values are being initialized
   */
  private void setDenormalizedValues(final Entity entity, Property.ForeignKeyProperty foreignKeyProperty,
                                     final boolean initialization) {
    final Collection<Property.DenormalizedProperty> denormalizedProperties =
            EntityRepository.getDenormalizedProperties(getEntityID(), foreignKeyProperty.getPropertyID());
    if (denormalizedProperties != null) {
      for (final Property.DenormalizedProperty denormalizedProperty : denormalizedProperties) {
        final Object value = entity == null ? null : entity.getValue(denormalizedProperty.getDenormalizedProperty());
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

    return valueOwner != null ? valueOwner.getValue(denormalizedViewProperty.getDenormalizedProperty().getPropertyID()) : null;
  }

  private String getDenormalizedViewValueFormatted(final Property.DenormalizedViewProperty denormalizedViewProperty) {
    final Entity valueOwner = (Entity) getValue(denormalizedViewProperty.getForeignKeyPropertyID());

    return valueOwner != null ? valueOwner.getFormattedValue(denormalizedViewProperty.getDenormalizedProperty()) : null;
  }

  private Key initializeSinglePropertyKey(final Property.ForeignKeyProperty foreignKeyProperty) {
    final Property referenceKeyProperty = foreignKeyProperty.getReferenceProperties().get(0);
    final Object value = referenceKeyProperty instanceof Property.PrimaryKeyProperty
            ? this.primaryKey.getValue(referenceKeyProperty.getPropertyID())
            : super.getValue(referenceKeyProperty.getPropertyID());

    return value == null ? null : new Entity.Key(foreignKeyProperty.getReferencedEntityID(), value);
  }

  private Key initializeMultiPropertyKey(final Property.ForeignKeyProperty foreignKeyProperty) {
    final Key primaryKey = new Entity.Key(foreignKeyProperty.getReferencedEntityID());
    for (final Property referenceKeyProperty : foreignKeyProperty.getReferenceProperties()) {
      final Object value = referenceKeyProperty instanceof Property.PrimaryKeyProperty
              ? this.primaryKey.getValue(referenceKeyProperty.getPropertyID())
              : super.getValue(referenceKeyProperty.getPropertyID());
      if (value == null) {
        return null;
      }
      else {
        primaryKey.setValue(foreignKeyProperty.getReferencedPropertyID(referenceKeyProperty), value);
      }
    }

    return primaryKey;
  }

  private boolean writablePropertiesModified() {
    for (final String propertyID : getOriginalValueKeys()) {
      final Property property = getProperty(propertyID);
      if ((property instanceof Property.TransientProperty) || (!property.isReadOnly() && property.isUpdatable())) {
        return true;
      }
    }

    return false;
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

    final Class type = Property.getTypeClass(property.getType());
    if (!type.equals(value.getClass()) && !type.isAssignableFrom(value.getClass())) {
      throw new IllegalArgumentException("Value of type " + type + " expected for property " + property + ", got: " + value.getClass());
    }

    return value;
  }

  /**
   * A class representing column key objects for entities, contains the values for those columns.
   */
  public static class Key extends ValueChangeMapImpl<String, Object> implements Serializable {

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
    public Key(final String entityID) {
      super(1);
      Util.rejectNullValue(entityID);
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
    public Key(final String entityID, final Object value) {
      this(entityID);
      if (isCompositeKey()) {
        throw new RuntimeException("Not a single value key");
      }

      final Property property = properties.get(0);
      initializeValue(property.getPropertyID(), value);
      if (singleIntegerKey) {
        hashCode = value == null ? INTEGER_NULL_VALUE : (Integer) value;
        hashCodeDirty = false;
      }
    }

    /**
     * @return the entity ID
     */
    public String getEntityID() {
      return entityID;
    }

    @Override
    public String getMapTypeID() {
      return getEntityID();
    }

    /**
     * @return a List containing the properties comprising this key
     */
    public List<Property.PrimaryKeyProperty> getProperties() {
      if (properties == null) {
        properties = EntityRepository.getPrimaryKeyProperties(entityID);
      }

      return properties;
    }

    /**
     * @return true if this key is comprised of multiple properties.
     */
    public boolean isCompositeKey() {
      return getPropertyCount() > 1;
    }

    /**
     * @return the first key property
     */
    public Property.PrimaryKeyProperty getFirstKeyProperty() {
      if (getPropertyCount() == 0) {
        throw new RuntimeException("No properties defined for primary key");
      }

      return getProperties().get(0);
    }

    /**
     * @return the first value contained in this key, useful for single property keys
     */
    public Object getFirstKeyValue() {
      return getValue(getFirstKeyProperty().getPropertyID());
    }

    @Override
    public Object setValue(final String propertyID, final Object newValue) {
      hashCodeDirty = true;
      if (singleIntegerKey) {
        if (!(newValue == null || newValue instanceof Integer)) {
          throw new IllegalArgumentException("Expecting a Integer value for Key: " + entityID + ", "
                  + propertyID + ", got " + newValue + "; " + newValue.getClass());
        }
        hashCode = newValue == null ? INTEGER_NULL_VALUE : (Integer) newValue;
        hashCodeDirty = false;
      }

      return super.setValue(propertyID, newValue);
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
    public ValueChangeMap<String, Object> getInstance() {
      return new Key(entityID);
    }

    /**
     * @return the number of properties comprising this key
     */
    public int getPropertyCount() {
      return singleIntegerKey ? 1 : getProperties().size();
    }

    /**
     * Key objects are equal if the entityIDs match as well as all property values.
     * @param object the object to compare with
     * @return true if object is equal to this key
     */
    @SuppressWarnings({"SimplifiableIfStatement"})
    @Override
    public boolean equals(final Object object) {
      if (this == object) {
        return true;
      }
      if (object instanceof Key) {
        final Key key = (Key) object;
        if (singleIntegerKey) {
          return key.singleIntegerKey && hashCode() == key.hashCode() && entityID.equals(key.entityID);
        }
        else {
          return !key.singleIntegerKey && entityID.equals(key.entityID) && super.equals(key);
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
        boolean nullValue = values.size() == 0;
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

        hashCode = nullValue ? INTEGER_NULL_VALUE : hash;
        hashCodeDirty = false;
      }

      return hashCode;
    }

    /**
     * @return true if one of the properties has a null value
     */
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

    /**
     * Copies the values from <code>valueMap</code> to this entity key,
     * assuming it is a Key instance
     * @param sourceKey the key to copy
     * @throws IllegalArgumentException in case the entityIDs don't match or
     * <code>valueMap</code> is not a Key instance.
     */
    @Override
    public void setAs(final ValueChangeMap<String, Object> sourceKey) {
      if (sourceKey != null && !(sourceKey instanceof Key)) {
        throw new IllegalArgumentException("Not a Entity.Key instance: " + sourceKey);
      }

      final Key key = (Key) sourceKey;
      if (key != null && !key.getEntityID().equals(getEntityID())) {
        throw new IllegalArgumentException("Entity ID mismatch, expected: " + getEntityID() + ", actual: " + key.getEntityID());
      }

      super.setAs(sourceKey);
      if (key == null) {
        hashCode = INTEGER_NULL_VALUE;
        hashCodeDirty = false;
      }
      else {
        hashCode = key.hashCode;
        hashCodeDirty = key.hashCodeDirty;
      }
    }

    @Override
    public Object copyValue(final Object value) {
      return copyPropertyValue(value);
    }

    public static List<Key> copy(final List<Key> entityKeys) {
      final List<Key> copies = new ArrayList<Key>(entityKeys.size());
      for (final Key key : entityKeys) {
        copies.add((Key) key.getCopy());
      }

      return copies;
    }
  }

  /**
   * Acts as a proxy for retrieving values from Entity objects, allowing for plugged
   * in entity specific functionality, such as providing toString() and compareTo() implementations
   */
  public static class Proxy {
    private final Collator collator = Collator.getInstance();

    public int compareTo(final Entity entity, final Entity entityToCompare) {
      return collator.compare(entity.toString(), entityToCompare.toString());
    }

    public String toString(final Entity entity) {
      final String entityID = entity.getEntityID();
      final ToString<String> stringProvider = EntityRepository.getStringProvider(entityID);

      return stringProvider == null ? new StringBuilder(entityID).append(": ").append(entity.getPrimaryKey()).toString() : stringProvider.toString(entity);
    }

    public Object getDerivedValue(final Entity entity, final Property.DerivedProperty property) {
      throw new RuntimeException("getDerivedValue() has not been overriden in Entity.Proxy for: " + entity + ", " + property);
    }

    public String getFormattedValue(final Entity entity, final Property property, final Format format) {
      final Object value = entity.getValue(property);
      return value == null ? "" : (format != null ? format.format(value) : value.toString());
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public Color getBackgroundColor(final Entity entity) {
      return null;
    }
  }
}
