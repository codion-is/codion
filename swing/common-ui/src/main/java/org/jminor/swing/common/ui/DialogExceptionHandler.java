/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui;

import java.awt.Window;

/**
 * Handles an exception by displaying a error dialog.
 */
public interface DialogExceptionHandler {
  /**
   * Handle the given exception and display it to the user.
   * @param exception the exception
   * @param dialogParent the Window to use as parent to the exception dialog
   */
  void displayException(final Throwable exception, final Window dialogParent);
}
