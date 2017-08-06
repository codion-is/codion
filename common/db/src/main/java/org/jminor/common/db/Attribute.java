/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

/**
 * Specifies a simple attribute.
 */
public interface Attribute {

  /**
   * @return the caption
   */
  String getCaption();

  /**
   * @return a String describing this attribute
   */
  String getDescription();

  /**
   * @return the Class representing the values of this attribute
   */
  Class<?> getTypeClass();

  /**
   * @param value the value to validate
   * @throws IllegalArgumentException in case {@code value} is of a type incompatible with this attribute
   */
  void validateType(final Object value);
}
