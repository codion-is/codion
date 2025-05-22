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
package is.codion.framework.db;

import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.condition.Condition;

import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;

final class DefaultSelect implements Select, Serializable {

	@Serial
	private static final long serialVersionUID = 1;

	private final Condition where;
	private final Condition having;
	private final @Nullable Map<ForeignKey, Integer> foreignKeyReferenceDepths;
	private final Collection<Attribute<?>> attributes;
	private final @Nullable OrderBy orderBy;
	private final @Nullable Integer referenceDepth;
	private final boolean forUpdate;
	private final @Nullable Integer limit;
	private final @Nullable Integer offset;
	private final int queryTimeout;

	private DefaultSelect(DefaultBuilder builder) {
		this.where = builder.where;
		this.having = builder.having;
		this.foreignKeyReferenceDepths = builder.foreignKeyReferenceDepths == null ?
						null :
						unmodifiableMap(builder.foreignKeyReferenceDepths);
		this.attributes = builder.attributes;
		this.orderBy = builder.orderBy;
		this.referenceDepth = builder.referenceDepth;
		this.forUpdate = builder.forUpdate;
		this.limit = builder.limit;
		this.offset = builder.offset;
		this.queryTimeout = builder.queryTimeout;
	}

	@Override
	public Condition where() {
		return where;
	}

	@Override
	public Condition having() {
		return having;
	}

	@Override
	public Optional<OrderBy> orderBy() {
		return Optional.ofNullable(orderBy);
	}

	@Override
	public OptionalInt limit() {
		return limit == null ? OptionalInt.empty() : OptionalInt.of(limit);
	}

	@Override
	public OptionalInt offset() {
		return offset == null ? OptionalInt.empty() : OptionalInt.of(offset);
	}

	@Override
	public boolean forUpdate() {
		return forUpdate;
	}

	@Override
	public OptionalInt referenceDepth() {
		return referenceDepth == null ? OptionalInt.empty() : OptionalInt.of(referenceDepth);
	}

	@Override
	public OptionalInt referenceDepth(ForeignKey foreignKey) {
		Integer foreignKeyReferenceDepth = foreignKeyReferenceDepths().get(requireNonNull(foreignKey));
		if (foreignKeyReferenceDepth != null) {
			return OptionalInt.of(foreignKeyReferenceDepth);
		}

		return referenceDepth();
	}

	@Override
	public Map<ForeignKey, Integer> foreignKeyReferenceDepths() {
		return foreignKeyReferenceDepths == null ? emptyMap() : foreignKeyReferenceDepths;
	}

	@Override
	public int queryTimeout() {
		return queryTimeout;
	}

	@Override
	public Collection<Attribute<?>> attributes() {
		return attributes;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof DefaultSelect)) {
			return false;
		}
		DefaultSelect that = (DefaultSelect) object;
		return forUpdate == that.forUpdate &&
						limit == that.limit &&
						offset == that.offset &&
						where.equals(that.where) &&
						Objects.equals(having, that.having) &&
						Objects.equals(foreignKeyReferenceDepths, that.foreignKeyReferenceDepths) &&
						attributes.equals(that.attributes) &&
						Objects.equals(orderBy, that.orderBy) &&
						Objects.equals(referenceDepth, that.referenceDepth);
	}

	@Override
	public int hashCode() {
		return Objects.hash(forUpdate, limit, offset, where, foreignKeyReferenceDepths, attributes, orderBy, referenceDepth);
	}

	@Override
	public String toString() {
		return "Select{" +
						"where=" + where +
						", having=" + having +
						", foreignKeyReferenceDepths=" + foreignKeyReferenceDepths +
						", attributes=" + attributes +
						", orderBy=" + orderBy +
						", referenceDepth=" + referenceDepth +
						", forUpdate=" + forUpdate +
						", limit=" + limit +
						", offset=" + offset +
						", queryTimeout=" + queryTimeout + "}";
	}

	static final class DefaultBuilder implements Select.Builder {

		private final Condition where;

		private @Nullable Map<ForeignKey, Integer> foreignKeyReferenceDepths;
		private Collection<Attribute<?>> attributes = emptyList();

		private Condition having;
		private @Nullable OrderBy orderBy;
		private @Nullable Integer referenceDepth;
		private boolean forUpdate;
		private @Nullable Integer limit;
		private @Nullable Integer offset;
		private int queryTimeout = EntityConnection.DEFAULT_QUERY_TIMEOUT_SECONDS;

		DefaultBuilder(Condition where) {
			this.where = requireNonNull(where);
			this.having = Condition.all(where.entityType());
		}

		@Override
		public Builder orderBy(@Nullable OrderBy orderBy) {
			this.orderBy = orderBy;
			return this;
		}

		@Override
		public Builder limit(@Nullable Integer limit) {
			this.limit = limit;
			return this;
		}

		@Override
		public Builder offset(@Nullable Integer offset) {
			this.offset = offset;
			return this;
		}

		@Override
		public Builder forUpdate() {
			this.forUpdate = true;
			this.referenceDepth = 0;
			return this;
		}

		@Override
		public Builder referenceDepth(int referenceDepth) {
			this.referenceDepth = referenceDepth;
			return this;
		}

		@Override
		public Builder referenceDepth(ForeignKey foreignKey, int referenceDepth) {
			requireNonNull(foreignKey);
			if (foreignKeyReferenceDepths == null) {
				foreignKeyReferenceDepths = new HashMap<>();
			}
			foreignKeyReferenceDepths.put(foreignKey, referenceDepth);
			return this;
		}

		@Override
		public <T extends Attribute<?>> Builder attributes(T... attributes) {
			this.attributes = requireNonNull(attributes).length == 0 ? emptyList() : unmodifiableList(asList(attributes));
			return this;
		}

		@Override
		public Builder attributes(Collection<? extends Attribute<?>> attributes) {
			this.attributes = requireNonNull(attributes).isEmpty() ? emptyList() : unmodifiableList(new ArrayList<>(attributes));
			return this;
		}

		@Override
		public Builder queryTimeout(int queryTimeout) {
			if (queryTimeout < 0) {
				throw new IllegalArgumentException("Query timeout must be greater than or equal to 0");
			}
			this.queryTimeout = queryTimeout;
			return this;
		}

		@Override
		public Builder having(Condition having) {
			this.having = requireNonNull(having);
			return this;
		}

		@Override
		public Select build() {
			return new DefaultSelect(this);
		}
	}
}
