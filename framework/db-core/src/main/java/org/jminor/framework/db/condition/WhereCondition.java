/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.condition;

import org.jminor.framework.domain.property.ColumnProperty;

import java.util.List;

/**
 * A where condition
 */
public interface WhereCondition {

  /**
   * @return the EntityCondition
   */
  EntityCondition getEntityCondition();

  /**
   * @return the values
   */
  List getValues();

  /**
   * @return the ColumnProperties in the same order as their respective values
   */
  List<ColumnProperty> getColumnProperties();

  /**
   * @return a where clause without the WHERE keyword and using the ? substitution character
   */
  String getWhereClause();
}
