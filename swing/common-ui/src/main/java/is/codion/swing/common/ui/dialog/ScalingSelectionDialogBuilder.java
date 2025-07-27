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

import is.codion.swing.common.ui.control.Control;

import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import java.util.function.Consumer;

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
	 * @param initialSelection the initally selected font size ratio, default 100%
	 * @return this builder
	 */
	ScalingSelectionDialogBuilder initialSelection(int initialSelection);

	/**
	 * Displays a dialog allowing the user the select a scaling multiplier.
	 * @param scalingSelected called when the OK button is pressed
	 */
	void selectScaling(Consumer<Integer> scalingSelected);

	/**
	 * Creates a {@link Control} for selecting the scaling.
	 * @param scalingSelected called when the OK button is pressed
	 * @return a Control for displaying a dialog for selecting a scaling
	 */
	Control createControl(Consumer<Integer> scalingSelected);
}
