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
 * An abstract base implementation of {@link ComponentValue}.
 * @param <T> the value type
 * @param <C> the component type
 */
public abstract class AbstractComponentValue<T, C extends JComponent> extends AbstractValue<T> implements ComponentValue<T, C> {

  private final C component;

  /**
   * Instantiates a new nullable {@link AbstractComponentValue}
   * @param component the component
   * @throws NullPointerException in case component is null
   */
  protected AbstractComponentValue(C component) {
    this(component, null);
  }

  /**
   * Instantiates a new {@link AbstractComponentValue}
   * @param component the component
   * @param nullValue the value to use instead of null
   * @throws NullPointerException in case component is null
   */
  protected AbstractComponentValue(C component, T nullValue) {
    super(nullValue);
    this.component = requireNonNull(component, "component");
  }

  @Override
  public final T get() {
    return getComponentValue();
  }

  @Override
  public final C getComponent() {
    return component;
  }

  @Override
  protected final void setValue(T value) {
    if (SwingUtilities.isEventDispatchThread()) {
      setComponentValue(value);
    }
    else {
      try {
        SwingUtilities.invokeAndWait(() -> setComponentValue(value));
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
   * Returns the value from the underlying component
   * @return the value from the underlying component
   * @see #getComponent()
   */
  protected abstract T getComponentValue();

  /**
   * Sets the given value in the underlying component. Note that this method is called on the EDT.
   * @param value the value to display in the underlying component
   * @see #getComponent()
   */
  protected abstract void setComponentValue(T value);
}
