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
 * Copyright (c) 2019 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.entity.attribute.Column;

import java.util.List;

/**
 * Specifies an order by clause.
 * @see #ascending(Column[])
 * @see #descending(Column[])
 * @see #builder()
 */
public interface OrderBy {

	/**
	 * @return the order by columns comprising this order by clause
	 */
	List<OrderByColumn> orderByColumns();

	/**
	 * Specifies an order by column and whether it's ascending or descending
	 */
	interface OrderByColumn {

		/**
		 * @return the column to order by
		 */
		Column<?> column();

		/**
		 * @return true if the order is ascending, false for descending
		 */
		boolean ascending();

		/**
		 * @return the {@link NullOrder} when ordering by this column
		 */
		NullOrder nullOrder();

		/**
		 * @return true if this ordering should ignore case
		 */
		boolean ignoreCase();
	}

	/**
	 * Specifies how to handle null values during order by.
	 */
	enum NullOrder {

		/**
		 * Nulls first.
		 */
		NULLS_FIRST,

		/**
		 * Nulls last.
		 */
		NULLS_LAST,

		/**
		 * Database default, as in, no null ordering directive.
		 */
		DEFAULT
	}

	/**
	 * Builds a {@link OrderBy} instance.
	 */
	interface Builder {

		/**
		 * Adds an 'ascending' order by for the given columns
		 * @param columns the columns
		 * @return this builder instance
		 * @throws IllegalArgumentException in case {@code columns} is empty
		 */
		Builder ascending(Column<?>... columns);

		/**
		 * Adds an 'ascending' order by ignoring case for the given columns
		 * @param columns the columns
		 * @return this builder instance
		 * @throws IllegalArgumentException in case {@code columns} is empty
		 */
		Builder ascendingIgnoreCase(Column<String>... columns);

		/**
		 * Adds an 'ascending' order by for the given columns
		 * @param nullOrder the null order
		 * @param columns the columns
		 * @return this builder instance
		 * @throws IllegalArgumentException in case {@code columns} is empty
		 */
		Builder ascending(NullOrder nullOrder, Column<?>... columns);

		/**
		 * Adds an 'ascending' order by ignoring case for the given columns
		 * @param nullOrder the null order
		 * @param columns the columns
		 * @return this builder instance
		 * @throws IllegalArgumentException in case {@code columns} is empty
		 */
		Builder ascendingIgnoreCase(NullOrder nullOrder, Column<String>... columns);

		/**
		 * Adds a 'descending' order by for the given columns
		 * @param columns the columns
		 * @return this builder instance
		 * @throws IllegalArgumentException in case {@code columns} is empty
		 */
		Builder descending(Column<?>... columns);

		/**
		 * Adds a 'descending' order by ignoring case for the given columns
		 * @param columns the columns
		 * @return this builder instance
		 * @throws IllegalArgumentException in case {@code columns} is empty
		 */
		Builder descendingIgnoreCase(Column<?>... columns);

		/**
		 * Adds a 'descending' order by for the given columns
		 * @param nullOrder the null order
		 * @param columns the columns
		 * @return this builder instance
		 * @throws IllegalArgumentException in case {@code columns} is empty
		 */
		Builder descending(NullOrder nullOrder, Column<?>... columns);

		/**
		 * Adds a 'descending' order by ignoring case for the given columns
		 * @param nullOrder the null order
		 * @param columns the columns
		 * @return this builder instance
		 * @throws IllegalArgumentException in case {@code columns} is empty
		 */
		Builder descendingIgnoreCase(NullOrder nullOrder, Column<String>... columns);

		/**
		 * @return a new {@link OrderBy} instance based on this builder
		 */
		OrderBy build();
	}

	/**
	 * Creates a {@link OrderBy.Builder} instance.
	 * @return a {@link OrderBy.Builder} instance
	 */
	static OrderBy.Builder builder() {
		return new DefaultOrderBy.DefaultOrderByBuilder();
	}

	/**
	 * @param columns the columns to order by ascending
	 * @return a new ascending OrderBy instance based on the given columns
	 */
	static OrderBy ascending(Column<?>... columns) {
		return builder().ascending(columns).build();
	}

	/**
	 * @param columns the columns to order by descending
	 * @return a new descending OrderBy instance based on the given columns
	 */
	static OrderBy descending(Column<?>... columns) {
		return builder().descending(columns).build();
	}
}
