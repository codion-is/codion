/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static is.codion.common.Util.nullOrEmpty;
import static java.util.Objects.requireNonNull;

final class DefaultDomainType implements DomainType, Serializable {

  private static final long serialVersionUID = 1;

  private static final Map<String, DefaultDomainType> DOMAIN_TYPES = new ConcurrentHashMap<>();

  private final String domainName;
  private final Map<String, EntityType<?>> entityTypes = new ConcurrentHashMap<>();

  private DefaultDomainType(final String domainName) {
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
  public EntityType<Entity> entityType(final String name) {
    return entityType(name, Entity.class);
  }

  @Override
  public <T extends Entity> EntityType<T> entityType(final String name, final Class<T> entityClass) {
    return (EntityType<T>) entityTypes.computeIfAbsent(requireNonNull(name, "name"), entityTypeName ->
            EntityType.entityType(entityTypeName, this.domainName, entityClass));
  }

  @Override
  public <T extends Entity> EntityType<T> entityType(final String name, final String resourceBundleName) {
    return (EntityType<T>) entityTypes.computeIfAbsent(requireNonNull(name, "name"), entityTypeName ->
            EntityType.entityType(entityTypeName, this.domainName, resourceBundleName));
  }

  @Override
  public <T extends Entity> EntityType<T> entityType(final String name, final Class<T> entityClass,
                                                     final String resourceBundleName) {
    return (EntityType<T>) entityTypes.computeIfAbsent(requireNonNull(name, "name"), entityTypeName ->
            EntityType.entityType(entityTypeName, this.domainName, entityClass, resourceBundleName));
  }

  @Override
  public boolean contains(final EntityType<?> entityType) {
    return entityTypes.containsKey(requireNonNull(entityType).getName());
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

  private void readObject(final ObjectInputStream stream) throws IOException, ClassNotFoundException {
    stream.defaultReadObject();
    DOMAIN_TYPES.put(domainName, this);
  }

  static DomainType getOrCreateDomainType(final String domainName) {
    return DOMAIN_TYPES.computeIfAbsent(requireNonNull(domainName), DefaultDomainType::new);
  }

  static DomainType getDomainType(final String domainName) {
    final DomainType domainType = DOMAIN_TYPES.get(requireNonNull(domainName, "domainName"));
    if (domainType == null) {
      throw new IllegalArgumentException("Domain: " + domainName + " has not been defined");
    }

    return domainType;
  }
}
