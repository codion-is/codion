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

import static java.util.Objects.requireNonNull;

final class DefaultSelectQuery implements SelectQuery {

	private final String columns;
	private final String from;
	private final String where;
	private final String groupBy;
	private final String having;
	private final String orderBy;

	DefaultSelectQuery(DefaultSelectQueryBuilder builder) {
		this.columns = builder.columns;
		this.from = builder.from;
		this.where = builder.where;
		this.groupBy = builder.groupBy;
		this.having = builder.having;
		this.orderBy = builder.orderBy;
	}

	@Override
	public String columns() {
		return columns;
	}

	@Override
	public String from() {
		return from;
	}

	@Override
	public String where() {
		return where;
	}

	@Override
	public String groupBy() {
		return groupBy;
	}

	@Override
	public String having() {
		return having;
	}

	@Override
	public String orderBy() {
		return orderBy;
	}

	static class DefaultSelectQueryBuilder implements Builder {

		private String from;
		private String columns;
		private String where;
		private String groupBy;
		private String having;
		private String orderBy;

		@Override
		public Builder columns(String columns) {
			if (requireNonNull(columns).trim().toLowerCase().startsWith("select")) {
				throw new IllegalArgumentException("columns clause should not include the 'SELECT' keyword");
			}
			this.columns = columns;
			return this;
		}

		@Override
		public Builder from(String from) {
			if (requireNonNull(from).trim().toLowerCase().startsWith("from")) {
				throw new IllegalArgumentException("from clause should not include the 'FROM' keyword");
			}
			this.from = from;
			return this;
		}

		@Override
		public Builder where(String where) {
			if (requireNonNull(where).trim().toLowerCase().startsWith("where")) {
				throw new IllegalArgumentException("where clause should not include the 'WHERE' keyword");
			}
			this.where = where;

			return this;
		}

		@Override
		public Builder groupBy(String groupBy) {
			if (requireNonNull(groupBy).trim().toLowerCase().startsWith("group by")) {
				throw new IllegalArgumentException("group by clause should not include the 'GROUP BY' keywords");
			}
			this.groupBy = groupBy;

			return this;
		}

		@Override
		public Builder having(String having) {
			if (requireNonNull(having).trim().toLowerCase().startsWith("having")) {
				throw new IllegalArgumentException("having clause should not include the 'HAVING' keywords");
			}
			this.having = having;

			return this;
		}

		@Override
		public Builder orderBy(String orderBy) {
			if (requireNonNull(orderBy).trim().toLowerCase().startsWith("order by")) {
				throw new IllegalArgumentException("orderBy clause should not include the 'ORDER BY' keywords");
			}
			this.orderBy = orderBy;

			return this;
		}

		@Override
		public SelectQuery build() {
			return new DefaultSelectQuery(this);
		}
	}
}
