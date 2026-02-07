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
 * Copyright (c) 2021 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.reactive.state.ObservableState;

import javax.swing.Action;
import javax.swing.JComponent;
import java.util.function.Supplier;

/**
 * Builds a modal dialog for displaying the given {@code component},
 * with OK and Cancel buttons based on the given actions.
 * An OK action must be provided and the default Cancel action simply disposes the dialog.
 */
public interface OkCancelDialogBuilder extends ActionDialogBuilder<OkCancelDialogBuilder> {

	/**
	 * Specifies the component to display in the dialog.
	 */
	interface OkCancelDialogComponentStep {

		/**
		 * Note: sets the layout to {@link java.awt.BorderLayout} and
		 * adds the component at location {@link java.awt.BorderLayout#CENTER}
		 * @param component the component to display
		 * @return this builder instance
		 */
		OkCancelDialogBuilder component(JComponent component);

		/**
		 * Note: sets the layout to {@link java.awt.BorderLayout} and
		 * adds the component at location {@link java.awt.BorderLayout#CENTER}
		 * @param component supplies the component to display
		 * @return this builder instance
		 */
		OkCancelDialogBuilder component(Supplier<? extends JComponent> component);
	}

	/**
	 * Note that this is overridden by {@link #okAction(Action)}.
	 * @param okEnabled the state observer controlling the ok enabled state
	 * @return this builder instance
	 * @throws IllegalStateException in case {@link #okAction(Action)} has already been set
	 */
	OkCancelDialogBuilder okEnabled(ObservableState okEnabled);

	/**
	 * Note that this is overridden by {@link #cancelAction(Action)}.
	 * @param cancelEnabled the state observer controlling the cancel enabled state
	 * @return this builder instance
	 * @throws IllegalStateException in case {@link #cancelAction(Action)} has already been set
	 */
	OkCancelDialogBuilder cancelEnabled(ObservableState cancelEnabled);

	/**
	 * @param onOk called on ok pressed, before the dialog has been disposed
	 * @return this builder instance
	 * @throws IllegalStateException in case {@link #okAction(Action)} has already been set
	 */
	OkCancelDialogBuilder onOk(Runnable onOk);

	/**
	 * @param onCancel called on cancel pressed, before the dialog has been disposed
	 * @return this builder instance
	 * @throws IllegalStateException in case {@link #cancelAction(Action)} has already been set
	 */
	OkCancelDialogBuilder onCancel(Runnable onCancel);

	/**
	 * Note that this action is responsible for closing the dialog.
	 * @param okAction the action for the OK button, this action must dispose the dialog
	 * @return this builder instance
	 * @throws IllegalStateException in case {@link #onOk(Runnable)} has already been set
	 */
	OkCancelDialogBuilder okAction(Action okAction);

	/**
	 * Note that this action is responsible for closing the dialog.
	 * @param cancelAction the action for the Cancel button, this action must dispose the dialog
	 * @return this builder instance
	 * @throws IllegalStateException in case {@link #onCancel(Runnable)} has already been set
	 */
	OkCancelDialogBuilder cancelAction(Action cancelAction);
}
