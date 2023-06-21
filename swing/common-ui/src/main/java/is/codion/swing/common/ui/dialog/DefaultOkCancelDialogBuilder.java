/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.i18n.Messages;
import is.codion.common.state.StateObserver;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.control.Control;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JDialog;

import static java.util.Objects.requireNonNull;

final class DefaultOkCancelDialogBuilder extends DefaultActionDialogBuilder<OkCancelDialogBuilder> implements OkCancelDialogBuilder {

  private StateObserver okEnabledState;
  private StateObserver cancelEnabledState;
  private Runnable onOk;
  private Runnable onCancel;
  private Action okAction;
  private Action cancelAction;

  DefaultOkCancelDialogBuilder(JComponent component) {
    super(component);
  }

  @Override
  public OkCancelDialogBuilder action(Action action) {
    throw new UnsupportedOperationException("Adding an action directly is not supported");
  }

  @Override
  public OkCancelDialogBuilder defaultAction(Action defaultAction) {
    throw new UnsupportedOperationException("Adding a default action is not supported");
  }

  @Override
  public OkCancelDialogBuilder escapeAction(Action escapeAction) {
    throw new UnsupportedOperationException("Adding an escape action is not supported");
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
  public JDialog build() {
    controls().removeAll();
    if (okAction == null) {
      okAction = Control.builder(onOk == null ? new DefaultOkCommand(component()) : new PerformAndCloseCommand(onOk, component()))
            .name(Messages.ok())
            .mnemonic(Messages.okMnemonic())
            .enabledState(okEnabledState)
            .build();
    }
    if (cancelAction == null) {
      cancelAction = Control.builder(onCancel == null ? new DefaultCancelCommand(component()) : new PerformAndCloseCommand(onCancel, component()))
            .name(Messages.cancel())
            .mnemonic(Messages.cancelMnemonic())
            .enabledState(cancelEnabledState)
            .build();
    }
    super.defaultAction(okAction);
    super.escapeAction(cancelAction);

    return super.build();
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
