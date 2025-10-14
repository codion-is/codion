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
 * Copyright (c) 2020 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * A default {@link Entities} implementation.
 */
final class DefaultEntities implements Entities, Serializable {

	@Serial
	private static final long serialVersionUID = 1;

	private final DomainType domainType;
	private final Map<String, DefaultEntityDefinition> entityDefinitions = new LinkedHashMap<>();

	private transient boolean validateForeignKeys = VALIDATE_FOREIGN_KEYS.getOrThrow();

	/**
	 * Instantiates a new DefaultEntities for the given domainType
	 * @param domainType the domainType
	 */
	DefaultEntities(DomainType domainType) {
		this.domainType = requireNonNull(domainType);
		EntitySerializer.setSerializer(domainType.name(), createSerializer(this));
	}

	@Override
	public DomainType domainType() {
		return domainType;
	}

	@Override
	public EntityDefinition definition(EntityType entityType) {
		return definitionInternal(requireNonNull(entityType).name());
	}

	@Override
	public EntityDefinition definition(String entityTypeName) {
		return definitionInternal(requireNonNull(entityTypeName));
	}

	@Override
	public boolean contains(EntityType entityType) {
		return entityDefinitions.containsKey(requireNonNull(entityType).name());
	}

	@Override
	public Collection<EntityDefinition> definitions() {
		return unmodifiableCollection(entityDefinitions.values());
	}

	@Override
	public Entity.Builder entity(EntityType entityType) {
		return new DefaultEntityBuilder(definition(entityType));
	}

	@Override
	public Entity.Key.Builder key(EntityType entityType) {
		return new DefaultKeyBuilder(definition(entityType));
	}

	@Override
	public <T> Entity.Key primaryKey(EntityType entityType, T value) {
		return definition(entityType).primaryKey(value);
	}

	@Override
	public <T> List<Entity.Key> primaryKeys(EntityType entityType, T... values) {
		return primaryKeys(entityType, asList(values));
	}

	@Override
	public <T> List<Entity.Key> primaryKeys(EntityType entityType, Collection<T> values) {
		EntityDefinition definition = definition(entityType);

		return requireNonNull(values).stream()
						.map(definition::primaryKey)
						.collect(toList());
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + ": " + domainType;
	}

	void validateForeignKeys(boolean validateForeignKeys) {
		this.validateForeignKeys = validateForeignKeys;
	}

	void add(EntityDefinition definition) {
		requireNonNull(definition);
		if (entityDefinitions.containsKey(definition.type().name())) {
			throw new IllegalArgumentException("Entity has already been defined: " +
							definition.type() + ", for table: " + definition.table());
		}
		validateForeignKeys(definition);
		entityDefinitions.put(definition.type().name(), (DefaultEntityDefinition) definition);
		populateForeignDefinitions();
	}

	private EntityDefinition definitionInternal(String entityTypeName) {
		EntityDefinition definition = entityDefinitions.get(entityTypeName);
		if (definition == null) {
			throw new IllegalArgumentException("Undefined entity: " + entityTypeName);
		}

		return definition;
	}

	private void validateForeignKeys(EntityDefinition definition) {
		EntityType entityType = definition.type();
		for (ForeignKey foreignKey : definition.foreignKeys().get()) {
			EntityType referencedType = foreignKey.referencedType();
			EntityDefinition referencedEntity = referencedType.equals(entityType) ?
							definition : entityDefinitions.get(referencedType.name());
			if (referencedEntity == null && validateForeignKeys) {
				throw new IllegalArgumentException("Entity '" + referencedType
								+ "' referenced by entity '" + entityType + "' via foreign key '"
								+ foreignKey + "' has not been defined");
			}
			if (referencedEntity != null) {
				foreignKey.references().stream()
								.map(ForeignKey.Reference::foreign)
								.forEach(referencedAttribute -> validateReference(foreignKey, referencedAttribute, referencedEntity));
			}
		}
	}

	private void populateForeignDefinitions() {
		for (DefaultEntityDefinition definition : entityDefinitions.values()) {
			for (ForeignKey foreignKey : definition.foreignKeys().get()) {
				EntityDefinition referencedDefinition = entityDefinitions.get(foreignKey.referencedType().name());
				if (referencedDefinition != null && !definition.hasReferencedEntityDefinition(foreignKey)) {
					definition.setReferencedEntityDefinition(foreignKey, referencedDefinition);
				}
			}
		}
	}

	@Serial
	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		stream.defaultReadObject();
		EntitySerializer.setSerializer(domainType.name(), createSerializer(this));
	}

	private static EntitySerializer createSerializer(Entities entities) {
		return new EntitySerializer(entities, STRICT_DESERIALIZATION.getOrThrow());
	}

	private static void validateReference(ForeignKey foreignKey, Attribute<?> referencedAttribute, EntityDefinition referencedEntity) {
		if (!referencedEntity.attributes().contains(referencedAttribute)) {
			throw new IllegalArgumentException("Attribute " + referencedAttribute + " referenced by foreign key "
							+ foreignKey + " not found in referenced entity");
		}
	}
}
