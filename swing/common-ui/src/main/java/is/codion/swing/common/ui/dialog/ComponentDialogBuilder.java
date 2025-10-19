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

import is.codion.common.observer.Observer;
import is.codion.common.state.State;

import org.jspecify.annotations.Nullable;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JDialog;
import java.awt.Dimension;
import java.awt.event.WindowEvent;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A builder for JDialog containing a single component.
 */
public interface ComponentDialogBuilder extends DialogBuilder<ComponentDialogBuilder> {

	/**
	 * Note: sets the layout to {@link java.awt.BorderLayout} and
	 * adds the component at location {@link java.awt.BorderLayout#CENTER}
	 * @param component the component to display
	 * @return this builder instance
	 */
	ComponentDialogBuilder component(JComponent component);

	/**
	 * Note: sets the layout to {@link java.awt.BorderLayout} and
	 * adds the component at location {@link java.awt.BorderLayout#CENTER}
	 * @param component the component to display
	 * @return this builder instance
	 */
	ComponentDialogBuilder component(Supplier<? extends JComponent> component);

	/**
	 * @param modal true if the dialog should be modal, default true
	 * @return this builder instance
	 */
	ComponentDialogBuilder modal(boolean modal);

	/**
	 * @param resizable true if the dialog should be resizable, default true
	 * @return this builder instance
	 */
	ComponentDialogBuilder resizable(boolean resizable);

	/**
	 * @param size the size of the dialog
	 * @return this builder instance
	 */
	ComponentDialogBuilder size(@Nullable Dimension size);

	/**
	 * @param enterAction the action to associate with the ENTER key
	 * @return this builder instance
	 */
	ComponentDialogBuilder enterAction(Action enterAction);

	/**
	 * Sets the Observer which triggers the closing of the dialog, note that {@link #disposeOnEscape(boolean)}
	 * has no effect if a closeObserver is specified.
	 * @param closeObserver if specified the dialog will be disposed of when and only when this observer is notified
	 * @return this builder instance
	 */
	ComponentDialogBuilder closeObserver(@Nullable Observer<?> closeObserver);

	/**
	 * @param confirmCloseListener this listener, if specified, will be queried for confirmation before
	 * the dialog is closed, using the State instance to signal confirmation, the dialog
	 * will only be closed if that state is active after a call to {@link Consumer#accept(Object)}
	 * @return this builder instance
	 */
	ComponentDialogBuilder confirmCloseListener(@Nullable Consumer<State> confirmCloseListener);

	/**
	 * @param disposeOnEscape if yes then the dialog is disposed when the ESC button is pressed,
	 * has no effect if a <code>closeEvent</code> is specified
	 * @return this builder instance
	 */
	ComponentDialogBuilder disposeOnEscape(boolean disposeOnEscape);

	/**
	 * @param onShown called each time the dialog is shown
	 * @return this builder instance
	 */
	ComponentDialogBuilder onShown(Consumer<JDialog> onShown);

	/**
	 * @param onOpened called when dialog is opened
	 * @return this builder instance
	 */
	ComponentDialogBuilder onOpened(Consumer<WindowEvent> onOpened);

	/**
	 * @param onClosed called when dialog is closed
	 * @return this builder instance
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
