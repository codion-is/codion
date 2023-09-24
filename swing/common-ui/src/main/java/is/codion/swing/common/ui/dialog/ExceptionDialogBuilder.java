/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson.
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
