/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.DomainType;

import java.util.Collection;
import java.util.List;

/**
 * A repository specifying the {@link EntityDefinition}s for a given domain.
 * Factory for {@link Entity} and {@link Key} instances.
 */
public interface Entities {

  /**
   * @return the {@link DomainType} this {@link Entities} instance is associated with
   */
  DomainType getDomainType();

  /**
   * Returns the {@link EntityDefinition} for the given entityType
   * @param entityType the entityType
   * @return the entity definition
   * @throws IllegalArgumentException in case the definition is not found
   */
  EntityDefinition getDefinition(EntityType<?> entityType);

  /**
   * Returns the {@link EntityDefinition} for the given entityType name
   * @param entityTypeName the name of the entityType
   * @return the entity definition
   * @throws IllegalArgumentException in case the definition is not found
   */
   EntityDefinition getDefinition(String entityTypeName);

  /**
   * @param entityType the entityType
   * @return true if this domain contains a definition for the given type
   */
  boolean contains(EntityType<?> entityType);

  /**
   * Returns all {@link EntityDefinition}s available
   * @return all entity definitions
   */
  Collection<EntityDefinition> getDefinitions();

  /**
   * Creates a new {@link Entity} instance with the given entityType
   * @param entityType the entityType
   * @return a new {@link Entity} instance
   */
  Entity entity(EntityType<?> entityType);

  /**
   * Creates a new {@link Entity} instance with the given primary key
   * @param key the primary key
   * @return a new {@link Entity} instance
   */
  Entity entity(Key key);

  /**
   * Creates a new {@link Key} instance with the given entityType
   * @param entityType the entityType
   * @return a new {@link Key} instance
   */
  Key primaryKey(EntityType<?> entityType);

  /**
   * Creates a new {@link Key} instance with the given entityType, initialised with the given value
   * @param entityType the entityType
   * @param value the key value, assumes a single integer key
   * @return a new {@link Key} instance
   * @throws IllegalArgumentException in case the given primary key is a composite key
   * @throws NullPointerException in case entityType or value is null
   */
  Key primaryKey(EntityType<?> entityType, Integer value);

  /**
   * Creates a new {@link Key} instance with the given entityType, initialised with the given value
   * @param entityType the entityType
   * @param value the key value, assumes a single long key
   * @return a new {@link Key} instance
   * @throws IllegalArgumentException in case the given primary key is a composite key
   * @throws NullPointerException in case entityType or value is null
   */
  Key primaryKey(EntityType<?> entityType, Long value);

  /**
   * Creates new {@link Key} instances with the given entityType, initialised with the given values
   * @param entityType the entityType
   * @param values the key values, assumes a single integer key
   * @return new {@link Key} instances
   * @throws IllegalArgumentException in case the given primary key is a composite key
   * @throws NullPointerException in case entityType or values is null
   */
  List<Key> primaryKeys(EntityType<?> entityType, Integer... values);

  /**
   * Creates new {@link Key} instances with the given entityType, initialised with the given values
   * @param entityType the entityType
   * @param values the key values, assumes a single integer key
   * @return new {@link Key} instances
   * @throws IllegalArgumentException in case the given primary key is a composite key
   * @throws NullPointerException in case entityType or values is null
   */
  List<Key> primaryKeys(EntityType<?> entityType, Long... values);

  /**
   * Copies the given entities, with new copied instances of all foreign key value entities.
   * @param entities the entities to copy
   * @return deep copies of the entities, in the same order as they are received
   */
  List<Entity> deepCopyEntities(List<? extends Entity> entities);

  /**
   * Copies the given entity.
   * @param entity the entity to copy
   * @return copy of the given entity
   */
  Entity copyEntity(Entity entity);

  /**
   * Copies the given entity, with new copied instances of all foreign key value entities.
   * @param entity the entity to copy
   * @return a deep copy of the given entity
   */
  Entity deepCopyEntity(Entity entity);

  /**
   * Casts the given entities to the given type.
   * @param type the type
   * @param entities the entities
   * @param <T> the entity type
   * @return typed entities
   * @throws IllegalArgumentException in case any of the entities is not of the given type
   */
  <T extends Entity> List<T> castTo(EntityType<T> type, List<Entity> entities);

  /**
   * Casts the given entity to the given type.
   * @param type the type
   * @param entity the entity
   * @param <T> the entity type
   * @return a typed entity
   * @throws IllegalArgumentException in case the entity is not of the given type
   */
  <T extends Entity> T castTo(EntityType<T> type, Entity entity);
}
