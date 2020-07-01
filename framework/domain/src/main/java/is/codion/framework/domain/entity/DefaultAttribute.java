/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

import static is.codion.common.Util.nullOrEmpty;
import static java.util.Objects.requireNonNull;

final class DefaultAttribute<T> implements Attribute<T> {

  private static final long serialVersionUID = 1;

  private final String name;
  private final Class<T> typeClass;
  private final int hashCode;
  private final EntityType<?> entityType;

  DefaultAttribute(final String name, final Class<T> typeClass, final EntityType<?> entityType) {
    if (nullOrEmpty(name)) {
      throw new IllegalArgumentException("name must be a non-empty string");
    }
    this.name = name;
    this.entityType = requireNonNull(entityType, "entityType");
    this.typeClass = typeClass;
    this.hashCode = Objects.hash(name, entityType);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Class<T> getTypeClass() {
    return typeClass;
  }

  @Override
  public EntityType<?> getEntityType() {
    return entityType;
  }

  @Override
  public T validateType(final T value) {
    if (value != null && typeClass != value.getClass() && !typeClass.isAssignableFrom(value.getClass())) {
      throw new IllegalArgumentException("Value of type " + typeClass +
              " expected for property " + this + " in entity " + entityType + ", got: " + value.getClass());
    }

    return value;
  }

  @Override
  public boolean isType(final Class<?> typeClass) {
    return this.typeClass == typeClass;
  }

  @Override
  public boolean isNumerical() {
    return isInteger() || isDecimal() || isLong();
  }

  @Override
  public boolean isTemporal() {
    return isLocalDate() || isLocalDateTime() || isLocalTime();
  }

  @Override
  public boolean isLocalDate() {
    return isType(LocalDate.class);
  }

  @Override
  public boolean isLocalDateTime() {
    return isType(LocalDateTime.class);
  }

  @Override
  public boolean isLocalTime() {
    return isType(LocalTime.class);
  }

  @Override
  public boolean isCharacter() {
    return isType(Character.class);
  }

  @Override
  public boolean isString() {
    return isType(String.class);
  }

  @Override
  public boolean isLong() {
    return isType(Long.class);
  }

  @Override
  public boolean isInteger() {
    return isType(Integer.class);
  }

  @Override
  public boolean isDouble() {
    return isType(Double.class);
  }

  @Override
  public boolean isBigDecimal() {
    return isType(BigDecimal.class);
  }

  @Override
  public boolean isDecimal() {
    return isDouble() || isBigDecimal();
  }

  @Override
  public boolean isBoolean() {
    return isType(Boolean.class);
  }

  @Override
  public boolean isByteArray() {
    return isType(byte[].class);
  }

  @Override
  public boolean isEntity() {
    return isType(Entity.class);
  }

  @Override
  public boolean equals(final Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof DefaultAttribute)) {
      return false;
    }
    final DefaultAttribute<?> that = (DefaultAttribute<?>) object;

    return name.equals(that.name) && entityType.equals(that.entityType);
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  @Override
  public String toString() {
    return "entityType: " + entityType + ", name: " + name;
  }
}
