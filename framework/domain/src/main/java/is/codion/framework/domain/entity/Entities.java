/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.common.Configuration;
import is.codion.common.properties.PropertyValue;
import is.codion.framework.domain.DomainType;

import java.util.Collection;
import java.util.List;

/**
 * A repository specifying the {@link EntityDefinition}s for a given domain.
 * Factory for {@link Entity} and {@link Key} instances.
 * @see #entity(EntityType)
 * @see #builder(EntityType)
 * @see #primaryKey(EntityType, Object)
 * @see #primaryKeys(EntityType, Object[])
 * @see #keyBuilder(EntityType)
 */
public interface Entities {

  PropertyValue<Boolean> LEGACY_SERIALIZATION =
          Configuration.booleanValue("codion.domain.legacySerialization", false);

  /**
   * @return the {@link DomainType} this {@link Entities} instance is associated with
   */
  DomainType domainType();

  /**
   * Returns the {@link EntityDefinition} for the given entityType
   * @param entityType the entityType
   * @return the entity definition
   * @throws IllegalArgumentException in case the definition is not found
   */
  EntityDefinition definition(EntityType entityType);

  /**
   * Returns the {@link EntityDefinition} for the given entityType name
   * @param entityTypeName the name of the entityType
   * @return the entity definition
   * @throws IllegalArgumentException in case the definition is not found
   */
  EntityDefinition definition(String entityTypeName);

  /**
   * @param entityType the entityType
   * @return true if this domain contains a definition for the given type
   */
  boolean contains(EntityType entityType);

  /**
   * Returns all {@link EntityDefinition}s available
   * @return all entity definitions
   */
  Collection<EntityDefinition> definitions();

  /**
   * Creates a new empty {@link Entity} instance of the given entityType
   * @param entityType the entityType
   * @return a new {@link Entity} instance
   */
  Entity entity(EntityType entityType);

  /**
   * Creates a new {@link Entity.Builder} instance.
   * @param entityType the entityType
   * @return a new {@link Entity.Builder}
   */
  Entity.Builder builder(EntityType entityType);

  /**
   * Creates a new {@link Key} instance of the given entityType, initialised with the given value
   * @param entityType the entityType
   * @param value the key value, assumes a single value key
   * @param <T> the key value type
   * @return a new {@link Key} instance
   * @throws IllegalStateException in case the given primary key is a composite key
   * @throws IllegalArgumentException in case the value is not of the correct type
   * @throws NullPointerException in case entityType is null
   */
  <T> Key primaryKey(EntityType entityType, T value);

  /**
   * Creates new {@link Key} instances of the given entityType, initialised with the given values
   * @param entityType the entityType
   * @param values the key values, assumes a single value key
   * @param <T> the key value type
   * @return new {@link Key} instances
   * @throws IllegalStateException in case the given primary key is a composite key
   * @throws IllegalArgumentException in case any of the values is not of the correct type
   * @throws NullPointerException in case entityType or values is null
   */
  <T> List<Key> primaryKeys(EntityType entityType, T... values);

  /**
   * Creates a new {@link Key.Builder} instance for the given entity type.
   * @param entityType the entity type
   * @return a new builder
   */
  Key.Builder keyBuilder(EntityType entityType);
}
