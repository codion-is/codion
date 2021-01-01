/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import is.codion.common.value.AbstractValue;

import javax.swing.SwingUtilities;

import static java.util.Objects.requireNonNull;

/**
 * A default {@link ComponentValue} implementation.
 * @param <V> the value type
 * @param <C> the component type
 */
public abstract class AbstractComponentValue<V, C> extends AbstractValue<V> implements ComponentValue<V, C> {

  private final C component;

  /**
   * Instantiates a new nullable {@link AbstractComponentValue}
   * @param component the component
   */
  public AbstractComponentValue(final C component) {
    this(component, null);
  }

  /**
   * Instantiates a new {@link AbstractComponentValue}
   * @param component the component
   * @param nullValue the value to use instead of null
   */
  public AbstractComponentValue(final C component, final V nullValue) {
    super(nullValue);
    this.component = requireNonNull(component, "component");
  }

  @Override
  public final V get() {
    return getComponentValue(component);
  }

  @Override
  public final C getComponent() {
    return component;
  }

  @Override
  protected final void doSet(final V value) {
    if (SwingUtilities.isEventDispatchThread()) {
      setComponentValue(component, value);
    }
    else {
      try {
        SwingUtilities.invokeAndWait(() -> setComponentValue(component, value));
      }
      catch (final InterruptedException ex) {
        Thread.currentThread().interrupt();
        throw new RuntimeException(ex);
      }
      catch (final Exception e) {
        throw new RuntimeException(e);
      }
    }
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
