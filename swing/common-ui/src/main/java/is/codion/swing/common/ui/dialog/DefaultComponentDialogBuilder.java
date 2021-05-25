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
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import static java.util.Objects.requireNonNull;

final class DefaultComponentDialogBuilder extends AbstractDialogBuilder<ComponentDialogBuilder> implements ComponentDialogBuilder {

  private final JComponent component;

  private boolean modal = true;
  private boolean resizable = true;
  private Action enterAction;
  private Action onClosedAction;
  private EventObserver<?> closeEvent;
  private EventDataListener<State> confirmCloseListener;
  private boolean disposeOnEscape = true;

  DefaultComponentDialogBuilder(final JComponent component) {
    this.component = requireNonNull(component);
  }

  @Override
  public ComponentDialogBuilder modal(final boolean modal) {
    this.modal = modal;
    return this;
  }

  @Override
  public ComponentDialogBuilder resizable(final boolean resizable) {
    this.resizable = resizable;
    return this;
  }

  @Override
  public ComponentDialogBuilder enterAction(final Action enterAction) {
    this.enterAction = requireNonNull(enterAction);
    return this;
  }

  @Override
  public ComponentDialogBuilder onClosedAction(final Action onClosedAction) {
    this.onClosedAction = onClosedAction;
    return this;
  }

  @Override
  public ComponentDialogBuilder closeEvent(final EventObserver<?> closeEvent) {
    this.closeEvent = closeEvent;
    return this;
  }

  @Override
  public ComponentDialogBuilder confirmCloseListener(final EventDataListener<State> confirmCloseListener) {
    this.confirmCloseListener = confirmCloseListener;
    return this;
  }

  @Override
  public ComponentDialogBuilder disposeOnEscape(final boolean disposeOnEscape) {
    this.disposeOnEscape = disposeOnEscape;
    return this;
  }

  @Override
  public JDialog show() {
    final JDialog dialog = build();
    dialog.setVisible(true);

    return dialog;
  }

  @Override
  public JDialog build() {
    final JDialog dialog = new JDialog(owner, title);
    if (icon != null) {
      dialog.setIconImage(icon.getImage());
    }
    dialog.setLayout(Layouts.borderLayout());
    dialog.add(component, BorderLayout.CENTER);
    dialog.pack();
    if (owner != null) {
      dialog.setLocationRelativeTo(owner);
    }
    else {
      Windows.centerWindow(dialog);
    }
    dialog.setModal(modal);
    dialog.setResizable(resizable);

    if (enterAction != null) {
      KeyEvents.builder()
              .keyEvent(KeyEvent.VK_ENTER)
              .condition(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .onKeyPressed()
              .action(enterAction)
              .enable(dialog.getRootPane());
    }

    final Action disposeAction = new DisposeDialogAction(() -> dialog, confirmCloseListener);
    dialog.addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosing(final WindowEvent e) {
          disposeAction.actionPerformed(null);
        }
      });
    if (closeEvent == null) {
      dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
      if (disposeOnEscape) {
        KeyEvents.builder()
                .keyEvent(KeyEvent.VK_ESCAPE)
                .condition(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .onKeyPressed()
                .action(new DisposeDialogOnEscapeAction(dialog, confirmCloseListener))
                .enable(dialog.getRootPane());
      }
    }
    else {
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
}
