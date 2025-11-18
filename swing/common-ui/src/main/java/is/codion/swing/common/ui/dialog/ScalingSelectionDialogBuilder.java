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
 * Copyright (c) 2023 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.model.CancelException;

import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;

/**
 * Builds a dialog for selecting the scaling.
 */
public interface ScalingSelectionDialogBuilder {

	/**
	 * @param owner the dialog owner
	 * @return this builder
	 */
	ScalingSelectionDialogBuilder owner(@Nullable JComponent owner);

	/**
	 * Displays a dialog allowing the user the select a scaling multiplier.
	 * @param title the dialog title
	 * @return the selected scaling
	 * @throws CancelException in case of cancel
	 */
	int show(String title);
}
