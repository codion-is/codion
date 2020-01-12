/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.value;

import org.jminor.common.value.AbstractValue;

import javax.swing.SwingUtilities;

import static java.util.Objects.requireNonNull;

/**
 * A default {@link ComponentValue} implementation.
 * @param <V> the Value type
 * @param <C> the component type
 */
public abstract class AbstractComponentValue<V, C> extends AbstractValue<V> implements ComponentValue<V, C> {

  private final C component;
  private final boolean nullable;

  /**
   * Instantiates a new nullable {@link AbstractComponentValue}
   * @param component the component
   */
  public AbstractComponentValue(final C component) {
    this(component, true);
  }

  /**
   * Instantiates a new {@link AbstractComponentValue}
   * @param component the component
   * @param nullable true if this value can be null
   */
  public AbstractComponentValue(final C component, final boolean nullable) {
    this.component = requireNonNull(component);
    this.nullable = nullable;
  }

  /** {@inheritDoc} */
  @Override
  public final V get() {
    return getComponentValue(component);
  }

  /** {@inheritDoc} */
  @Override
  public final void set(final V value) {
    if (SwingUtilities.isEventDispatchThread()) {
      setComponentValue(component, value);
    }
    else {
      try {
        SwingUtilities.invokeAndWait(() -> setComponentValue(component, value));
      }
      catch(final InterruptedException ex){
        Thread.currentThread().interrupt();
        throw new RuntimeException(ex);
      }
      catch (final Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public final C getComponent() {
    return component;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isNullable() {
    return nullable;
  }

  /**
   * Returns the value according to the component
   * @param component the component
   * @return the value from the given component
   */
  protected abstract V getComponentValue(C component);

  /**
   * Sets the given value in the input component.
   * @param component the component
   * @param value the value to display in the input component
   */
  protected abstract void setComponentValue(C component, V value);
}
