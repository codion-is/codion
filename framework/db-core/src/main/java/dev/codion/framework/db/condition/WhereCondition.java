/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.db.condition;

import dev.codion.framework.domain.property.ColumnProperty;

import java.util.List;

/**
 * A where condition
 */
public interface WhereCondition {

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
