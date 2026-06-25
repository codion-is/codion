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
package is.codion.common.model.filter;

/**
 * Represents the sorting order for a column.
 * <p>A UI-agnostic equivalent of {@code javax.swing.SortOrder}, mirroring its constants 1:1.
 */
public enum SortOrder {

	/**
	 * Ascending sort order.
	 */
	ASCENDING,

	/**
	 * Descending sort order.
	 */
	DESCENDING,

	/**
	 * No sort order.
	 */
	UNSORTED
}
