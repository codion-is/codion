/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

/**
 * Specifies a simple attribute.
 */
public interface Attribute {

  String getCaption();

  /**
   * @return a String describing this attribute
   */
  String getDescription();

  /**
   * @return true if this attribute has a description
   */
  boolean hasDescription();

  /**
   * @return the Class representing the values of this attribute
   */
  Class<?> getTypeClass();
}
