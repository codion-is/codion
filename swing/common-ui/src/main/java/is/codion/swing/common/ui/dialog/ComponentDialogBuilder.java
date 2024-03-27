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

import is.codion.common.event.EventObserver;
import is.codion.common.state.State;

import javax.swing.Action;
import javax.swing.JDialog;
import java.awt.Dimension;
import java.awt.event.WindowEvent;
import java.util.function.Consumer;

/**
 * A builder for JDialog containing a single component.
 */
public interface ComponentDialogBuilder extends DialogBuilder<ComponentDialogBuilder> {

	/**
	 * @param modal true if the dialog should be modal
	 * @return this DialogBuilder instance
	 */
	ComponentDialogBuilder modal(boolean modal);

	/**
	 * @param resizable true if the dialog should be resizable
	 * @return this DialogBuilder instance
	 */
	ComponentDialogBuilder resizable(boolean resizable);

	/**
	 * @param size the size of the dialog
	 * @return this DialogBuilder instance
	 */
	ComponentDialogBuilder size(Dimension size);

	/**
	 * @param enterAction the action to associate with the ENTER key
	 * @return this DialogBuilder instance
	 */
	ComponentDialogBuilder enterAction(Action enterAction);

	/**
	 * Sets the Event which triggers the closing of the dialog, note that {@link #disposeOnEscape(boolean)}
	 * has no effect if the closeEvent is specified.
	 * @param closeEvent if specified the dialog will be disposed of when and only when this event occurs
	 * @return this DialogBuilder instance
	 */
	ComponentDialogBuilder closeEvent(EventObserver<?> closeEvent);

	/**
	 * @param confirmCloseListener this listener, if specified, will be queried for confirmation before
	 * the dialog is closed, using the State instance to signal confirmation, the dialog
	 * will only be closed if that state is active after a call to {@link Consumer#accept(Object)}
	 * @return this DialogBuilder instance
	 */
	ComponentDialogBuilder confirmCloseListener(Consumer<State> confirmCloseListener);

	/**
	 * @param disposeOnEscape if yes then the dialog is disposed when the ESC button is pressed,
	 * has no effect if a <code>closeEvent</code> is specified
	 * @return this DialogBuilder instance
	 */
	ComponentDialogBuilder disposeOnEscape(boolean disposeOnEscape);

	/**
	 * @param onShown called each time the dialog is shown
	 * @return this builder instance
	 */
	ComponentDialogBuilder onShown(Consumer<JDialog> onShown);

	/**
	 * @param onOpened called when dialog is opened
	 * @return this DialogBuilder instance
	 */
	ComponentDialogBuilder onOpened(Consumer<WindowEvent> onOpened);

	/**
	 * @param onClosed called when dialog is closed
	 * @return this DialogBuilder instance
	 */
	ComponentDialogBuilder onClosed(Consumer<WindowEvent> onClosed);

	/**
	 * @return a new JDialog instance based on this builder.
	 */
	JDialog build();

	/**
	 * Builds and shows the dialog.
	 * @return a new JDialog instance based on this builder.
	 */
	JDialog show();
}
