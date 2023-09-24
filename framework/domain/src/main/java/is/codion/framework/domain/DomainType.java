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
package is.codion.framework.domain;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;

import static java.util.Objects.requireNonNull;

/**
 * Identifies a domain model and serves as a factory for {@link EntityType}
 * instances associated with this domain model type.
 */
public interface DomainType {

  /**
   * @return the domain name
   */
  String name();

  /**
   * Instantiates a {@link EntityType} associated with this domain type.
   * If this entity type has been defined previously that instance is returned.
   * @param name the entity type name
   * @return a {@link EntityType} with the given name
   */
  EntityType entityType(String name);

  /**
   * Instantiates a {@link EntityType} associated with this domain type.
   * If this entity type has been defined previously that instance is returned.
   * @param name the entity type name
   * @param entityClass the entity representation class
   * @param <T> the Entity type
   * @return a {@link EntityType} with the given name
   */
  <T extends Entity> EntityType entityType(String name, Class<T> entityClass);

  /**
   * Instantiates a {@link EntityType} associated with this domain type.
   * If this entity type has been defined previously that instance is returned.
   * @param name the entity type name
   * @param resourceBundleName the name of a resource bundle to use for captions, if any
   * @return a {@link EntityType} with the given name
   */
  EntityType entityType(String name, String resourceBundleName);

  /**
   * Instantiates a {@link EntityType} associated with this domain type.
   * If this entity type has been defined previously that instance is returned.
   * @param name the entity type name
   * @param entityClass the entity representation class
   * @param resourceBundleName the name of a resource bundle to use for captions, if any
   * @param <T> the Entity type
   * @return a {@link EntityType} with the given name
   */
  <T extends Entity> EntityType entityType(String name, Class<T> entityClass, String resourceBundleName);

  /**
   * @param entityType the entity type
   * @return true if this domain type contains the given entity type
   */
  boolean contains(EntityType entityType);

  /**
   * Returns a new {@link DomainType} using the given classes simple name as domain name.
   * @param domainClass the domain class
   * @return a {@link DomainType}
   */
  static DomainType domainType(Class<?> domainClass) {
    return domainType(requireNonNull(domainClass, "domainClass").getSimpleName());
  }

  /**
   * Returns a {@link DomainType} instance with the given name.
   * @param domainName domain name
   * @return a {@link DomainType} with the given name
   */
  static DomainType domainType(String domainName) {
    return DefaultDomainType.getOrCreateDomainType(domainName);
  }

  /**
   * Returns the domain type with the given name.
   * @param domainName the domain name
   * @return the domain type with the given name
   * @throws IllegalArgumentException in case the domain has not been defined
   */
  static DomainType domainTypeByName(String domainName) {
    return DefaultDomainType.getDomainType(domainName);
  }
}
