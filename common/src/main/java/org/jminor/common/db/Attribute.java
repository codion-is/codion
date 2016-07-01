/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
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
}
