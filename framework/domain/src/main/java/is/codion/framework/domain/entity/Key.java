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
   * @param <T> the value type
   * @return true if the value mapped to the given property is null or none exists
   */
  <T> boolean isNull(Attribute<T> attribute);

  /**
   * Returns true if a non-null value is mapped to the given property.
   * @param attribute the attribute
   * @param <T> the value type
   * @return true if a non-null value is mapped to the given property
   */
  <T> boolean isNotNull(Attribute<T> attribute);

  /**
   * @return true if this primary key is based on a single integer column
   */
  boolean isSingleIntegerKey();

  /**
   * @return true if this key is comprised of multiple properties.
   */
  boolean isCompositeKey();

  /**
   * @param <T> the attribute type
   * @return the first key property, useful for single property keys
   */
  <T> Attribute<T> getFirstAttribute();

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
}
