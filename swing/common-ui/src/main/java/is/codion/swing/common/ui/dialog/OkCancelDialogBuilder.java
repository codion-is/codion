/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.swing.common.ui.control.Control;

import javax.swing.Action;
import javax.swing.JDialog;


/**
 * Builds a modal dialog for displaying the given {@code component},
 * with OK and Cancel buttons based on the given actions.
 * An OK action must be provided and the default Cancel action simply disposes the dialog.
 */
public interface OkCancelDialogBuilder extends DialogBuilder<OkCancelDialogBuilder> {

  /**
   * @param command calloed on ok pressed, before the dialog has been disposed
   * @return this builder instance
   */
  OkCancelDialogBuilder onOk(Control.Command command);

  /**
   * @param command called on cancel pressed, before the dialog has been disposed
   * @return this builder instance
   */
  OkCancelDialogBuilder onCancel(Control.Command command);

  /**
   * @param okAction the action for the OK button, this action must dispose the dialog
   * @return this builder instance
   */
  OkCancelDialogBuilder okAction(Action okAction);

  /**
   * @param cancelAction the action for the Cancel button
   * @return this builder instance
   */
  OkCancelDialogBuilder cancelAction(Action cancelAction);

  /**
   * Builds and shows the dialog.
   * @return a new JDialog instance based on this builder.
   * @throws IllegalStateException in case no component has been specified
   */
  JDialog show();

  /**
   * @return a new JDialog instance based on this builder.
   * @throws IllegalStateException in case no component has been specified
   */
  JDialog build();
}
