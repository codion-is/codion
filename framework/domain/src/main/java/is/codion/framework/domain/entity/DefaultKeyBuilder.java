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
 * Copyright (c) 2021 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;

import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

final class DefaultKeyBuilder implements Entity.Key.Builder {

	private final EntityDefinition definition;
	private final Map<Column<?>, @Nullable Object> values = new HashMap<>();

	private boolean primary = true;

	DefaultKeyBuilder(Entity.Key key) {
		this.definition = key.definition();
		this.primary = key.primary();
		key.columns().forEach(column -> values.put(column, key.get(column)));
	}

	DefaultKeyBuilder(EntityDefinition definition) {
		this.definition = definition;
	}

	@Override
	public <T> Entity.Key.Builder with(Column<T> column, @Nullable T value) {
		ColumnDefinition<T> columnDefinition = definition.columns().definition(column);
		if (!columnDefinition.primaryKey()) {
			primary = false;
		}
		values.put(column, value);

		return this;
	}

	@Override
	public Entity.Key build() {
		if (values.size() == 1) {
			Column<?> column = values.keySet().iterator().next();

			return new SingleColumnKey(definition, column, values.get(column), primary);
		}

		return new CompositeColumnKey(definition, initializeValues(new HashMap<>(values)), primary);
	}

	private Map<Column<?>, @Nullable Object> initializeValues(Map<Column<?>, @Nullable Object> values) {
		if (primary && !values.isEmpty()) {
			//populate any missing primary key attributes with null values,
			//DefaultKey.equals() relies on the key attributes being present
			definition.primaryKey().columns().forEach(attribute -> values.putIfAbsent(attribute, null));
		}

		return values;
	}
}
