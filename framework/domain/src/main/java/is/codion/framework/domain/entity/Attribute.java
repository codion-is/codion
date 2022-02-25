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
   * @return the Class representing this attribute
   */
  Class<T> getTypeClass();

  /**
   * @return the type of the entity this Attribute is associated with
   */
  EntityType getEntityType();

  /**
   * @param value the value to validate
   * @return the value
   * @throws IllegalArgumentException in case {@code value} is of a type incompatible with this attribute
   */
  T validateType(T value);

  /**
   * @param typeClass the type class to check
   * @return true if this attributes type class is the same as the one given
   */
  boolean isType(Class<?> typeClass);

  /**
   * @return true if this is a numerical attribute, that is, integer, decimal or long
   */
  boolean isNumerical();

  /**
   * @return true if this is a Temporal based attribute
   */
  boolean isTemporal();

  /**
   * @return true if this is a date attribute
   */
  boolean isLocalDate();

  /**
   * @return true if this is a timestamp attribute
   */
  boolean isLocalDateTime();

  /**
   * @return true if this is a time attribute
   */
  boolean isLocalTime();

  /**
   * @return true if this is an offset date time attribute
   */
  boolean isOffsetDateTime();

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
   * @return true if this is an integer attribute
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
   * @return true if this is a byte array attribute
   */
  boolean isByteArray();

  /**
   * @return true if this is an Entity attribute
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
