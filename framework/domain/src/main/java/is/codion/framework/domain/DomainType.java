/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain;

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
   * @param name the entity type name
   * @return a {@link EntityType} with the given name
   * @throws IllegalArgumentException in case an entity type with the given name already exists
   */
  EntityType entityType(String name);

  /**
   * Returns the {@link EntityType} with the given name.
   * @param name the entity type name
   * @return the {@link EntityType} with the given name.
   * @throws IllegalArgumentException in case the entity type is not found
   */
  EntityType getEntityType(String name);

  /**
   * Returns a new {@link DomainType} using the given classes simple name as domain name.
   * @param domainClass the domain class
   * @return a {@link DomainType}
   */
  static DomainType domainType(final Class<?> domainClass) {
    return domainType(requireNonNull(domainClass, "domainClass").getSimpleName());
  }

  /**
   * Returns a new {@link DomainType} instance with the given name.
   * @param domainName domain name
   * @return a {@link DomainType} with the given name
   */
  static DomainType domainType(final String domainName) {
    return new DefaultDomainType(domainName);
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
