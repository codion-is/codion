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

import java.util.Collection;

/**
 * A builder for a selection dialog.
 * @param <T> the value type
 */
public interface SelectionDialogBuilder<T, B extends SelectionDialogBuilder<T, B>> extends DialogBuilder<B> {

	/**
	 * @param defaultSelection the item selected by default
	 * @return this SelectionDialogBuilder instance
	 * @throws IllegalArgumentException in case the selection values do not contain the default selection item
	 */
	B defaultSelection(T defaultSelection);

	/**
	 * @param defaultSelection the items selected by default
	 * @return this SelectionDialogBuilder instance
	 * @throws IllegalArgumentException in case the selection values do not contain the default selection items
	 */
	B defaultSelection(Collection<T> defaultSelection);

	/**
	 * @param allowEmptySelection if true then the dialog accepts an empty selection, default false
	 * @return this SelectionDialogBuilder instance
	 */
	B allowEmptySelection(boolean allowEmptySelection);
}
