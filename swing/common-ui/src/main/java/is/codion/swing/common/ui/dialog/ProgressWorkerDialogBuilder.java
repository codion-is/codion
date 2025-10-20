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

import is.codion.common.property.PropertyValue;
import is.codion.swing.common.model.worker.ProgressWorker;
import is.codion.swing.common.ui.control.Control;

import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.border.Border;
import java.awt.Dimension;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static is.codion.common.Configuration.integerValue;

/**
 * A builder for a {@link ProgressWorker} implementation which displays a progress bar in a modal dialog
 * while background work is being performed.
 * The progress bar can be of type 'indeterminate' or with the progress ranging from 0 to 100.
 * <p>
 * By default, the progress dialog uses a delayed show/hide mechanism to prevent flickering
 * for fast operations:
 * <ul>
 * <li>The dialog is only shown if the operation takes longer than {@link ProgressWorkerDialogBuilder#SHOW_DELAY} milliseconds (default: 350ms)
 * <li>If shown, the dialog remains visible for at least {@link ProgressWorkerDialogBuilder#HIDE_DELAY} milliseconds (default: 800ms)
 * </ul>
 * These delays can be customized via {@link #delay(int, int)} or the system properties.
 * @param <T> the type of result this {@link ProgressWorker} produces.
 * @param <V> the type of intermediate result this {@link ProgressWorker} produces.
 * @see ProgressWorker.ProgressResultTask#execute(ProgressWorker.ProgressReporter) to indicate work progress
 * @see #delay(int, int)
 */
public interface ProgressWorkerDialogBuilder<T, V> extends DialogBuilder<ProgressWorkerDialogBuilder<T, V>> {

	/**
	 * Specifies the delay in milliseconds before showing a progress dialog.
	 * Progress dialogs are only shown if the operation takes longer than this delay,
	 * preventing dialog flicker for fast operations.
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: 350
	 * </ul>
	 * @see #HIDE_DELAY
	 */
	PropertyValue<Integer> SHOW_DELAY =
					integerValue(ProgressWorkerDialogBuilder.class.getName() + ".showDelay", 350);

	/**
	 * Specifies the minimum duration in milliseconds that a progress dialog should remain visible.
	 * If a progress dialog is shown, it will remain visible for at least this duration
	 * even if the operation completes faster, preventing dialog flicker.
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: 800
	 * </ul>
	 * @see #SHOW_DELAY
	 */
	PropertyValue<Integer> HIDE_DELAY =
					integerValue(ProgressWorkerDialogBuilder.class.getName() + ".hideDelay", 800);

	/**
	 * Provides builders for a given task type.
	 */
	interface BuilderFactory {

		/**
		 * @param task the task to run
		 * @return a new indeterminate {@link ProgressWorkerDialogBuilder} instance
		 */
		ProgressWorkerDialogBuilder<?, ?> task(ProgressWorker.Task task);

		/**
		 * @param task the task to run
		 * @param <T> the worker result type
		 * @return a new indeterminate {@link ProgressWorkerDialogBuilder} instance
		 */
		<T> ProgressWorkerDialogBuilder<T, ?> task(ProgressWorker.ResultTask<T> task);

		/**
		 * Note, also sets the progress bar type to 'determinate'.
		 * @param task the task to run
		 * @param <V> the worker intermediate result type
		 * @return a new determinate {@link ProgressWorkerDialogBuilder} instance
		 * @see ProgressWorkerDialogBuilder#indeterminate(boolean)
		 */
		<V> ProgressWorkerDialogBuilder<?, V> task(ProgressWorker.ProgressTask<V> task);

		/**
		 * Note, also sets the progress bar type to 'determinate'.
		 * @param task the task to run
		 * @param <T> the worker result type
		 * @param <V> the worker intermediate result type
		 * @return a new determinate {@link ProgressWorkerDialogBuilder} instance
		 * @see ProgressWorkerDialogBuilder#indeterminate(boolean)
		 */
		<T, V> ProgressWorkerDialogBuilder<T, V> task(ProgressWorker.ProgressResultTask<T, V> task);
	}

	/**
	 * @param indeterminate true if the progress bar should be indeterminate
	 * @return this Builder instance
	 */
	ProgressWorkerDialogBuilder<T, V> indeterminate(boolean indeterminate);

