/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui;

import java.awt.Window;

/**
 * An interface describing an exception handler
 */
public interface ExceptionHandler {
  /**
   * Handle the given exception and display it to the user.
   * @param exception the exception
   * @param dialogParent the Window to use as parent to the exception dialog
   */
  void handleException(final Throwable exception, final Window dialogParent);
}
