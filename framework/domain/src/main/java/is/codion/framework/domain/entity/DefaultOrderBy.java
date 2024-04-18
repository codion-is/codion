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
 * Copyright (c) 2019 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.entity.attribute.Column;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

final class DefaultOrderBy implements OrderBy, Serializable {

	private static final long serialVersionUID = 1;

	private final List<OrderByColumn> orderByColumns;

	private DefaultOrderBy(DefaultOrderByBuilder builder) {
		this.orderByColumns = unmodifiableList(builder.orderByColumns);
	}

	@Override
	public List<OrderByColumn> orderByColumns() {
		return orderByColumns;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof DefaultOrderBy)) {
			return false;
		}
		DefaultOrderBy that = (DefaultOrderBy) object;
		return orderByColumns.equals(that.orderByColumns);
	}

	@Override
	public int hashCode() {
		return orderByColumns.hashCode();
	}

	@Override
	public String toString() {
		return "OrderBy{" +
						"orderByColumns=" + orderByColumns +
						'}';
	}

	private static final class DefaultOrderByColumn implements OrderByColumn, Serializable {

		private static final long serialVersionUID = 1;

		private final Column<?> column;
		private final NullOrder nullOrder;
		private final boolean ascending;
		private final boolean ignoreCase;

		private DefaultOrderByColumn(Column<?> column, NullOrder nullOrder,
																 boolean ascending, boolean ignoreCase) {
			this.column = requireNonNull(column, "column");
			this.nullOrder = requireNonNull(nullOrder, "nullOrder");
			this.ascending = ascending;
			this.ignoreCase = ignoreCase;
		}

		@Override
		public Column<?> column() {
			return column;
		}

		@Override
		public NullOrder nullOrder() {
			return nullOrder;
		}

		@Override
		public boolean ascending() {
			return ascending;
		}

		@Override
		public boolean ignoreCase() {
			return ignoreCase;
		}

		@Override
		public boolean equals(Object object) {
			if (this == object) {
				return true;
			}
			if (object == null || getClass() != object.getClass()) {
				return false;
			}
			DefaultOrderByColumn that = (DefaultOrderByColumn) object;
			return column.equals(that.column) &&
							nullOrder.equals(that.nullOrder) &&
							ascending == that.ascending &&
							ignoreCase == that.ignoreCase;
		}

		@Override
		public int hashCode() {
			return Objects.hash(column, nullOrder, ascending, ignoreCase);
		}

		@Override
		public String toString() {
			return "OrderByColumn{" +
							"column=" + column +
							", nullOrder=" + nullOrder +
							", ascending=" + ascending +
							", ignoreCase=" + ignoreCase +
							'}';
		}
	}

	static final class DefaultOrderByBuilder implements Builder {

		private final List<OrderByColumn> orderByColumns = new ArrayList<>(1);

		@Override
		public Builder ascending(Column<?>... columns) {
			add(true, NullOrder.DEFAULT, false, requireNonNull(columns));
			return this;
		}

		@Override
		public Builder ascendingIgnoreCase(Column<String>... columns) {
			add(true, NullOrder.DEFAULT, true, requireNonNull(columns));
			return this;
		}

		@Override
		public Builder ascending(NullOrder nullOrder, Column<?>... columns) {
			add(true, requireNonNull(nullOrder), false, requireNonNull(columns));
			return this;
		}

		@Override
		public Builder ascendingIgnoreCase(NullOrder nullOrder, Column<String>... columns) {
			add(true, requireNonNull(nullOrder), true, requireNonNull(columns));
			return this;
		}

		@Override
		public Builder descending(Column<?>... columns) {
			add(false, NullOrder.DEFAULT, false, requireNonNull(columns));
			return this;
		}

		@Override
		public Builder descendingIgnoreCase(Column<?>... columns) {
			add(false, NullOrder.DEFAULT, true, requireNonNull(columns));
			return this;
		}

		@Override
		public Builder descending(NullOrder nullOrder, Column<?>... columns) {
			add(false, requireNonNull(nullOrder), false, requireNonNull(columns));
			return this;
		}

		@Override
		public Builder descendingIgnoreCase(NullOrder nullOrder, Column<String>... columns) {
			add(false, requireNonNull(nullOrder), true, requireNonNull(columns));
			return this;
		}

		@Override
		public OrderBy build() {
			return new DefaultOrderBy(this);
		}

		private void add(boolean ascending, NullOrder nullOrder,
										 boolean ignoreCase, Column<?>... columns) {
			if (columns.length == 0) {
				throw new IllegalArgumentException("One or more columns required for order by");
			}
			for (Column<?> column : columns) {
				for (OrderByColumn orderByColumn : orderByColumns) {
					if (requireNonNull(column).equals(orderByColumn.column())) {
						throw new IllegalArgumentException("Order by already contains column: " + column);
					}
				}
				orderByColumns.add(new DefaultOrderByColumn(column, nullOrder, ascending, ignoreCase));
			}
		}
	}
}
