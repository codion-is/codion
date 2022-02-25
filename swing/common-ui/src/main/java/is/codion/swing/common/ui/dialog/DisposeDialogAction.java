/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.event.EventDataListener;
import is.codion.common.state.State;

import javax.swing.AbstractAction;
import javax.swing.JDialog;
import java.awt.event.ActionEvent;
import java.util.function.Supplier;

final class DisposeDialogAction extends AbstractAction {

  private final Supplier<JDialog> dialogSupplier;
  private final EventDataListener<State> confirmCloseListener;

  DisposeDialogAction(final Supplier<JDialog> dialogSupplier, final EventDataListener<State> confirmCloseListener) {
    super("DisposeDialogAction");
    this.dialogSupplier = dialogSupplier;
    this.confirmCloseListener = confirmCloseListener;
  }

  @Override
  public void actionPerformed(final ActionEvent e) {
    closeIfConfirmed(dialogSupplier.get(), confirmCloseListener);
  }

  static void closeIfConfirmed(final JDialog dialog, final EventDataListener<State> confirmCloseListener) {
    if (dialog != null) {
      if (confirmCloseListener == null) {
        dialog.dispose();
      }
      else {
        State confirmClose = State.state();
        confirmCloseListener.onEvent(confirmClose);
        if (confirmClose.get()) {
          dialog.dispose();
        }
      }
    }
  }
}
