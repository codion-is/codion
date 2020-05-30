/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.common.Util;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

class DefaultAttribute<T> implements Attribute<T> {

  private static final long serialVersionUID = 1;

  private final String name;
  private final int type;
  private final Class<T> typeClass;

  DefaultAttribute(final String name, final int type) {
    this(name, type, getTypeClass(type));
  }

  DefaultAttribute(final String name, final int type, final Class<T> typeClass) {
    if (Util.nullOrEmpty(name)) {
      throw new IllegalArgumentException("name must be a non-empty string");
    }
    this.name = name;
    this.type = type;
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

  /**
   * @param sqlType the type
   * @return the Class representing the given type
   */
  private static Class getTypeClass(final int sqlType) {
    switch (sqlType) {
      case Types.BIGINT:
        return Long.class;
      case Types.INTEGER:
        return Integer.class;
      case Types.DOUBLE:
        return Double.class;
      case Types.DECIMAL:
        return BigDecimal.class;
      case Types.DATE:
        return LocalDate.class;
      case Types.TIME:
        return LocalTime.class;
      case Types.TIMESTAMP:
        return LocalDateTime.class;
      case Types.VARCHAR:
        return String.class;
      case Types.BOOLEAN:
        return Boolean.class;
      case Types.CHAR:
        return Character.class;
      case Types.BLOB:
        return byte[].class;
      default:
        return Object.class;
    }
  }
}
