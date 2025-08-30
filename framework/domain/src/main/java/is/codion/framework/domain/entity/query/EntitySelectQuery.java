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

import org.jspecify.annotations.Nullable;

/**
 * Defines a select query or parts of a select query, that is, from, columns, where, groupBy, having and orderBy clauses.
 * {@link Builder} provided by {@link #builder()}.
 */
public sealed interface EntitySelectQuery permits DefaultEntitySelectQuery {

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
	sealed interface Builder permits DefaultBuilder {

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
