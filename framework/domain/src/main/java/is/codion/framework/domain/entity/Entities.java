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
 * Copyright (c) 2019 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity;

import is.codion.common.Configuration;
import is.codion.common.property.PropertyValue;
import is.codion.framework.domain.DomainType;

import java.util.Collection;
import java.util.List;

/**
 * A repository containing the {@link EntityDefinition}s for a given domain.
 * Factory for {@link Entity} and {@link Entity.Key} instances.
 * @see #entity(EntityType)
 * @see #builder(EntityType)
 * @see #primaryKey(EntityType, Object)
 * @see #primaryKeys(EntityType, Object[])
 * @see #keyBuilder(EntityType)
 */
public interface Entities {

  /**
   * Specifies whether strict deserialization should be used. This means that when an unknown attribute<br>
   * is encountered during deserialization, an exception is thrown, instead of silently dropping the associated value.<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  PropertyValue<Boolean> STRICT_DESERIALIZATION =
          Configuration.booleanValue("codion.domain.strictDeserialization", true);

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
   * Returns all {@link EntityDefinition}s found in this Entities instance
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
   * Creates a new {@link Entity.Builder} instance for the given entityType
   * @param entityType the entityType
   * @return a new {@link Entity.Builder}
   */
  Entity.Builder builder(EntityType entityType);

  /**
   * Creates a new {@link Entity.Key} instance of the given entityType, initialised with the given value
   * @param entityType the entityType
   * @param value the key value, assumes a single value key
   * @param <T> the key value type
   * @return a new {@link Entity.Key} instance
   * @throws IllegalStateException in case the given primary key is a composite key
   * @throws IllegalArgumentException in case the value is not of the correct type
   * @throws NullPointerException in case entityType is null
   */
  <T> Entity.Key primaryKey(EntityType entityType, T value);

  /**
   * Creates new {@link Entity.Key} instances of the given entityType, initialised with the given values
   * @param entityType the entityType
   * @param values the key values, assumes a single value key
   * @param <T> the key value type
   * @return new {@link Entity.Key} instances
   * @throws IllegalStateException in case the given primary key is a composite key
   * @throws IllegalArgumentException in case any of the values is not of the correct type
   * @throws NullPointerException in case entityType or values is null
   */
  <T> List<Entity.Key> primaryKeys(EntityType entityType, T... values);

  /**
   * Creates a new {@link Entity.Key.Builder} instance for the given entity type
   * @param entityType the entity type
   * @return a new builder
   */
  Entity.Key.Builder keyBuilder(EntityType entityType);
}
