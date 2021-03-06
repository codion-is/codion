/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.value;

import is.codion.common.event.EventObserver;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * A wrapper class for setting and getting a value.
 * A factory class for {@link Value} instances.
 * @param <V> the type of the value
 */
public interface Value<V> extends ValueObserver<V> {

  /**
   * Sets the value, setting the same value again does not trigger a value change
   * @param value the value
   */
  void set(V value);

  /**
   * Returns a ValueObserver notified each time this value changes.
   * @return a ValueObserver for this value
   */
  ValueObserver<V> getObserver();

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
   * Note that after a call to this method the value of this value is the same as the original value.
   * @param originalValueObserver the original value to link this value to
   */
  void link(ValueObserver<V> originalValueObserver);

  /**
   * Adds a validator to this {@link Value}.
   * Adding the same validator again has no effect.
   * @param validator the validator
   * @throws IllegalArgumentException in case the current value is invalid according to the validator
   */
  void addValidator(Validator<V> validator);

  /**
   * @return the validators
   */
  Collection<Validator<V>> getValidators();

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

  /**
   * Instantiates a new Value instance wrapping a null initial value
   * @param <V> type to wrap
   * @return a Value for the given type
   */
  static <V> Value<V> value() {
    return value(null);
  }

  /**
   * Instantiates a new Value
   * @param initialValue the initial value
   * @param <V> type to wrap
   * @return a Value for the given type with the given initial value
   */
  static <V> Value<V> value(final V initialValue) {
    return value(initialValue, null);
  }

  /**
   * Instantiates a new Value
   * @param initialValue the initial value
   * @param nullValue the actual value to use when the value is set to null
   * @param <V> type to wrap
   * @return a Value for the given type with the given initial value
   */
  static <V> Value<V> value(final V initialValue, final V nullValue) {
    return new DefaultValue<>(initialValue, nullValue);
  }

  /**
   * Instantiates a new empty ValueSet
   * @param <V> the value type
   * @return a ValueSet
   */
  static <V> ValueSet<V> valueSet() {
    return valueSet(Collections.emptySet());
  }

  /**
   * Instantiates a new ValueSet
   * @param initialValues the initial values, may not be null
   * @param <V> the value type
   * @return a ValueSet
   */
  static <V> ValueSet<V> valueSet(final Set<V> initialValues) {
    return new DefaultValueSet<>(initialValues);
  }

  /**
   * Instantiates a new PropertyValue based on a class property
   * @param owner the property owner
   * @param propertyName the name of the property
   * @param valueClass the value class
   * @param valueChangeObserver an observer notified each time the value changes
   * @param <V> type to wrap
   * @return a Value for the given property
   */
  static <V> PropertyValue<V> propertyValue(final Object owner, final String propertyName, final Class<V> valueClass,
                                            final EventObserver<V> valueChangeObserver) {
    return new DefaultPropertyValue<>(owner, propertyName, valueClass, valueChangeObserver);
  }
}
