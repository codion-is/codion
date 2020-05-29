/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.common.event.EventDataListener;
import is.codion.framework.domain.property.Attribute;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.domain.property.Property;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * Represents a row in a database table.
 */
public interface Entity extends Comparable<Entity>, Serializable {

  /**
   * @return the  entityId
   */
  String getEntityId();

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
   * @param propertyId the id of the property for which to retrieve the value
   * @return the value of the given property
   */
  <T> T get(Attribute<T> propertyId);

  /**
   * Retrieves the value mapped to the given property
   * @param property the property
   * @return the value mapped to the given property, null if no such mapping exists
   */
  Object get(Property property);

  /**
   * Returns the original value associated with the property identified by {@code propertyId}.
   * @param propertyId the id of the property for which to retrieve the original value
   * @return the original value of the given property
   */
  <T> T getOriginal(Attribute<T> propertyId);

  /**
   * Returns the original value associated with the given property or the current value if it has not been modified.
   * @param property the property for which to retrieve the original value
   * @return the original value
   */
  Object getOriginal(Property property);

  /**
   * This method returns a String representation of the value associated with the given property,
   * if the property has a format it is used.
   * @param propertyId the id of the property for which to retrieve the value
   * @return a String representation of the value of {@code property}
   */
  String getAsString(Attribute<?> propertyId);

  /**
   * Retrieves a string representation of the value mapped to the given property, an empty string is returned
   * in case of null values.
   * @param property the property
   * @return the value mapped to the given property as a string, an empty string if null
   */
  String getAsString(Property property);

  /**
   * Returns the Entity instance referenced by the given foreign key property.
   * If the underlying reference property contains a value, that is,
   * a foreign key value exists but the actual referenced entity has not
   * been loaded, an "empty" entity is returned, containing only the primary
   * key value. Null is returned only if the actual reference property is null.
   * @param foreignKeyPropertyId the id of the property for which to retrieve the value
   * @return the value of the property identified by {@code propertyId},
   * assuming it is an Entity
   * @throws IllegalArgumentException if the property is not a foreign key property
   * @see #isLoaded(String)
   */
  Entity getForeignKey(Attribute<Entity> foreignKeyPropertyId);

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
   * @param propertyId the id of the property
   * @param value the value
   * @return the previous value
   * @throws IllegalArgumentException in case the value type does not fit the property
   */
  <T> T put(Attribute<T> propertyId, T value);

  /**
   * Maps the given value to the given property, returning the old value if any.
   * @param property the property
   * @param value the value
   * @return the previous value mapped to the given property
   */
  Object put(Property property, Object value);

  /**
   * Removes the given property and value from this value map along with the original value if any.
   * If no value is mapped to the given property, this method has no effect.
   * @param property the property to remove
   * @return the value that was removed, null if no value was found
   */
  Object remove(Property property);

  /**
   * @param propertyId the propertyId
   * @return true if the value associated with the given property has been modified
   */
  boolean isModified(Attribute<?> propertyId);

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
   * After a call to this method this Entity contains the same values and original values as the source entity.
   * A null argument to this method clears the destination entity of all values and original values.
   * Value change events for affected properties are fired after all values have been set, in no particular order.
   * @param entity the entity to copy or null for clearing the destination map
   */
  void setAs(Entity entity);

  /**
   * Returns true if the entity referenced via the given foreign key property has been loaded
   * @param foreignKeyPropertyId the property id
   * @return true if the reference entity has been loaded
   */
  boolean isLoaded(Attribute<Entity> foreignKeyPropertyId);

  /**
   * @param property the property for which to retrieve the color
   * @return the color to use when displaying this property in a table
   */
  Object getColor(Property property);

  /**
   * Reverts the value associated with the given property to its original value.
   * If the value has not been modified then calling this method has no effect.
   * @param propertyId the id of the property for which to revert the value
   */
  void revert(Attribute<?> propertyId);

  /**
   * Reverts the value associated with the given property to its original value.
   * If the value has not been modified or the property is not found then calling this method has no effect.
   * @param property the property for which to revert the value
   */
  void revert(Property property);

