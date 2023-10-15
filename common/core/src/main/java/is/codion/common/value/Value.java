/*
 * Copyright (c) 2012 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.value;

import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * An observable wrapper for a value.
 * A factory class for {@link Value} instances.
 * @param <T> the type of the wrapped value
 * @see #value()
 */
public interface Value<T> extends ValueObserver<T>, Consumer<T> {

  /**
   * Specifies when a Value instance notifies its listeners.
   */
  enum Notify {
    /**
     * Notify listeners when the underlying value is set via {@link Value#set(Object)},
     * regardless of whether or not the new value is equal to the previous value.
     */
    WHEN_SET,
    /**
     * Notify listeners when the underlying value is changed via {@link Value#set(Object)},
     * that is, only when the new value is not equal to the previous value.
     */
    WHEN_CHANGED
  }

  /**
   * Sets the value
   * @param value the value
   * @throws IllegalArgumentException in case the given value is invalid
   * @see #addValidator(Validator)
   */
  @Override
  void accept(T value);

  /**
   * Sets the value. Note that if the value is equal to the current value according to {@link java.util.Objects#equals}
   * the underlying value is still set, but no change event is triggered.
   * @param value the value
   * @return true if the underlying value changed
   * @throws IllegalArgumentException in case the given value is invalid
   * @see #addValidator(Validator)
   */
  boolean set(T value);

  /**
   * Returns a {@link ValueObserver} notified each time this value changes.
   * @return a {@link ValueObserver} for this value
   */
  ValueObserver<T> observer();

  /**
   * Creates a bidirectional link between this and the given original value,
   * so that changes in one are reflected in the other.
   * Note that after a call to this method this value is the same as {@code originalValue}.
   * @param originalValue the original value to link this value to
   * @throws IllegalStateException in case the values are already linked
   * @throws IllegalArgumentException in case the original value is not valid according to this values validators
   */
  void link(Value<T> originalValue);

  /**
   * Unlinks this value from the given original value
   * @param originalValue the original value to unlink from this one
   * @throws IllegalStateException in case the values are not linked
   */
  void unlink(Value<T> originalValue);

  /**
   * Creates a unidirectional link between this value and the given original value observer,
   * so that changes in the original value are reflected in this one.
   * Note that after a call to this method the value of this value is the same as the original value.
   * @param originalValue the original value to link this value to
   * @throws IllegalArgumentException in case the original value is not valid according to this values validators
   */
  void link(ValueObserver<T> originalValue);

  /**
   * Unlinks this value from the given original value observer
   * @param originalValue the original value to unlink
   */
  void unlink(ValueObserver<T> originalValue);

  /**
   * Adds a validator to this {@link Value}.
   * Adding the same validator again has no effect.
   * @param validator the validator
   * @return true if this value did not already contain the specified validator
   * @throws IllegalArgumentException in case the current value is invalid according to the validator
   */
  boolean addValidator(Validator<T> validator);

  /**
   * Removes the given validator from this value
   * @param validator the validator
   * @return true if this value contained the specified validator
   */
  boolean removeValidator(Validator<T> validator);

  /**
   * Validate the given value using all validators
   * @param value the value to validate
   * @throws IllegalArgumentException in case the given value is invalid according to a validator
   */
  void validate(T value);

  /**
   * A {@link Validator} for {@link Value}s.
   * @param <T> the value type
   */
  interface Validator<T> {

    /**
     * Validates the given value.
     * @param value the value to validate
     * @throws IllegalArgumentException in case of an invalid value
     */
    void validate(T value) throws IllegalArgumentException;
  }

  /**
   * Creates a new {@link Value} instance, wrapping a null initial value, using {@link Notify#WHEN_CHANGED}.
   * @param <T> the value type
   * @return a Value for the given type
   */
  static <T> Value<T> value() {
    return value(Notify.WHEN_CHANGED);
  }

  /**
   * Creates a new {@link Value} instance, wrapping a null initial value
   * @param <T> the value type
   * @param notify specifies when this value notifies its listeners
   * @return a Value for the given type
   */
  static <T> Value<T> value(Notify notify) {
    return value(null, notify);
  }

  /**
   * Creates a new {@link Value} instance, using {@link Notify#WHEN_CHANGED}.
   * @param initialValue the initial value
   * @param <T> the value type
   * @return a {@link Value} with given initial value
   */
  static <T> Value<T> value(T initialValue) {
    return new DefaultValue<>(initialValue, null, Notify.WHEN_CHANGED);
  }

  /**
   * Creates a new {@link Value} instance
   * @param initialValue the initial value
   * @param notify specifies when this value notifies its listeners
   * @param <T> the value type
   * @return a {@link Value} with given initial value
   */
  static <T> Value<T> value(T initialValue, Notify notify) {
    return new DefaultValue<>(initialValue, null, notify);
  }

  /**
   * Creates a new {@link Value} instance, using {@link Notify#WHEN_CHANGED}.
   * @param initialValue the initial value
   * @param nullValue the actual value to use when the value is set to null
   * @param <T> the value type
   * @return a {@link Value} with given initial value
   * @throws NullPointerException in case {@code nullValue} is null
   */
  static <T> Value<T> value(T initialValue, T nullValue) {
    return value(initialValue, requireNonNull(nullValue), Notify.WHEN_CHANGED);
  }

  /**
   * Creates a new {@link Value} instance
   * @param initialValue the initial value
   * @param nullValue the actual value to use when the value is set to null
   * @param notify specifies when this value notifies its listeners
   * @param <T> the value type
   * @return a {@link Value} with given initial value
   * @throws NullPointerException in case {@code nullValue} is null
   */
  static <T> Value<T> value(T initialValue, T nullValue, Notify notify) {
    return new DefaultValue<>(initialValue, requireNonNull(nullValue), notify);
  }
}
