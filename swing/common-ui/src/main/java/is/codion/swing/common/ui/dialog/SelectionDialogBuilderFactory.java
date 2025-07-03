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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.dialog;

import java.util.Collection;

/**
 * A factory for selection builders.
 */
public interface SelectionDialogBuilderFactory {

	/**
	 * @return a new FileSelectionDialogBuilder
	 */
	FileSelectionDialogBuilder files();

	/**
	 * @param values the values to select from
	 * @param <T> the value type
	 * @return a new {@link javax.swing.JList} based selection dialog builder
	 * @throws IllegalArgumentException in case values is empty
	 */
	<T> ListSelectionDialogBuilder<T> list(Collection<T> values);

	/**
	 * @param values the values to select from
	 * @param <T> the value type
	 * @return a new {@link javax.swing.JComboBox} based selection dialog builder
	 * @throws IllegalArgumentException in case values is empty
	 */
	<T> ComboBoxSelectionDialogBuilder<T> comboBox(Collection<T> values);

	/**
	 * @return a builder for a dialog for selecting a look and feel
	 */
	LookAndFeelSelectionDialogBuilder lookAndFeel();

	/**
	 * @return a builder for a dialog for selecting the scaling
	 */
	ScalingSelectionDialogBuilder scaling();
}
