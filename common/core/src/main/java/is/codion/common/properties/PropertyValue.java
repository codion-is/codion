/*
 * Copyright (c) 2019 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.properties;

import is.codion.common.value.Value;

/**
 * A Value associated with a named property.
 * @param <T> the value type
 */
public interface PropertyValue<T> extends Value<T> {

  /**
   * @return the name of the property this value represents
   */
  String getPropertyName();

  /**
   * Returns the underlying value, if the value is null then a {@link IllegalStateException} is thrown.
   * @return the value, if available
   * @throws IllegalStateException in case the underlying value is null
   */
  T getOrThrow() throws IllegalStateException;

  /**
   * Returns the underlying value, if the value is null then a {@link IllegalStateException} is thrown.
   * @param message the error message to use when throwing
   * @return the value, if available
   * @throws IllegalStateException in case the underlying value is null
   */
  T getOrThrow(String message) throws IllegalStateException;

  /**
   * Sets this value to null as well as removing it from the underlying store and clearing the system property.
   */
  void clear();
}
