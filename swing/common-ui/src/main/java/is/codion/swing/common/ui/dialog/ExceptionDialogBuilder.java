/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

/**
 * An exception dialog builder.
 */
public interface ExceptionDialogBuilder extends DialogBuilder<ExceptionDialogBuilder> {

  /**
   * @param message the message to display
   * @return this ExceptionDialogBuilder instance
   */
  ExceptionDialogBuilder message(String message);

  /**
   * Displays an exception dialog for the given exception
   * @param exception the exception to display
   */
  void show(Throwable exception);
}
