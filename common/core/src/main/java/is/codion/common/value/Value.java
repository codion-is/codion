/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.value;

/**
 * A wrapper class for setting and getting a value
 * @param <V> the type of the value
 */
public interface Value<V> extends ValueObserver<V> {

  /**
   * Sets the value, setting the same value again does not trigger a value change
   * @param value the value
   */
  void set(V value);

  /**
   * Creates a bidirectional link between this and the given original value,
   * so that changes in one are reflected in the other.
   * Note that after a call to this method this value is the same as {@code originalValue}.
   * @param originalValue the original value to link this value to
   */
  void link(Value<V> originalValue);

  /**
   * Creates a unidirectional link between this value and the given original value observer,
   * so that changes in the original value are reflected in this one.
   * Note that after a call to this method the value of this value the same as the original value.
   * @param originalValueObserver the original value to link this value to
   */
  void link(ValueObserver<V> originalValueObserver);

  /**
   * Sets the validator for this {@link Value}.
   * If null then the default no-op validator is used.
   * @param validator the validator
   * @throws IllegalArgumentException in case the current value is invalid
   */
  void setValidator(Validator<V> validator);

  /**
   * A Validator for {@link Value}s.
   * @param <V> the value type
   */
  interface Validator<V> {

    /**
     * Validates the given value.
     * @param value the value to validate
     * @throws IllegalArgumentException in case of an invalid value
     */
    void validate(final V value) throws IllegalArgumentException;
  }
}
