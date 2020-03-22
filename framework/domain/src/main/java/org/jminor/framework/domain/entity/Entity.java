/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.entity;

import org.jminor.common.event.EventDataListener;
import org.jminor.common.valuemap.ValueMap;
import org.jminor.framework.domain.property.ColumnProperty;
import org.jminor.framework.domain.property.ForeignKeyProperty;
import org.jminor.framework.domain.property.Property;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.Format;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Represents a row in a database table, providing access to the column values via the {@link ValueMap} interface.
 */
public interface Entity extends ValueMap<Property, Object>, Comparable<Entity>, Serializable {

  /**
   * @return the entity ID
   */
  String getEntityId();

  /**
   * @return the entity definition
   */
  EntityDefinition getDefinition();

  /**
   * Returns the primary key of this entity.
   * If the entity has no primary key properties defined, this key contains not values.
   * @return the primary key of this entity
   */
  Key getKey();

  /**
   * Returns the primary key of this entity, in its original state.
   * If the entity has no primary key properties defined, this key contains not values.
   * @return the primary key of this entity in its original state
   */
  Key getOriginalKey();

  /**
   * Returns the value associated with the property identified by {@code propertyId}.
   * @param propertyId the ID of the property for which to retrieve the value
   * @return the value of the given property
   */
  Object get(String propertyId);

  /**
   * Returns the original value associated with the property identified by {@code propertyId}.
   * @param propertyId the ID of the property for which to retrieve the original value
   * @return the original value of the given property
   */
  Object getOriginal(String propertyId);

  /**
   * Returns the value associated with the property identified by {@code propertyId},
   * assuming it is a String.
   * @param propertyId the ID of the property for which to retrieve the value
   * @return the value of the property identified by {@code propertyId},
   * assuming it is a String
   * @throws ClassCastException if the value is not a String instance
   */
  String getString(String propertyId);

  /**
   * Returns the value associated with the property identified by {@code propertyId},
   * assuming it is an Integer.
   * @param propertyId the ID of the property for which to retrieve the value
   * @return the value of the property identified by {@code propertyId},
   * assuming it is an Integer
   * @throws ClassCastException if the value is not a Integer instance
   */
  Integer getInteger(String propertyId);

  /**
   * Returns the value associated with the property identified by {@code propertyId},
   * assuming it is a Long.
   * @param propertyId the ID of the property for which to retrieve the value
   * @return the value of the property identified by {@code propertyId},
   * assuming it is a Long
   * @throws ClassCastException if the value is not a Integer instance
   */
  Long getLong(String propertyId);

  /**
   * Returns the value associated with the property identified by {@code propertyId},
   * assuming it is a Character.
   * @param propertyId the ID of the property for which to retrieve the value
   * @return the value of the property identified by {@code propertyId},
   * assuming it is a Character
   * @throws ClassCastException if the value is not a Character instance
   */
  Character getCharacter(String propertyId);

  /**
   * Returns the value associated with the property identified by {@code propertyId},
   * assuming it is a Double.
   * @param propertyId the ID of the property for which to retrieve the value
   * @return the value of the property identified by {@code propertyId},
   * assuming it is a Double.
   * @throws ClassCastException if the value is not a Double instance
   * @see Property#getMaximumFractionDigits()
   */
  Double getDouble(String propertyId);

  /**
   * Returns the value associated with the property identified by {@code propertyId},
   * assuming it is a BigDecimal.
   * @param propertyId the ID of the property for which to retrieve the value
   * @return the value of the property identified by {@code propertyId},
   * assuming it is a BigDecimal.
   * @throws ClassCastException if the value is not a BigDecimal instance
   * @see Property#getMaximumFractionDigits()
   */
  BigDecimal getBigDecimal(String propertyId);

  /**
   * Returns the value associated with the property identified by {@code propertyId},
   * assuming it is a LocalTime.
   * @param propertyId the ID of the date property for which to retrieve the value
   * @return the value of the property identified by {@code propertyId},
   * assuming it is a LocalTime
   * @throws ClassCastException if the value is not a LocalTime instance
   */
  LocalTime getTime(String propertyId);

