/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

import static is.codion.common.Util.nullOrEmpty;
import static java.util.Objects.requireNonNull;

class DefaultAttribute<T> implements Attribute<T> {

  private static final long serialVersionUID = 1;

  private final String name;
  private final int type;
  private final Class<T> typeClass;
  private final int hashCode;
  private final EntityType entityType;

  DefaultAttribute(final String name, final Class<T> typeClass, final EntityType entityType) {
    if (nullOrEmpty(name)) {
      throw new IllegalArgumentException("name must be a non-empty string");
    }
    this.name = name;
    this.entityType = requireNonNull(entityType, "entityType");
    this.type = getSqlType(requireNonNull(typeClass, "typeClass"));
    this.typeClass = typeClass;
    this.hashCode = Objects.hash(name, entityType);
  }

  @Override
  public final String getName() {
    return name;
  }

  @Override
  public final int getType() {
    return type;
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
  public final T validateType(final T value) {
    if (value != null && typeClass != value.getClass() && !typeClass.isAssignableFrom(value.getClass())) {
      throw new IllegalArgumentException("Value of type " + typeClass +
              " expected for property " + this + " in entity " + entityType + ", got: " + value.getClass());
    }

    return value;
  }

  @Override
  public final boolean isType(final int type) {
    return this.type == type;
  }

  @Override
  public final boolean isNumerical() {
    return isInteger() || isDecimal() || isLong();
  }

  @Override
  public final boolean isTemporal() {
    return isDate() || isTimestamp() || isTime();
  }

  @Override
  public final boolean isDate() {
    return isType(Types.DATE);
  }

  @Override
  public final boolean isTimestamp() {
    return isType(Types.TIMESTAMP);
  }

  @Override
  public final boolean isTime() {
    return isType(Types.TIME);
  }

  @Override
  public final boolean isCharacter() {
    return isType(Types.CHAR);
  }

  @Override
  public final boolean isString() {
    return isType(Types.VARCHAR);
  }

  @Override
  public final boolean isLong() {
    return isType(Types.BIGINT);
  }

  @Override
  public final boolean isInteger() {
    return isType(Types.INTEGER);
  }

  @Override
  public final boolean isDouble() {
    return isType(Types.DOUBLE);
  }

  @Override
  public final boolean isBigDecimal() {
    return isType(Types.DECIMAL);
  }

  @Override
  public final boolean isDecimal() {
    return isDouble() || isBigDecimal();
  }

  @Override
  public final boolean isBoolean() {
    return isType(Types.BOOLEAN);
  }

  @Override
  public final boolean isBlob() {
    return isType(Types.BLOB);
  }

  @Override
  public boolean isEntity() {
    return false;
  }

  @Override
  public final boolean equals(final Object object) {
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
  public final int hashCode() {
    return hashCode;
  }

  @Override
  public final String toString() {
    return "entityType: " + entityType + ", name: " + name;
  }

  private static int getSqlType(final Class<?> clazz) {
    if (clazz.equals(Long.class)) {
      return Types.BIGINT;
    }
    if (clazz.equals(Integer.class)) {
      return Types.INTEGER;
    }
    if (clazz.equals(Double.class)) {
      return Types.DOUBLE;
    }
    if (clazz.equals(BigDecimal.class)) {
      return Types.DECIMAL;
    }
    if (clazz.equals(LocalDate.class)) {
      return Types.DATE;
    }
    if (clazz.equals(LocalTime.class)) {
      return Types.TIME;
    }
    if (clazz.equals(LocalDateTime.class)) {
      return Types.TIMESTAMP;
    }
    if (clazz.equals(String.class)) {
      return Types.VARCHAR;
    }
    if (clazz.equals(Boolean.class)) {
      return Types.BOOLEAN;
    }
    if (clazz.equals(byte[].class)) {
      return Types.BLOB;
    }
    if (clazz.equals(Entity.class)) {
      return Types.JAVA_OBJECT;
    }
    if (Object.class.isAssignableFrom(clazz)) {
      return Types.JAVA_OBJECT;
    }

    return Types.OTHER;
  }
}
