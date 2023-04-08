/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.state.StateObserver;

import javax.swing.JComponent;

/**
 * Displays the component from a given component value in a dialog and returns the value if the user accepts the input.
 */
public interface InputDialogBuilder<T> {

  /**
   * @param owner the dialog owner
   * @return this builder instance
   */
  InputDialogBuilder<T> owner(JComponent owner);

  /**
   * @param title the dialog title
   * @return this builder instance
   */
  InputDialogBuilder<T> title(String title);

  /**
   * @param caption the label caption
   * @return this builder instance
   */
  InputDialogBuilder<T> caption(String caption);

  /**
   * A StateObserver indicating whethere the input is valid, this state controls the enabled state of the OK button.
   * @param inputValidState a StateObserver indicating whether the input value is valid
   * @return this builder instance
   */
  InputDialogBuilder<T> inputValidState(StateObserver inputValidState);

  /**
   * Shows the input dialog and returns the value if the user presses OK
   * @return the value from the component value if the user accepts the input
   * @throws is.codion.common.model.CancelException in case the user cancels
   */
  T show();
}
