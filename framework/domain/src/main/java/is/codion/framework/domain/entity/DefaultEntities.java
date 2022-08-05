/*
 * Copyright (c) 2020 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.DomainType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.unmodifiableCollection;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * A default {@link Entities} implementation.
 */
public abstract class DefaultEntities implements Entities, Serializable {

  private static final long serialVersionUID = 1;

  private static final Map<DomainType, Entities> REGISTERED_ENTITIES = new ConcurrentHashMap<>();

  private final DomainType domainType;
  private final Map<String, DefaultEntityDefinition> entityDefinitions = new LinkedHashMap<>();

  private transient boolean strictForeignKeys = EntityDefinition.STRICT_FOREIGN_KEYS.get();

  /**
   * Instantiates a new DefaultEntities for the given domainType
   * @param domainType the domainType
   */
  protected DefaultEntities(DomainType domainType) {
    this.domainType = requireNonNull(domainType, "domainType");
    REGISTERED_ENTITIES.put(domainType, this);
  }

  @Override
  public final DomainType getDomainType() {
    return domainType;
  }

  @Override
  public final EntityDefinition getDefinition(EntityType entityType) {
    return getDefinitionInternal(requireNonNull(entityType, "entityType").name());
  }

  @Override
  public final EntityDefinition getDefinition(String entityTypeName) {
    return getDefinitionInternal(requireNonNull(entityTypeName, "entityTypeName"));
  }

  @Override
  public final boolean contains(EntityType entityType) {
    return entityDefinitions.containsKey(requireNonNull(entityType).name());
  }

  @Override
  public final Collection<EntityDefinition> getDefinitions() {
    return unmodifiableCollection(entityDefinitions.values());
  }

  @Override
  public final Entity entity(EntityType entityType) {
    return getDefinition(entityType).entity();
  }

  @Override
  public final Entity.Builder builder(EntityType entityType) {
    return new DefaultEntityBuilder(getDefinition(entityType));
  }

  @Override
  public final <T> Key primaryKey(EntityType entityType, T value) {
    return getDefinition(entityType).primaryKey(value);
  }

  @Override
  public final <T> List<Key> primaryKeys(EntityType entityType, T... values) {
    return Arrays.stream(requireNonNull(values, "values"))
            .map(value -> primaryKey(entityType, value))
            .collect(toList());
  }

  @Override
  public final Key.Builder keyBuilder(EntityType entityType) {
    return new DefaultKeyBuilder(getDefinition(entityType));
  }

  /**
   * Retrieves the Entities for the given domain type.
   * @param domainType the domain type for which to retrieve the entity definitions
   * @return the Entities instance registered for the given domainType
   * @throws IllegalArgumentException in case the domain has not been registered
   */
  static Entities getEntities(String domainName) {
    Entities entities = REGISTERED_ENTITIES.get(DomainType.getDomainType(domainName));
    if (entities == null) {
      throw new IllegalArgumentException("Entities for domain '" + domainName + "' have not been registered");
    }

    return entities;
  }

  protected final void setStrictForeignKeys(boolean strictForeignKeys) {
    this.strictForeignKeys = strictForeignKeys;
  }

  protected final void add(EntityDefinition definition) {
    if (entityDefinitions.containsKey(definition.getEntityType().name())) {
      throw new IllegalArgumentException("Entity has already been defined: " +
              definition.getEntityType() + ", for table: " + definition.getTableName());
    }
    validateForeignKeyProperties(definition);
    entityDefinitions.put(definition.getEntityType().name(), (DefaultEntityDefinition) definition);
    populateForeignDefinitions();
  }

  private EntityDefinition getDefinitionInternal(String entityTypeName) {
    EntityDefinition definition = entityDefinitions.get(entityTypeName);
    if (definition == null) {
      throw new IllegalArgumentException("Undefined entity: " + entityTypeName);
    }

    return definition;
  }

  private void validateForeignKeyProperties(EntityDefinition definition) {
    EntityType entityType = definition.getEntityType();
    for (ForeignKey foreignKey : definition.getForeignKeys()) {
      EntityType referencedType = foreignKey.referencedEntityType();
      EntityDefinition referencedEntity = referencedType.equals(entityType) ?
              definition : entityDefinitions.get(referencedType.name());
      if (referencedEntity == null && strictForeignKeys) {
        throw new IllegalArgumentException("Entity '" + referencedType
                + "' referenced by entity '" + entityType + "' via foreign key '"
                + foreignKey + "' has not been defined");
      }
      if (referencedEntity != null) {
        foreignKey.references().stream()
                .map(ForeignKey.Reference::referencedAttribute)
                .forEach(attribute -> {
                  if (!referencedEntity.containsAttribute(attribute)) {
                    throw new IllegalArgumentException("Property referenced by foreign key not found in referenced entity: " + attribute);
                  }
                });
      }
    }
  }

  private void populateForeignDefinitions() {
    for (DefaultEntityDefinition definition : entityDefinitions.values()) {
      for (ForeignKey foreignKey : definition.getForeignKeys()) {
        EntityDefinition referencedDefinition = entityDefinitions.get(foreignKey.referencedEntityType().name());
        if (referencedDefinition != null && !definition.hasReferencedEntityDefinition(foreignKey)) {
          definition.setReferencedEntityDefinition(foreignKey, referencedDefinition);
        }
      }
    }
  }

  private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
    stream.defaultReadObject();
    REGISTERED_ENTITIES.put(domainType, this);
  }
}
