/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.common.event.EventDataListener;
import is.codion.framework.domain.attribute.Attribute;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.Property;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

/**
 * Represents a row in a database table.
 */
public interface Entity extends Comparable<Entity>, Serializable {

  /**
   * @return the entityId
   */
  Identity getEntityId();

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
   * Returns the value associated with the property based on {@code attribute}.
   * @param attribute the attribute for which to retrieve the value
   * @param <T> the value type
   * @return the value of the given property
   */
  <T> T get(Attribute<T> attribute);

  /**
   * Returns the original value associated with the property based on {@code attribute}.
   * @param attribute the attribute for which to retrieve the original value
   * @param <T> the value type
   * @return the original value of the given property
   */
  <T> T getOriginal(Attribute<T> attribute);

  /**
   * This method returns a String representation of the value associated with the given attribute,
   * if the property has a format it is used.
   * @param attribute the attribute for which to retrieve the value
   * @param <T> the value type
   * @return a String representation of the value of {@code attribute}
   */
  <T> String getAsString(Attribute<T> attribute);

  /**
   * Returns the Entity instance referenced by the given foreign key attribute.
   * If the underlying reference property contains a value, that is,
   * a foreign key value exists but the actual referenced entity has not
   * been loaded, an "empty" entity is returned, containing only the primary
   * key value. Null is returned only if the actual reference property is null.
   * @param foreignKeyAttribute the attribute for which to retrieve the value
   * @return the value of the property based on {@code foreignKeyAttribute},
   * assuming it is an Entity
   * @throws IllegalArgumentException if the attribute is not a foreign key attribute
   * @see #isLoaded(Attribute)
   */
  Entity getForeignKey(Attribute<Entity> foreignKeyAttribute);

  /**
   * Returns the primary key of the entity referenced by the given {@link Attribute},
   * if the reference is null this method returns null.
   * @param foreignKeyAttribute the foreign key attribute for which to retrieve the underlying {@link Entity.Key}
   * @return the primary key of the underlying entity, null if no entity is referenced
   */
  Key getReferencedKey(Attribute<Entity> foreignKeyAttribute);

  /**
   * Returns true if the value of the given foreign key is null, in case of composite
   * foreign keys a single null value of a non-null property is enough.
   * @param foreignKeyAttribute the foreign key attribute
   * @return true if the foreign key value is null
   */
  boolean isForeignKeyNull(Attribute<Entity> foreignKeyAttribute);

  /**
   * Sets the value of the given attribute, returning the old value if any
   * @param attribute the attribute
   * @param value the value
   * @param <T> the value type
   * @return the previous value
   */
  <T> T put(Attribute<T> attribute, T value);

  /**
   * @param attribute the attribute
   * @return true if the value associated with the given attribute has been modified
   */
  boolean isModified(Attribute<?> attribute);

  /**
   * Clears the primary key values from this entity,
   * current as well as original values if any
   */
  void clearKeyValues();

  /**
   * @param entityId the entityId
   * @return true if this entity is of the given type
   */
  boolean is(Identity entityId);

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
   * Returns true if the entity referenced via the given foreign key attribute has been loaded
   * @param foreignKeyAttribute the attribute
   * @return true if the reference entity has been loaded
   */
  boolean isLoaded(Attribute<Entity> foreignKeyAttribute);

  /**
   * @param property the property for which to retrieve the color
   * @return the color to use when displaying this property in a table
   */
  Object getColor(Property<?> property);

  /**
   * Reverts the value associated with the given attribute to its original value.
   * If the value has not been modified then calling this method has no effect.
   * @param attribute the attribute for which to revert the value
   * @param <T> the value type
   */
  <T> void revert(Attribute<T> attribute);

  /**
   * Saves the value associated with the given attribute, that is, removes the original value.
   * If no original value exists calling this method has no effect.
   * @param attribute the attribute for which to save the value
   */
  void save(Attribute<?> attribute);

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
   * Returns true if a null value is mapped to the given attribute or if no mapping is found.
   * In case of foreign key attributes the value of the underlying reference attribute is checked.
   * @param attribute the attribute
   * @return true if the value mapped to the given attribute is null or no value is mapped
   */
  boolean isNull(Attribute<?> attribute);

  /**
   * Returns true if a this Entity contains a non-null value mapped to the given attribute
   * In case of foreign key attributes the value of the underlying reference attribute is checked.
   * @param attribute the attribute
   * @return true if a non-null value is mapped to the given attribute
   */
  boolean isNotNull(Attribute<?> attribute);

  /**
   * Returns true if this Entity contains a value for the given attribute, that value can be null.
   * @param attribute the attribute
   * @return true if a value is mapped to this attribute
   */
  boolean containsKey(Attribute<?> attribute);

  /**
   * @return an unmodifiable view of the keys mapping the values in this Entity
   */
  Set<Attribute<?>> keySet();

  /**
   * @return an unmodifiable view of the keys mapping the original values in this Entity
   */
  Set<Attribute<?>> originalKeySet();

