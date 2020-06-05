/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.property.ColumnProperty;

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;

/**
 * Represents a row in a database table.
 */
public interface Entity extends Comparable<Entity>, Serializable {

  /**
   * @return the entity type
   */
  EntityType getEntityType();

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
   * If the value has not been modified the current value is returned.
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
   * @param foreignKeyAttribute the foreign key attribute for which to retrieve the underlying {@link Key}
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
   * @param entityType the entity type
   * @return true if this entity is of the given type
   */
  boolean is(EntityType entityType);

  /**
   * @param entity the entity to compare to
   * @return true if all {@link ColumnProperty} values are equal
   */
  boolean valuesEqual(Entity entity);

  /**
   * After a call to this method this Entity contains the same values and original values as the source entity.
   * A null argument to this method clears the destination entity of all values and original values.
   * @param entity the entity to copy or null for clearing the destination map
   * @return the affected attributes
   */
  Collection<Attribute<?>> setAs(Entity entity);

  /**
   * Returns true if the entity referenced via the given foreign key attribute has been loaded
   * @param foreignKeyAttribute the attribute
   * @return true if the reference entity has been loaded
   */
  boolean isLoaded(Attribute<Entity> foreignKeyAttribute);

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
   * @param <T> the value type
   */
  <T> void save(Attribute<T> attribute);

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
   * @param <T> the value type
   * @return true if the value mapped to the given attribute is null or no value is mapped
   */
  <T> boolean isNull(Attribute<T> attribute);

  /**
   * Returns true if a this Entity contains a non-null value mapped to the given attribute
   * In case of foreign key attributes the value of the underlying reference attribute is checked.
   * @param attribute the attribute
   * @param <T> the value type
   * @return true if a non-null value is mapped to the given attribute
   */
  <T> boolean isNotNull(Attribute<T> attribute);

  /**
   * Returns true if this Entity contains a value for the given attribute, that value can be null.
   * @param attribute the attribute
   * @param <T> the value type
   * @return true if a value is mapped to this attribute
   */
  <T> boolean containsKey(Attribute<T> attribute);

  /**
   * @return an unmodifiable view of the keys mapping the values in this Entity
   */
  Set<Attribute<?>> keySet();

  /**
   * @return an unmodifiable view of the keys mapping the original values in this Entity
   */
  Set<Attribute<?>> originalKeySet();

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
}
