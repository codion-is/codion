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
package is.codion.framework.domain.entity.query;

import is.codion.framework.domain.entity.query.DefaultEntitySelectQuery.DefaultBuilder;
import is.codion.framework.domain.entity.query.DefaultEntitySelectQuery.DefaultBuilder.DefaultWithAsStep;
import is.codion.framework.domain.entity.query.DefaultEntitySelectQuery.DefaultBuilder.DefaultWithRecursiveStep;

import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Defines a select query or parts of a select query, that is, from, columns, where, groupBy, having and orderBy clauses.
 * Supports Common Table Expressions (CTEs) via the {@link Builder#with(String)} method.
 * {@link Builder} provided by {@link #builder()}.
 */
public sealed interface EntitySelectQuery permits DefaultEntitySelectQuery {

	/**
	 * @return the map of CTE names to their queries, empty if no CTEs defined
	 */
	Map<String, String> with();

	/**
	 * @return true if the WITH clause should be marked as RECURSIVE
	 */
	boolean withRecursive();

	/**
	 * @return the COLUMNS clause
	 */
	@Nullable String columns();

	/**
	 * @return the FROM clause
	 */
	@Nullable String from();

	/**
	 * @return the WHERE clause
	 */
	@Nullable String where();

	/**
	 * @return the GROUP BY clause
	 */
	@Nullable String groupBy();

	/**
	 * @return the HAVING clause
	 */
	@Nullable String having();

	/**
	 * @return the order by clause
	 */
	@Nullable String orderBy();

	/**
	 * Creates a {@link Builder}
	 * @return a new {@link EntitySelectQuery.Builder} instance.
	 */
	static Builder builder() {
		return new DefaultBuilder();
	}

	/**
	 * Builds a {@link EntitySelectQuery}.
	 */
	sealed interface Builder permits DefaultBuilder, Builder.WithRecursiveStep {

		/**
		 * Adds a common table expression (CTE) to the query.
		 * CTEs are prepended to the query using the WITH clause.
		 * <p>
		 * Example:
		 * <pre>{@code
		 * EntitySelectQuery.builder()
		 *     .with("active_customers")
		 *     .as("SELECT * FROM customer WHERE active = true")
		 *     .from("active_customers")
		 *     .columns("id, name")
		 *     .build()
		 * }</pre>
		 * Generates:
		 * <pre>{@code
		 * WITH active_customers AS (SELECT * FROM customer WHERE active = true)
		 * SELECT id, name
		 * FROM active_customers
		 * }</pre>
		 */
		sealed interface WithAsStep permits DefaultWithAsStep {

			/**
			 * @param query the CTE query, without the WITH keyword
			 * @return this Builder instance
			 * @throws IllegalArgumentException if query contains the WITH keyword
			 */
			WithRecursiveStep as(String query);
		}

		/**
		 * Specifies whether this CTE requires the WITH clause to be marked as RECURSIVE.
		 */
		sealed interface WithRecursiveStep extends Builder permits DefaultWithRecursiveStep {

			/**
			 * Specifies that this CTE requires the WITH clause to be marked as RECURSIVE
			 * @return the Builder instance
			 */
			Builder recursive();
		}

		/**
		 * @param name the CTE name, must be unique within this query
		 * @return this Builder instance
		 * @throws IllegalArgumentException if query contains the WITH keyword
		 */
		WithAsStep with(String name);

		/**
		 * Specifies the columns clause to use, without the SELECT keyword.
		 * @param columns the columns clause
		 * @return this Builder instance
		 */
		Builder columns(String columns);

		/**
		 * Specifies the from clause to use.
		 * @param from the from clause, without the FROM keyword
		 * @return this Builder instance
		 */
		Builder from(String from);

		/**
		 * Specifies the where clause to use, without the WHERE keyword.
		 * @param where the where clause
		 * @return this Builder instance
		 */
		Builder where(String where);

		/**
		 * Specifies the group by clause to use, without the GROUP BY keywords.
		 * @param groupBy the group by clause
		 * @return this Builder instance
		 */
		Builder groupBy(String groupBy);

		/**
		 * Specifies the having clause to use, without the HAVING keyword.
		 * @param having the having clause
		 * @return this Builder instance
		 */
		Builder having(String having);

		/**
		 * Specifies the order by clause to use, without the ORDER BY keywords.
		 * @param orderBy the order by clause
		 * @return this Builder instance
		 */
		Builder orderBy(String orderBy);

		/**
		 * @return a new {@link EntitySelectQuery} instance
		 */
		EntitySelectQuery build();
	}
}