  /**
   * @param attribute the attribute
   * @return the property associated with the given attribute
   */
  Property<?> getProperty(Attribute<?> attribute);

  /**
   * @return the number of values in this map
   */
  int size();

  /**
   * Removes the given property and value from this Entity along with the original value if any.
   * If no value is mapped to the given property, this method has no effect.
   * @param attribute the attribute to remove
   * @param <T> the value type
   * @return the previous value mapped to the given key
   */
  <T> T remove(Attribute<T> attribute);

  /**
   * @return true if one or more values have been modified.
   */
  boolean isModified();

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
     * @return the entityId
     */
    Identity getEntityId();

    /**
     * @return a List containing the properties comprising this key
     */
    List<ColumnProperty<?>> getProperties();

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
     * @param attribute the attribute
     * @return true if the value mapped to the given property is null or none exists
     */
    boolean isNull(Attribute<?> attribute);

    /**
     * Returns true if a non-null value is mapped to the given property.
     * @param attribute the attribute
     * @return true if a non-null value is mapped to the given property
     */
    boolean isNotNull(Attribute<?> attribute);

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
    ColumnProperty<?> getFirstProperty();

    /**
     * @return the first value contained in this key, useful for single property keys
     */
    Object getFirstValue();

    /**
     * @param attribute the attribute
     * @param value the value to associate with the property
     * @param <T> the value type
     * @return the previous value
     */
    <T> T put(Attribute<T> attribute, T value);

    /**
     * @param attribute the attribute
     * @param <T> the value type
     * @return the value associated with the given property
     */
    <T> T get(Attribute<T> attribute);

    /**
     * After a call to this method this Key contains the same values as the source key.
     * A null argument to this method clears the destination key of all values.
     * Value change events for affected properties are fired after all values have been set, in no particular order.
     * @param sourceKey the key to copy or null for clearing the destination key
     */
    void setAs(Key sourceKey);

    /**
     * @return the number of values in this key
     */
    int size();
  }

  /**
   * Factory for {@link Attribute} instances associated with this identity.
   */
  interface Identity extends is.codion.framework.domain.identity.Identity {

    /**
     * Creates a new {@link Attribute}, associated with this Identity.
     * @param name the attribute name
     * @param typeClass the class representing the attribute value type
     * @param <T> the attribute type
     * @return a new {@link Attribute}
     */
    <T> Attribute<T> attribute(String name, Class<T> typeClass);

    /**
     * Creates a new {@link Attribute} associated with this Identity.
     * Use this when you don't have access to an actual Attribute instance, only its name
     * and identity, but need to access the value associated with it.
     * @param name the attribute name
     * @return a new {@link Attribute}
     */
    Attribute<Object> objectAttribute(String name);

    /**
     * Creates a new Long based attribute, associated with this Identity.
     * @param name the attribute name.
     * @return a new Long based attribute.
     */
    Attribute<Long> longAttribute(String name);

    /**
     * Creates a new Integer based attribute, associated with this Identity.
     * @param name the attribute name.
     * @return a new Integer based attribute.
     */
    Attribute<Integer> integerAttribute(String name);

    /**
     * Creates a new Double based attribute, associated with this Identity.
     * @param name the attribute name.
     * @return a new Double based attribute.
     */
    Attribute<Double> doubleAttribute(String name);

    /**
     * Creates a new BigDecimal based attribute, associated with this Identity.
     * @param name the attribute name.
     * @return a new BigDecimal based attribute.
     */
    Attribute<BigDecimal> bigDecimalAttribute(String name);

    /**
     * Creates a new LocalDate based attribute, associated with this Identity.
     * @param name the attribute name.
     * @return a new LocalDate based attribute.
     */
    Attribute<LocalDate> localDateAttribute(String name);

    /**
     * Creates a new LocalTime based attribute, associated with this Identity.
     * @param name the attribute name.
     * @return a new LocalTime based attribute.
     */
    Attribute<LocalTime> localTimeAttribute(String name);

    /**
     * Creates a new LocalDateTime based attribute, associated with this Identity.
     * @param name the attribute name.
     * @return a new LocalDateTime based attribute.
     */
    Attribute<LocalDateTime> localDateTimeAttribute(String name);

    /**
     * Creates a new String based attribute, associated with this Identity.
     * @param name the attribute name.
     * @return a new String based attribute.
     */
    Attribute<String> stringAttribute(String name);

    /**
     * Creates a new Boolean based attribute, associated with this Identity.
     * @param name the attribute name.
     * @return a new Boolean based attribute.
     */
    Attribute<Boolean> booleanAttribute(String name);

    /**
     * Creates a new {@link Attribute}, associated with this Identity.
     * @param name the attribute name
     * @return a new {@link Attribute}
     */
    Attribute<Entity> entityAttribute(String name);

    /**
     * Creates a new {@link Attribute}, associated with this Identity.
     * @param name the attribute name
     * @return a new {@link Attribute}
     */
    Attribute<byte[]> blobAttribute(String name);
  }
}
