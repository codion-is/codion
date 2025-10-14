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

import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;

final class DefaultEntitySelectQuery implements EntitySelectQuery {

	private final Map<String, String> with;
	private final boolean withRecursive;
	private final @Nullable String columns;
	private final @Nullable String from;
	private final @Nullable String where;
	private final @Nullable String groupBy;
	private final @Nullable String having;
	private final @Nullable String orderBy;

	DefaultEntitySelectQuery(DefaultBuilder builder) {
		this.with = builder.with.isEmpty() ? emptyMap() : unmodifiableMap(new LinkedHashMap<>(builder.with));
		this.withRecursive = builder.withRecursive;
		this.columns = builder.columns;
		this.from = builder.from;
		this.where = builder.where;
		this.groupBy = builder.groupBy;
		this.having = builder.having;
		this.orderBy = builder.orderBy;
	}

	@Override
	public Map<String, String> with() {
		return with;
	}

	@Override
	public boolean withRecursive() {
		return withRecursive;
	}

	@Override
	public @Nullable String columns() {
		return columns;
	}

	@Override
	public @Nullable String from() {
		return from;
	}

	@Override
	public @Nullable String where() {
		return where;
	}

	@Override
	public @Nullable String groupBy() {
		return groupBy;
	}

	@Override
	public @Nullable String having() {
		return having;
	}

	@Override
	public @Nullable String orderBy() {
		return orderBy;
	}

	static final class DefaultBuilder implements Builder {

		private final Map<String, String> with = new LinkedHashMap<>();

		private boolean withRecursive;
		private @Nullable String from;
		private @Nullable String columns;
		private @Nullable String where;
		private @Nullable String groupBy;
		private @Nullable String having;
		private @Nullable String orderBy;

		@Override
		public Builder with(String name, String query) {
			if (requireNonNull(query).trim().toLowerCase().startsWith("with")) {
				throw new IllegalArgumentException("with clause should not include the 'WITH' keyword");
			}
			with.put(requireNonNull(name), query);
			return this;
		}

		@Override
		public Builder withRecursive(String name, String query) {
			with(name, query);
			this.withRecursive = true;
			return this;
		}

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
		public EntitySelectQuery build() {
			return new DefaultEntitySelectQuery(this);
		}
	}
}
