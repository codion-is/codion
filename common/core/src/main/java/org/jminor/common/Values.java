/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * A factory class for {@link Value} objects
 */
public final class Values {

  private Values() {}

  /**
   * Instantiates a new Value instance wrapping a null initial value
   * @param <V> type to wrap
   * @return a Value for the given type
   */
  public static <V> Value<V> value() {
    return value(null);
  }

  /**
   * Instantiates a new Value
   * @param initialValue the initial value
   * @param <V> type to wrap
   * @return a Value for the given type with the given initial value
   */
  public static <V> Value<V> value(final V initialValue) {
    return value(initialValue, null);
  }

  /**
   * Instantiates a new Value
   * @param initialValue the initial value
   * @param nullValue the actual value to use when the value is set to null
   * @param <V> type to wrap
   * @return a Value for the given type with the given initial value
   */
  public static <V> Value<V> value(final V initialValue, final V nullValue) {
    return new DefaultValue<>(nullValue, initialValue);
  }

  /**
   * Instantiates a new Value based on a bean property
   * @param owner the property owner
   * @param beanPropertyName the name of the bean property
   * @param valueClass the class of the bean value
   * @param valueChangeEvent an event which fires each time the bean value changes
   * @param <V> type to wrap
   * @return a Value for the given bean property
   */
  public static <V> Value<V> beanValue(final Object owner, final String beanPropertyName, final Class<V> valueClass,
                                       final EventObserver<V> valueChangeEvent) {
    return new BeanValue<>(owner, beanPropertyName, valueClass, valueChangeEvent);
  }

  /**
   * Instantiates a boolean Value based on a {@link State}.
   * Null values are translated to 'false'.
   * @param state the state to base the value on
   * @return a boolean state based on the given value
   */
  public static Value<Boolean> stateValue(final State state) {
    return new StateValue(state);
  }

  /**
   * Instantiates a State linked to the given boolean value.
   * @param booleanValue the boolean value to link to the state
   * @return a State linked to the given value
   * @throws IllegalArgumentException in case the boolean value is nullable
   * @see Value#isNullable()
   */
  public static State valueState(final Value<Boolean> booleanValue) {
    if (booleanValue.isNullable()) {
      throw new IllegalArgumentException("States can not be created for a nullable value");
    }
    final State state = States.state();
    Values.link(booleanValue, Values.stateValue(state));

    return state;
  }

  /**
   * Instantiates a new ValueObserver for the given value.
   * @param value the value to observe
   * @param <V> the value type
   * @return a ValueObserver for the given value
   */
  public static <V> ValueObserver<V> valueObserver(final Value<V> value) {
    return new DefaultValueObserver<>(value);
  }

  /**
   * Links the two values together so that changes in one are reflected in the other
   * @param originalValue the original value
   * @param linkedValue the linked value
   * @param <V> the value type
   */
  public static <V> void link(final Value<V> originalValue, final Value<V> linkedValue) {
    link(originalValue, linkedValue, false);
  }

  /**
   * Links the two values together so that changes in one are reflected in the other
   * @param originalValue the original value
   * @param linkedValue the linked value
   * @param readOnly if true the original value is not updated if the linked value changes
   * @param <V> the value type
   */
  public static <V> void link(final Value<V> originalValue, final Value<V> linkedValue, final boolean readOnly) {
    new ValueLink<>(originalValue, linkedValue, readOnly);
  }

  private static final class DefaultValue<V> implements Value<V> {

    private final Event<V> changeEvent = Events.event();
    private final V nullValue;
    private V value;

    private DefaultValue(final V nullValue, final V initialValue) {
      this.nullValue = nullValue;
      this.value = initialValue == null ? nullValue : initialValue;
    }

    @Override
    public void set(final V value) {
      final V actualValue = value == null ? nullValue : value;
      if (!Objects.equals(this.value, actualValue)) {
        this.value = actualValue;
        changeEvent.fire(this.value);
      }
    }

    @Override
    public V get() {
      return value;
    }

    @Override
    public boolean isNullable() {
      return nullValue == null;
    }

    @Override
    public EventObserver<V> getChangeObserver() {
      return changeEvent.getObserver();
    }

    @Override
    public ValueObserver<V> getValueObserver() {
      return valueObserver(this);
    }
  }

  private static final class BeanValue<V> implements Value<V> {

    private final EventObserver<V> changeEvent;
    private final Object valueOwner;
    private final Method getMethod;
    private Method setMethod;

