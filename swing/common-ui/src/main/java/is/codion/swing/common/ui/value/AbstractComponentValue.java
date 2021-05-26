/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import is.codion.common.model.CancelException;
import is.codion.common.state.State;
import is.codion.common.value.AbstractValue;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.lang.reflect.InvocationTargetException;

import static java.util.Objects.requireNonNull;

/**
 * A default {@link ComponentValue} implementation.
 * @param <V> the value type
 * @param <C> the component type
 */
public abstract class AbstractComponentValue<V, C extends JComponent> extends AbstractValue<V> implements ComponentValue<V, C> {

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
  public final V showDialog(final JComponent owner) {
    return showDialog(owner, null);
  }

  @Override
  public final V showDialog(final JComponent owner, final String title) {
    final State okPressed = State.state();
    final JPanel basePanel = new JPanel(Layouts.borderLayout());
    basePanel.add(component, BorderLayout.CENTER);
    basePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
    Dialogs.okCancelDialogBuilder(basePanel)
            .owner(owner)
            .title(title)
            .onOk(() -> okPressed.set(true))
            .show();
    if (okPressed.get()) {
      return get();
    }

    throw new CancelException();
  }

  @Override
  protected final void setValue(final V value) {
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
      catch (final InvocationTargetException e) {
        final Throwable cause = e.getCause();
        if (cause instanceof RuntimeException) {
          throw (RuntimeException) cause;
        }

        throw new RuntimeException(cause);
      }
      catch (final RuntimeException e) {
        throw e;
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
