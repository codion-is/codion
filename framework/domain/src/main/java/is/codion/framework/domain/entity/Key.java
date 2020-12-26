/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import java.util.Collection;
import java.util.Optional;

/**
 * Represents a unique attribute value combination for a given entity.
 */
public interface Key {

  /**
   * @return the entity type
   */
  EntityType<?> getEntityType();

  /**
   * @return the attributes comprising this key
   */
  Collection<Attribute<?>> getAttributes();

  /**
   * @return true if this key represents a primary key for a entity, note that this is true
   * for empty keys representing entities without a defined primary key
   */
  boolean isPrimaryKey();

  /**
   * @return true if this key contains no values or if it contains a null value for a non-nullable key attribute
   */
  boolean isNull();

  /**
   * @return true if no non-nullable values are null
   */
  boolean isNotNull();

  /**
   * Returns true if a null value is mapped to the given attribute or no mapping exists.
   * @param attribute the attribute
   * @param <T> the value type
   * @return true if the value mapped to the given attribute is null or none exists
   */
  <T> boolean isNull(Attribute<T> attribute);

  /**
   * Returns true if a non-null value is mapped to the given attribute.
   * @param attribute the attribute
   * @param <T> the value type
   * @return true if a non-null value is mapped to the given attribute
   */
  <T> boolean isNotNull(Attribute<T> attribute);

  /**
   * Returns this keys attribute. Note that this method throws an exception if this key is a composite key.
   * @param <T> the attribute type
   * @return the key attribute, useful for single attribute keys
   * @throws IllegalStateException in case this is a composite key
   */
  <T> Attribute<T> getAttribute();

  /**
   * Returns a new key instance based on this key, but with the given value.
   * Note that this method throws an exception if this key is a composite key.
   * @param value the value to associate with the attribute
   * @param <T> the value type
   * @return a Key based on this instance, but with the given value
   * @throws IllegalStateException in case this is a composite key
   */
  <T> Key withValue(T value);

  /**
   * Returns the value of this key. Note that this method throws an exception if this key is a composite key.
   * @param <T> the value type
   * @return the first value contained in this key, useful for single attribute keys
   * @throws IllegalStateException in case this is a composite key
   */
  <T> T get();

  /**
   * Returns the value of this key, wrapped in an {@link Optional}. Note that this method throws an exception if this key is a composite key.
   * @param <T> the value type
   * @return the first value contained in this key, wrapped in an {@link Optional}, useful for single attribute keys
   * @throws IllegalStateException in case this is a composite key
   */
  <T> Optional<T> getOptional();

  /**
   * Returns a new key instance based on this key, but with the given value.
   * @param attribute the attribute
   * @param value the value to associate with the attribute
   * @param <T> the value type
   * @return a Key based on this instance, but with the given value
   */
  <T> Key withValue(Attribute<T> attribute, T value);

  /**
   * @param attribute the attribute
   * @param <T> the value type
   * @return the value associated with the given attribute
   */
  <T> T get(Attribute<T> attribute);

  /**
   * @param attribute the attribute
   * @param <T> the value type
   * @return the value associated with the given attribute, wrapped in an {@link Optional}
   */
  <T> Optional<T> getOptional(Attribute<T> attribute);
}
