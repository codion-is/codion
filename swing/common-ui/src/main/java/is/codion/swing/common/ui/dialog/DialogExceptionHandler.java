/*
 * Copyright (c) 2008 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import java.awt.Window;

/**
 * Handles an exception by displaying an error dialog.
 * @see #getInstance()
 */
public interface DialogExceptionHandler {

  /**
   * Handle the given exception and display it to the user.
   * @param exception the exception
   * @param dialogParent the Window to use as parent to the exception dialog
   */
  void displayException(Throwable exception, Window dialogParent);

  /**
   * Returns a default {@link DialogExceptionHandler} instance.
   * Note that this instance ignores {@link is.codion.common.model.CancelException}s.
   * @return a default {@link DialogExceptionHandler} instance
   */
  static DialogExceptionHandler getInstance() {
    return DefaultDialogExceptionHandler.INSTANCE;
  }
}
