/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.attribute;

import is.codion.framework.domain.identity.Identity;
import is.codion.framework.domain.property.Property;

import java.io.Serializable;

/**
 * Typed {@link Attribute} to base a {@link Property} on.
 * Note that Attributes are equal if their names and entityIds are equal, the typeClass does not factor into equality.
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
   * @return the id of the entity this Attribute is associated with
   */
  Identity getEntityId();

  /**
   * @param value the value to validate
   * @return the value
   * @throws IllegalArgumentException in case {@code value} is of a type incompatible with this property
   */
  T validateType(T value);

  /**
   * @param type the type to check ({@link java.sql.Types})
   * @return true if the type of this attribute is the one given
   */
  boolean isType(int type);

  /**
   * @return true if this is a numerical attribute, that is, Integer or Double
   */
  boolean isNumerical();

  /**
   * @return true if this is a time based attribute, Date (LocalDate), Timestamp (LocalDatetime) or Time (LocalTime)
   */
  boolean isTemporal();

  /**
   * @return true if this is a date attribute
   */
  boolean isDate();

  /**
   * @return true if this is a timestamp attribute
   */
  boolean isTimestamp();

  /**
   * @return true if this is a time attribute
   */
  boolean isTime();

  /**
   * @return true if this is a character attribute
   */
  boolean isCharacter();

  /**
   * @return true if this is a string attribute
   */
  boolean isString();

  /**
   * @return true if this is a long attribute
   */
  boolean isLong();

  /**
   * @return true if this is a integer attribute
   */
  boolean isInteger();

  /**
   * @return true if this is a double attribute
   */
  boolean isDouble();

  /**
   * @return true if this is a BigDecimal attribute
   */
  boolean isBigDecimal();

  /**
   * @return true if this is a decimal attribute
   */
  boolean isDecimal();

  /**
   * @return true if this is a boolean attribute
   */
  boolean isBoolean();

  /**
   * @return true if this is a blob attribute
   */
  boolean isBlob();
}
