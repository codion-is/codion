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
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.db;

import is.codion.framework.db.EntityConnection.Update;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.condition.Condition;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;

final class DefaultUpdate implements Update, Serializable {

	@Serial
	private static final long serialVersionUID = 1;

	private final Condition where;
	private final Map<Column<?>, Object> values;

	private DefaultUpdate(DefaultBuilder builder) {
		this.where = builder.where;
		this.values = unmodifiableMap(builder.values);
	}

	@Override
	public Condition where() {
		return where;
	}

	@Override
	public Map<Column<?>, Object> values() {
		return values;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof DefaultUpdate)) {
			return false;
		}
		DefaultUpdate that = (DefaultUpdate) object;
		return Objects.equals(where, that.where) &&
						Objects.equals(values, that.values);
	}

	@Override
	public int hashCode() {
		return Objects.hash(where, values);
	}

	@Override
	public String toString() {
		return "Update{" +
						"where=" + where +
						", values=" + values + "}";
	}

	static final class DefaultBuilder implements Update.Builder {

		private final Condition where;
		private final Map<Column<?>, Object> values = new LinkedHashMap<>();

		DefaultBuilder(Condition where) {
			this.where = requireNonNull(where);
		}

		@Override
		public <T> Builder set(Column<?> column, T value) {
			if (values.containsKey(requireNonNull(column))) {
				throw new IllegalStateException("Update already contains a value for column: " + column);
			}
			values.put(column, value);

			return this;
		}

		@Override
		public Update build() {
			if (values.isEmpty()) {
				throw new IllegalStateException("No values provided for update");
			}

			return new DefaultUpdate(this);
		}
	}
}
