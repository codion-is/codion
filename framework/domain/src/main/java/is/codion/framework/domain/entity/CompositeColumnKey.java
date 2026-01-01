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
 * Copyright (c) 2019 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

import static is.codion.framework.domain.entity.EntitySerializer.serializerForDomain;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

final class CompositeColumnKey implements Entity.Key, Serializable {

	@Serial
	private static final long serialVersionUID = 1;

	List<Column<?>> columns;
	boolean primary;
	Map<Column<?>, @Nullable Object> values;
	boolean singleIntegerKey;
	private @Nullable Integer cachedHashCode = null;
	boolean hashCodeDirty = true;
	EntityDefinition definition;

	CompositeColumnKey(EntityDefinition definition, Map<Column<?>, Object> values, boolean primary) {
		values.forEach((column, value) -> ((Column<Object>) column).type().validateType(value));
		this.values = unmodifiableMap(values);
		this.columns = unmodifiableList(new ArrayList<>(values.keySet()));
		this.definition = definition;
		this.primary = primary;
		if (!this.columns.isEmpty()) {
			this.singleIntegerKey = columns.size() == 1 && columns.get(0).type().isInteger();
		}
	}

	@Override
	public EntityType type() {
		return definition.type();
	}

	@Override
	public EntityDefinition definition() {
		return definition;
	}

	@Override
	public Collection<Column<?>> columns() {
		return columns;
	}

	@Override
	public boolean primary() {
		return primary;
	}

	@Override
	public <T> Column<T> column() {
		assertSingleValueKey();

		return (Column<T>) columns.get(0);
	}

	@Override
	public @Nullable <T> T value() {
		assertSingleValueKey();

		return (T) values.get(columns.get(0));
	}

	@Override
	public <T> Optional<T> optional() {
		return Optional.ofNullable(value());
	}

	@Override
	public @Nullable <T> T get(Column<T> column) {
		if (!values.containsKey(requireNonNull(column))) {
			throw new IllegalArgumentException("Column " + column + " is not part of key: " + definition.type());
		}

		return (T) values.get(definition.columns().definition(column).attribute());
	}

	@Override
	public <T> Optional<T> optional(Column<T> column) {
		return Optional.ofNullable(get(column));
	}

	@Override
	public Builder copy() {
		return new DefaultKeyBuilder(this);
	}

	@Override
	public String toString() {
		return columns.stream()
						.map(attribute -> attribute.name() + ":" + values.get(attribute))
						.collect(joining(","));
	}

	/**
	 * Keys are equal if all attributes and their associated values are equal.
	 * Empty and null keys are only equal to themselves.
	 * @param object the object to check for equality
	 * @return true if object is equal to this key
	 */
	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof Entity.Key)) {
			return false;
		}
		Entity.Key entityKey = (Entity.Key) object;
		if (isNull() || entityKey.isNull()) {
			return false;
		}
		if (object instanceof CompositeColumnKey) {
			CompositeColumnKey otherKey = (CompositeColumnKey) object;
			if (columns.size() == 1 && otherKey.columns.size() == 1) {
				Column<?> column = columns.get(0);
				Column<?> otherColumn = otherKey.columns.get(0);

				return Objects.equals(values.get(column), otherKey.values.get(otherColumn)) && column.equals(otherColumn);
			}

			return values.equals(otherKey.values);
		}
		if (object instanceof SingleColumnKey && columns.size() == 1) {
			SingleColumnKey singleColumnKey = (SingleColumnKey) object;
			Column<?> column = columns.get(0);

			return column.equals(singleColumnKey.column) && Objects.equals(values.get(column), singleColumnKey.value);
		}

		return false;
	}

	/**
	 * @return a hash code based on the values of this key, for single integer keys the hash code is simply the key value.
	 */
	@Override
	public int hashCode() {
		if (hashCodeDirty) {
			cachedHashCode = computeHashCode();
			hashCodeDirty = false;
		}

		return cachedHashCode == null ? 0 : cachedHashCode;
	}

	@Override
	public boolean isNull() {
		if (hashCodeDirty) {
			cachedHashCode = computeHashCode();
			hashCodeDirty = false;
		}

		return cachedHashCode == null;
	}

	@Override
	public boolean isNull(Column<?> column) {
		return get(column) == null;
	}

	private @Nullable Integer computeHashCode() {
		if (values.isEmpty()) {
			return null;
		}
		if (columns.size() > 1) {
			return computeMultipleValueHashCode();
		}

		return computeSingleValueHashCode();
	}

	private @Nullable Integer computeMultipleValueHashCode() {
		int hash = 0;
		for (int i = 0; i < columns.size(); i++) {
			ColumnDefinition<?> columnDefinition = definition.columns().definition(columns.get(i));
			Object value = values.get(columnDefinition.attribute());
			if (!columnDefinition.nullable() && value == null) {
				return null;
			}
			if (value != null) {
				hash = hash + value.hashCode();
			}
		}

		return hash;
	}

	private @Nullable Integer computeSingleValueHashCode() {
		Object value = value();
		if (value == null) {
			return null;
		}
		else if (singleIntegerKey) {
			return (Integer) value;
		}

		return value.hashCode();
	}

	private void assertSingleValueKey() {
		if (columns.isEmpty()) {
			throw new NoSuchElementException("Key contains no values");
		}
		if (columns.size() > 1) {
			throw new IllegalStateException("Key is a composite key");
		}
	}

	@Serial
	private void writeObject(ObjectOutputStream stream) throws IOException {
		stream.writeObject(definition.type().domainType().name());
		EntitySerializer.serialize(this, stream);
	}

	@Serial
	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		serializerForDomain((String) stream.readObject()).deserialize(this, stream);
	}
}
