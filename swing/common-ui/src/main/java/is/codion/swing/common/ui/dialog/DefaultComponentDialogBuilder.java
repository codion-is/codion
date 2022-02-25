/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventObserver;
import is.codion.common.state.State;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

final class DefaultComponentDialogBuilder extends AbstractDialogBuilder<ComponentDialogBuilder> implements ComponentDialogBuilder {

  private final JComponent component;

  private boolean modal = true;
  private boolean resizable = true;
  private Dimension size;
  private JComponent locationRelativeTo;
  private Action enterAction;
  private Consumer<JDialog> onShown;
  private Consumer<WindowEvent> onOpened;
  private Consumer<WindowEvent> onClosed;
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
  public ComponentDialogBuilder size(final Dimension size) {
    this.size = requireNonNull(size);
    return this;
  }

  @Override
  public ComponentDialogBuilder enterAction(final Action enterAction) {
    this.enterAction = requireNonNull(enterAction);
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
  public ComponentDialogBuilder locationRelativeTo(final JComponent locationRelativeTo) {
    this.locationRelativeTo = requireNonNull(locationRelativeTo);
    return this;
  }

  @Override
  public ComponentDialogBuilder onShown(final Consumer<JDialog> onShown) {
    this.onShown = onShown;
    return this;
  }

  @Override
  public ComponentDialogBuilder onOpened(final Consumer<WindowEvent> onOpened) {
    this.onOpened = requireNonNull(onOpened);
    return this;
  }

  @Override
  public ComponentDialogBuilder onClosed(final Consumer<WindowEvent> onClosed) {
    this.onClosed = requireNonNull(onClosed);
    return this;
  }

  @Override
  public JDialog show() {
    JDialog dialog = build();
    dialog.setVisible(true);

    return dialog;
  }

  @Override
  public JDialog build() {
    JDialog dialog = createDialog(owner, title, icon, component, size, locationRelativeTo, modal, resizable, onShown);
    if (enterAction != null) {
      KeyEvents.builder(KeyEvent.VK_ENTER)
              .condition(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .onKeyPressed()
              .action(enterAction)
              .enable(dialog.getRootPane());
    }

    Action disposeAction = new DisposeDialogAction(() -> dialog, confirmCloseListener);
    dialog.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(final WindowEvent e) {
        disposeAction.actionPerformed(null);
      }
      @Override
      public void windowClosed(final WindowEvent e) {
        if (onClosed != null) {
          onClosed.accept(e);
        }
      }
      @Override
      public void windowOpened(final WindowEvent e) {
        if (onOpened != null) {
          onOpened.accept(e);
        }
      }
    });
    if (closeEvent == null) {
      dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
      if (disposeOnEscape) {
        KeyEvents.builder(KeyEvent.VK_ESCAPE)
                .condition(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .onKeyPressed()
                .action(new DisposeDialogOnEscapeAction(dialog, confirmCloseListener))
                .enable(dialog.getRootPane());
      }
    }
    else {
      closeEvent.addListener(() -> disposeAction.actionPerformed(null));
    }

    return dialog;
  }

  static JDialog createDialog(final Window owner, final String title, final ImageIcon icon,
                              final JComponent component, final Dimension size, final JComponent locationRelativeTo,
                              final boolean modal, final boolean resizable, final Consumer<JDialog> onShown) {
    JDialog dialog = new JDialog(owner, title);
    if (icon != null) {
      dialog.setIconImage(icon.getImage());
    }
    dialog.setLayout(Layouts.borderLayout());
    dialog.add(component, BorderLayout.CENTER);
    if (size != null) {
      dialog.setSize(size);
    }
    else {
      dialog.pack();
    }
    if (locationRelativeTo != null) {
      dialog.setLocationRelativeTo(locationRelativeTo);
    }
    else {
      dialog.setLocationRelativeTo(owner);
    }
    dialog.setModal(modal);
    dialog.setResizable(resizable);
    if (onShown != null) {
      dialog.addComponentListener(new ComponentAdapter() {
        @Override
        public void componentShown(final ComponentEvent e) {
          onShown.accept(dialog);
        }
      });
    }

    return dialog;
  }
}
