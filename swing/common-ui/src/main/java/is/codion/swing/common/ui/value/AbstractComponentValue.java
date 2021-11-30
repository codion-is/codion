/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import is.codion.common.i18n.Messages;
import is.codion.common.model.CancelException;
import is.codion.common.state.State;
import is.codion.common.value.AbstractValue;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.Panels;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
  public AbstractComponentValue(final C component) {
    this(component, null);
  }

  /**
   * Instantiates a new {@link AbstractComponentValue}
   * @param component the component
   * @param nullValue the value to use instead of null
   */
  public AbstractComponentValue(final C component, final T nullValue) {
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
  public final T showDialog(final JComponent owner) {
    return showDialog(owner, null);
  }

  @Override
  public final T showDialog(final JComponent owner, final String title) {
    final State okPressed = State.state();
    final JPanel basePanel = new JPanel(Layouts.borderLayout());
    basePanel.add(component, BorderLayout.CENTER);
    basePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
    showDialog(owner, title, basePanel, okPressed);
    if (okPressed.get()) {
      return get();
    }

    throw new CancelException();
  }

  @Override
  protected final void setValue(final T value) {
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
  protected abstract T getComponentValue(C component);

  /**
   * Sets the given value in the input component. Note that this method is called on the EDT.
   * @param component the component
   * @param value the value to display in the input component
   */
  protected abstract void setComponentValue(C component, T value);

  private static void showDialog(final JComponent owner, final String title, final JPanel centerPanel,
                                 final State okPressed) {
    final JDialog dialog = new JDialog(Windows.getParentWindow(owner), title);
    dialog.setLayout(Layouts.borderLayout());
    dialog.add(centerPanel, BorderLayout.CENTER);

    final Control okControl = Control.builder(() -> {
              okPressed.set(true);
              dialog.dispose();
            })
            .caption(Messages.get(Messages.OK))
            .mnemonic(Messages.get(Messages.OK_MNEMONIC).charAt(0))
            .build();
    final Control cancelControl = Control.builder(dialog::dispose)
            .caption(Messages.get(Messages.CANCEL))
            .mnemonic(Messages.get(Messages.CANCEL_MNEMONIC).charAt(0))
            .build();
    dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    dialog.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(final WindowEvent e) {
        cancelControl.actionPerformed(null);
      }
    });
    KeyEvents.builder(KeyEvent.VK_ESCAPE)
            .condition(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .action(cancelControl)
            .enable(dialog.getRootPane());
    KeyEvents.builder(KeyEvent.VK_ENTER).condition(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .onKeyPressed()
            .action(okControl)
            .enable(dialog.getRootPane());
    dialog.setLayout(Layouts.borderLayout());
    dialog.add(centerPanel, BorderLayout.CENTER);
    final JPanel buttonBasePanel = new JPanel(Layouts.flowLayout(FlowLayout.CENTER));
    buttonBasePanel.add(Panels.createOkCancelButtonPanel(okControl, cancelControl));
    dialog.add(buttonBasePanel, BorderLayout.SOUTH);
    dialog.pack();
    dialog.setLocationRelativeTo(owner);
    dialog.setModal(true);
    dialog.setResizable(true);
    dialog.setVisible(true);
  }
}
