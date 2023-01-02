/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.i18n.Messages;
import is.codion.common.state.StateObserver;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.Consumer;

import static is.codion.swing.common.ui.dialog.DefaultComponentDialogBuilder.createDialog;
import static java.util.Objects.requireNonNull;

final class DefaultOkCancelDialogBuilder extends AbstractDialogBuilder<OkCancelDialogBuilder> implements OkCancelDialogBuilder {

  private final JComponent component;

  private boolean modal = true;
  private boolean resizable = true;
  private Dimension size;
  private Consumer<JDialog> onShown;
  private StateObserver okEnabledState;
  private StateObserver cancelEnabledState;
  private Runnable onOk;
  private Runnable onCancel;
  private Action okAction;
  private Action cancelAction;
  private int buttonPanelConstraints = FlowLayout.RIGHT;
  private Border buttonPanelBorder = BorderFactory.createEmptyBorder(10, 10, 10, 10);

  DefaultOkCancelDialogBuilder(JComponent component) {
    this.component = requireNonNull(component);
  }

  @Override
  public OkCancelDialogBuilder modal(boolean modal) {
    this.modal = modal;
    return this;
  }

  @Override
  public OkCancelDialogBuilder resizable(boolean resizable) {
    this.resizable = resizable;
    return this;
  }

  @Override
  public OkCancelDialogBuilder size(Dimension size) {
    this.size = requireNonNull(size);
    return this;
  }

  @Override
  public OkCancelDialogBuilder buttonPanelConstraints(int buttonPanelConstraints) {
    this.buttonPanelConstraints = buttonPanelConstraints;
    return this;
  }

  @Override
  public OkCancelDialogBuilder buttonPanelBorder(Border buttonPanelBorder) {
    this.buttonPanelBorder = buttonPanelBorder;
    return this;
  }

  @Override
  public OkCancelDialogBuilder okEnabledState(StateObserver okEnabledState) {
    if (okAction != null) {
      throw new IllegalStateException("OK action has already been set");
    }
    this.okEnabledState = requireNonNull(okEnabledState);

    return this;
  }

  @Override
  public OkCancelDialogBuilder cancelEnabledState(StateObserver cancelEnabledState) {
    if (cancelAction != null) {
      throw new IllegalStateException("Cancel action has already been set");
    }
    this.cancelEnabledState = requireNonNull(cancelEnabledState);

    return this;
  }

  @Override
  public OkCancelDialogBuilder onOk(Runnable onOk) {
    if (okAction != null) {
      throw new IllegalStateException("OK action has already been set");
    }
    this.onOk = requireNonNull(onOk);

    return this;
  }

  @Override
  public OkCancelDialogBuilder onCancel(Runnable onCancel) {
    if (cancelAction != null) {
      throw new IllegalStateException("Cancel action has already been set");
    }
    this.onCancel = requireNonNull(onCancel);

    return this;
  }

  @Override
  public OkCancelDialogBuilder okAction(Action okAction) {
    if (onOk != null) {
      throw new IllegalStateException("onOk has already been set");
    }
    this.okAction = requireNonNull(okAction);
    return this;
  }

  @Override
  public OkCancelDialogBuilder cancelAction(Action cancelAction) {
    if (onCancel != null) {
      throw new IllegalStateException("onCancel has already been set");
    }
    this.cancelAction = requireNonNull(cancelAction);
    return this;
  }

  @Override
  public OkCancelDialogBuilder onShown(Consumer<JDialog> onShown) {
    this.onShown = onShown;
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
    if (okAction == null) {
      okAction = createControl(onOk == null ? new DefaultOkCommand(component) : new PerformAndCloseCommand(onOk, component), okEnabledState);
    }
    if (cancelAction == null) {
      cancelAction = createControl(onCancel == null ? new DefaultCancelCommand(component) : new PerformAndCloseCommand(onCancel, component), cancelEnabledState);
    }
    JButton okButton = new JButton(okAction);
    okButton.setText(Messages.ok());
    okButton.setMnemonic(Messages.okMnemonic());
    JButton cancelButton = new JButton(cancelAction);
    cancelButton.setText(Messages.cancel());
    cancelButton.setMnemonic(Messages.cancelMnemonic());
    JPanel buttonPanel = new JPanel(Layouts.gridLayout(1, 2));
    buttonPanel.add(okButton);
    buttonPanel.add(cancelButton);
    JPanel buttonBasePanel = new JPanel(Layouts.flowLayout(buttonPanelConstraints));
    buttonBasePanel.add(buttonPanel, BorderLayout.SOUTH);
    buttonBasePanel.setBorder(buttonPanelBorder);
    JPanel panel = new JPanel(new BorderLayout());
    panel.add(component, BorderLayout.CENTER);
    panel.add(buttonBasePanel, BorderLayout.SOUTH);

    JDialog dialog = createDialog(owner, titleProvider, icon, panel, size, locationRelativeTo, location, modal, resizable, onShown);
    dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    dialog.addWindowListener(new CancelOnWindowClosingListener(cancelAction));
    dialog.getRootPane().setDefaultButton(okButton);
    KeyEvents.builder(KeyEvent.VK_ESCAPE)
            .condition(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .action(cancelAction)
            .enable(dialog.getRootPane());

    return dialog;
  }

  private static Control createControl(Control.Command command, StateObserver enabledState) {
    Control.Builder builder = Control.builder(command);
    if (enabledState != null) {
      builder.enabledState(enabledState);
    }

    return builder.build();
  }

  private static final class CancelOnWindowClosingListener extends WindowAdapter {

    private final Action cancelAction;

    private CancelOnWindowClosingListener(Action cancelAction) {
      this.cancelAction = cancelAction;
    }

    @Override
    public void windowClosing(WindowEvent e) {
      cancelAction.actionPerformed(null);
    }
  }

  private static final class PerformAndCloseCommand implements Control.Command {

    private final Runnable command;
    private final JComponent component;

    private PerformAndCloseCommand(Runnable command, JComponent component) {
      this.command = command;
      this.component = component;
    }

    @Override
    public void perform() throws Exception {
      command.run();
      Utilities.disposeParentWindow(component);
    }
  }

  private static final class DefaultOkCommand implements Control.Command {

    private final JComponent component;

    private DefaultOkCommand(JComponent component) {
      this.component = component;
    }

    @Override
    public void perform() throws Exception {
      Utilities.disposeParentWindow(component);
    }
  }

  private static final class DefaultCancelCommand implements Control.Command {

    private final JComponent component;

    private DefaultCancelCommand(JComponent component) {
      this.component = component;
    }

    @Override
    public void perform() throws Exception {
      Utilities.disposeParentWindow(component);
    }
  }
}