	/**
	 * Note that calling this method renders the progress bar determinate
	 * @param maximum the maximum progress, 100 by default
	 * @return this Builder instance
	 * @see #indeterminate(boolean)
	 */
	ProgressWorkerDialogBuilder<T, V> maximum(int maximum);

	/**
	 * @param stringPainted the string painted status of the progress bar
	 * @return this Builder instance
	 */
	ProgressWorkerDialogBuilder<T, V> stringPainted(boolean stringPainted);

	/**
	 * @param border the border to add around the progress bar
	 * @return this Builder instance
	 */
	ProgressWorkerDialogBuilder<T, V> border(@Nullable Border border);

	/**
	 * @param northComponent if specified this component will be added to the BorderLayout.NORTH position of the dialog
	 * @return this Builder instance
	 */
	ProgressWorkerDialogBuilder<T, V> northComponent(@Nullable JComponent northComponent);

	/**
	 * @param westComponent if specified this component will be added to the BorderLayout.WEST position of the dialog
	 * @return this Builder instance
	 */
	ProgressWorkerDialogBuilder<T, V> westComponent(@Nullable JComponent westComponent);

	/**
	 * @param eastComponent if specified this component will be added to the BorderLayout.EAST position of the dialog
	 * @return this Builder instance
	 */
	ProgressWorkerDialogBuilder<T, V> eastComponent(@Nullable JComponent eastComponent);

	/**
	 * @param control the control to be added to the dialog as a button
	 * @return this Builder instance
	 */
	ProgressWorkerDialogBuilder<T, V> control(Supplier<? extends Control> control);

	/**
	 * @param control this control will be added to the dialog as a button
	 * @return this Builder instance
	 */
	ProgressWorkerDialogBuilder<T, V> control(Control control);

	/**
	 * @param progressBarSize the size of the progress bar
	 * @return this Builder instance
	 */
	ProgressWorkerDialogBuilder<T, V> progressBarSize(@Nullable Dimension progressBarSize);

	/**
	 * Configures dialog delay settings to prevent flicker for fast operations.
	 * The dialog will only be shown if the operation takes longer than {@code show} milliseconds.
	 * If the dialog is shown, it will remain visible for at least {@code close} milliseconds
	 * even if the operation completes faster.
	 * @param show the delay in milliseconds before showing the dialog
	 * @param hide the minimum duration in milliseconds to keep the dialog visible once shown
	 * @return this Builder instance
	 * @see ProgressWorkerDialogBuilder#SHOW_DELAY
	 * @see ProgressWorkerDialogBuilder#HIDE_DELAY
	 */
	ProgressWorkerDialogBuilder<T, V> delay(int show, int hide);

	/**
	 * @param onPublish called on the Event Dispatch Thread when chunks are available for publishing
	 * @return this builder instance
	 */
	ProgressWorkerDialogBuilder<T, V> onPublish(Consumer<List<V>> onPublish);

	/**
	 * @param onResult executed on the Event Dispatch Thread after a successful run
	 * @return this Builder instance
	 */
	ProgressWorkerDialogBuilder<T, V> onResult(Runnable onResult);

	/**
	 * @param onResult executed on the Event Dispatch Thread after a successful run
	 * @return this Builder instance
	 */
	ProgressWorkerDialogBuilder<T, V> onResult(Consumer<T> onResult);

	/**
	 * @param title the dialog title
	 * @param message if specified then this message is displayed after the task has successfully run
	 * @return this Builder instance
	 */
	ProgressWorkerDialogBuilder<T, V> onResult(String title, String message);

	/**
	 * @param onException the exception handler
	 * @return this Builder instance
	 */
	ProgressWorkerDialogBuilder<T, V> onException(Consumer<Exception> onException);

	/**
	 * @param exceptionTitle the title of the exception dialog
	 * @return this Builder instance
	 */
	ProgressWorkerDialogBuilder<T, V> onException(String exceptionTitle);

	/**
	 * Builds and executes a new {@link ProgressWorker} based on this builder
	 * @return a {@link ProgressWorker} based on this builder
	 */
	ProgressWorker<T, V> execute();

	/**
	 * @return a {@link ProgressWorker} based on this builder
	 */
	ProgressWorker<T, V> build();
}
