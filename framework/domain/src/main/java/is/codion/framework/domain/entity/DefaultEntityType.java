/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

import static is.codion.common.Util.nullOrEmpty;
import static java.util.Objects.requireNonNull;

final class DefaultEntityType<T extends Entity> implements EntityType<T>, Serializable {

  private static final long serialVersionUID = 1;

  private final String domainName;
  private final String name;
  private final Class<T> entityClass;
  private final int hashCode;

  DefaultEntityType(final String domainName, final String typeName, final Class<T> entityClass) {
    if (nullOrEmpty(typeName)) {
      throw new IllegalArgumentException("typeName must be a non-empty string");
    }
    if (nullOrEmpty(domainName)) {
      throw new IllegalArgumentException("domainName must be a non-empty string");
    }
    this.domainName = domainName;
    this.name = typeName;
    this.entityClass = requireNonNull(entityClass, "entityClass");
    this.hashCode = Objects.hash(typeName, domainName);
  }

  @Override
  public String getDomainName() {
    return domainName;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Class<T> getEntityClass() {
    return entityClass;
  }

  @Override
  public <T> Attribute<T> attribute(final String name, final Class<T> typeClass) {
    return new DefaultAttribute<>(name, typeClass, this);
  }

  @Override
  public Attribute<Object> objectAttribute(final String name) {
    return attribute(name, Object.class);
  }

  @Override
  public Attribute<Long> longAttribute(final String name) {
    return attribute(name, Long.class);
  }

  @Override
  public Attribute<Integer> integerAttribute(final String name) {
    return attribute(name, Integer.class);
  }

  @Override
  public Attribute<Double> doubleAttribute(final String name) {
    return attribute(name, Double.class);
  }

  @Override
  public Attribute<BigDecimal> bigDecimalAttribute(final String name) {
    return attribute(name, BigDecimal.class);
  }

  @Override
  public Attribute<LocalDate> localDateAttribute(final String name) {
    return attribute(name, LocalDate.class);
  }

  @Override
  public Attribute<LocalTime> localTimeAttribute(final String name) {
    return attribute(name, LocalTime.class);
  }

  @Override
  public Attribute<LocalDateTime> localDateTimeAttribute(final String name) {
    return attribute(name, LocalDateTime.class);
  }

  @Override
  public Attribute<String> stringAttribute(final String name) {
    return attribute(name, String.class);
  }

  @Override
  public Attribute<Character> characterAttribute(final String name) {
    return attribute(name, Character.class);
  }

  @Override
  public Attribute<Boolean> booleanAttribute(final String name) {
    return attribute(name, Boolean.class);
  }

  @Override
  public Attribute<Entity> entityAttribute(final String name) {
    return attribute(name, Entity.class);
  }

  @Override
  public Attribute<byte[]> blobAttribute(final String name) {
    return attribute(name, byte[].class);
  }

  @Override
  public ConditionType conditionType(final String name) {
    return new DefaultConditionType(this, name);
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  @Override
  public boolean equals(final Object object) {
    if (this == object) {
      return true;
    }
    if (object == null || getClass() != object.getClass()) {
      return false;
    }
    final DefaultEntityType<?> that = (DefaultEntityType<?>) object;

    return name.equals(that.name) && domainName.equals(that.domainName);
  }
}
