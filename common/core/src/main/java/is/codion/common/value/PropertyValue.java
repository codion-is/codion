/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.value;

/**
 * A Value associated with a named property.
 * @param <T> the value type
 */
public interface PropertyValue<T> extends Value<T> {

  /**
   * @return the name of the property this value is associated with
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
}
