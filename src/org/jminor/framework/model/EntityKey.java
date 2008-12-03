/*
 * Copyright (c) 2008, Bj�rn Darri Sigur�sson. All Rights Reserved.
 */
package org.jminor.framework.model;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class for representing column key objects for entities, contains the values for those columns
 */
public class EntityKey implements Externalizable {

  /**
   * the entity ID
   */
  String entityID;

  /**
   * Contains the values of this key
   */
  Map<String, Object> keyValues;

  /**
   * The number of properties in this this key
   */
  int propertyCount;

  /**
   * True if this is a single integer key
   */
  private boolean isSingleIntegerKey;

  /**
   * Caching the hash code
   */
  private int hashCode = -Integer.MAX_VALUE;

  /**
   * True if the hash code value has been invalidated and needs to be recalculated
   */
  private boolean hashCodeDirty = true;

  /**
   * Caching this extremely frequently referenced attribute
   */
  transient List<Property.PrimaryKeyProperty> properties;

  /**
   * For the Externalizable implementation
   */
  public EntityKey() {}

  /**
   * Instantiates a new EntityKey for the given entity type
   * @param entityID the entity ID
   */
  public EntityKey(final String entityID) {
    this.entityID = entityID;
    this.properties = EntityRepository.get().getPrimaryKeyProperties(entityID);
    this.propertyCount = properties.size();
    this.isSingleIntegerKey = propertyCount == 1 && properties.get(0).propertyType == Type.INT;
    this.keyValues = new HashMap<String, Object>(propertyCount);
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
    return properties;
  }

  /**
   * @param propertyID the property identifier
   * @return the key property identified by propertyID, null if the property is not found
   */
  public Property.PrimaryKeyProperty getProperty(final String propertyID) {
    for (final Property.PrimaryKeyProperty property : properties)
      if (property.propertyID.equals(propertyID))
        return property;

    return null;
  }

  /**
   * @return the first key property
   */
  public Property getFirstKeyProperty() {
    return properties.size() > 0 ? properties.get(0) : null;
  }

  /**
   * @return the column names of the properties comprising this key
   */
  public String[] getKeyColumnNames() {
    return EntityRepository.get().getPrimaryKeyColumnNames(entityID);
  }

  /**
   * @return true if this is a single integer column key
   */
  public boolean isSingleIntegerKey() {
    return isSingleIntegerKey;
  }

  /**
   * @param propertyID the property identifier
   * @return true if this key contains a value for propertyID
   */
  public boolean hasValue(final String propertyID) {
    return keyValues.containsKey(propertyID);
  }

  /**
   * @param propertyID the ID associated with the property
   * @return true if this key contains a property with the given identifier
   */
  public boolean containsProperty(final String propertyID) {
    for (final Property.PrimaryKeyProperty property : properties)
      if (property.propertyID.equals(propertyID))
        return true;

    return false;
  }

  /**
   * @return the first value contained in this key, useful for single property keys
   */
  public Object getFirstKeyValue() {
    return keyValues.values().iterator().next();
  }

  /**
   * @param propertyID the property identifier
   * @return the value of the property identified by propertyID, if the key
   * does not contain a value for the property, the default value is returned
   */
  public Object getValue(final String propertyID) {
    if (!keyValues.containsKey(propertyID))
      return getProperty(propertyID).getDefaultValue();

    return keyValues.get(propertyID);
  }

  /**
   * @return a string representation of this key
   */
  public String toString() {
    final StringBuffer ret = new StringBuffer();
    int i = 0;
    for (final Property.PrimaryKeyProperty property : properties) {
      ret.append(property.propertyID).append("=");
      ret.append(getValue(property.propertyID));
      if (i++ < propertyCount -1)
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
    return propertyCount;
  }

  /**
   * @param object the object to compare with
   * @return true if object is equal to this key
   */
  public boolean equals(final Object object) {
    return object instanceof EntityKey && equals((EntityKey) object);
  }

  /**
   * @param key the key to compare with
   * @return true if key is equal to this key
   */
  public boolean equals(final EntityKey key) {
    return key.entityID.equals(entityID) && hashCode() == key.hashCode();
  }

  /**
   * @return a "pseudo" hash code
   */
  public int hashCode() {
    if (hashCodeDirty) {
      int hash = 0;
      for (final Object value : keyValues.values())
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
    if (isSingleIntegerKey)
      return hashCode() == -Integer.MAX_VALUE;

    if (hashCode() == -Integer.MAX_VALUE)
      return true;

    for (final Property property : properties)
      if (Entity.isValueNull(property.propertyType, keyValues.get(property.propertyID)))
        return true;

    return false;
  }

  /**
   * Clears all values from this key
   */
  public void clear() {
    keyValues.clear();
    hashCode = -Integer.MAX_VALUE;
    hashCodeDirty = true;
  }

  /** {@inheritDoc} */
  public void writeExternal(final ObjectOutput out) throws IOException {
    out.writeObject(entityID);
    out.writeObject(keyValues);
    out.writeInt(propertyCount);
    out.writeBoolean(isSingleIntegerKey);
    out.writeBoolean(hashCodeDirty);
    out.writeInt(hashCode);
  }

  /** {@inheritDoc} */
  @SuppressWarnings({"unchecked"})
  public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
    entityID = (String) in.readObject();
    keyValues = (Map<String, Object>) in.readObject();
    propertyCount = in.readInt();
    isSingleIntegerKey = in.readBoolean();
    hashCodeDirty = in.readBoolean();
    hashCode = in.readInt();
    properties = EntityRepository.get().getPrimaryKeyProperties(entityID);
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
    clear();
    if (key != null) {
      for (final Property.PrimaryKeyProperty property : properties) {
        final String propertyID = property.propertyID;
        final Object newValue = key.getValue(propertyID);
        keyValues.put(propertyID, newValue instanceof Entity ? ((Entity)newValue).getCopy() : newValue);
      }

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
    keyValues.put(propertyID, newValue);
    hashCodeDirty = true;
    if (isSingleIntegerKey) {
      if (!(newValue == null || newValue instanceof Integer))
        throw new IllegalArgumentException("Expecting a Integer value for EntityKey: " + entityID + ", "
                + propertyID + ", got " + newValue + "; " + newValue.getClass());
      hashCode = newValue == null ? -Integer.MAX_VALUE : (Integer) newValue;
      hashCodeDirty = false;
    }
  }
}
