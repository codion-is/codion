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
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.ValueAttributeDefinition;

import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Collections.EMPTY_MAP;
import static java.util.Objects.requireNonNull;

final class DefaultEntityBuilder implements Entity.Builder {

	private final EntityDefinition definition;
	private final Map<Attribute<?>, @Nullable Object> values;
	private final Map<Attribute<?>, @Nullable Object> originalValues;
	private final Map<Attribute<?>, @Nullable Object> builderValues = new LinkedHashMap<>();

	DefaultEntityBuilder(Key key) {
		this(requireNonNull(key).definition());
		key.columns().forEach(attribute -> with((Attribute<Object>) attribute, key.get(attribute)));
	}

	DefaultEntityBuilder(EntityDefinition definition) {
		this.definition = definition;
		this.values = new HashMap<>();
		this.originalValues = EMPTY_MAP;
		initialize();
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
		if (nonGeneratedPrimaryKeyColumn(attributeDefinition)) {
			values.put(attribute, value);// overwrite the default null value
		}
		else {
			builderValues.put(attribute, value);
		}

		return this;
	}

	@Override
	public Entity.Builder clear(Attribute<?> attribute) {
		remove(requireNonNull(attribute));

		return this;
	}

	@Override
	public Entity.Builder withDefaults() {
		definition.attributes().definitions().stream()
						.filter(ValueAttributeDefinition.class::isInstance)
						.map(ValueAttributeDefinition.class::cast)
						.filter(ValueAttributeDefinition::hasDefaultValue)
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
	public Entity build() {
		Entity entity = definition.entity(values, originalValues);
		builderValues.forEach((attribute, value) -> entity.set((Attribute<Object>) attribute, value));

		return entity;
	}

	private void remove(Attribute<?> column) {
		values.remove(column);
		originalValues.remove(column);
	}

	private void original(Column<?> column) {
		if (originalValues.containsKey(column)) {
			values.put(column, originalValues.get(column));
		}
	}

	private void initialize() {
		// For entities without primary keys, we must initialize all non-generated columns to null
		if (definition.primaryKey().columns().isEmpty()) {
			definition.columns().definitions().stream()
							.filter(columnDefinition -> !columnDefinition.generated())
							.forEach(columnDefinition -> values.put(columnDefinition.attribute(), null));
		}// Initialize non-generated PK columns to null
		else {
			definition.primaryKey().columns().stream()
							.map(column -> definition.columns().definition(column))
							.filter(columnDefinition -> !columnDefinition.generated())
							.forEach(columnDefinition -> values.put(columnDefinition.attribute(), null));
		}
	}

	private static <T> boolean nonGeneratedPrimaryKeyColumn(AttributeDefinition<T> attributeDefinition) {
		if (!(attributeDefinition instanceof ColumnDefinition<T>)) {
			return false;
		}
		ColumnDefinition<T> columnDefinition = (ColumnDefinition<T>) attributeDefinition;

		return columnDefinition.primaryKey() && !columnDefinition.generated();
	}
}
