/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventObserver;
import is.codion.common.state.State;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JDialog;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

final class DefaultDialogBuilder implements Dialogs.Builder {

  private Container owner;
  private JComponent component;
  private String title;
  private boolean modal = true;
  private Action enterAction;
  private Action onClosedAction;
  private EventObserver<?> closeEvent;
  private EventDataListener<State> confirmCloseListener;
  private boolean disposeOnEscape = true;

  @Override
  public Dialogs.Builder owner(final Container owner) {
    this.owner = requireNonNull(owner);
    return this;
  }

  @Override
  public Dialogs.Builder component(final JComponent component) {
    this.component = requireNonNull(component);
    return this;
  }

  @Override
  public Dialogs.Builder title(final String title) {
    this.title = title;
    return this;
  }

  @Override
  public Dialogs.Builder modal(final boolean modal) {
    this.modal = modal;
    return this;
  }

  @Override
  public Dialogs.Builder enterAction(final Action enterAction) {
    this.enterAction = requireNonNull(enterAction);
    return this;
  }

  @Override
  public Dialogs.Builder onClosedAction(final Action onClosedAction) {
    this.onClosedAction = onClosedAction;
    return this;
  }

  @Override
  public Dialogs.Builder closeEvent(final EventObserver<?> closeEvent) {
    this.closeEvent = closeEvent;
    return this;
  }

  @Override
  public Dialogs.Builder confirmCloseListener(final EventDataListener<State> confirmCloseListener) {
    this.confirmCloseListener = confirmCloseListener;
    return this;
  }

  @Override
  public Dialogs.Builder disposeOnEscape(final boolean disposeOnEscape) {
    this.disposeOnEscape = disposeOnEscape;
    return this;
  }

  @Override
  public JDialog build() {
    final JDialog dialog = new JDialog(Windows.getParentWindow(owner), title);
    dialog.setLayout(Layouts.borderLayout());
    dialog.add(component, BorderLayout.CENTER);
    dialog.pack();
    dialog.setLocationRelativeTo(owner);
    dialog.setModal(modal);
    dialog.setResizable(true);

    if (enterAction != null) {
      KeyEvents.builder()
              .keyEvent(KeyEvent.VK_ENTER)
              .condition(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .onKeyPressed()
              .action(enterAction)
              .enable(dialog.getRootPane());
    }

    final Action disposeAction = new DisposeDialogAction(dialog, confirmCloseListener);
    if (closeEvent == null) {
      dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      if (disposeOnEscape) {
        KeyEvents.builder()
                .keyEvent(KeyEvent.VK_ESCAPE)
                .condition(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .onKeyPressed()
                .action(new DisposeDialogOnEscapeAction(dialog, confirmCloseListener))
                .enable(dialog.getRootPane());
      }
      dialog.addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosing(final WindowEvent e) {
          disposeAction.actionPerformed(null);
        }
      });
    }
    else {
      dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
      closeEvent.addListener(() -> disposeAction.actionPerformed(null));
    }
    if (onClosedAction != null) {
      dialog.addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosed(final WindowEvent e) {
          onClosedAction.actionPerformed(new ActionEvent(dialog, -1, null));
        }
      });
    }

    return dialog;
  }

  static <T extends Component> List<T> getComponentsOfType(final Container container, final Class<T> clazz) {
    final List<T> components = new ArrayList<>();
    for (final Component component : container.getComponents()) {
      if (clazz.isAssignableFrom(component.getClass())) {
        components.add((T) component);
      }
      if (component instanceof Container) {
        components.addAll(getComponentsOfType((Container) component, clazz));
      }
    }

    return components;
  }
}
