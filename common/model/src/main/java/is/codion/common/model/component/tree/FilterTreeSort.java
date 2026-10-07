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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.model.component.tree;

import is.codion.common.model.filter.FilterModel;
import is.codion.common.model.filter.SortOrder;

import java.util.function.Function;

/**
 * <p>Handles the sorting of a {@link FilterTreeModel}, the comparator applied within each group of siblings.
 * <p>Unsorted, siblings are in the order the children function returned them.
 * @param <T> the item type
 */
public interface FilterTreeSort<T> extends FilterModel.Sort<T> {

	/**
	 * Compares by the comparator of the top level, the children of the root, in the current sort order, the comparator
	 * for each parent applying when sorting its children, see {@link FilterTreeModel.Builder#comparators(Function)}.
	 * @param item the item
	 * @param other the other item
	 * @return the comparison result, 0 when not sorted
	 */
	@Override
	int compare(T item, T other);

	/**
	 * Sorts ascending
	 */
	void ascending();

	/**
	 * Sorts descending
	 */
	void descending();

	/**
	 * Clears the sort, the siblings returning to the order the children function returned them
	 */
	void clear();

	/**
	 * @return the current sort order, {@link SortOrder#UNSORTED} in case the model has no comparator
	 */
	SortOrder order();
}
