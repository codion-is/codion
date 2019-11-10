/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.property;

/**
 * A property based on a subquery, returning a single value
 */
public interface SubqueryProperty extends ColumnProperty {

  /**
   * @return the subquery string
   */
  String getSubQuery();
}
