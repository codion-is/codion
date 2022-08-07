/*
 * Copyright (c) 2021 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json.domain;

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
  EntityObjectMapper createEntityObjectMapper(Entities entities);

  /**
   * Returns true if this mapper factory is compatible with the given domain type.
   * @param domainType the domain type
   * @return true if this mapper factory is compatible with the given domain type
   */
  boolean isCompatibleWith(DomainType domainType);

  /**
   * Returns the first available {@link EntityObjectMapperFactory} instance compatible with the given domain type,
   * if no such mapper factory is available a default one is returned.
   * @param domainType the domain type for which to find a mapper factory
   * @return a {@link EntityObjectMapperFactory} instance compatible with the given domain type.
   */
  static EntityObjectMapperFactory instance(DomainType domainType) {
    requireNonNull(domainType);
    ServiceLoader<EntityObjectMapperFactory> loader = ServiceLoader.load(EntityObjectMapperFactory.class);
    for (EntityObjectMapperFactory factory : loader) {
      if (factory.isCompatibleWith(domainType)) {
        return factory;
      }
    }

    return new EntityObjectMapperFactory() {
      @Override
      public EntityObjectMapper createEntityObjectMapper(Entities entities) {
        return EntityObjectMapper.createEntityObjectMapper(entities);
      }

      @Override
      public boolean isCompatibleWith(DomainType domainType) {
        return true;
      }
    };
  }
}
