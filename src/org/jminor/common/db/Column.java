/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import org.jminor.common.model.Attribute;

/**
 * User: Björn Darri<br>
 * Date: 17.7.2010<br>
 * Time: 20:35:19
 */
public interface Column extends Attribute {

  String getColumnName();

  int getSelectIndex();

  boolean isDenormalized();

  boolean columnHasDefaultValue();

  boolean isReference();

  int getType();

  /**
   * Specifies whether or not this column is updatable
   * @return true if this attribute is column
   */
  boolean isUpdatable();
}
