/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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
   * A builder for {@link PropertyValue}
   * @param <T> the value type
   */
  interface Builder<T> {

    /**
     * @param defaultValue the default value to use if no initial value is present
     * @return this builder instance
     */
    Builder<T> defaultValue(T defaultValue);

    /**
     * @param nullValue the value to use instead of null
     * @return this builder instance
     */
    Builder<T> nullValue(T nullValue);

    /**
     * Builds a value based on this builder instance
     * @return a new {@link PropertyValue} instance
     * @throws IllegalStateException in case a Value for the given property name has already been built
     */
    PropertyValue<T> build();
  }
}
