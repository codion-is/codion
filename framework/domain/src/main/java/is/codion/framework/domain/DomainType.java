/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;

import java.io.Serializable;

import static java.util.Objects.requireNonNull;

/**
 * Identifies a domain model and serves as a factory for {@link EntityType}
 * instances associated with this domain model type.
 */
public interface DomainType extends Serializable {

  /**
   * @return the domain name
   */
  String getName();

  /**
   * Instantiates a {@link EntityType} associated with this domain type.
   * If this entity type has been defined previously that instance is returned.
   * @param name the entity type name
   * @return a {@link EntityType} with the given name
   */
  EntityType<Entity> entityType(String name);

  /**
   * Instantiates a {@link EntityType} associated with this domain type.
   * If this entity type has been defined previously that instance is returned.
   * @param name the entity type name
   * @param entityClass the entity representation class
   * @param <T> the Entity type
   * @return a {@link EntityType} with the given name
   */
  <T extends Entity> EntityType<T> entityType(String name, Class<T> entityClass);

  /**
   * @param entityType the entity type
   * @return true if this domain type contains the given entity type
   */
  boolean contains(EntityType<?> entityType);

  /**
   * Returns a new {@link DomainType} using the given classes simple name as domain name.
   * @param domainClass the domain class
   * @return a {@link DomainType}
   */
  static DomainType domainType(final Class<?> domainClass) {
    return domainType(requireNonNull(domainClass, "domainClass").getSimpleName());
  }

  /**
   * Returns a {@link DomainType} instance with the given name.
   * @param domainName domain name
   * @return a {@link DomainType} with the given name
   */
  static DomainType domainType(final String domainName) {
    return DefaultDomainType.getOrCreateDomainType(domainName);
  }

  /**
   * Returns a {@link DomainType} extending the one given, sharing entity definitions.
   * @param domainToExtend the domain type to extend
   * @param domainClass the domain class
   * @return an extended {@link DomainType}
   */
  static DomainType extendDomain(final DomainType domainToExtend, final Class<?> domainClass) {
    return extendDomain(domainToExtend, domainClass.getSimpleName());
  }

  /**
   * Returns a {@link DomainType} extending the one given, sharing entity definitions.
   * @param domainToExtend the domain type to extend
   * @param domainName the domain name
   * @return an extended {@link DomainType}
   */
  static DomainType extendDomain(final DomainType domainToExtend, final String domainName) {
    return DefaultDomainType.getOrExtendDomainType(domainToExtend, domainName);
  }

  /**
   * Returns the domain type with the given name.
   * @param domainName the domain name
   * @return the domain type with the given name
   * @throws IllegalArgumentException in case the domain has not been defined
   */
  static DomainType getDomainType(final String domainName) {
    return DefaultDomainType.getDomainType(domainName);
  }
}
