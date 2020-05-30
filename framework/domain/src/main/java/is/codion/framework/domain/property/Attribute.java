/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import java.io.Serializable;

/**
 * Typed identifier for a {@link Property}.
 * Note that Attributes are equal if their names are equal, the type does not factor into equality.
 * @param <T> the attribute type
 */
public interface Attribute<T> extends Serializable {

  /**
   * @return the name of this attribute.
   */
  String getName();

  /**
   * @return the sql type representing this attribute
   * @see java.sql.Types
   */
  int getType();

  /**
   * @return the Class representing this attribute type
   */
  Class<T> getTypeClass();

  /**
   * @param type the type to check ({@link java.sql.Types})
   * @return true if the type of this property is the one given
   */
  boolean isType(int type);

  /**
   * @return true if this is a numerical Property, that is, Integer or Double
   */
  boolean isNumerical();

  /**
   * @return true if this is a time based property, Date (LocalDate), Timestamp (LocalDatetime) or Time (LocalTime)
   */
  boolean isTemporal();

  /**
   * @return true if this is a date property
   */
  boolean isDate();

  /**
   * @return true if this is a timestamp property
   */
  boolean isTimestamp();

  /**
   * @return true if this is a time property
   */
  boolean isTime();

  /**
   * @return true if this is a character property
   */
  boolean isCharacter();

  /**
   * @return true if this is a string property
   */
  boolean isString();

  /**
   * @return true if this is a long property
   */
  boolean isLong();

  /**
   * @return true if this is a integer property
   */
  boolean isInteger();

  /**
   * @return true if this is a double property
   */
  boolean isDouble();

  /**
   * @return true if this is a BigDecimal property
   */
  boolean isBigDecimal();

  /**
   * @return true if this is a decimal property
   */
  boolean isDecimal();

  /**
   * @return true if this is a boolean property
   */
  boolean isBoolean();

  /**
   * @return true if this is a blob property
   */
  boolean isBlob();
}
