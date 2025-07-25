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
package is.codion.framework.domain.entity;

import is.codion.framework.domain.entity.Entity.Key;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.Column;

import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Collections.EMPTY_MAP;
import static java.util.Objects.requireNonNull;

final class DefaultEntityBuilder implements Entity.Builder {

	private final EntityDefinition definition;
	private final Map<Attribute<?>, Object> values;
	private final Map<Attribute<?>, Object> originalValues;
	private final Map<Attribute<?>, Object> builderValues = new LinkedHashMap<>();

	DefaultEntityBuilder(Key key) {
		this(requireNonNull(key).definition());
		key.columns().forEach(attribute -> with((Attribute<Object>) attribute, key.get(attribute)));
	}

	DefaultEntityBuilder(EntityDefinition definition) {
		this.definition = definition;
		this.values = new HashMap<>();
		this.originalValues = EMPTY_MAP;
	}

	DefaultEntityBuilder(EntityDefinition definition, Map<Attribute<?>, Object> values,
											 @Nullable Map<Attribute<?>, Object> originalValues) {
		this.definition = definition;
		this.values = new HashMap<>(values);
		this.originalValues = originalValues == null ? EMPTY_MAP : new HashMap<>(originalValues);
	}

	@Override
	public <T> Entity.Builder with(Attribute<T> attribute, @Nullable T value) {
		AttributeDefinition<T> attributeDefinition = definition.attributes().definition(attribute);
		if (attributeDefinition.derived()) {
			throw new IllegalArgumentException("Can not set the value of a derived attribute");
		}
		builderValues.put(attribute, value);

		return this;
	}

	@Override
	public Entity.Builder withDefaults() {
		definition.attributes().definitions().stream()
						.filter(AttributeDefinition::hasDefaultValue)
						.forEach(attributeDefinition -> builderValues.put(attributeDefinition.attribute(), attributeDefinition.defaultValue()));

		return this;
	}

	@Override
	public Entity.Builder clearPrimaryKey() {
		definition.primaryKey().columns().forEach(this::remove);

		return this;
	}

	@Override
	public Entity.Builder originalPrimaryKey() {
		definition.primaryKey().columns().forEach(this::original);

		return this;
	}

	@Override
	public Key.Builder key() {
		DefaultKeyBuilder builder = new DefaultKeyBuilder(definition);
		if (!values.isEmpty()) {
			definition.primaryKey().columns().forEach(column -> {
				if (values.containsKey(column)) {
					builder.with((Column<Object>) column, values.get(column));
				}
			});
		}

		return builder;
	}

	@Override
	public Entity build() {
		Entity entity = definition.entity(values, originalValues);
		builderValues.forEach((attribute, value) -> entity.set((Attribute<Object>) attribute, value));

		return entity;
	}

	private void remove(Column<?> column) {
		values.remove(column);
		originalValues.remove(column);
	}

	private void original(Column<?> column) {
		if (originalValues.containsKey(column)) {
			values.put(column, originalValues.get(column));
		}
	}
}
