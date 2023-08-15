/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.Configuration;
import is.codion.common.property.PropertyValue;

/**
 * An exception dialog builder.
 */
public interface ExceptionDialogBuilder extends DialogBuilder<ExceptionDialogBuilder> {

  /**
   * Specifies whether an ExceptionPanel should display system properties in the detail panel<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  PropertyValue<Boolean> DISPLAY_SYSTEM_PROPERTIES =
          Configuration.booleanValue("is.codion.swing.common.ui.dialog.ExceptionDialogBuilder.displaySystemProperties", true);

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
