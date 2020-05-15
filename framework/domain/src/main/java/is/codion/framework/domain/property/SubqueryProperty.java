/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

/**
 * A property based on a subquery, returning a single value
 */
public interface SubqueryProperty extends ColumnProperty {

  /**
   * @return the subquery string
   */
  String getSubQuery();
}
