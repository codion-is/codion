/*
 * Copyright (c) 2004 - 2013, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.lang.reflect.Method;

/**
 * A factory class for Value objects
 */
public final class Values {

  private Values() {}

  /**
   * Instantiates a new Value
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
    return new DefaultValue<V>(initialValue);
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
  public static <V> Value<V> beanValue(final Object owner, final String beanPropertyName, final Class valueClass,
                                       final EventObserver valueChangeEvent) {
    return new BeanValue<V>(owner, beanPropertyName, valueClass, valueChangeEvent);
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
   * @param modelValue the model value
   * @param uiValue the ui value
   * @param <V> the value type
   */
  public static <V> void link(final Value<V> modelValue, final Value<V> uiValue) {
    link(modelValue, uiValue, false);
  }

  /**
   * Links the two values together
   * @param modelValue the model value
   * @param uiValue the ui value
   * @param readOnly if true the model value is not updated if the ui value changes
   * @param <V> the value type
   */
  public static <V> void link(final Value<V> modelValue, final Value<V> uiValue, final boolean readOnly) {
    new ValueLink<V>(modelValue, uiValue, readOnly);
  }

  private static final class DefaultValue<V> implements Value<V> {

    private final Event changeEvent = Events.event();
    private V value;

    private DefaultValue(final V initialValue) {
      this.value = initialValue;
    }

    /** {@inheritDoc} */
    @Override
    public void set(final V value) {
      if (!Util.equal(this.value, value)) {
        this.value = value;
        changeEvent.fire();
      }
    }

    /** {@inheritDoc} */
    @Override
    public V get() {
      return value;
    }

    /** {@inheritDoc} */
    @Override
    public EventObserver getChangeObserver() {
      return changeEvent.getObserver();
    }
  }

  private static final class BeanValue<V> implements Value<V> {

    private final Object valueOwner;
    private final Method getMethod;
    private Method setMethod;
    private final EventObserver changeEvent;

    private BeanValue(final Object valueOwner, final String propertyName, final Class<?> valueClass, final EventObserver changeEvent) {
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
      catch (NoSuchMethodException e) {
        throw new IllegalArgumentException("Bean property get method for " + propertyName + ", type: " + valueClass + " not found in class " + valueOwner.getClass().getName(), e);
      }
      try {
        this.setMethod = Util.getSetMethod(valueClass, propertyName, valueOwner);
      }
      catch (NoSuchMethodException ignored) {
        this.setMethod = null;
      }
    }

    /** {@inheritDoc} */
    @Override
    public V get() {
      try {
        return (V) getMethod.invoke(valueOwner);
      }
      catch (RuntimeException re) {
        throw re;
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    /** {@inheritDoc} */
    @Override
    public void set(final V value) {
      if (setMethod == null) {
        throw new IllegalStateException("Trying to set a readOnly value");
      }
      try {
        setMethod.invoke(valueOwner, value);
      }
      catch (RuntimeException re) {
        throw re;
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    /** {@inheritDoc} */
    @Override
    public EventObserver getChangeObserver() {
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

    /** {@inheritDoc} */
    @Override
    public void set(final Boolean value) {
      state.setActive(value);
    }

    /** {@inheritDoc} */
    @Override
    public Boolean get() {
      return state.isActive();
    }

    /** {@inheritDoc} */
    @Override
    public EventObserver getChangeObserver() {
      return changeEvent.getObserver();
    }
  }

  /**
   * A class for linking a UI component to a model value.
   * @param <V> the type of the value
   */
  private static final class ValueLink<V> {

    /**
     * The Object wrapping the model value
     */
    private final Value<V> modelValue;

    /**
     * The Object wrapping the ui value
     */
    private final Value<V> uiValue;

    /**
     * True while the UI value is being updated
     */
    private boolean isUpdatingUI = false;

    /**
     * True while the model value is being updated
     */
    private boolean isUpdatingModel = false;

    /**
     * Instantiates a new ValueLink
     * @param modelValue the value wrapper for the linked value
     * @param readOnly if true then this link will be uni-directional
     */
    private ValueLink(final Value<V> modelValue, final Value<V> uiValue, final boolean readOnly) {
      this.modelValue = Util.rejectNullValue(modelValue, "modelValue");
      this.uiValue = Util.rejectNullValue(uiValue, "uiValue");
      updateUI();
      bindEvents(modelValue, uiValue, readOnly);
    }

    /**
     * Updates the model according to the UI.
     */
    private void updateModel() {
      if (!isUpdatingUI) {
        try {
          isUpdatingModel = true;
          modelValue.set(uiValue.get());
        }
        finally {
          isUpdatingModel = false;
        }
      }
    }

    /**
     * Updates the UI according to the model.
     */
    private void updateUI() {
      if (!isUpdatingModel) {
        try {
          isUpdatingUI = true;
          uiValue.set(modelValue.get());
        }
        finally {
          isUpdatingUI = false;
        }
      }
    }

    private void bindEvents(final Value<V> modelValue, final Value<V> uiValue, final boolean readOnly) {
      if (modelValue.getChangeObserver() != null) {
        modelValue.getChangeObserver().addListener(new EventListener() {
          /** {@inheritDoc} */
          @Override
          public void eventOccurred() {
            updateUI();
          }
        });
      }
      if (!readOnly && uiValue.getChangeObserver() != null) {
        uiValue.getChangeObserver().addListener(new EventListener() {
          /** {@inheritDoc} */
          @Override
          public void eventOccurred() {
            updateModel();
          }
        });
      }
    }
  }
}
