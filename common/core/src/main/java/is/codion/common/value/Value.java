/*
 * Copyright (c) 2012 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.value;

import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventObserver;

import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * A wrapper class for setting and getting a value.
 * A factory class for {@link Value} instances.
 * @param <T> the type of the wrapped value
 * @see #value()
 */
public interface Value<T> extends ValueObserver<T>, EventDataListener<T> {

  /**
   * Sets the value
   * @param value the value
   * @throws IllegalArgumentException in case the given value is invalid
   * @see #addValidator(Validator)
   */
  void set(T value);

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
   * @param originalValueObserver the original value to link this value to
   * @throws IllegalArgumentException in case the original value is not valid according to this values validators
   */
  void link(ValueObserver<T> originalValueObserver);

  /**
   * Unlinks this value from the given original value observer
   * @param originalValueObserver the original value observer to unlink
   */
  void unlink(ValueObserver<T> originalValueObserver);

  /**
   * @return an unmodifiable set containing the values that have been linked to this value
   */
  Set<Value<T>> linkedValues();

  /**
   * Adds a validator to this {@link Value}.
   * Adding the same validator again has no effect.
   * @param validator the validator
   * @throws IllegalArgumentException in case the current value is invalid according to the validator
   */
  void addValidator(Validator<T> validator);

  /**
   * Removes the given validator from this value
   * @param validator the validator
   */
  void removeValidator(Validator<T> validator);

  /**
   * @return the validators
   */
  Collection<Validator<T>> validators();

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
   * Creates a new {@link Value} instance, wrapping a null initial value
   * @param <T> the value type
   * @return a Value for the given type
   */
  static <T> Value<T> value() {
    return value(null);
  }

  /**
   * Creates a new {@link Value} instance
   * @param initialValue the initial value
   * @param <T> the value type
   * @return a {@link Value} with given initial value
   */
  static <T> Value<T> value(T initialValue) {
    return new DefaultValue<>(initialValue, null);
  }

  /**
   * Creates a new {@link Value} instance
   * @param initialValue the initial value
   * @param nullValue the actual value to use when the value is set to null
   * @param <T> the value type
   * @return a {@link Value} with given initial value
   * @throws NullPointerException in case {@code nullValue} is null
   */
  static <T> Value<T> value(T initialValue, T nullValue) {
    return new DefaultValue<>(initialValue, requireNonNull(nullValue));
  }

  /**
   * Creates a new {@link Value} instance based on the given getter and setter.
   * The setter may throw an unchecked exception in case of an invalid value.
   * @param <T> the value type
   * @param getter the getter
   * @param setter the setter
   * @return a new {@link Value} instance based on the given setter and getter
   * @throws NullPointerException in case either getter or setter is null
   */
  static <T> Value<T> value(Supplier<T> getter, Consumer<T> setter) {
    return value(getter, setter, null);
  }

  /**
   * Creates a {@link Value} based on the given getter and setter.
   * The setter may throw an unchecked exception in case of an invalid value.
   * @param <T> the value type
   * @param getter the getter
   * @param setter the setter
   * @param nullValue the value to use instead of null
   * @return a new {@link Value} instance based on the given setter and getter
   * @throws NullPointerException in case either getter or setter is null
   */
  static <T> Value<T> value(Supplier<T> getter, Consumer<T> setter, T nullValue) {
    return new GetterSetterValue<>(getter, setter, nullValue);
  }

  /**
   * Creates a new {@link Value} based on a class property
   * @param owner the property owner
   * @param propertyName the name of the property
   * @param valueClass the value class
   * @param valueChangeObserver an observer notified each time the value changes
   * @param <T> the value type
   * @return a {@link Value} for the given property
   */
  static <T> Value<T> propertyValue(Object owner, String propertyName, Class<T> valueClass,
                                    EventObserver<T> valueChangeObserver) {
    return new DefaultPropertyValue<>(owner, propertyName, valueClass, valueChangeObserver);
  }
}
