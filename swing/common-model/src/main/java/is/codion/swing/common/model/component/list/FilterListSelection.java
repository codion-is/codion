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
package is.codion.swing.common.model.component.list;

import is.codion.common.model.filter.FilterModel.VisibleItems;
import is.codion.common.model.selection.MultiSelection;

import javax.swing.ListSelectionModel;

/**
 * A {@link ListSelectionModel}
 * @param <T> the list item type
 */
public interface FilterListSelection<T> extends ListSelectionModel, MultiSelection<T> {

	/**
	 * Instantiates a new {@link FilterListSelection} instance based on the given items
	 * @param items the {@link VisibleItems} to select from
	 * @param <T> the list item type
	 * @return a new {@link FilterListSelection} instance
	 */
	static <T> FilterListSelection<T> filterListSelection(VisibleItems<T> items) {
		return new DefaultListSelection<>(items);
	}
}
