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
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.dialog;

import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.border.Border;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.function.Consumer;

/**
 * Builds a dialog with a button panel based on actions.
 */
public interface ActionDialogBuilder<B extends ActionDialogBuilder<B>> extends DialogBuilder<B> {

	/**
	 * @param action the action to add
	 * @return this builder instance
	 */
	B action(Action action);

	/**
	 * A default action is triggered by the Enter key
	 * @param defaultAction the default action to add
	 * @return this builder instance
	 */
	B defaultAction(Action defaultAction);

	/**
	 * An escape action is triggered by the Escape key
	 * @param escapeAction the escape action to add
	 * @return this builder instance
	 */
	B escapeAction(Action escapeAction);

	/**
	 * @param modal true if the dialog should be modal
	 * @return this builder instance
	 */
	B modal(boolean modal);

	/**
	 * @param resizable true if the dialog should be resizable
	 * @return this builder instance
	 */
	B resizable(boolean resizable);

	/**
	 * @param size the size of the dialog
	 * @return this builder instance
	 */
	B size(Dimension size);

	/**
	 * Default {@link FlowLayout#TRAILING}
	 * @param buttonPanelConstraints the {@link FlowLayout} panel constraints for the button panel
	 * @return this builder instance
	 */
	B buttonPanelConstraints(int buttonPanelConstraints);

	/**
	 * @param buttonPanelBorder the button panel border
	 * @return this builder instance
	 */
	B buttonPanelBorder(Border buttonPanelBorder);

	/**
	 * @param onShown called each time the dialog is shown
	 * @return this builder instance
	 */
	B onShown(Consumer<JDialog> onShown);

	/**
	 * Builds and shows the dialog.
	 * @return a new JDialog instance based on this builder.
	 * @throws IllegalStateException in case no controls have been specified
	 */
	JDialog show();

	/**
	 * @return a new JDialog instance based on this builder.
	 * @throws IllegalStateException in case no controls have been specified
	 */
	JDialog build();
}
