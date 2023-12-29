/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.event.EventObserver;
import is.codion.common.state.State;
import is.codion.common.value.ValueObserver;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.awt.event.KeyEvent.VK_ENTER;
import static java.awt.event.KeyEvent.VK_ESCAPE;
import static java.util.Objects.requireNonNull;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
import static javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW;

final class DefaultComponentDialogBuilder extends AbstractDialogBuilder<ComponentDialogBuilder> implements ComponentDialogBuilder {

  private final JComponent component;

  private boolean modal = true;
  private boolean resizable = true;
  private Dimension size;
  private Action enterAction;
  private Consumer<JDialog> onShown;
  private Consumer<WindowEvent> onOpened;
  private Consumer<WindowEvent> onClosed;
  private EventObserver<?> closeEvent;
  private Consumer<State> confirmCloseListener;
  private boolean disposeOnEscape = true;

  DefaultComponentDialogBuilder(JComponent component) {
    this.component = requireNonNull(component);
  }

  @Override
  public ComponentDialogBuilder modal(boolean modal) {
    this.modal = modal;
    return this;
  }

  @Override
  public ComponentDialogBuilder resizable(boolean resizable) {
    this.resizable = resizable;
    return this;
  }

  @Override
  public ComponentDialogBuilder size(Dimension size) {
    this.size = requireNonNull(size);
    return this;
  }

  @Override
  public ComponentDialogBuilder enterAction(Action enterAction) {
    this.enterAction = requireNonNull(enterAction);
    return this;
  }

  @Override
  public ComponentDialogBuilder closeEvent(EventObserver<?> closeEvent) {
    this.closeEvent = closeEvent;
    return this;
  }

  @Override
  public ComponentDialogBuilder confirmCloseListener(Consumer<State> confirmCloseListener) {
    this.confirmCloseListener = confirmCloseListener;
    return this;
  }

  @Override
  public ComponentDialogBuilder disposeOnEscape(boolean disposeOnEscape) {
    this.disposeOnEscape = disposeOnEscape;
    return this;
  }

  @Override
  public ComponentDialogBuilder onShown(Consumer<JDialog> onShown) {
    this.onShown = onShown;
    return this;
  }

  @Override
  public ComponentDialogBuilder onOpened(Consumer<WindowEvent> onOpened) {
    this.onOpened = requireNonNull(onOpened);
    return this;
  }

  @Override
  public ComponentDialogBuilder onClosed(Consumer<WindowEvent> onClosed) {
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
    JDialog dialog = createDialog(owner, titleProvider, icon, component, size, locationRelativeTo,
            location, modal, resizable, onShown, keyEventBuilders);
    if (enterAction != null) {
      KeyEvents.builder(VK_ENTER)
              .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .action(enterAction)
              .enable(dialog.getRootPane());
    }

    Action disposeAction = new DisposeDialogAction(new DialogSupplier(dialog), confirmCloseListener);
    dialog.addWindowListener(new DialogListener(disposeAction, onClosed, onOpened));
    if (closeEvent == null) {
      dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
      if (disposeOnEscape) {
        KeyEvents.builder(VK_ESCAPE)
                .condition(WHEN_IN_FOCUSED_WINDOW)
                .action(new DisposeDialogOnEscapeAction(dialog, confirmCloseListener))
                .enable(dialog.getRootPane());
      }
    }
    else {
      closeEvent.addListener(new CloseListener(disposeAction));
    }

    return dialog;
  }

  static JDialog createDialog(Window owner, ValueObserver<String> titleProvider, ImageIcon icon,
                              JComponent component, Dimension size, Component locationRelativeTo,
                              Point location, boolean modal, boolean resizable, Consumer<JDialog> onShown,
                              List<KeyEvents.Builder> keyEventBuilders) {
    JDialog dialog = new JDialog(owner);
    if (titleProvider != null) {
      dialog.setTitle(titleProvider.get());
      titleProvider.addDataListener(new DialogTitleListener(dialog));
    }
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
    if (location != null) {
      dialog.setLocation(location);
    }
    else if (locationRelativeTo != null) {
      dialog.setLocationRelativeTo(locationRelativeTo);
    }
    else {
      dialog.setLocationRelativeTo(owner);
    }
    dialog.setModal(modal);
    dialog.setResizable(resizable);
    keyEventBuilders.forEach(keyEventBuilder -> keyEventBuilder.enable(dialog.getRootPane()));
    if (onShown != null) {
      dialog.addComponentListener(new OnShownAdapter(dialog, onShown));
    }

    return dialog;
  }

  private static final class DialogSupplier implements Supplier<JDialog> {

    private final JDialog dialog;

    private DialogSupplier(JDialog dialog) {
      this.dialog = dialog;
    }

    @Override
    public JDialog get() {
      return dialog;
    }
  }

  private static final class DialogListener extends WindowAdapter {

    private final Action disposeAction;
    private final Consumer<WindowEvent> onClosed;
    private final Consumer<WindowEvent> onOpened;

    private DialogListener(Action disposeAction, Consumer<WindowEvent> onClosed, Consumer<WindowEvent> onOpened) {
      this.disposeAction = disposeAction;
      this.onClosed = onClosed;
      this.onOpened = onOpened;
    }

    @Override
    public void windowClosing(WindowEvent e) {
      disposeAction.actionPerformed(null);
    }

    @Override
    public void windowClosed(WindowEvent e) {
      if (onClosed != null) {
        onClosed.accept(e);
      }
    }

    @Override
    public void windowOpened(WindowEvent e) {
      if (onOpened != null) {
        onOpened.accept(e);
      }
    }
  }

  private static final class CloseListener implements Runnable {

    private final Action disposeAction;

    private CloseListener(Action disposeAction) {
      this.disposeAction = disposeAction;
    }

    @Override
    public void run() {
      disposeAction.actionPerformed(null);
    }
  }

  private static final class DialogTitleListener implements Consumer<String> {

    private final JDialog dialog;

    private DialogTitleListener(JDialog dialog) {
      this.dialog = dialog;
    }

    @Override
    public void accept(String title) {
      dialog.setTitle(title);
    }
  }

  private static final class OnShownAdapter extends ComponentAdapter {

    private final JDialog dialog;
    private final Consumer<JDialog> onShown;

    private OnShownAdapter(JDialog dialog, Consumer<JDialog> onShown) {
      this.dialog = dialog;
      this.onShown = onShown;
    }

    @Override
    public void componentShown(ComponentEvent e) {
      onShown.accept(dialog);
    }
  }
}
