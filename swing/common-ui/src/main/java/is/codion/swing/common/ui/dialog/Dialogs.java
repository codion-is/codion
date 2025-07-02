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
 * Copyright (c) 2020 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.swing.common.model.worker.ProgressWorker.ProgressResultTask;
import is.codion.swing.common.model.worker.ProgressWorker.ProgressTask;
import is.codion.swing.common.model.worker.ProgressWorker.ResultTask;
import is.codion.swing.common.model.worker.ProgressWorker.Task;
import is.codion.swing.common.ui.component.builder.ComponentBuilder;
import is.codion.swing.common.ui.component.value.ComponentValue;

import java.awt.Window;
import java.util.Collection;

import static java.util.Objects.requireNonNull;

/**
 * A utility class for displaying Dialogs.
 */
public final class Dialogs {

	private Dialogs() {}

	/**
	 * @return a new {@link ComponentDialogBuilder} instance.
	 */
	public static ComponentDialogBuilder dialog() {
		return new DefaultComponentDialogBuilder();
	}

	/**
	 * @return a new {@link ProgressDialog.Builder} instance.
	 */
	public static ProgressDialog.Builder progressDialog() {
		return new ProgressDialog.DefaultBuilder();
	}

	/**
	 * @param task the task to run
	 * @return a new indeterminate {@link ProgressWorkerDialogBuilder} instance
	 */
	public static ProgressWorkerDialogBuilder<?, ?> progressWorkerDialog(Task task) {
		return new DefaultProgressWorkerDialogBuilder<>(requireNonNull(task));
	}

	/**
	 * @param task the task to run
	 * @param <T> the worker result type
	 * @return a new indeterminate {@link ProgressWorkerDialogBuilder} instance
	 */
	public static <T> ProgressWorkerDialogBuilder<T, ?> progressWorkerDialog(ResultTask<T> task) {
		return new DefaultProgressWorkerDialogBuilder<>(requireNonNull(task));
	}

	/**
	 * Note, also sets the progress bar type to 'determinate'.
	 * @param task the task to run
	 * @param <V> the worker intermediate result type
	 * @return a new determinate {@link ProgressWorkerDialogBuilder} instance
	 * @see ProgressWorkerDialogBuilder#indeterminate(boolean)
	 */
	public static <V> ProgressWorkerDialogBuilder<?, V> progressWorkerDialog(ProgressTask<V> task) {
		return new DefaultProgressWorkerDialogBuilder<>(requireNonNull(task)).indeterminate(false);
	}

	/**
	 * Note, also sets the progress bar type to 'determinate'.
	 * @param task the task to run
	 * @param <T> the worker result type
	 * @param <V> the worker intermediate result type
	 * @return a new determinate {@link ProgressWorkerDialogBuilder} instance
	 * @see ProgressWorkerDialogBuilder#indeterminate(boolean)
	 */
	public static <T, V> ProgressWorkerDialogBuilder<T, V> progressWorkerDialog(ProgressResultTask<T, V> task) {
		requireNonNull(task);

		return new DefaultProgressWorkerDialogBuilder<>(task).indeterminate(false);
	}

	/**
	 * @return a new login dialog builder
	 */
	public static LoginDialogBuilder loginDialog() {
		return new DefaultLoginDialogBuilder();
	}

	/**
	 * @return a new exception dialog builder
	 */
	public static ExceptionDialogBuilder exceptionDialog() {
		return new DefaultExceptionDialogBuilder();
	}

	/**
	 * @return a new FileSelectionDialogBuilder
	 */
	public static FileSelectionDialogBuilder fileSelectionDialog() {
		return new DefaultFileSelectionDialogBuilder();
	}

	/**
	 * @param values the values to select from
	 * @param <T> the value type
	 * @return a new {@link javax.swing.JList} based selection dialog builder
	 * @throws IllegalArgumentException in case values is empty
	 */
	public static <T> ListSelectionDialogBuilder<T> listSelectionDialog(Collection<T> values) {
		return new DefaultListSelectionDialogBuilder<>(values);
	}

	/**
	 * @param values the values to select from
	 * @param <T> the value type
	 * @return a new {@link javax.swing.JComboBox} based selection dialog builder
	 * @throws IllegalArgumentException in case values is empty
	 */
	public static <T> ComboBoxSelectionDialogBuilder<T> comboBoxSelectionDialog(Collection<T> values) {
		return new DefaultComboBoxSelectionDialogBuilder<>(values);
	}

	/**
	 * @return a new OkCancelDialogBuilder
	 */
	public static OkCancelDialogBuilder okCancelDialog() {
		return new DefaultOkCancelDialogBuilder();
	}

	/**
	 * @param <B> the builder type
	 * @return a new ActionDialogBuilder
	 */
	public static <B extends ActionDialogBuilder<B>> ActionDialogBuilder<B> actionDialog() {
		return new DefaultActionDialogBuilder<>();
	}

	/**
	 * @return a new CalendarDialogBuilder
	 */
	public static CalendarDialogBuilder calendarDialog() {
		return new DefaultCalendarDialogBuilder();
	}

	/**
	 * @return a builder for a dialog for selecting a look and feel
	 */
	public static LookAndFeelSelectionDialogBuilder lookAndFeelSelectionDialog() {
		return new DefaultLookAndFeelSelectionDialogBuilder();
	}

	/**
	 * @return a builder for a dialog for selecting the font size
	 */
	public static FontSizeSelectionDialogBuilder fontSizeSelectionDialog() {
		return new DefaultFontSizeSelectionDialogBuilder();
	}

	/**
	 * @param componentBuilder the builder which component to display
	 * @param <T> the value type
	 * @return a builder for an input dialog
	 */
	public static <T> InputDialogBuilder<T> inputDialog(ComponentBuilder<T, ?, ?> componentBuilder) {
		return new DefaultInputDialogBuilder<>(requireNonNull(componentBuilder).buildValue());
	}

	/**
	 * @param componentValue the value which component to display
	 * @param <T> the value type
	 * @return a builder for an input dialog
	 */
	public static <T> InputDialogBuilder<T> inputDialog(ComponentValue<T, ?> componentValue) {
		return new DefaultInputDialogBuilder<>(componentValue);
	}

	/**
	 * Displays the given exception in a dialog
	 * @param exception the exception
	 * @param dialogParent the dialog parent window
	 */
	public static void displayExceptionDialog(Throwable exception, Window dialogParent) {
		new DefaultExceptionDialogBuilder()
						.owner(dialogParent)
						.show(exception);
	}
}
