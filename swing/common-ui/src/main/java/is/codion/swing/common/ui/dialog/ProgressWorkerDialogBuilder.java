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

import is.codion.swing.common.model.worker.ProgressWorker;
import is.codion.swing.common.ui.control.Control;

import javax.swing.JPanel;
import javax.swing.border.Border;
import java.awt.Dimension;
import java.util.List;
import java.util.function.Consumer;

/**
 * A builder for a {@link ProgressWorker} implementation which displays a progress bar in a modal dialog
 * while background work is being performed.
 * The progress bar can be of type 'indeterminate' or with the progress ranging from 0 to 100.
 * @param <T> the type of result this {@link ProgressWorker} produces.
 * @param <V> the type of intermediate result this {@link ProgressWorker} produces.
 * @see ProgressWorker.ProgressResultTask#execute(ProgressWorker.ProgressReporter) to indicate work progress
 */
public interface ProgressWorkerDialogBuilder<T, V> extends DialogBuilder<ProgressWorkerDialogBuilder<T, V>> {

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
	ProgressWorkerDialogBuilder<T, V> border(Border border);

	/**
	 * @param northPanel if specified this panel will be added to the BorderLayout.NORTH position of the dialog
	 * @return this Builder instance
	 */
	ProgressWorkerDialogBuilder<T, V> northPanel(JPanel northPanel);

	/**
	 * @param westPanel if specified this panel will be added to the BorderLayout.WEST position of the dialog
	 * @return this Builder instance
	 */
	ProgressWorkerDialogBuilder<T, V> westPanel(JPanel westPanel);

	/**
	 * @param eastPanel if specified this panel will be added to the BorderLayout.EAST position of the dialog
	 * @return this Builder instance
	 */
	ProgressWorkerDialogBuilder<T, V> eastPanel(JPanel eastPanel);

	/**
	 * @param controlBuilder the control built by this builder will be added to the dialog as a button
	 * @return this Builder instance
	 */
	ProgressWorkerDialogBuilder<T, V> control(Control.Builder<?, ?> controlBuilder);

	/**
	 * @param control this control will be added to the dialog as a button
	 * @return this Builder instance
	 */
	ProgressWorkerDialogBuilder<T, V> control(Control control);

	/**
	 * @param progressBarSize the size of the progress bar
	 * @return this Builder instance
	 */
	ProgressWorkerDialogBuilder<T, V> progressBarSize(Dimension progressBarSize);

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
	 * @param resultMessage if specified then this message is displayed after the task has successfully run
	 * @return this Builder instance
	 */
	ProgressWorkerDialogBuilder<T, V> onResult(String resultMessage);

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