  /**
   * Returns the value associated with the property identified by {@code propertyId},
   * assuming it is a LocalDate.
   * @param propertyId the ID of the date property for which to retrieve the value
   * @return the value of the property identified by {@code propertyId},
   * assuming it is a LocalDate
   * @throws ClassCastException if the value is not a LocalDate instance
   */
  LocalDate getDate(String propertyId);

  /**
   * Returns the value associated with the property identified by {@code propertyId},
   * assuming it is a LocalDateTime.
   * @param propertyId the ID of the date property for which to retrieve the value
   * @return the value of the property identified by {@code propertyId},
   * assuming it is a LocalDatetime
   * @throws ClassCastException if the value is not a LocalDateTime instance
   */
  LocalDateTime getTimestamp(String propertyId);

  /**
   * Returns the value associated with the property identified by {@code propertyId},
   * assuming it is a Boolean.
   * @param propertyId the ID of the property for which to retrieve the value
   * @return the value of the property identified by {@code propertyId},
   * assuming it is a Boolean
   * @throws ClassCastException if the value is not a Boolean instance
   */
  Boolean getBoolean(String propertyId);

  /**
   * Returns the value associated with the property identified by {@code propertyId},
   * assuming it is a byte array.
   * @param propertyId the ID of the property for which to retrieve the value
   * @return the value of the property identified by {@code propertyId},
   * assuming it is a byte array
   * @throws ClassCastException if the value is not a byte array instance
   */
  byte[] getBlob(String propertyId);

  /**
   * Returns the value associated with the property identified by {@code propertyId},
   * formatted with the given Format.
   * @param propertyId the ID of the property for which to retrieve the formatted value
   * @param format the format object
   * @return the value of the property identified by {@code propertyId}, formatted with {@code format}
   */
  String getFormatted(String propertyId, Format format);

  /**
   * Returns the value associated with the given property, formatted with the given Format.
   * @param property the property for which to retrieve the formatted value
   * @param format the format object
   * @return the value of the given property, formatted with {@code format}
   */
  String getFormatted(Property property, Format format);

  /**
   * This method returns a String representation of the value associated with the given property,
   * if the property has a format it is used.
   * @param propertyId the ID of the property for which to retrieve the value
   * @return a String representation of the value of {@code property}
   * @see #getFormatted(Property, java.text.Format)
   */
  String getAsString(String propertyId);

  /**
   * Returns the Entity instance referenced by the given foreign key property.
   * If the underlying reference property contains a value, that is,
   * a foreign key value exists but the actual referenced entity has not
   * been loaded, an "empty" entity is returned, containing only the primary
   * key value. Null is returned only if the actual reference property is null.
   * @param foreignKeyPropertyId the ID of the property for which to retrieve the value
   * @return the value of the property identified by {@code propertyId},
   * assuming it is an Entity
   * @throws IllegalArgumentException if the property is not a foreign key property
   * @see #isLoaded(String)
   */
  Entity getForeignKey(String foreignKeyPropertyId);

  /**
   * Returns the Entity instance referenced by the given foreign key property.
   * If the underlying reference property contains a value, that is,
   * a foreign key value exists but the actual referenced entity has not
   * been loaded, an "empty" entity is returned, containing only the primary
   * key value. Null is returned only if the actual reference property is null.
   * @param foreignKeyProperty the foreign key property for which to retrieve the value
   * @return the value of the foreign key property
   * @see #isLoaded(String)
   */
  Entity getForeignKey(ForeignKeyProperty foreignKeyProperty);

  /**
   * Returns the primary key of the entity referenced by the given {@link ForeignKeyProperty},
   * if the reference is null this method returns null.
   * @param foreignKeyProperty the foreign key property for which to retrieve the underlying {@link Entity.Key}
   * @return the primary key of the underlying entity, null if no entity is referenced
   */
  Key getReferencedKey(ForeignKeyProperty foreignKeyProperty);

  /**
   * Returns true if the value of the given foreign key is null, in case of composite
   * foreign keys a single null value of a non-null property is enough.
   * @param foreignKeyProperty the foreign key property
   * @return true if the foreign key value is null
   */
  boolean isForeignKeyNull(ForeignKeyProperty foreignKeyProperty);

