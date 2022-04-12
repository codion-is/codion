/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.AbstractValue;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.lang.reflect.InvocationTargetException;

import static java.util.Objects.requireNonNull;

/**
 * A default {@link ComponentValue} implementation.
 * @param <T> the value type
 * @param <C> the component type
 */
public abstract class AbstractComponentValue<T, C extends JComponent> extends AbstractValue<T> implements ComponentValue<T, C> {

  private final C component;

  /**
   * Instantiates a new nullable {@link AbstractComponentValue}
   * @param component the component
   */
  public AbstractComponentValue(C component) {
    this(component, null);
  }

  /**
   * Instantiates a new {@link AbstractComponentValue}
   * @param component the component
   * @param nullValue the value to use instead of null
   */
  public AbstractComponentValue(C component, T nullValue) {
    super(nullValue);
    this.component = requireNonNull(component, "component");
  }

  @Override
  public final T get() {
    return getComponentValue(component);
  }

  @Override
  public final C getComponent() {
    return component;
  }

  @Override
  protected final void setValue(T value) {
    if (SwingUtilities.isEventDispatchThread()) {
      setComponentValue(component, value);
    }
    else {
      try {
        SwingUtilities.invokeAndWait(() -> setComponentValue(component, value));
      }
      catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        throw new RuntimeException(ex);
      }
      catch (InvocationTargetException e) {
        Throwable cause = e.getCause();
        if (cause instanceof RuntimeException) {
          throw (RuntimeException) cause;
        }

        throw new RuntimeException(cause);
      }
      catch (RuntimeException e) {
        throw e;
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * Returns the value according to the component
   * @param component the component
   * @return the value from the given component
   */
  protected abstract T getComponentValue(C component);

  /**
   * Sets the given value in the input component. Note that this method is called on the EDT.
   * @param component the component
   * @param value the value to display in the input component
   */
  protected abstract void setComponentValue(C component, T value);
}
