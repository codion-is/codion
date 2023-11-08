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
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.unmodifiableCollection;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * A default {@link Entities} implementation.
 */
public abstract class DefaultEntities implements Entities, Serializable {

  private static final long serialVersionUID = 1;

  private final DomainType domainType;
  private final Map<String, DefaultEntityDefinition> entityDefinitions = new LinkedHashMap<>();

  private transient boolean strictForeignKeys = EntityDefinition.STRICT_FOREIGN_KEYS.get();

  /**
   * Instantiates a new DefaultEntities for the given domainType
   * @param domainType the domainType
   */
  protected DefaultEntities(DomainType domainType) {
    this.domainType = requireNonNull(domainType, "domainType");
    DefaultKey.setSerializer(domainType.name(), createSerializer(this));
  }

  @Override
  public final DomainType domainType() {
    return domainType;
  }

  @Override
  public final EntityDefinition definition(EntityType entityType) {
    return definitionInternal(requireNonNull(entityType, "entityType").name());
  }

  @Override
  public final EntityDefinition definition(String entityTypeName) {
    return definitionInternal(requireNonNull(entityTypeName, "entityTypeName"));
  }

  @Override
  public final boolean contains(EntityType entityType) {
    return entityDefinitions.containsKey(requireNonNull(entityType).name());
  }

  @Override
  public final Collection<EntityDefinition> definitions() {
    return unmodifiableCollection(entityDefinitions.values());
  }

  @Override
  public final Entity entity(EntityType entityType) {
    return definition(entityType).entity();
  }

  @Override
  public final Entity.Builder builder(EntityType entityType) {
    return new DefaultEntityBuilder(definition(entityType));
  }

  @Override
  public final <T> Entity.Key primaryKey(EntityType entityType, T value) {
    return definition(entityType).primaryKey(value);
  }

  @Override
  public final <T> List<Entity.Key> primaryKeys(EntityType entityType, T... values) {
    EntityDefinition definition = definition(entityType);

    return Arrays.stream(requireNonNull(values, "values"))
            .map(definition::primaryKey)
            .collect(toList());
  }

  @Override
  public final Entity.Key.Builder keyBuilder(EntityType entityType) {
    return new DefaultKeyBuilder(definition(entityType));
  }

  @Override
  public final String toString() {
    return getClass().getSimpleName() + ": " + domainType;
  }

  protected final void setStrictForeignKeys(boolean strictForeignKeys) {
    this.strictForeignKeys = strictForeignKeys;
  }

  protected final void add(EntityDefinition definition) {
    if (entityDefinitions.containsKey(definition.entityType().name())) {
      throw new IllegalArgumentException("Entity has already been defined: " +
              definition.entityType() + ", for table: " + definition.tableName());
    }
    validateForeignKeys(definition);
    entityDefinitions.put(definition.entityType().name(), (DefaultEntityDefinition) definition);
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
    EntityType entityType = definition.entityType();
    for (ForeignKey foreignKey : definition.foreignKeys().get()) {
      EntityType referencedType = foreignKey.referencedType();
      EntityDefinition referencedEntity = referencedType.equals(entityType) ?
              definition : entityDefinitions.get(referencedType.name());
      if (referencedEntity == null && strictForeignKeys) {
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

  private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
    stream.defaultReadObject();
    DefaultKey.setSerializer(domainType.name(), createSerializer(this));
  }

  private static EntitySerializer createSerializer(Entities entities) {
    return new EntitySerializer(entities, STRICT_DESERIALIZATION.get());
  }

  private static void validateReference(ForeignKey foreignKey, Attribute<?> referencedAttribute, EntityDefinition referencedEntity) {
    if (!referencedEntity.attributes().contains(referencedAttribute)) {
      throw new IllegalArgumentException("Attribute " + referencedAttribute + " referenced by foreign key "
              + foreignKey + " not found in referenced entity");
    }
  }
}
