/*
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.state.State;

import javax.swing.AbstractAction;
import javax.swing.JDialog;
import java.awt.event.ActionEvent;
import java.util.function.Consumer;
import java.util.function.Supplier;

final class DisposeDialogAction extends AbstractAction {

  private final Supplier<JDialog> dialogSupplier;
  private final Consumer<State> confirmCloseListener;

  DisposeDialogAction(Supplier<JDialog> dialogSupplier, Consumer<State> confirmCloseListener) {
    super("DisposeDialogAction");
    this.dialogSupplier = dialogSupplier;
    this.confirmCloseListener = confirmCloseListener;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    closeIfConfirmed(dialogSupplier.get(), confirmCloseListener);
  }

  static void closeIfConfirmed(JDialog dialog, Consumer<State> confirmCloseListener) {
    if (dialog != null) {
      if (confirmCloseListener == null) {
        dialog.dispose();
      }
      else {
        State confirmClose = State.state();
        confirmCloseListener.accept(confirmClose);
        if (confirmClose.get()) {
          dialog.dispose();
        }
      }
    }
  }
}
