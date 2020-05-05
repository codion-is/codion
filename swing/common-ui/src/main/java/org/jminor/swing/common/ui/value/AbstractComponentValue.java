/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.value;

import org.jminor.common.value.AbstractValue;
import org.jminor.common.value.Nullable;

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
    this(component, Nullable.YES);
  }

  /**
   * Instantiates a new {@link AbstractComponentValue}
   * @param component the component
   * @param nullable {@link Nullable#NO} if this value can not be null
   */
  public AbstractComponentValue(final C component, final Nullable nullable) {
    this.component = requireNonNull(component, "component");
    this.nullable = nullable == Nullable.YES;
  }

  @Override
  public final V get() {
    return getComponentValue(component);
  }

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

  @Override
  public final C getComponent() {
    return component;
  }

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
   * Sets the given value in the input component. Note that this method is called on the EDT.
   * @param component the component
   * @param value the value to display in the input component
   */
  protected abstract void setComponentValue(C component, V value);
}
