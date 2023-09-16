/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.state.StateObserver;

import javax.swing.Action;

/**
 * Builds a modal dialog for displaying the given {@code component},
 * with OK and Cancel buttons based on the given actions.
 * An OK action must be provided and the default Cancel action simply disposes the dialog.
 */
public interface OkCancelDialogBuilder extends ActionDialogBuilder<OkCancelDialogBuilder> {

  /**
   * Note that this is overridden by {@link #okAction(Action)}.
   * @param okEnabled the state observer controlling the ok enabled state
   * @return this builder instance
   * @throws IllegalStateException in case an ok action has already been set
   */
  OkCancelDialogBuilder okEnabled(StateObserver okEnabled);

  /**
   * Note that this is overridden by {@link #cancelAction(Action)}.
   * @param cancelEnabled the state observer controlling the cancel enabled state
   * @return this builder instance
   * @throws IllegalStateException in case a cancel action has already been set
   */
  OkCancelDialogBuilder cancelEnabled(StateObserver cancelEnabled);

  /**
   * @param onOk called on ok pressed, before the dialog has been disposed
   * @return this builder instance
   * @throws IllegalStateException in case an ok action has already been set
   */
  OkCancelDialogBuilder onOk(Runnable onOk);

  /**
   * @param onCancel called on cancel pressed, before the dialog has been disposed
   * @return this builder instance
   * @throws IllegalStateException in case a cancel action has already been set
   */
  OkCancelDialogBuilder onCancel(Runnable onCancel);

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
}
