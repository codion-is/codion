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
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.json.domain;

import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Entities;

import java.util.ServiceLoader;

import static java.util.Objects.requireNonNull;

/**
 * Provides {@link EntityObjectMapper} instances for a given domain.<br>
 * {@link DefaultEntityObjectMapperFactory} is provided for
 */
public interface EntityObjectMapperFactory {

  /**
   * Creates a new {@link EntityObjectMapper} instance.
   * @param entities the domain entities
   * @return a new {@link EntityObjectMapper} instance.
   */
  default EntityObjectMapper entityObjectMapper(Entities entities) {
    return EntityObjectMapper.entityObjectMapper(entities);
  }

  /**
   * Returns true if this mapper factory is compatible with the given domain type.
   * @param domainType the domain type
   * @return true if this mapper factory is compatible with the given domain type
   */
  boolean compatibleWith(DomainType domainType);

  /**
   * Returns the first available {@link EntityObjectMapperFactory} instance compatible with the given domain type,
   * if no such mapper factory is available a default one, compatible with all domain models, is returned.
   * @param domainType the domain type for which to find a mapper factory
   * @return a {@link EntityObjectMapperFactory} instance compatible with the given domain type.
   */
  static EntityObjectMapperFactory instance(DomainType domainType) {
    requireNonNull(domainType);
    ServiceLoader<EntityObjectMapperFactory> loader = ServiceLoader.load(EntityObjectMapperFactory.class);
    for (EntityObjectMapperFactory factory : loader) {
      if (factory.compatibleWith(domainType)) {
        return factory;
      }
    }

    //compatible with all domain models
    return mapperDomainType -> true;
  }
}