  /**
   * Sets the value of the given property
   * @param propertyId the ID of the property
   * @param value the value
   * @return the previous value
   * @throws IllegalArgumentException in case the value type does not fit the property
   */
  Object put(String propertyId, Object value);

  /**
   * @param propertyId the propertyId
   * @return true if the value associated with the given property has been modified
   */
  boolean isModified(String propertyId);

  /**
   * Clears the primary key values from this entity,
   * current as well as original values if any
   */
  void clearKeyValues();

  /**
   * @param entityId the entityId
   * @return true if this entity is of the given type
   */
  boolean is(String entityId);

  /**
   * @param entity the entity to compare to
   * @return true if all {@link ColumnProperty} values are equal
   */
  boolean valuesEqual(Entity entity);

  /**
   * Returns true if the entity referenced via the given foreign key property has been loaded
   * @param foreignKeyPropertyId the property id
   * @return true if the reference entity has been loaded
   */
  boolean isLoaded(String foreignKeyPropertyId);

  /**
   * @param property the property for which to retrieve the color
   * @return the color to use when displaying this property in a table
   */
  Object getColor(Property property);

  /**
   * Reverts the value associated with the given property to its original value.
   * If the value has not been modified then calling this method has no effect.
   * @param propertyId the ID of the property for which to revert the value
   */
  void revert(String propertyId);

  /**
   * Saves the value associated with the given key, that is, removes the original value.
   * If no original value exists calling this method has no effect.
   * @param propertyId the ID of the property for which to save the value
   */
  void save(String propertyId);

  /**
   * Returns true if a null value is mapped to the given property or if no mapping is found.
   * In case of foreign key properties the value of the underlying reference property is checked.
   * @param propertyId the ID of the property
   * @return true if the value mapped to the given property is null or no value is mapped
   */
  boolean isNull(String propertyId);

  /**
   * Returns true if a this Entity contains a non-null value mapped to the given property
   * In case of foreign key properties the value of the underlying reference property is checked.
   * @param propertyId the ID of the property
   * @return true if a non-null value is mapped to the given property
   */
  boolean isNotNull(String propertyId);

  /**
   * Returns true if this Entity contains a value for the given property, that value can be null.
   * @param propertyId the propertyId
   * @return true if a value is mapped to this property
   */
  boolean containsKey(String propertyId);

  /**
   * Removes the given property and value from this Entity along with the original value if any.
   * If no value is mapped to the given property, this method has no effect.
   * @param propertyId the ID of the property to remove
   * @return the previous value mapped to the given key
   */
  Object remove(String propertyId);

  /**
   * Adds a listener notified each time a value changes
   * Adding the same listener multiple times has no effect.
   * @param valueListener the listener
   * @see ValueChange
   */
  void addValueListener(EventDataListener<ValueChange> valueListener);

  /**
   * Removes the given value listener if it has been registered with this Entity.
   * @param valueListener the listener to remove
   */
  void removeValueListener(EventDataListener<ValueChange> valueListener);

  /**
   * A class representing a primary key.
   */
  interface Key extends ValueMap<ColumnProperty, Object>, Serializable {

    /**
     * @return the entity ID
     */
    String getEntityId();

    /**
     * @return a List containing the properties comprising this key
     */
    List<ColumnProperty> getProperties();

    /**
     * @return true if this key contains no values or if it contains a null value for a non-nullable key property
     */
    boolean isNull();

    /**
     * @return true if no non-nullable values are null
     */
    boolean isNotNull();

    /**
     * Returns true if a null value is mapped to the given property or no mapping exists.
     * @param propertyId the propertyId
     * @return true if the value mapped to the given property is null or none exists
     */
    boolean isNull(String propertyId);

    /**
     * Returns true if a non-null value is mapped to the given property.
     * @param propertyId the propertyId
     * @return true if a non-null value is mapped to the given property
     */
    boolean isNotNull(String propertyId);

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
    ColumnProperty getFirstProperty();

    /**
     * @return the first value contained in this key, useful for single property keys
     */
    Object getFirstValue();

    /**
     * @param propertyId the propertyId
     * @param value the value to associate with the property
     * @return the previous value
     */
    Object put(String propertyId, Object value);

    /**
     * @param propertyId the propertyId
     * @return the value associated with the given property
     */
    Object get(String propertyId);
  }
}
