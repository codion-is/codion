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
   * Sets this value to null as well as removing it from the underlying store and clearing the system property.
   */
  void clear();

  /**
   * A builder for {@link PropertyValue}
   * @param <T> the value type
   */
  interface Builder<T> {

    /**
     * This value is used as the initial value if a value is not present in a configuration file  or as a system property.
     * When specified this value is used when the value is set to null via {@link Value#set(Object)}.
     * @param defaultValue the default value
     * @return this builder instance
     */
    Builder<T> defaultValue(T defaultValue);

    /**
     * Builds a value based on this builder instance
     * @return a new {@link PropertyValue} instance
     * @throws IllegalStateException in case a Value for the given property name has already been built
     */
    PropertyValue<T> build();
  }
}
