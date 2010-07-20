/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

/**
 * User: Björn Darri<br>
 * Date: 18.7.2010<br>
 * Time: 20:53:51
 */
public interface Attribute {

  String getCaption();

  String getDescription();

  /**
   * @return true if this attribute has a description
   */
  boolean hasDescription();

  Class<?> getTypeClass();

  /**
   * Specifies whether or not this attribute is read only
   * @return true if this attribute is read only
   */
  boolean isReadOnly();

  boolean isNullable();

  Object getDefaultValue();

  int getMaxLength();

  int getMaximumFractionDigits();
}
