/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain;

import is.codion.framework.domain.entity.EntityType;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static is.codion.common.Util.nullOrEmpty;
import static java.util.Objects.requireNonNull;

final class DefaultDomainType implements DomainType {

  private static final long serialVersionUID = 1;

  private static final Map<String, DomainType> DOMAIN_TYPES = new ConcurrentHashMap<>();

  private final String domainName;

  private final transient Map<String, EntityType> entityTypes = new HashMap<>();

  DefaultDomainType(final String domainName) {
    if (nullOrEmpty(domainName)) {
      throw new IllegalArgumentException("domainName must be a non-empty string");
    }
    this.domainName = domainName;
  }

  @Override
  public String getName() {
    return domainName;
  }

  @Override
  public EntityType entityType(final String name) {
    final EntityType entityType;
    synchronized (entityTypes) {
      if (entityTypes.containsKey(name)) {
        throw new IllegalArgumentException("entityType with name '" + name + "' has already been defined for domain: " + domainName);
      }
      entityType = EntityType.entityType(name, this.domainName);
      entityTypes.put(name, entityType);
    }

    return entityType;
  }

  @Override
  public EntityType getEntityType(final String name) {
    synchronized (entityTypes) {
      final EntityType entityType = entityTypes.get(name);
      if (entityType == null) {
        throw new IllegalArgumentException("entityType with name '" + name + "' not found in domain: " + domainName);
      }

      return entityType;
    }
  }

  @Override
  public boolean equals(final Object object) {
    if (this == object) {
      return true;
    }
    if (object == null || getClass() != object.getClass()) {
      return false;
    }
    final DefaultDomainType that = (DefaultDomainType) object;

    return domainName.equals(that.domainName);
  }

  @Override
  public int hashCode() {
    return domainName.hashCode();
  }

  @Override
  public String toString() {
    return domainName;
  }

  static void register(final DomainType domainType) {
    DOMAIN_TYPES.put(requireNonNull(domainType, "domainType").getName(), domainType);
  }

  static DomainType getDomainType(final String domainName) {
    final DomainType domainType = DOMAIN_TYPES.get(requireNonNull(domainName, "domainName"));
    if (domainType == null) {
      throw new IllegalArgumentException("Undefined domain: " + domainName);
    }

    return domainType;
  }
}
