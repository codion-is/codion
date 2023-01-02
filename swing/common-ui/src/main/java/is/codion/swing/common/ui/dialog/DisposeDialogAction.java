/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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

  DisposeDialogAction(Supplier<JDialog> dialogSupplier, EventDataListener<State> confirmCloseListener) {
    super("DisposeDialogAction");
    this.dialogSupplier = dialogSupplier;
    this.confirmCloseListener = confirmCloseListener;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    closeIfConfirmed(dialogSupplier.get(), confirmCloseListener);
  }

  static void closeIfConfirmed(JDialog dialog, EventDataListener<State> confirmCloseListener) {
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
