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
 * Copyright (c) 2020 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.swing.common.ui.dialog.ActionDialogBuilder.ActionDialogComponentStep;
import is.codion.swing.common.ui.dialog.OkCancelDialogBuilder.OkCancelDialogComponentStep;

import org.jspecify.annotations.Nullable;

import java.awt.Window;

/**
 * A utility class for displaying Dialogs.
 */
public final class Dialogs {

	private Dialogs() {}

	/**
	 * @return a new {@link ComponentDialogBuilder} instance.
	 */
	public static ComponentDialogBuilder builder() {
		return new DefaultComponentDialogBuilder();
	}

	/**
	 * @return a new {@link ProgressDialog.Builder} instance.
	 */
	public static ProgressDialog.Builder progress() {
		return new ProgressDialog.DefaultBuilder();
	}

	/**
	 * @return a {@link ProgressWorkerDialogBuilder.BuilderFactory}
	 */
	public static ProgressWorkerDialogBuilder.BuilderFactory progressWorker() {
		return DefaultProgressWorkerDialogBuilder.BUILDER_FACTORY;
	}

	/**
	 * @return a new login dialog builder
	 */
	public static LoginDialogBuilder login() {
		return new DefaultLoginDialogBuilder();
	}

	/**
	 * @return a new exception dialog builder
	 */
	public static ExceptionDialogBuilder exception() {
		return new DefaultExceptionDialogBuilder();
	}

	/**
	 * @return a new {@link SelectionDialogBuilderFactory}
	 */
	public static SelectionDialogBuilderFactory select() {
		return DefaultSelectionDialogBuilderFactory.INSTANCE;
	}

	/**
	 * @return a {@link OkCancelDialogComponentStep} instance
	 */
	public static OkCancelDialogComponentStep okCancel() {
		return DefaultOkCancelDialogBuilder.OK_CANCEL_COMPONENT;
	}

	/**
	 * @return a {@link ActionDialogComponentStep} instance
	 */
	public static ActionDialogComponentStep action() {
		return DefaultActionDialogBuilder.ACTION_COMPONENT;
	}

	/**
	 * @return a new {@link CalendarDialogBuilder}
	 */
	public static CalendarDialogBuilder calendar() {
		return new DefaultCalendarDialogBuilder();
	}

	/**
	 * @return a {@link InputDialogBuilder.ComponentStep} for an input dialog
	 */
	public static InputDialogBuilder.ComponentStep input() {
		return DefaultInputDialogBuilder.COMPONENT;
	}

	/**
	 * Displays the given exception in a dialog
	 * @param exception the exception
	 * @param dialogParent the dialog parent window
	 */
	public static void displayException(Throwable exception, @Nullable Window dialogParent) {
		new DefaultExceptionDialogBuilder()
						.owner(dialogParent)
						.show(exception);
	}
}
