/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class for representing column key objects for entities, contains the values for those columns
 */
public class EntityKey implements Serializable {

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
  public EntityKey(final String entityID) {
    if (entityID == null)
      throw new IllegalArgumentException("EntityKey can not be instantiated without an entityID");
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
      if (property.propertyID.equals(propertyID))
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
      if (property.propertyID.equals(propertyID))
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
      ret.append(property.propertyID).append("=").append(getValue(property.propertyID));
      if (i++ < getPropertyCount()-1)
        ret.append(", ");
    }

    return ret.toString();
  }

  /**
   * @return an identical deep copy of this entity key
   */
  public EntityKey copy() {
    final EntityKey ret = new EntityKey(entityID);
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
    return this == object || object instanceof EntityKey && ((EntityKey) object).entityID.equals(entityID)
            && object.hashCode() == hashCode();
  }

  /**
   * @param key the key to compare with
   * @return true if key is equal to this key
   */
  public boolean equals(final EntityKey key) {
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

    for (final Property property : properties)
      if (Entity.isValueNull(property.propertyType, values.get(property.propertyID)))
        return true;

    return false;
  }

  public static List<EntityKey> copyEntityKeys(final List<EntityKey> entityKeys) {
    final ArrayList<EntityKey> ret = new ArrayList<EntityKey>(entityKeys.size());
    for (final EntityKey key : entityKeys)
      ret.add(key.copy());

    return ret;
  }

  /**
   * Copies the values from key to this entity key
   * @param key the key to copy
   */
  void setValue(final EntityKey key) {
    values.clear();
    hashCode = -Integer.MAX_VALUE;
    hashCodeDirty = true;
    if (key != null) {
      for (final Property.PrimaryKeyProperty property : getProperties())
        values.put(property.propertyID, Entity.copyPropertyValue(key.getValue(property.propertyID)));

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
        throw new IllegalArgumentException("Expecting a Integer value for EntityKey: " + entityID + ", "
                + propertyID + ", got " + newValue + "; " + newValue.getClass());
      hashCode = newValue == null ? -Integer.MAX_VALUE : (Integer) newValue;
      hashCodeDirty = false;
    }
  }

  /**
   * @return true if this is a single integer column key
   */
  private boolean isSingleIntegerKey() {
    return getPropertyCount() == 1 && getFirstKeyProperty().propertyType == Type.INT;
  }
}
