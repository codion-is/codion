/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.temporal.Temporal;
import java.util.Objects;

import static is.codion.common.Util.nullOrEmpty;
import static java.util.Objects.requireNonNull;

class DefaultAttribute<T> implements Attribute<T>, Serializable {

  private static final long serialVersionUID = 1;

  private final String name;
  private final Class<T> typeClass;
  private final int hashCode;
  private final EntityType entityType;

  DefaultAttribute(String name, Class<T> typeClass, EntityType entityType) {
    if (nullOrEmpty(name)) {
      throw new IllegalArgumentException("name must be a non-empty string");
    }
    this.name = name;
    this.entityType = requireNonNull(entityType, "entityType");
    this.typeClass = requireNonNull(typeClass, "typeClass");
    this.hashCode = Objects.hash(name, entityType);
  }

  @Override
  public final String getName() {
    return name;
  }

  @Override
  public final Class<T> getTypeClass() {
    return typeClass;
  }

  @Override
  public final EntityType getEntityType() {
    return entityType;
  }

  @Override
  public final T validateType(T value) {
    if (value != null && typeClass != value.getClass() && !typeClass.isAssignableFrom(value.getClass())) {
      throw new IllegalArgumentException("Value of type " + typeClass +
              " expected for property " + this + " in entity " + entityType + ", got: " + value.getClass());
    }

    return value;
  }

  @Override
  public final boolean isType(Class<?> typeClass) {
    return this.typeClass.equals(typeClass);
  }

  @Override
  public final boolean isNumerical() {
    return isInteger() || isDecimal() || isLong();
  }

  @Override
  public final boolean isTemporal() {
    return Temporal.class.isAssignableFrom(typeClass);
  }

  @Override
  public final boolean isLocalDate() {
    return isType(LocalDate.class);
  }

  @Override
  public final boolean isLocalDateTime() {
    return isType(LocalDateTime.class);
  }

  @Override
  public final boolean isLocalTime() {
    return isType(LocalTime.class);
  }

  @Override
  public final boolean isOffsetDateTime() {
    return isType(OffsetDateTime.class);
  }

  @Override
  public final boolean isCharacter() {
    return isType(Character.class);
  }

  @Override
  public final boolean isString() {
    return isType(String.class);
  }

  @Override
  public final boolean isLong() {
    return isType(Long.class);
  }

  @Override
  public final boolean isInteger() {
    return isType(Integer.class);
  }

  @Override
  public final boolean isDouble() {
    return isType(Double.class);
  }

  @Override
  public final boolean isBigDecimal() {
    return isType(BigDecimal.class);
  }

  @Override
  public final boolean isDecimal() {
    return isDouble() || isBigDecimal();
  }

  @Override
  public final boolean isBoolean() {
    return isType(Boolean.class);
  }

  @Override
  public final boolean isByteArray() {
    return isType(byte[].class);
  }

  @Override
  public final boolean isEntity() {
    return isType(Entity.class);
  }

  @Override
  public final boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof DefaultAttribute)) {
      return false;
    }
    DefaultAttribute<?> that = (DefaultAttribute<?>) object;

    return hashCode == that.hashCode && name.equals(that.name) && entityType.equals(that.entityType);
  }

  @Override
  public final int hashCode() {
    return hashCode;
  }

  @Override
  public final String toString() {
    return entityType.getName() + "." + name;
  }
}
