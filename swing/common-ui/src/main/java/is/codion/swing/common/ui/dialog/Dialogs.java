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

import static java.util.Objects.requireNonNull;

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
	 * @param task the task to run
	 * @return a new indeterminate {@link ProgressWorkerDialogBuilder} instance
	 */
	public static ProgressWorkerDialogBuilder<?, ?> progressWorker(Task task) {
		return new DefaultProgressWorkerDialogBuilder<>(requireNonNull(task));
	}

	/**
	 * @param task the task to run
	 * @param <T> the worker result type
	 * @return a new indeterminate {@link ProgressWorkerDialogBuilder} instance
	 */
	public static <T> ProgressWorkerDialogBuilder<T, ?> progressWorker(ResultTask<T> task) {
		return new DefaultProgressWorkerDialogBuilder<>(requireNonNull(task));
	}

	/**
	 * Note, also sets the progress bar type to 'determinate'.
	 * @param task the task to run
	 * @param <V> the worker intermediate result type
	 * @return a new determinate {@link ProgressWorkerDialogBuilder} instance
	 * @see ProgressWorkerDialogBuilder#indeterminate(boolean)
	 */
	public static <V> ProgressWorkerDialogBuilder<?, V> progressWorker(ProgressTask<V> task) {
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
	public static <T, V> ProgressWorkerDialogBuilder<T, V> progressWorker(ProgressResultTask<T, V> task) {
		requireNonNull(task);

		return new DefaultProgressWorkerDialogBuilder<>(task).indeterminate(false);
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
		return new DefaultSelectionDialogBuilderFactory();
	}

	/**
	 * @return a new OkCancelDialogBuilder
	 */
	public static OkCancelDialogBuilder okCancel() {
		return new DefaultOkCancelDialogBuilder();
	}

	/**
	 * @param <B> the builder type
	 * @return a new ActionDialogBuilder
	 */
	public static <B extends ActionDialogBuilder<B>> ActionDialogBuilder<B> action() {
		return new DefaultActionDialogBuilder<>();
	}

	/**
	 * @return a new CalendarDialogBuilder
	 */
	public static CalendarDialogBuilder calendar() {
		return new DefaultCalendarDialogBuilder();
	}

	/**
	 * @param componentBuilder the builder which component to display
	 * @param <T> the value type
	 * @return a builder for an input dialog
	 */
	public static <T> InputDialogBuilder<T> input(ComponentBuilder<T, ?, ?> componentBuilder) {
		return new DefaultInputDialogBuilder<>(requireNonNull(componentBuilder).buildValue());
	}

	/**
	 * @param componentValue the value which component to display
	 * @param <T> the value type
	 * @return a builder for an input dialog
	 */
	public static <T> InputDialogBuilder<T> input(ComponentValue<T, ?> componentValue) {
		return new DefaultInputDialogBuilder<>(componentValue);
	}

	/**
	 * Displays the given exception in a dialog
	 * @param exception the exception
	 * @param dialogParent the dialog parent window
	 */
	public static void displayException(Throwable exception, Window dialogParent) {
		new DefaultExceptionDialogBuilder()
						.owner(dialogParent)
						.show(exception);
	}
}