    private BeanValue(final Object valueOwner, final String propertyName, final Class valueClass, final EventObserver<V> changeEvent) {
      if (Util.nullOrEmpty(propertyName)) {
        throw new IllegalArgumentException("propertyName is null or an empty string");
      }
      try {
        this.valueOwner = Objects.requireNonNull(valueOwner, "valueOwner");
        this.changeEvent = changeEvent;
        this.getMethod = Util.getGetMethod(valueClass, propertyName, valueOwner);
      }
      catch (final NoSuchMethodException e) {
        throw new IllegalArgumentException("Bean property get method for " + propertyName + ", type: " + valueClass +
                " not found in class " + valueOwner.getClass().getName(), e);
      }
      try {
        this.setMethod = Util.getSetMethod(valueClass, propertyName, valueOwner);
      }
      catch (final NoSuchMethodException ignored) {/*ignored*/
        this.setMethod = null;
      }
    }

    @Override
    public V get() {
      try {
        return (V) getMethod.invoke(valueOwner);
      }
      catch (final RuntimeException re) {
        throw re;
      }
      catch (final Exception e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void set(final V value) {
      if (setMethod == null) {
        throw new IllegalStateException("Bean property set method not found: " + getMethod.getName());
      }
      try {
        setMethod.invoke(valueOwner, value);
      }
      catch (final RuntimeException re) {
        throw re;
      }
      catch (final Exception e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public boolean isNullable() {
      return true;
    }

    @Override
    public EventObserver<V> getChangeObserver() {
      return changeEvent;
    }

    @Override
    public ValueObserver<V> getValueObserver() {
      return valueObserver(this);
    }
  }

  /**
   * A boolean value based on a State, null values are translated to 'false'
   */
  private static final class StateValue implements Value<Boolean> {

    private final State state;

    private StateValue(final State state) {
      this.state = Objects.requireNonNull(state);
    }

    @Override
    public void set(final Boolean value) {
      state.setActive(value != null && value);
    }

    @Override
    public Boolean get() {
      return state.isActive();
    }

    @Override
    public boolean isNullable() {
      return false;
    }

    @Override
    public EventObserver<Boolean> getChangeObserver() {
      return state.getObserver();
    }

    @Override
    public ValueObserver<Boolean> getValueObserver() {
      return valueObserver(this);
    }
  }

  /**
   * A class for linking two values.
   * @param <V> the type of the value
   */
  private static final class ValueLink<V> {

    /**
     * The Object wrapping the original value
     */
    private final Value<V> originalValue;

    /**
     * The Object wrapping the linked value
     */
    private final Value<V> linkedValue;

    /**
     * True while the linked value is being updated
     */
    private boolean isUpdatingLinked = false;

    /**
     * True while the original value is being updated
     */
    private boolean isUpdatingOriginal = false;

    /**
     * Instantiates a new ValueLink
     * @param originalValue the value wrapper for the linked value
     * @param linkedValue the value to link to the original value
     * @param readOnly if true then this link will be uni-directional
     */
    private ValueLink(final Value<V> originalValue, final Value<V> linkedValue, final boolean readOnly) {
      this.originalValue = Objects.requireNonNull(originalValue, "originalValue");
      this.linkedValue = Objects.requireNonNull(linkedValue, "linkedValue");
      this.linkedValue.set(this.originalValue.get());
      bindEvents(originalValue, linkedValue, readOnly);
    }

    private void bindEvents(final Value<V> originalValue, final Value<V> linkedValue, final boolean readOnly) {
      if (originalValue.getChangeObserver() != null) {
        originalValue.getChangeObserver().addListener(() -> updateLinkedValue(originalValue, linkedValue));
      }
      if (!readOnly && linkedValue.getChangeObserver() != null) {
        linkedValue.getChangeObserver().addListener(() -> updateOriginalValue(originalValue, linkedValue));
      }
    }

    private void updateOriginalValue(final Value<V> originalValue, final Value<V> linkedValue) {
      if (!isUpdatingLinked) {
        try {
          isUpdatingOriginal = true;
          originalValue.set(linkedValue.get());
        }
        finally {
          isUpdatingOriginal = false;
        }
      }
    }

    private void updateLinkedValue(final Value<V> originalValue, final Value<V> linkedValue) {
      if (!isUpdatingOriginal) {
        try {
          isUpdatingLinked = true;
          linkedValue.set(originalValue.get());
        }
        finally {
          isUpdatingLinked = false;
        }
      }
    }
  }

  private static final class DefaultValueObserver<V> implements ValueObserver<V> {

    private final Value<V> value;

    private DefaultValueObserver(final Value<V> value) {
      this.value = Objects.requireNonNull(value, "value");
    }

    @Override
    public V get() {
      return value.get();
    }

    @Override
    public boolean isNullable() {
      return value.isNullable();
    }

    @Override
    public EventObserver<V> getChangeObserver() {
      return value.getChangeObserver();
    }
  }
}
