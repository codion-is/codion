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
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.state.StateObserver;

import javax.swing.Action;

/**
 * Builds a modal dialog for displaying the given {@code component},
 * with OK and Cancel buttons based on the given actions.
 * An OK action must be provided and the default Cancel action simply disposes the dialog.
 */
public interface OkCancelDialogBuilder extends ActionDialogBuilder<OkCancelDialogBuilder> {

	/**
	 * Note that this is overridden by {@link #okAction(Action)}.
	 * @param okEnabled the state observer controlling the ok enabled state
	 * @return this builder instance
	 * @throws IllegalStateException in case an ok action has already been set
	 */
	OkCancelDialogBuilder okEnabled(StateObserver okEnabled);

	/**
	 * Note that this is overridden by {@link #cancelAction(Action)}.
	 * @param cancelEnabled the state observer controlling the cancel enabled state
	 * @return this builder instance
	 * @throws IllegalStateException in case a cancel action has already been set
	 */
	OkCancelDialogBuilder cancelEnabled(StateObserver cancelEnabled);

	/**
	 * @param onOk called on ok pressed, before the dialog has been disposed
	 * @return this builder instance
	 * @throws IllegalStateException in case an ok action has already been set
	 */
	OkCancelDialogBuilder onOk(Runnable onOk);

	/**
	 * @param onCancel called on cancel pressed, before the dialog has been disposed
	 * @return this builder instance
	 * @throws IllegalStateException in case a cancel action has already been set
	 */
	OkCancelDialogBuilder onCancel(Runnable onCancel);

	/**
	 * @param okAction the action for the OK button, this action must dispose the dialog
	 * @return this builder instance
	 */
	OkCancelDialogBuilder okAction(Action okAction);

	/**
	 * @param cancelAction the action for the Cancel button
	 * @return this builder instance
	 */
	OkCancelDialogBuilder cancelAction(Action cancelAction);
}
