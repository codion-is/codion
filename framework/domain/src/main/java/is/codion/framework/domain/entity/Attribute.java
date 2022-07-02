/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.property.Property;

/**
 * Typed {@link Attribute} to base a {@link Property} on.
 * Note that attribute names are case-sensitive and Attributes are equal if their
 * names and entityTypes are equal, the typeClass does not factor into equality.
 * @param <T> the attribute type
 */
public interface Attribute<T> {

  /**
   * @return the name of this attribute.
   */
  String getName();

  /**
   * @return the Class representing the attribute value
   */
  Class<T> getTypeClass();

  /**
   * @return the entity type this Attribute is associated with
   */
  EntityType getEntityType();

  /**
   * @param value the value to validate
   * @return the validated value
   * @throws IllegalArgumentException in case {@code value} is of a type incompatible with this attribute
   * @see #getTypeClass()
   */
  T validateType(T value);

  /**
   * @param typeClass the type class to check
   * @return true if this attributes type class is the same as the one given
   * @see #getTypeClass()
   */
  boolean isType(Class<?> typeClass);

  /**
   * @return true if this attribute represents a numerical value.
   */
  boolean isNumerical();

  /**
   * @return true if this attribute represents a {@link java.time.temporal.Temporal} value.
   */
  boolean isTemporal();

  /**
   * @return true if this attribute represents a {@link java.time.LocalDate} value.
   */
  boolean isLocalDate();

  /**
   * @return true if this attribute represents a {@link java.time.LocalDateTime} value.
   */
  boolean isLocalDateTime();

  /**
   * @return true if this attribute represents a {@link java.time.LocalTime} value.
   */
  boolean isLocalTime();

  /**
   * @return true if this attribute represents a {@link java.time.OffsetDateTime} value.
   */
  boolean isOffsetDateTime();

  /**
   * @return true if this attribute represents a {@link Character} value.
   */
  boolean isCharacter();

  /**
   * @return true if this attribute represents a {@link String} value.
   */
  boolean isString();

  /**
   * @return true if this attribute represents a {@link Long} value.
   */
  boolean isLong();

  /**
   * @return true if this attribute represents a {@link Integer} value.
   */
  boolean isInteger();

  /**
   * @return true if this attribute represents a {@link Double} value.
   */
  boolean isDouble();

  /**
   * @return true if this attribute represents a {@link java.math.BigDecimal} value.
   */
  boolean isBigDecimal();

  /**
   * @return true if this attribute represents a decimal number value.
   */
  boolean isDecimal();

  /**
   * @return true if this attribute represents a {@link Boolean} value.
   */
  boolean isBoolean();

  /**
   * @return true if this attribute represents a byte array value.
   */
  boolean isByteArray();

  /**
   * @return true if this attribute represents a {@link Entity} value.
   */
  boolean isEntity();

  /**
   * Creates a new {@link Attribute}, associated with the given entityType.
   * @param entityType the entityType owning this attribute
   * @param name the attribute name
   * @param typeClass the class representing the attribute value type
   * @param <T> the attribute type
   * @return a new {@link Attribute}
   */
  static <T> Attribute<T> attribute(EntityType entityType, String name, Class<T> typeClass) {
    return new DefaultAttribute<>(name, typeClass, entityType);
  }
}
