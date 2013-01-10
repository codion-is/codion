/*
 * Copyright (c) 2004 - 2013, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.lang.reflect.Method;

public final class Values {

  private Values() {}

  public static <V> Value<V> value() {
    return value(null);
  }

  public static <V> Value<V> value(final V initialValue) {
    return new ValueImpl<V>(initialValue);
  }

  public static <V> Value<V> beanValue(final Object owner, final String beanPropertyName, final Class valueClass,
                                       final EventObserver valueChangeEvent) {
    return new BeanValue<V>(owner, beanPropertyName, valueClass, valueChangeEvent);
  }

  /**
   * Links the two values together
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

  private static final class ValueImpl<V> implements Value<V> {

    private final Event changeEvent = Events.event();
    private V value;

    private ValueImpl(final V initialValue) {
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
    public EventObserver getChangeEvent() {
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
    public EventObserver getChangeEvent() {
      return changeEvent;
    }
  }

  /**
   * An abstract base class for linking a UI component to a model value.
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
      if (modelValue.getChangeEvent() != null) {
        modelValue.getChangeEvent().addListener(new EventAdapter() {
          /** {@inheritDoc} */
          @Override
          public void eventOccurred() {
            updateUI();
          }
        });
      }
      if (!readOnly && uiValue.getChangeEvent() != null) {
        uiValue.getChangeEvent().addListener(new EventAdapter() {
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
