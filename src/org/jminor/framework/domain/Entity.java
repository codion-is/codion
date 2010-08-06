/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.model.valuemap.ValueChangeMap;
import org.jminor.common.model.valuemap.ValueMap;

import java.sql.Timestamp;
import java.text.Format;
import java.util.Date;
import java.util.List;

/**
 * An ORM entity interface, providing access to the property values via the ValueMap interface.
 */
public interface Entity extends ValueChangeMap<String, Object>, Comparable<Entity> {

  /**
   * @return the entity ID
   */
  String getEntityID();

  /**
   * @return the primary key of this entity
   */
  Key getPrimaryKey();

  /**
   * @return the primary key of this entity in it's original state
   */
  Key getOriginalPrimaryKey();

  /**
   * Retrieves the property identified by propertyID from the entity repository
   * @param propertyID the ID of the property to retrieve
   * @return the property identified by propertyID
   */
  Property getProperty(final String propertyID);

  /**
   * @param property the property for which to retrieve the value
   * @return the value of the given property
   */
  Object getValue(final Property property);

  /**
   * @param propertyID the ID of the property for which to retrieve the value
   * @return the value of the property identified by <code>propertyID</code>,
   * assuming it is a String
   * @throws ClassCastException if the value is not a String instance
   */
  String getStringValue(final String propertyID);

  /**
   * @param propertyID the ID of the property for which to retrieve the value
   * @return the value of the property identified by <code>propertyID</code>,
   * assuming it is an Integer
   * @throws ClassCastException if the value is not a Integer instance
   */
  Integer getIntValue(final String propertyID);

  /**
   * @param propertyID the ID of the property for which to retrieve the value
   * @return the value of the property identified by <code>propertyID</code>,
   * assuming it is a Character
   * @throws ClassCastException if the value is not a Character instance
   */
  Character getCharValue(final String propertyID);

  /**
   * @param propertyID the ID of the property for which to retrieve the value
   * @return the value of the property identified by <code>propertyID</code>,
   * assuming it is a Double
   * @throws ClassCastException if the value is not a Double instance
   */
  Double getDoubleValue(final String propertyID);

  /**
   * @param propertyID the ID of the date property for which to retrieve the value
   * @return the value of the property identified by <code>propertyID</code>,
   * assuming it is a Date
   * @throws ClassCastException if the value is not a Date instance
   */
  Date getDateValue(final String propertyID);

  /**
   * @param propertyID the ID of the date property for which to retrieve the value
   * @return the value of the property identified by <code>propertyID</code>,
   * assuming it is a Timestamp
   * @throws ClassCastException if the value is not a Timestamp instance
   */
  Timestamp getTimestampValue(final String propertyID);

  /**
   * @param propertyID the ID of the property for which to retrieve the value
   * @return the value of the property identified by <code>propertyID</code>,
   * assuming it is a Boolean
   * @throws ClassCastException if the value is not a Boolean instance
   */
  Boolean getBooleanValue(final String propertyID);

  /**
   * @param propertyID the ID of the property for which to retrieve the formatted value
   * @return the value of the property identified by <code>propertyID</code>, formatting it
   * with the format object associated with the property
   */
  String getFormattedValue(final String propertyID);

  /**
   * @param propertyID the ID of the property for which to retrieve the formatted value
   * @param format the format object
   * @return the value of the property identified by <code>propertyID</code>, formatted with <code>format</code>
   */
  String getFormattedValue(final String propertyID, final Format format);

  /**
   * @param property the property for which to retrieve the formatted value
   * @return the value of the given property formatted with the format object associated with the property
   */
  String getFormattedValue(final Property property);

  /**
   * @param property the property for which to retrieve the formatted value
   * @param format the format object
   * @return the value of the given property, formatted with <code>format</code>
   */
  String getFormattedValue(final Property property, final Format format);

  /**
   * This method returns a String representation of the value associated with the given property,
   * if the property has a format it is used.
   * @param property the property for which to retrieve the value
   * @return a String representation of the value of <code>property</code>
   * @see #getFormattedValue(Property, java.text.Format)
   */
  String getValueAsString(final Property property);

  /**
   * @param foreignKeyPropertyID the ID of the property for which to retrieve the value
   * @return the value of the property identified by <code>propertyID</code>,
   * assuming it is an Entity
   * @throws RuntimeException if the property is not a foreign key property
   */
  Entity getForeignKeyValue(final String foreignKeyPropertyID);

  /**
   * Returns the primary key of the entity referenced by the given ForeignKeyProperty,
   * if the reference is null this method returns null.
   * @param foreignKeyProperty the foreign key property for which to retrieve the underlying EntityKey
   * @return the primary key of the underlying entity, null if no entity is referenced
   */
  Key getReferencedPrimaryKey(final Property.ForeignKeyProperty foreignKeyProperty);

  /**
   * Returns true if the value of the given foreign key is null, in case of composite
   * foreign keys a single null value is enough.
   * @param foreignKeyProperty the foreign key property
   * @return true if the foreign key value is null
   */
  boolean isForeignKeyNull(final Property.ForeignKeyProperty foreignKeyProperty);

  /**
   * Sets the value of the given property
   * @param property the property
   * @param value the value
   * @return the previous value
   */
  Object setValue(final Property property, final Object value);

  /**
   * @return true if the this entity instance has a null primary key
   */
  boolean isNull();

  /**
   * @param property the property
   * @return true if the given property has a null value
   */
  boolean isValueNull(final Property property);

  /**
   * @param property the property
   * @return true if this entity contains a value for the given property
   */
  boolean containsValue(final Property property);

  /**
   * Initializes the given value assuming it has no previously set value.
   * This method does not propagate foreign key values nor set denormalized values.
   * This method should be used with care, if at all.
   * @param property the property for which to initialize the value
   * @param value the value
   */
  void initializeValue(final Property property, final Object value);

  /**
   * @param entityID the entityID
   * @return true if this entity is of the given type
   */
  boolean is(final String entityID);

  /**
   * @param entity the entity to compare to
   * @return true if all property values are equal
   */
  boolean propertyValuesEqual(final Entity entity);

  /**
   * Returns true if the entity referenced via the given foreign key property has been loaded
   * @param foreignKeyPropertyID the property id
   * @return true if the reference entity has been loaded
   */
  boolean isLoaded(final String foreignKeyPropertyID);

  /**
   * A class representing column key objects for entities, contains the values for those columns.
   */
  interface Key extends ValueMap<String, Object> {

    /**
     * @return the entity ID
     */
    String getEntityID();

    /**
     * @return a List containing the properties comprising this key
     */
    List<Property.PrimaryKeyProperty> getProperties();

    /**
     * @return the number of properties comprising this key
     */
    int getPropertyCount();

    /**
     * @return true if one of the properties has a null value
     */
    boolean isNull();

    /**
     * @return true if this primary key is based on a single integer column
     */
    boolean isSingleIntegerKey();

    /**
     * @return true if this key is comprised of multiple properties.
     */
    boolean isCompositeKey();

    /**
     * @return the first key property, useful for single property keys
     */
    Property.PrimaryKeyProperty getFirstKeyProperty();

    /**
     * @return the first value contained in this key, useful for single property keys
     */
    Object getFirstKeyValue();
  }
}
