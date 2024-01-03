/*
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static is.codion.common.NullOrEmpty.nullOrEmpty;
import static java.util.Objects.requireNonNull;

final class DefaultDomainType implements DomainType, Serializable {

  private static final long serialVersionUID = 1;

  private static final Map<String, DefaultDomainType> DOMAIN_TYPES = new ConcurrentHashMap<>();
  private static final Map<DomainType, Map<String, EntityType>> DOMAIN_ENTITY_TYPES = new ConcurrentHashMap<>();

  private final String domainName;

  private DefaultDomainType(String domainName) {
    if (nullOrEmpty(domainName)) {
      throw new IllegalArgumentException("domainName must be a non-empty string");
    }
    this.domainName = domainName;
  }

  @Override
  public String name() {
    return domainName;
  }

  @Override
  public EntityType entityType(String name) {
    return entityType(name, Entity.class);
  }

  @Override
  public <T extends Entity> EntityType entityType(String name, Class<T> entityClass) {
    return entityTypes().computeIfAbsent(requireNonNull(name, "name"), entityTypeName ->
            EntityType.entityType(entityTypeName, this, entityClass));
  }

  @Override
  public EntityType entityType(String name, String resourceBundleName) {
    return entityTypes().computeIfAbsent(requireNonNull(name, "name"), entityTypeName ->
            EntityType.entityType(entityTypeName, this, resourceBundleName));
  }

  @Override
  public <T extends Entity> EntityType entityType(String name, Class<T> entityClass,
                                                  String resourceBundleName) {
    return entityTypes().computeIfAbsent(requireNonNull(name, "name"), entityTypeName ->
            EntityType.entityType(entityTypeName, this, entityClass, resourceBundleName));
  }

  @Override
  public boolean contains(EntityType entityType) {
    return entityTypes().containsKey(requireNonNull(entityType).name());
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null || getClass() != object.getClass()) {
      return false;
    }
    DefaultDomainType that = (DefaultDomainType) object;

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

  private Map<String, EntityType> entityTypes() {
    return DOMAIN_ENTITY_TYPES.computeIfAbsent(this, k -> new ConcurrentHashMap<>());
  }

  private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
    stream.defaultReadObject();
    DOMAIN_TYPES.put(domainName, this);
  }

  static DomainType getOrCreateDomainType(String domainName) {
    return DOMAIN_TYPES.computeIfAbsent(requireNonNull(domainName), DefaultDomainType::new);
  }

  static DomainType getDomainType(String domainName) {
    DomainType domainType = DOMAIN_TYPES.get(requireNonNull(domainName, "domainName"));
    if (domainType == null) {
      throw new IllegalArgumentException("Domain: " + domainName + " has not been defined");
    }

    return domainType;
  }
}
