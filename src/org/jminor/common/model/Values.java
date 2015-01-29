/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.lang.reflect.Method;

/**
 * A factory class for Value objects
 */
public final class Values {

  private Values() {}

  /**
   * Instantiates a new Value instance wrapping a null value
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
    return new DefaultValue<>(initialValue);
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
   * Instantiates a boolean Value based on a {@link State}
   * @param state the state to base the value on
   * @return a boolean state based on the given value
   */
  public static Value<Boolean> stateValue(final State state) {
    return new StateValue(state);
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
   * @param readOnly if true the model value is not updated if the ui value changes
   * @param <V> the value type
   */
  public static <V> void link(final Value<V> originalValue, final Value<V> linkedValue, final boolean readOnly) {
    new ValueLink<>(originalValue, linkedValue, readOnly);
  }

  private static final class DefaultValue<V> implements Value<V> {

    private final Event<V> changeEvent = Events.event();
    private V value;

    private DefaultValue(final V initialValue) {
      this.value = initialValue;
    }

    @Override
    public void set(final V value) {
      if (!Util.equal(this.value, value)) {
        this.value = value;
        changeEvent.fire();
      }
    }

    @Override
    public V get() {
      return value;
    }

    @Override
    public EventObserver<V> getObserver() {
      return changeEvent.getObserver();
    }
  }

  private static final class BeanValue<V> implements Value<V> {

    private final EventObserver<V> changeEvent;
    private final Object valueOwner;
    private final Method getMethod;
    private Method setMethod;

    private BeanValue(final Object valueOwner, final String propertyName, final Class<?> valueClass, final EventObserver<V> changeEvent) {
      Util.rejectNullValue(valueOwner, "valueOwner");
      Util.rejectNullValue(valueClass, "valueClass");
      if (Util.nullOrEmpty(propertyName)) {
        throw new IllegalArgumentException("propertyName is null or an empty string");
      }
      try {
        this.valueOwner = valueOwner;
        this.changeEvent = changeEvent;
        this.getMethod = Util.getGetMethod(valueClass, propertyName, valueOwner);
      }
      catch (final NoSuchMethodException e) {
        throw new IllegalArgumentException("Bean property get method for " + propertyName + ", type: " + valueClass + " not found in class " + valueOwner.getClass().getName(), e);
      }
      try {
        this.setMethod = Util.getSetMethod(valueClass, propertyName, valueOwner);
      }
      catch (final NoSuchMethodException ignored) {
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
        throw new IllegalStateException("Trying to set a readOnly value");
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
    public EventObserver<V> getObserver() {
      return changeEvent;
    }
  }

  /**
   * A boolean value based on a State
   */
  private static final class StateValue implements Value<Boolean> {
    private final State state;
    private final Event<Boolean> changeEvent = Events.event();

    private StateValue(final State state) {
      this.state = state;
      state.addListener(new EventListener() {
        @Override
        public void eventOccurred() {
          changeEvent.fire(state.isActive());
        }
      });
    }

    @Override
    public void set(final Boolean value) {
      state.setActive(value);
    }

    @Override
    public Boolean get() {
      return state.isActive();
    }

    @Override
    public EventObserver<Boolean> getObserver() {
      return changeEvent.getObserver();
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
     * True while the UI value is being updated
     */
    private boolean isUpdatingLinked = false;

    /**
     * True while the model value is being updated
     */
    private boolean isUpdatingOriginal = false;

    /**
     * Instantiates a new ValueLink
     * @param originalValue the value wrapper for the linked value
     * @param readOnly if true then this link will be uni-directional
     */
    private ValueLink(final Value<V> originalValue, final Value<V> linkedValue, final boolean readOnly) {
      this.originalValue = Util.rejectNullValue(originalValue, "originalValue");
      this.linkedValue = Util.rejectNullValue(linkedValue, "linkedValue");
      updateLinked();
      bindEvents(originalValue, linkedValue, readOnly);
    }

    /**
     * Updates the original value according to the linked one.
     */
    private void updateOriginal() {
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

    /**
     * Updates the linked value according to the original
     */
    private void updateLinked() {
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

    private void bindEvents(final Value<V> originalValue, final Value<V> linkedValue, final boolean readOnly) {
      if (originalValue.getObserver() != null) {
        originalValue.getObserver().addListener(new EventListener() {
          @Override
          public void eventOccurred() {
            updateLinked();
          }
        });
      }
      if (!readOnly && linkedValue.getObserver() != null) {
        linkedValue.getObserver().addListener(new EventListener() {
          @Override
          public void eventOccurred() {
            updateOriginal();
          }
        });
      }
    }
  }
}
