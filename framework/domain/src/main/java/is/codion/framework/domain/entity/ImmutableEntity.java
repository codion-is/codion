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
 * Copyright (c) 2023 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.entity.attribute.Attribute;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static is.codion.framework.domain.entity.EntitySerializer.serializerForDomain;

sealed class ImmutableEntity extends DefaultEntity implements Serializable permits EmptyEntity {

	@Serial
	private static final long serialVersionUID = 1;

	private static final String ERROR_MESSAGE = "This entity instance is immutable";

	ImmutableEntity(DefaultEntity entity) {
		this(entity.definition, entity.values, entity.originalValues, new HashMap<>());
	}

	ImmutableEntity(EntityDefinition definition, Map<Attribute<?>, Object> valueMap,
									@Nullable Map<Attribute<?>, Object> originalValueMap, Map<Key, ImmutableEntity> immutables) {
		super(definition);
		values = new HashMap<>(valueMap);
		immutables.put(primaryKey(), this);
		replace(values, immutables);
		if (originalValueMap != null) {
			originalValues = new HashMap<>(originalValueMap);
			replace(originalValues, immutables);
		}
	}

	@Override
	public <T> T set(Attribute<T> attribute, T value) {
		throw new UnsupportedOperationException(ERROR_MESSAGE);
	}

	@Override
	public void save(Attribute<?> attribute) {
		throw new UnsupportedOperationException(ERROR_MESSAGE);
	}

	@Override
	public void save() {
		throw new UnsupportedOperationException(ERROR_MESSAGE);
	}

	@Override
	public void revert(Attribute<?> attribute) {
		throw new UnsupportedOperationException(ERROR_MESSAGE);
	}

	@Override
	public void revert() {
		throw new UnsupportedOperationException(ERROR_MESSAGE);
	}

	@Override
	public <T> T remove(Attribute<T> attribute) {
		throw new UnsupportedOperationException(ERROR_MESSAGE);
	}

	@Override
	public Map<Attribute<?>, Object> set(Entity entity) {
		throw new UnsupportedOperationException(ERROR_MESSAGE);
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

	private static void replace(Map<Attribute<?>, Object> valueMap, Map<Key, ImmutableEntity> immutables) {
		for (Map.Entry<Attribute<?>, Object> attributeValue : valueMap.entrySet()) {
			Object value = attributeValue.getValue();
			if (value instanceof DefaultEntity && !(value instanceof ImmutableEntity)) {
				attributeValue.setValue(immutable((DefaultEntity) value, immutables));
			}
		}
	}

	private static ImmutableEntity immutable(DefaultEntity entity, Map<Key, ImmutableEntity> immutables) {
		ImmutableEntity immutable = immutables.get(entity.primaryKey());
		if (immutable == null) {
			immutable = new ImmutableEntity(entity.definition, entity.values, entity.originalValues, immutables);
		}

		return immutable;
	}
}