  /**
   * Saves the value associated with the given key, that is, removes the original value.
   * If no original value exists calling this method has no effect.
   * @param propertyId the id of the property for which to save the value
   */
  void save(Attribute<?> propertyId);

  /**
   * Saves the value associated with the given property, that is, removes the original value.
   * If no original value exists calling this method has no effect.
   * @param property the property for which to save the value
   */
  void save(Property property);

  /**
   * Saves all the value modifications that have been made.
   * This value map will be unmodified after a call to this method.
   */
  void saveAll();

  /**
   * Reverts all value modifications that have been made.
   * This entity will be unmodified after a call to this method.
   * If no modifications have been made then calling this method has no effect.
   */
  void revertAll();

  /**
   * Returns true if a null value is mapped to the given property or if no mapping is found.
   * In case of foreign key properties the value of the underlying reference property is checked.
   * @param propertyId the id of the property
   * @return true if the value mapped to the given property is null or no value is mapped
   */
  boolean isNull(Attribute<?> propertyId);

  /**
   * Returns true if a null value is mapped to the given property or the property is not found.
   * @param property the property
   * @return true if the value mapped to the given property is null
   */
  boolean isNull(Property property);

  /**
   * Returns true if a this Entity contains a non-null value mapped to the given property
   * In case of foreign key properties the value of the underlying reference property is checked.
   * @param propertyId the id of the property
   * @return true if a non-null value is mapped to the given property
   */
  boolean isNotNull(Attribute<?> propertyId);

  /**
   * Returns true if a this ValueMap contains a non-null value mapped to the given property
   * @param property the property
   * @return true if the value mapped to the given property is not null
   */
  boolean isNotNull(Property property);

  /**
   * Returns true if this Entity contains a value for the given property, that value can be null.
   * @param propertyId the propertyId
   * @return true if a value is mapped to this property
   */
  boolean containsKey(Attribute<?> propertyId);

  /**
   * Returns true if this ValueMap contains a value for the given property, that value can be null.
   * @param property the property
   * @return true if a value is mapped to this property
   */
  boolean containsKey(Property property);

  /**
   * @return an unmodifiable view of the keys mapping the values in this Entity
   */
  Set<Property> keySet();

  /**
   * @return an unmodifiable view of the keys mapping the original values in this Entity
   */
  Set<Property> originalKeySet();

  /**
   * @return the number of values in this map
   */
  int size();

  /**
   * Removes the given property and value from this Entity along with the original value if any.
   * If no value is mapped to the given property, this method has no effect.
   * @param propertyId the id of the property to remove
   * @return the previous value mapped to the given key
   */
  <T> T remove(Attribute<T> propertyId);

  /**
   * @return true if one or more values have been modified.
   */
  boolean isModified();

  /**
   * Returns true if the value associated with the given property has been modified..
   * @param property the property
   * @return true if the value has changed
   */
  boolean isModified(Property property);

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
  interface Key extends Serializable {

    /**
     * @return the  entityId
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
    boolean isNull(Attribute<?> propertyId);

    /**
     * Returns true if a non-null value is mapped to the given property.
     * @param propertyId the propertyId
     * @return true if a non-null value is mapped to the given property
     */
    boolean isNotNull(Attribute<?> propertyId);

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
    <T> T put(Attribute<T> propertyId, T value);

    /**
     * @param propertyId the propertyId
     * @param value the value to associate with the property
     * @return the previous value
     */
    Object put(ColumnProperty property, Object value);

    /**
     * @param propertyId the propertyId
     * @return the value associated with the given property
     */
    <T> T get(Attribute<T> propertyId);

    /**
     * @param property the property
     * @return the value associated with the given property
     */
    Object get(ColumnProperty property);

    /**
     * After a call to this method this ValueMap contains the same values and original values as the source map.
     * A null argument to this method clears the destination map of all values and original values.
     * Value change events for affected keys are fired after all values have been set, in no particular order.
     * @param sourceKey the key to copy or null for clearing the destination map
     */
    void setAs(Key sourceKey);

    /**
     * @return the number of values in this key
     */
    int size();
  }
}
