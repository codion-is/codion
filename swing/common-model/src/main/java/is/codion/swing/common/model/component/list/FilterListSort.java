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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.model.component.list;

import is.codion.common.model.filter.FilterModel;

/**
 * Handles the column sorting states for a {@link FilterListModel}.
 * @param <T> the type representing a row in the table model
 */
public interface FilterListSort<T> extends FilterModel.Sort<T> {

	/**
	 * Sorts ascending
	 */
	void ascending();

	/**
	 * Sorts descending
	 */
	void descending();

	/**
	 * Clears the sort
	 */
	void clear();
}
