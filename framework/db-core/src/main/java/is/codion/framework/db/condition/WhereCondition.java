/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.framework.domain.property.ColumnProperty;

import java.util.List;

/**
 * A where condition
 */
public interface WhereCondition {

  /**
   * @return the values
   */
  List<?> getValues();

  /**
   * @return the ColumnProperties in the same order as their respective values
   */
  List<ColumnProperty<?>> getColumnProperties();

  /**
   * @return a where clause without the WHERE keyword and using the ? substitution character
   */
  String getWhereClause();
}
