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
 * Copyright (c) 2019 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.entity.DefaultOrderBy.DefaultOrderByBuilder;
import is.codion.framework.domain.entity.DefaultOrderBy.DefaultOrderByColumn;
import is.codion.framework.domain.entity.attribute.Column;

import java.util.List;

/**
 * Specifies an order by clause for entity queries.
 * <p>
 * OrderBy instances define how query results should be sorted, supporting multiple columns,
 * ascending/descending order, null value handling, and case-insensitive sorting for strings.
 * <p>
 * OrderBy can be used in entity definitions as default ordering, or in queries for custom sorting:
 * {@snippet class = is.codion.demos.chinook.javadoc.OrderBySnippets region = usage :
 * // Default ordering for the entity
 * Customer.TYPE.as()
 * 				.attributes(
 * 								Customer.LASTNAME.as()
 * 												.column()
 * 												.caption("Last name"),
 * 								Customer.FIRSTNAME.as()
 * 												.column()
 * 												.caption("First name"))
 * 				.orderBy(OrderBy.builder()
 * 								.ascending(Customer.LASTNAME, Customer.FIRSTNAME)
 * 								.build())
 * 				.build();
 *
 * // Query usage examples
 * // Simple ascending sort
 * List<Entity> customers = connection.select(
 * 				Select.all(Customer.TYPE)
 * 								.orderBy(OrderBy.ascending(Customer.LASTNAME)));
 *
 * // Multiple columns, mixed directions
 * List<Entity> tracksByRatingAndName = connection.select(
 * 				Select.all(Track.TYPE)
 * 								.orderBy(OrderBy.builder()
 * 												.descending(Track.RATING) // Highest rated first
 * 												.ascendingIgnoreCase(Track.NAME) // Case-insensitive names
 * 												.build()));
 *
 * // With null handling
 * List<Entity> tracksByComposer = connection.select(
 * 				Select.all(Track.TYPE)
 * 								.orderBy(OrderBy.builder()
 * 												.ascending(OrderBy.NullOrder.NULLS_LAST, Track.COMPOSER)
 * 												.build()));}
 * @see #ascending(Column[])
 * @see #descending(Column[])
 * @see #builder()
 */
public sealed interface OrderBy permits DefaultOrderBy {

	/**
	 * @return the order by columns comprising this order by clause
	 */
	List<OrderByColumn> orderByColumns();

	/**
	 * Specifies an order by column and whether it's ascending or descending
	 */
	sealed interface OrderByColumn permits DefaultOrderByColumn {

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
	 * {@snippet class = is.codion.demos.chinook.javadoc.OrderBySnippets region = builder :
	 * // Complex ordering with multiple columns and options
	 * OrderBy order = OrderBy.builder()
	 * 				.descending(Track.RATING) // Highest rated first
	 * 				.ascending(Track.ARTIST_NAME) // Then by artist
	 * 				.descending(OrderBy.NullOrder.NULLS_LAST, Track.COMPOSER) // Then by composer (nulls last)
	 * 				.ascendingIgnoreCase(Track.NAME) // Finally by name (case-insensitive)
	 * 				.build();
	 *
	 * // Use in query
	 * List<Entity> tracks = connection.select(
	 * 				Select.all(Track.TYPE)
	 * 								.orderBy(order));
	 *
	 * // Builder pattern allows conditional ordering
	 * OrderBy.Builder builder = OrderBy.builder();
	 * if (byRating) {
	 * 	builder.descending(Track.RATING);
	 * }
	 * builder.ascending(Track.NAME);
	 * if (byPlayCount) {
	 * 	builder.descending(Track.PLAY_COUNT);
	 * }
	 * OrderBy dynamicOrder = builder.build();}
	 */
	sealed interface Builder permits DefaultOrderByBuilder {

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
		return new DefaultOrderByBuilder();
	}

	/**
	 * Creates an ascending OrderBy for the given columns.
	 * {@snippet class = is.codion.demos.chinook.javadoc.OrderBySnippets region = ascending :
	 * // Single column ascending
	 * OrderBy byName = OrderBy.ascending(Artist.NAME);
	 *
	 * // Multiple columns ascending
	 * OrderBy byLastAndFirstName = OrderBy.ascending(Customer.LASTNAME, Customer.FIRSTNAME);
	 *
	 * // Usage in queries
	 * List<Entity> customers = connection.select(
	 * 				Select.all(Customer.TYPE)
	 * 								.orderBy(OrderBy.ascending(Customer.LASTNAME)));
	 *
	 * // Usage in entity definition as default ordering
	 * Customer.TYPE.as()
	 * 				.attributes(
	 * 								Customer.LASTNAME.as()
	 * 												.column()
	 * 												.caption("Last name"),
	 * 								Customer.FIRSTNAME.as()
	 * 												.column()
	 * 												.caption("First name"))
	 * 				.orderBy(OrderBy.ascending(Customer.LASTNAME, Customer.FIRSTNAME))
	 * 				.build();}
	 * @param columns the columns to order by ascending
	 * @return a new ascending OrderBy instance based on the given columns
	 */
	static OrderBy ascending(Column<?>... columns) {
		return builder().ascending(columns).build();
	}

	/**
	 * Creates a descending OrderBy for the given columns.
	 * {@snippet class = is.codion.demos.chinook.javadoc.OrderBySnippets region = descending :
	 * // Single column descending
	 * OrderBy byDateDescending = OrderBy.descending(Invoice.DATE);
	 *
	 * // Multiple columns descending
	 * OrderBy byRatingAndPlayCount = OrderBy.descending(Track.RATING, Track.PLAY_COUNT);
	 *
	 * // Usage - most recent invoices first
	 * List<Entity> recentInvoices = connection.select(
	 * 				Select.all(Invoice.TYPE)
	 * 								.orderBy(OrderBy.descending(Invoice.DATE)));
	 *
	 * // Combine with conditions
	 * List<Entity> recentCustomerInvoices = connection.select(
	 * 				Select.where(Invoice.CUSTOMER_FK.equalTo(customer))
	 * 								.orderBy(OrderBy.descending(Invoice.DATE)));}
	 * @param columns the columns to order by descending
	 * @return a new descending OrderBy instance based on the given columns
	 */
	static OrderBy descending(Column<?>... columns) {
		return builder().descending(columns).build();
	}
}
