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
 * Copyright (c) 2021 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.dialog;

import org.jspecify.annotations.Nullable;

import java.awt.Dimension;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

/**
 * A builder for a {@link javax.swing.JList} based selection dialog.
 * @param <T> the value type
 */
public interface ListSelectionDialogBuilder<T> extends SelectionDialogBuilder<T, ListSelectionDialogBuilder<T>> {

	/**
	 * @param defaultSelection the item selected by default
	 * @return this builder instance
	 * @throws IllegalArgumentException in case the selection values do not contain the default selection item
	 */
	ListSelectionDialogBuilder<T> defaultSelection(T defaultSelection);

	/**
	 * @param defaultSelection the items selected by default
	 * @return this builder instance
	 * @throws IllegalArgumentException in case the selection values do not contain the default selection items
	 */
	ListSelectionDialogBuilder<T> defaultSelection(Collection<T> defaultSelection);

	/**
	 * @param dialogSize the preferred dialog size
	 * @return this builder instance
	 */
	ListSelectionDialogBuilder<T> dialogSize(@Nullable Dimension dialogSize);

	/**
	 * @param comparator the comparator to use to sort the items
	 * @return this builder instance
	 */
	ListSelectionDialogBuilder<T> comparator(@Nullable Comparator<T> comparator);

	/**
	 * @return a {@link SelectionStep}
	 */
	SelectionStep<T> select();

	/**
	 * Provides selection for single or multiple items.
	 */
	interface SelectionStep<T> {

		/**
		 * @return the selected value, {@link Optional#empty()} if none was selected
		 * @throws is.codion.common.model.CancelException in case the user cancelled
		 */
		Optional<T> single();

		/**
		 * @return the selected values, an empty Collection if none was selected
		 * @throws is.codion.common.model.CancelException in case the user cancelled
		 */
		Collection<T> multiple();
	}
}
