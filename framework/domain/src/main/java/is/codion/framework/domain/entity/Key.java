/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Represents a unique attribute value combination for a given entity.
 */
public interface Key {

  /**
   * @return the entity type
   */
  EntityType type();

  /**
   * @return the entity definition
   */
  EntityDefinition definition();

  /**
   * @return the attributes comprising this key
   */
  List<Attribute<?>> attributes();

  /**
   * @return true if this key represents a primary key for an entity, note that this is true
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
   * @return true if the value mapped to the given attribute is null or none exists
   */
  boolean isNull(Attribute<?> attribute);

  /**
   * Returns true if a non-null value is mapped to the given attribute.
   * @param attribute the attribute
   * @return true if a non-null value is mapped to the given attribute
   */
  boolean isNotNull(Attribute<?> attribute);

  /**
   * Returns this keys attribute. Note that this method throws an exception if this key is a composite key.
   * @param <T> the attribute type
   * @return the key attribute, useful for single attribute keys
   * @throws IllegalStateException in case this is a composite key
   * @throws NoSuchElementException in case this key contains no values
   */
  <T> Attribute<T> attribute();

  /**
   * Returns the value of this key. Note that this method throws an exception if this key is a composite key.
   * @param <T> the value type
   * @return the first value contained in this key, useful for single attribute keys
   * @throws IllegalStateException in case this is a composite key
   * @throws NoSuchElementException in case this key contains no values
   */
  <T> T get();

  /**
   * Returns the value of this key, wrapped in an {@link Optional}. Note that this method throws an exception if this key is a composite key.
   * @param <T> the value type
   * @return the first value contained in this key, wrapped in an {@link Optional}, useful for single attribute keys
   * @throws IllegalStateException in case this is a composite key
   * @throws NoSuchElementException in case this key contains no values
   */
  <T> Optional<T> getOptional();

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

  /**
   * Creates a new {@link Key.Builder} instance, initialized with the values in this key.
   * @return a new builder based on this key
   */
  Builder copyBuilder();

  /**
   * A builder for {@link Key} instances.
   * Note that the resulting key is assumed to be a primary key
   * if any of the values is associated with a primary key attribute.
   */
  interface Builder {

    /**
     * Adds the given attribute value to this builder
     * @param attribute the attribute
     * @param value the value
     * @param <T> the value type
     * @return this builder instance
     */
    <T> Builder with(Attribute<T> attribute, T value);

    /**
     * Builds the key instance
     * @return a new Key instance
     */
    Key build();
  }
}
