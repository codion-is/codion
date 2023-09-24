/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.value;

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
  public final C component() {
    return component;
  }

  @Override
  protected final void setValue(T value) {
    if (SwingUtilities.isEventDispatchThread()) {
      setComponentValue(value);
      return;
    }
    try {
      SwingUtilities.invokeAndWait(() -> setComponentValue(value));
    }
    catch (Exception ex) {
      handleInvokeAndWaitException(ex);
    }
  }

  /**
   * Returns the value from the underlying component
   * @return the value from the underlying component
   * @see #component()
   */
  protected abstract T getComponentValue();

  /**
   * Sets the given value in the underlying component. Note that this method is called on the EDT.
   * @param value the value to display in the underlying component
   * @see #component()
   */
  protected abstract void setComponentValue(T value);

  private static void handleInvokeAndWaitException(Exception exception) {
    Throwable cause = exception;
    if (exception instanceof InvocationTargetException) {
      cause = exception.getCause();
    }
    if (cause instanceof InterruptedException) {
      Thread.currentThread().interrupt();
    }
    if (cause instanceof RuntimeException) {
      throw (RuntimeException) cause;
    }

    throw new RuntimeException(cause);
  }
}
