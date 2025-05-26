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
 * Specifies an order by clause for entity queries.
 * <p>
 * OrderBy instances define how query results should be sorted, supporting multiple columns,
 * ascending/descending order, null value handling, and case-insensitive sorting for strings.
 * <p>
 * OrderBy can be used in entity definitions as default ordering, or in queries for custom sorting:
 * {@snippet :
 * public class Store extends DefaultDomain {
 *     
 *     interface Customer {
 *         EntityType TYPE = DOMAIN.entityType("store.customer");
 *         Column<String> LAST_NAME = TYPE.stringColumn("last_name");
 *         Column<String> FIRST_NAME = TYPE.stringColumn("first_name");
 *         Column<LocalDate> BIRTH_DATE = TYPE.localDateColumn("birth_date");
 *         Column<Boolean> ACTIVE = TYPE.booleanColumn("active");
 *     }
 *     
 *     void defineCustomer() {
 *         // Default ordering for the entity
 *         Customer.TYPE.define(
 *                 Customer.LAST_NAME.define()
 *                     .column(),
 *                 Customer.FIRST_NAME.define()
 *                     .column(),
 *                 Customer.BIRTH_DATE.define()
 *                     .column(),
 *                 Customer.ACTIVE.define()
 *                     .column())
 *             .orderBy(OrderBy.builder()
 *                 .ascending(Customer.LAST_NAME, Customer.FIRST_NAME)
 *                 .build())
 *             .build();
 *     }
 * }
 * 
 * // Query usage examples
 * // Simple ascending sort
 * List<Entity> customers = connection.select(
 *     Select.where(all(Customer.TYPE))
 *         .orderBy(OrderBy.ascending(Customer.LAST_NAME))
 *         .build());
 * 
 * // Multiple columns, mixed directions
 * List<Entity> customersByActiveAndName = connection.select(
 *     Select.where(all(Customer.TYPE))
 *         .orderBy(OrderBy.builder()
 *             .descending(Customer.ACTIVE)  // Active customers first
 *             .ascendingIgnoreCase(Customer.LAST_NAME, Customer.FIRST_NAME)  // Case-insensitive names
 *             .build())
 *         .build());
 * 
 * // With null handling
 * List<Entity> customersByBirthDate = connection.select(
 *     Select.where(all(Customer.TYPE))
 *         .orderBy(OrderBy.builder()
 *             .ascending(OrderBy.NullOrder.NULLS_LAST, Customer.BIRTH_DATE)
 *             .build())
 *         .build());
 * }
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
	 * {@snippet :
	 * // Complex ordering with multiple columns and options
	 * OrderBy complexOrder = OrderBy.builder()
	 *     .descending(Product.FEATURED)  // Featured products first
	 *     .ascending(Product.CATEGORY)   // Then by category
	 *     .descending(OrderBy.NullOrder.NULLS_LAST, Product.RATING)  // Then by rating (nulls last)
	 *     .ascendingIgnoreCase(Product.NAME)  // Finally by name (case-insensitive)
	 *     .build();
	 * 
	 * // Use in query
	 * List<Entity> products = connection.select(
	 *     Select.where(all(Product.TYPE))
	 *         .orderBy(complexOrder)
	 *         .build());
	 * 
	 * // Builder pattern allows conditional ordering
	 * OrderBy.Builder orderBuilder = OrderBy.builder();
	 * if (sortByPriority) {
	 *     orderBuilder.descending(Task.PRIORITY);
	 * }
	 * orderBuilder.ascending(Task.DUE_DATE);
	 * if (includeCreatedDate) {
	 *     orderBuilder.descending(Task.CREATED_DATE);
	 * }
	 * OrderBy dynamicOrder = orderBuilder.build();
	 * }
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
	 * Creates an ascending OrderBy for the given columns.
	 * {@snippet :
	 * // Single column ascending
	 * OrderBy byName = OrderBy.ascending(Customer.NAME);
	 * 
	 * // Multiple columns ascending
	 * OrderBy byNameAndEmail = OrderBy.ascending(Customer.LAST_NAME, Customer.FIRST_NAME);
	 *
	 * // Usage in queries
	 * List<Entity> customers = connection.select(
	 *     Select.where(all(Customer.TYPE))
	 *         .orderBy(OrderBy.ascending(Customer.LAST_NAME))
	 *         .build());
	 * 
	 * // Usage in entity definition as default ordering
	 * Customer.TYPE.define(
	 *         Customer.LAST_NAME.define()
	 *             .column(),
	 *         Customer.FIRST_NAME.define()
	 *             .column())
	 *     .orderBy(OrderBy.ascending(Customer.LAST_NAME, Customer.FIRST_NAME))
	 *     .build();
	 * }
	 * @param columns the columns to order by ascending
	 * @return a new ascending OrderBy instance based on the given columns
	 */
	static OrderBy ascending(Column<?>... columns) {
		return builder().ascending(columns).build();
	}

	/**
	 * Creates a descending OrderBy for the given columns.
	 * {@snippet :
	 * // Single column descending
	 * OrderBy byDateDesc = OrderBy.descending(Order.ORDER_DATE);
	 * 
	 * // Multiple columns descending
	 * OrderBy byPriorityAndDate = OrderBy.descending(Task.PRIORITY, Task.DUE_DATE);
	 *
	 * // Usage - most recent orders first
	 * List<Entity> recentOrders = connection.select(
	 *     Select.where(all(Order.TYPE))
	 *         .orderBy(OrderBy.descending(Order.ORDER_DATE))
	 *         .build());
	 *
	 * // Combine with conditions
	 * List<Entity> recentCustomerOrders = connection.select(
	 *     Select.where(Order.CUSTOMER_FK.equalTo(customer))
	 *         .orderBy(OrderBy.descending(Order.ORDER_DATE))
	 *         .build());
	 * }
	 * @param columns the columns to order by descending
	 * @return a new descending OrderBy instance based on the given columns
	 */
	static OrderBy descending(Column<?>... columns) {
		return builder().descending(columns).build();
	}
}
