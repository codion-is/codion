/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.property.Property;

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
  protected DefaultEntities(final DomainType domainType) {
    this.domainType = requireNonNull(domainType, "domainType");
    REGISTERED_ENTITIES.put(domainType, this);
  }

  @Override
  public final DomainType getDomainType() {
    return domainType;
  }

  @Override
  public final EntityDefinition getDefinition(final EntityType<?> entityType) {
    return getDefinitionInternal(requireNonNull(entityType, "entityType").getName());
  }

  @Override
  public final EntityDefinition getDefinition(final String entityTypeName) {
    return getDefinitionInternal(requireNonNull(entityTypeName, "entityTypeName"));
  }

  @Override
  public final boolean contains(final EntityType<?> entityType) {
    return entityDefinitions.containsKey(requireNonNull(entityType).getName());
  }

  @Override
  public final Collection<EntityDefinition> getDefinitions() {
    return unmodifiableCollection(entityDefinitions.values());
  }

  @Override
  public final Entity entity(final EntityType<?> entityType) {
    return getDefinition(entityType).entity();
  }

  @Override
  public final Entity entity(final Key key) {
    return getDefinition(key.getEntityType()).entity(key);
  }

  @Override
  public final Entity.Builder builder(final EntityType<?> entityType) {
    return new DefaultEntityBuilder(getDefinition(entityType));
  }

  @Override
  public final Entity.Builder builder(final Key key) {
    final Entity.Builder builder = builder(requireNonNull(key).getEntityType());
    key.getAttributes().forEach(attribute -> builder.with((Attribute<Object>) attribute, key.get(attribute)));

    return builder;
  }

  @Override
  public final Key primaryKey(final EntityType<?> entityType) {
    return getDefinition(entityType).primaryKey();
  }

  @Override
  public final Key primaryKey(final EntityType<?> entityType, final Integer value) {
    return getDefinition(entityType).primaryKey(value);
  }

  @Override
  public final Key primaryKey(final EntityType<?> entityType, final Long value) {
    return getDefinition(entityType).primaryKey(value);
  }

  @Override
  public final List<Key> primaryKeys(final EntityType<?> entityType, final Integer... values) {
    requireNonNull(values, "values");
    return Arrays.stream(values).map(value -> primaryKey(entityType, value)).collect(toList());
  }

  @Override
  public final List<Key> primaryKeys(final EntityType<?> entityType, final Long... values) {
    requireNonNull(values, "values");
    return Arrays.stream(values).map(value -> primaryKey(entityType, value)).collect(toList());
  }

  /**
   * Retrieves the Entities for the given domain type.
   * @param domainType the domain type for which to retrieve the entity definitions
   * @return the Entities instance registered for the given domainType
   * @throws IllegalArgumentException in case the domain has not been registered
   */
  static Entities getEntities(final String domainName) {
    final Entities entities = REGISTERED_ENTITIES.get(DomainType.getDomainType(domainName));
    if (entities == null) {
      throw new IllegalArgumentException("Entities for domain '" + domainName + "' have not been registered");
    }

    return entities;
  }

  protected final void setStrictForeignKeys(final boolean strictForeignKeys) {
    this.strictForeignKeys = strictForeignKeys;
  }

  protected final EntityDefinition.Builder define(final EntityType<?> entityType, final String tableName,
                                                  final Property.Builder<?, ?>... propertyBuilders) {
    requireNonNull(propertyBuilders, "propertyBuilders");
    final DefaultEntityDefinition.DefaultBuilder definitionBuilder =
            new DefaultEntityDefinition(domainType.getName(), entityType, tableName,
                    Arrays.asList(propertyBuilders)).builder();
    addDefinition(definitionBuilder.get());

    return definitionBuilder;
  }

  protected final void addDefinition(final EntityDefinition definition) {
    if (entityDefinitions.containsKey(definition.getEntityType().getName())) {
      throw new IllegalArgumentException("Entity has already been defined: " +
              definition.getEntityType() + ", for table: " + definition.getTableName());
    }
    if (contains(definition.getEntityType())) {
      throw new IllegalArgumentException("Entity with the same entity type name has already been defined: " +
              definition.getEntityType() + ", for table: " + definition.getTableName());
    }
    validateForeignKeyProperties(definition);
    entityDefinitions.put(definition.getEntityType().getName(), (DefaultEntityDefinition) definition);
    populateForeignDefinitions();
  }

  private EntityDefinition getDefinitionInternal(final String entityTypeName) {
    final EntityDefinition definition = entityDefinitions.get(entityTypeName);
    if (definition == null) {
      throw new IllegalArgumentException("Undefined entity: " + entityTypeName);
    }

    return definition;
  }

  private void validateForeignKeyProperties(final EntityDefinition definition) {
    for (final ForeignKey foreignKey : definition.getForeignKeys()) {
      final EntityType<?> entityType = definition.getEntityType();
      if (!entityType.equals(foreignKey.getReferencedEntityType()) && strictForeignKeys) {
        final EntityDefinition foreignEntity = entityDefinitions.get(foreignKey.getReferencedEntityType().getName());
        if (foreignEntity == null) {
          throw new IllegalArgumentException("Entity '" + foreignKey.getReferencedEntityType()
                  + "' referenced by entity '" + entityType + "' via foreign key property '"
                  + foreignKey + "' has not been defined");
        }
      }
    }
  }

  private void populateForeignDefinitions() {
    for (final DefaultEntityDefinition definition : entityDefinitions.values()) {
      for (final ForeignKey foreignKey : definition.getForeignKeys()) {
        final EntityDefinition referencedDefinition = entityDefinitions.get(foreignKey.getReferencedEntityType().getName());
        if (referencedDefinition != null && !definition.hasForeignDefinition(foreignKey)) {
          definition.setForeignDefinition(foreignKey, referencedDefinition);
        }
      }
    }
  }

  private void readObject(final ObjectInputStream stream) throws IOException, ClassNotFoundException {
    stream.defaultReadObject();
    REGISTERED_ENTITIES.put(domainType, this);
  }

}
