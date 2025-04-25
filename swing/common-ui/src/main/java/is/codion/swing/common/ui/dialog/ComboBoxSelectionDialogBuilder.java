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

import java.util.Optional;

/**
 * A builder for a {@link javax.swing.JComboBox} based selection dialog.
 * @param <T> the value type
 */
public interface ComboBoxSelectionDialogBuilder<T> extends SelectionDialogBuilder<T, ComboBoxSelectionDialogBuilder<T>> {

	/**
	 * @return the selected value, {@link Optional#empty()} if none was selected
	 * @throws is.codion.common.model.CancelException in case the user cancelled
	 */
	Optional<T> select();
}
