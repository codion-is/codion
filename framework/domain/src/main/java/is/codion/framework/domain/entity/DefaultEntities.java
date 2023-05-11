/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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
  public final <T> Key primaryKey(EntityType entityType, T value) {
    return definition(entityType).primaryKey(value);
  }

  @Override
  public final <T> List<Key> primaryKeys(EntityType entityType, T... values) {
    EntityDefinition definition = definition(entityType);

    return Arrays.stream(requireNonNull(values, "values"))
            .map(definition::primaryKey)
            .collect(toList());
  }

  @Override
  public final Key.Builder keyBuilder(EntityType entityType) {
    return new DefaultKeyBuilder(definition(entityType));
  }

  /**
   * Retrieves the Entities for the given domain type.
   * @param domainName the name of the domain for which to retrieve the entity definitions
   * @return the Entities instance registered for the given domainType
   * @throws IllegalArgumentException in case the domain has not been registered
   */
  static Entities entities(String domainName) {
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
    if (entityDefinitions.containsKey(definition.type().name())) {
      throw new IllegalArgumentException("Entity has already been defined: " +
              definition.type() + ", for table: " + definition.tableName());
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
    for (ForeignKey foreignKey : definition.foreignKeys()) {
      EntityType referencedType = foreignKey.referencedType();
      EntityDefinition referencedEntity = referencedType.equals(entityType) ?
              definition : entityDefinitions.get(referencedType.name());
      if (strictForeignKeys && referencedEntity == null) {
        throw new IllegalArgumentException("Entity '" + referencedType
                + "' referenced by entity '" + entityType + "' via foreign key '"
                + foreignKey + "' has not been defined");
      }
      if (referencedEntity != null) {
        foreignKey.references().stream()
                .map(ForeignKey.Reference::referencedAttribute)
                .forEach(referencedAttribute -> validateReference(foreignKey, referencedAttribute, referencedEntity));
      }
    }
  }

  private void populateForeignDefinitions() {
    for (DefaultEntityDefinition definition : entityDefinitions.values()) {
      for (ForeignKey foreignKey : definition.foreignKeys()) {
        EntityDefinition referencedDefinition = entityDefinitions.get(foreignKey.referencedType().name());
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

  private static void validateReference(ForeignKey foreignKey, Attribute<?> referencedAttribute, EntityDefinition referencedEntity) {
    if (!referencedEntity.containsAttribute(referencedAttribute)) {
      throw new IllegalArgumentException("Attribute " + referencedAttribute + " referenced by foreign key "
              + foreignKey + " not found in referenced entity");
    }
  }
}
