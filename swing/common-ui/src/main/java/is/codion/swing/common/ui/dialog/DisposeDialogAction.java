/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.event.EventDataListener;
import is.codion.common.state.State;

import javax.swing.AbstractAction;
import javax.swing.JDialog;
import java.awt.event.ActionEvent;

final class DisposeDialogAction extends AbstractAction {

  private final JDialog dialog;
  private final EventDataListener<State> confirmCloseListener;

  DisposeDialogAction(final JDialog dialog, final EventDataListener<State> confirmCloseListener) {
    super("DisposeDialogAction");
    this.dialog = dialog;
    this.confirmCloseListener = confirmCloseListener;
  }

  @Override
  public void actionPerformed(final ActionEvent e) {
    closeIfConfirmed(dialog, confirmCloseListener);
  }

  static void closeIfConfirmed(final JDialog dialog, final EventDataListener<State> confirmCloseListener) {
    if (confirmCloseListener == null) {
      dialog.dispose();
    }
    else {
      final State confirmClose = State.state();
      confirmCloseListener.onEvent(confirmClose);
      if (confirmClose.get()) {
        dialog.dispose();
      }
    }
  }
}
