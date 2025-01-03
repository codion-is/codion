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
 * Copyright (c) 2022 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.laf.LookAndFeelProvider;

import javax.swing.JComponent;
import java.util.function.Consumer;

/**
 * Builds a dialog for selecting a look and feel.
 */
public interface LookAndFeelSelectionDialogBuilder {

	/**
	 * @param owner the dialog owner
	 * @return this builder
	 */
	LookAndFeelSelectionDialogBuilder owner(JComponent owner);

	/**
	 * @param enableOnSelection true if the Look and Feel should be enabled dynamically when selecting
	 * @return this builder
	 */
	LookAndFeelSelectionDialogBuilder enableOnSelection(boolean enableOnSelection);

	/**
	 * Displays a dialog allowing the user the select between all available Look and Feels.
	 * @param selectedLookAndFeel called when the OK button is pressed
	 */
	void selectLookAndFeel(Consumer<LookAndFeelProvider> selectedLookAndFeel);

	/**
	 * Creates a {@link Control} for selecting the Look and Feel.
	 * @param selectedLookAndFeel called when the OK button is pressed
	 * @return a Control for displaying a dialog for selecting a look and feel
	 */
	Control createControl(Consumer<LookAndFeelProvider> selectedLookAndFeel);
}
