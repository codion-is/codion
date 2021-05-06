/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import javax.swing.AbstractAction;
import java.awt.Dialog;
import java.awt.event.ActionEvent;

final class DisposeDialogAction extends AbstractAction {

  private final Dialog dialog;

  DisposeDialogAction(final Dialog dialog) {
    super("Dialogs.disposeDialogAction");
    this.dialog = dialog;
  }

  @Override
  public void actionPerformed(final ActionEvent e) {
    dialog.dispose();
  }
}
