/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.common.Util;
import is.codion.framework.domain.entity.Entity;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static java.util.Objects.requireNonNull;

class DefaultAttribute<T> implements Attribute<T> {

  private static final long serialVersionUID = 1;

  private final String name;
  private final int type;
  private final Class<T> typeClass;

  DefaultAttribute(final String name, final Class<T> typeClass) {
    if (Util.nullOrEmpty(name)) {
      throw new IllegalArgumentException("name must be a non-empty string");
    }
    requireNonNull(typeClass, "typeClass");
    this.name = name;
    this.type = getSqlType(typeClass);
    this.typeClass = typeClass;
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
  public final boolean equals(final Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof DefaultAttribute)) {
      return false;
    }

    final DefaultAttribute<?> that = (DefaultAttribute<?>) object;

    return name.equals(that.name);
  }

  @Override
  public final int hashCode() {
    return name.hashCode();
  }

  @Override
  public final String toString() {
    return name;
  }

  private int getSqlType(final Class<T> typeClass) {
    if (typeClass.equals(Long.class)) {
      return Types.BIGINT;
    }
    if (typeClass.equals(Integer.class)) {
      return Types.INTEGER;
    }
    if (typeClass.equals(Double.class)) {
      return Types.DOUBLE;
    }
    if (typeClass.equals(BigDecimal.class)) {
      return Types.DECIMAL;
    }
    if (typeClass.equals(LocalDate.class)) {
      return Types.DATE;
    }
    if (typeClass.equals(LocalTime.class)) {
      return Types.TIME;
    }
    if (typeClass.equals(LocalDateTime.class)) {
      return Types.TIMESTAMP;
    }
    if (typeClass.equals(String.class)) {
      return Types.VARCHAR;
    }
    if (typeClass.equals(Boolean.class)) {
      return Types.BOOLEAN;
    }
    if (typeClass.equals(byte[].class)) {
      return Types.BLOB;
    }
    if (typeClass.equals(Entity.class)) {
      return Types.JAVA_OBJECT;
    }
    if (Object.class.isAssignableFrom(typeClass)) {
      return Types.JAVA_OBJECT;
    }

    return Types.OTHER;
  }
}
