/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import java.io.Serializable;
import java.util.List;

/**
 * Represents a primary key.
 */
public interface Key extends Serializable {

  /**
   * @return the entity type
   */
  EntityType getEntityType();

  /**
   * @return a List containing the attributes comprising this key
   */
  List<Attribute<?>> getAttributes();

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
   * @return true if this primary key is based on a single integer column
   */
  boolean isSingleIntegerKey();

  /**
   * @return true if this key is comprised of multiple attributes.
   */
  boolean isCompositeKey();

  /**
   * Returns this keys attribute. Note that this method throws an exception if this key is a composite key.
   * @param <T> the attribute type
   * @return the key attribute, useful for single attribute keys
   * @throws IllegalStateException in case this is a composite key
   */
  <T> Attribute<T> getAttribute();

  /**
   * Sets the value of this key. Note that this method throws an exception if this key is a composite key.
   * @param value the value to associate with the attribute
   * @param <T> the value type
   * @return the previous value
   * @throws IllegalStateException in case this is a composite key
   */
  <T> T put(T value);

  /**
   * Returns the value of this key. Note that this method throws an exception if this key is a composite key.
   * @param <T> the value type
   * @return the first value contained in this key, useful for single attribute keys
   * @throws IllegalStateException in case this is a composite key
   */
  <T> T get();

  /**
   * @param attribute the attribute
   * @param value the value to associate with the attribute
   * @param <T> the value type
   * @return the previous value
   */
  <T> T put(Attribute<T> attribute, T value);

  /**
   * @param attribute the attribute
   * @param <T> the value type
   * @return the value associated with the given attribute
   */
  <T> T get(Attribute<T> attribute);
}
