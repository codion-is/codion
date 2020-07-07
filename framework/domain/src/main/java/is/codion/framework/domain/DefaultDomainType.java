/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.EntityTypes;

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
  private final Map<String, EntityType<?>> entityTypes;

  private DefaultDomainType(final String domainName) {
    this(null, domainName);
  }

  private DefaultDomainType(final DefaultDomainType domainToExtend, final String domainName) {
    if (nullOrEmpty(domainName)) {
      throw new IllegalArgumentException("domainName must be a non-empty string");
    }
    this.domainName = domainName;
    this.entityTypes = domainToExtend == null ? new ConcurrentHashMap<>() : domainToExtend.entityTypes;
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
            EntityTypes.entityType(entityTypeName, this.domainName, entityClass));
  }

  @Override
  public boolean contains(final EntityType<?> entityType) {
    return entityTypes.containsKey(requireNonNull(entityType).getName());
  }

  @Override
  public DomainType extend(final Class<?> domainClass) {
    return extend(requireNonNull(domainClass, "domainClass").getSimpleName());
  }

  @Override
  public DomainType extend(final String domainName) {
    return getOrExtendDomainType(this, requireNonNull(domainName, "domainName"));
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

  private static DomainType getOrExtendDomainType(final DomainType domainToExtend, final String domainName) {
    return DOMAIN_TYPES.computeIfAbsent(domainName, name ->
            new DefaultDomainType((DefaultDomainType) getDomainType(domainToExtend.getName()), domainName));
  }
}
