/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.value;

import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventObserver;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A wrapper class for setting and getting a value.
 * A factory class for {@link Value} instances.
 * @param <T> the type of the value
 */
public interface Value<T> extends ValueObserver<T>, EventDataListener<T> {

  /**
   * Sets the value, setting the same value again does not trigger a value change
   * @param value the value
   */
  void set(T value);

  /**
   * Returns a ValueObserver notified each time this value changes.
   * @return a ValueObserver for this value
   */
  ValueObserver<T> getObserver();

  /**
   * Creates a bidirectional link between this and the given original value,
   * so that changes in one are reflected in the other.
   * Note that after a call to this method this value is the same as {@code originalValue}.
   * @param originalValue the original value to link this value to
   * @throws IllegalArgumentException in case the values are already linked
   */
  void link(Value<T> originalValue);

  /**
   * Unlinks this value from the given original value
   * @param originalValue the original value to unlink from this one
   * @throws IllegalArgumentException in case the values are not linked
   */
  void unlink(Value<T> originalValue);

  /**
   * Creates a unidirectional link between this value and the given original value observer,
   * so that changes in the original value are reflected in this one.
   * Note that after a call to this method the value of this value is the same as the original value.
   * @param originalValueObserver the original value to link this value to
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
  Set<Value<T>> getLinkedValues();

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
  Collection<Validator<T>> getValidators();

  /**
   * A Validator for {@link Value}s.
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
   * Instantiates a new Value instance wrapping a null initial value
   * @param <T> type to wrap
   * @return a Value for the given type
   */
  static <T> Value<T> value() {
    return value(null);
  }

  /**
   * Instantiates a new Value
   * @param initialValue the initial value
   * @param <T> type to wrap
   * @return a Value for the given type with the given initial value
   */
  static <T> Value<T> value(T initialValue) {
    return value(initialValue, null);
  }

  /**
   * Instantiates a new Value
   * @param initialValue the initial value
   * @param nullValue the actual value to use when the value is set to null
   * @param <T> type to wrap
   * @return a Value for the given type with the given initial value
   */
  static <T> Value<T> value(T initialValue, T nullValue) {
    return new DefaultValue<>(initialValue, nullValue);
  }

  /**
   * Creates a {@link Value} based on the given getter and setter.
   * The setter may throw an unchecked exception in case of an invalid value.
   * @param <T> the value type
   * @param getter the getter
   * @param setter the setter
   * @return a Value based on the given setter and getter
   * @throws NullPointerException in case either getter or setter is null
   */
  static <T> Value<T> value(Supplier<T> getter, Consumer<T> setter) {
    return new GetterSetterValue<>(getter, setter);
  }

  /**
   * Instantiates a new empty ValueSet
   * @param <T> the value type
   * @return a ValueSet
   */
  static <T> ValueSet<T> valueSet() {
    return valueSet(Collections.emptySet());
  }

  /**
   * Instantiates a new ValueSet
   * @param initialValues the initial values, may not be null
   * @param <T> the value type
   * @return a ValueSet
   */
  static <T> ValueSet<T> valueSet(Set<T> initialValues) {
    return new DefaultValueSet<>(initialValues);
  }

  /**
   * Instantiates a new PropertyValue based on a class property
   * @param owner the property owner
   * @param propertyName the name of the property
   * @param valueClass the value class
   * @param valueChangeObserver an observer notified each time the value changes
   * @param <T> type to wrap
   * @return a Value for the given property
   */
  static <T> PropertyValue<T> propertyValue(Object owner, String propertyName, Class<T> valueClass,
                                            EventObserver<T> valueChangeObserver) {
    return new DefaultPropertyValue<>(owner, propertyName, valueClass, valueChangeObserver);
  }
}
