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

import is.codion.swing.common.ui.control.Control;

import javax.swing.JComponent;
import java.util.OptionalInt;

/**
 * Builds a dialog for selecting the font size.
 */
public interface FontSizeSelectionDialogBuilder {

	/**
	 * @param owner the dialog owner
	 * @return this builder
	 */
	FontSizeSelectionDialogBuilder owner(JComponent owner);

	/**
	 * Displays a dialog allowing the user the select a font size multiplier.
	 * @return the selected font size multiplier, an empty Optional if cancelled
	 */
	OptionalInt selectFontSize();

	/**
	 * Creates a {@link Control} for selecting the font size.
	 * @return a Control for displaying a dialog for selecting a font size
	 */
	Control createControl();
}
