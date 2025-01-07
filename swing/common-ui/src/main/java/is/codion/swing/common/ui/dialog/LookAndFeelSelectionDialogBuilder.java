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

import is.codion.common.Configuration;
import is.codion.common.property.PropertyValue;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.laf.LookAndFeelEnabler;

import javax.swing.JComponent;
import java.util.function.Consumer;

/**
 * Builds a dialog for selecting a look and feel.
 */
public interface LookAndFeelSelectionDialogBuilder {

	/**
	 * <p>Specifies whether to include the platform look and feels in the selection combo box by default, if auxiliary ones are provided.
	 * <p>Note that this has no effect if only the platform look and feels are provided.
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: false
	 * </ul>
	 * @see is.codion.swing.common.ui.laf.LookAndFeelProvider
	 */
	PropertyValue<Boolean> INCLUDE_PLATFORM_LOOK_AND_FEELS =
					Configuration.booleanValue(LookAndFeelSelectionDialogBuilder.class.getName() + ".includePlatformLookAndFeels", false);

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
	 * @param includePlatformLookAndFeels true if the platform look and feels should be included by default in the selection combo box
	 * @return this builder
	 * @see #INCLUDE_PLATFORM_LOOK_AND_FEELS
	 */
	LookAndFeelSelectionDialogBuilder includePlatformLookAndFeels(boolean includePlatformLookAndFeels);

	/**
	 * Displays a dialog allowing the user the select between all available Look and Feels.
	 * @param selectedLookAndFeel called when the OK button is pressed
	 */
	void selectLookAndFeel(Consumer<LookAndFeelEnabler> selectedLookAndFeel);

	/**
	 * Creates a {@link Control} for selecting the Look and Feel.
	 * @param selectedLookAndFeel called when the OK button is pressed
	 * @return a Control for displaying a dialog for selecting a look and feel
	 */
	Control createControl(Consumer<LookAndFeelEnabler> selectedLookAndFeel);
}
