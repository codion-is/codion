/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.condition;

import is.codion.framework.domain.entity.attribute.Column;

import java.util.List;

/**
 * Provides condition strings for where clauses
 */
public interface ConditionProvider {

  /**
   * Creates a query condition string for the given values
   * @param columns the condition columns
   * @param values the values
   * @return a query condition string
   */
  String toString(List<Column<?>> columns, List<?> values);
}
