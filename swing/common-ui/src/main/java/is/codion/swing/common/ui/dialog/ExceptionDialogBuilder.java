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
 * Copyright (c) 2021 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.utilities.property.PropertyValue;

import org.jspecify.annotations.Nullable;

import static is.codion.common.utilities.Configuration.booleanValue;

/**
 * An exception dialog builder.
 */
public interface ExceptionDialogBuilder extends DialogBuilder<ExceptionDialogBuilder> {

	/**
	 * Specifies whether an ExceptionPanel should include system properties in the detail panel
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 */
	PropertyValue<Boolean> SYSTEM_PROPERTIES =
					booleanValue(ExceptionDialogBuilder.class.getName() + ".systemProperties", true);

	/**
	 * @param message the message to display
	 * @return this builder instance
	 */
	ExceptionDialogBuilder message(@Nullable String message);

	/**
	 * @param systemProperties true if system properties should be displayed
	 * @return this builder instance
	 * @see #SYSTEM_PROPERTIES
	 */
	ExceptionDialogBuilder systemProperties(boolean systemProperties);

	/**
	 * Displays the exception dialog
	 * @param exception the exception to display
	 */
	void show(Throwable exception);
}
