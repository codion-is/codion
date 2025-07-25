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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.entity.attribute.Column;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import static is.codion.framework.domain.entity.EntitySerializer.serializerForDomain;
import static java.util.Collections.singletonList;

final class SingleColumnKey implements Entity.Key, Serializable {

	@Serial
	private static final long serialVersionUID = 1;

	EntityDefinition definition;
	@Nullable Object value;
	Column<?> column;
	boolean primaryKey;
	int hashCode;

	SingleColumnKey(EntityDefinition definition, Column<?> column, @Nullable Object value, boolean primaryKey) {
		this.definition = definition;
		this.column = column;
		this.value = value;
		this.primaryKey = primaryKey;
		this.hashCode = value instanceof Integer ? (int) value : Objects.hash(value, column);
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
		return singletonList(column);
	}

	@Override
	public boolean primary() {
		return primaryKey;
	}

	@Override
	public boolean isNull() {
		return value == null;
	}

	@Override
	public boolean isNull(Column<?> column) {
		validate(column);

		return isNull();
	}

	@Override
	public <T> Column<T> column() {
		return (Column<T>) column;
	}

	@Override
	public <T> @Nullable T value() {
		return (T) value;
	}

	@Override
	public <T> Optional<T> optional() {
		return Optional.ofNullable((T) value);
	}

	@Override
	public <T> @Nullable T get(Column<T> column) {
		validate(column);

		return (T) value;
	}

	@Override
	public <T> Optional<T> optional(Column<T> column) {
		validate(column);

		return optional();
	}

	@Override
	public Builder copy() {
		return new DefaultKeyBuilder(this);
	}

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
		if (object instanceof SingleColumnKey) {
			SingleColumnKey that = (SingleColumnKey) object;

			return Objects.equals(value, that.value) && Objects.equals(column, that.column);
		}
		if (object instanceof CompositeColumnKey) {
			CompositeColumnKey that = (CompositeColumnKey) object;

			return that.columns.size() == 1 && column.equals(that.column()) && Objects.equals(value, that.value());
		}

		return false;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public String toString() {
		return column.name() + ":" + value;
	}

	private void validate(Column<?> column) {
		if (!column.equals(this.column)) {
			throw new IllegalArgumentException("Column " + column + " is not part of key: " + definition.type());
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
