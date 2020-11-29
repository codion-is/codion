/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Represents a row in a database table.
 */
public interface Entity extends Comparable<Entity> {

  /**
   * @return the entity type
   */
  EntityType<?> getEntityType();

  /**
   * Sets the value of the given attribute, returning the old value if any
   * @param attribute the attribute
   * @param value the value
   * @param <T> the value type
   * @return the previous value
   */
  <T> T put(Attribute<T> attribute, T value);

  /**
   * Returns the value associated with {@code attribute}.
   * @param attribute the attribute for which to retrieve the value
   * @param <T> the value type
   * @return the value of the given attribute
   */
  <T> T get(Attribute<T> attribute);

  /**
   * Returns the value associated with {@code attribute}, wrapped in an {@link Optional}.
   * @param attribute the attribute for which to retrieve the value
   * @param <T> the value type
   * @return the value of the given attribute, wrapped in an {@link Optional}
   */
  <T> Optional<T> getOptional(Attribute<T> attribute);

  /**
   * Returns the original value associated with {@code attribute}.
   * If the value has not been modified the current value is returned.
   * @param attribute the attribute for which to retrieve the original value
   * @param <T> the value type
   * @return the original value of the given attribute
   */
  <T> T getOriginal(Attribute<T> attribute);

  /**
   * This method returns a String representation of the value associated with the given attribute,
   * if the associated property has a format it is used.
   * @param attribute the attribute for which to retrieve the value
   * @param <T> the value type
   * @return a String representation of the value of {@code attribute}
   */
  <T> String getAsString(Attribute<T> attribute);

  /**
   * Reverts the value associated with the given attribute to its original value.
   * If the value has not been modified then calling this method has no effect.
   * @param attribute the attribute for which to revert the value
   * @param <T> the value type
   */
  <T> void revert(Attribute<T> attribute);

  /**
   * Reverts all value modifications that have been made.
   * This entity will be unmodified after a call to this method.
   * If no modifications have been made then calling this method has no effect.
   */
  void revertAll();

  /**
   * Saves the value associated with the given attribute, that is, removes the original value.
   * If no original value exists calling this method has no effect.
   * @param attribute the attribute for which to save the value
   * @param <T> the value type
   */
  <T> void save(Attribute<T> attribute);

  /**
   * Saves all the value modifications that have been made.
   * This entity will be unmodified after a call to this method.
   * @see #isModified()
   */
  void saveAll();

  /**
   * Removes the given value from this Entity along with the original value if any.
   * If no value is mapped to the given attribute, this method has no effect.
   * @param attribute the attribute to remove
   * @param <T> the value type
   * @return the previous value mapped to the given attribute
   */
  <T> T remove(Attribute<T> attribute);

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
  <T> boolean containsValue(Attribute<T> attribute);

  /**
   * Returns the Entity instance referenced by the given foreign key attribute.
   * If the underlying reference attribute contains a value, that is,
   * a foreign key value exists but the actual referenced entity has not
   * been loaded, an "empty" entity is returned, containing only the primary
   * key value. Null is returned only if the actual reference attribute is null.
   * @param foreignKey the foreign key for which to retrieve the value
   * @return the value of {@code foreignKey},
   * assuming it is an Entity
   * @throws IllegalArgumentException if the attribute is not a foreign key attribute
   * @see #isLoaded(ForeignKey)
   */
  Entity getForeignKey(ForeignKey foreignKey);

  /**
   * Returns the primary key of the entity referenced by the given {@link Attribute},
   * if the reference is null this method returns null.
   * @param foreignKey the foreign key for which to retrieve the underlying {@link Key}
   * @return the primary key of the underlying entity, null if no entity is referenced
   */
  Key getReferencedKey(ForeignKey foreignKey);

  /**
   * Returns true if the value of the given foreign key is null, in case of composite
   * foreign keys a single null value of a non-null attribute is enough.
   * @param foreignKey the foreign key
   * @return true if the foreign key value is null
   */
  boolean isForeignKeyNull(ForeignKey foreignKey);

  /**
   * @param attribute the attribute
   * @param <T> the attribute type
   * @return true if the value associated with the given attribute has been modified
   */
  <T> boolean isModified(Attribute<T> attribute);

  /**
   * Returns true if one or more writable attributes have been modified, read only and non-updatable attributes
   * are excluded unless they are transient.
   * @return true if one or more attributes have been modified since the entity was instantiated
   */
  boolean isModified();

  /**
   * Clears the primary key values from this entity,
   * current as well as original values if any
   * @return this Entity instance
   */
  Entity clearPrimaryKey();

  /**
   * Compares all column based values in the given entity to the values in this entity instance.
   * Returns true if these two entities contain values for the same columns and all the values are equal.
   * @param entity the entity to compare to
   * @return true if all the column based values in this entity instance are present and equal to the values in the given entity
   * @throws IllegalArgumentException in case the entity is not of the same type
   */
  boolean columnValuesEqual(Entity entity);

  /**
   * After a call to this method this Entity contains the same values and original values as the source entity.
   * A null argument to this method clears the destination entity of all values and original values.
   * @param entity the entity to copy or null for clearing all values in this instance
   * @return the affected attributes
   */
  Collection<Attribute<?>> setAs(Entity entity);

  /**
   * Returns true if the entity referenced via the given foreign key attribute has been loaded
   * @param foreignKey the attribute
   * @return true if the reference entity has been loaded
   */
  boolean isLoaded(ForeignKey foreignKey);

  /**
   * Returns the primary key of this entity.
   * If the entity has no primary key attribute defined, this key contains no values.
   * @return the primary key of this entity
   */
  Key getPrimaryKey();

  /**
   * Returns the primary key of this entity, in its original state.
   * If the entity has no primary key attributes defined, this key contains no values.
   * @return the primary key of this entity in its original state
   */
  Key getOriginalPrimaryKey();

  /**
   * @return an unmodifiable view of the entries in this Entity
   */
  Set<Map.Entry<Attribute<?>, Object>> entrySet();

  /**
   * @return an unmodifiable view of the original entries values in this Entity, that is,
   * the original values of attributes that have been modified
   */
  Set<Map.Entry<Attribute<?>, Object>> originalEntrySet();
}
