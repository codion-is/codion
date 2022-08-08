/*
 * Copyright (c) 2020 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import java.util.List;

/**
 * Provides condition strings for where clauses
 */
public interface ConditionProvider {

  /**
   * Creates a query condition string for the given values
   * @param attributes the condition attributes
   * @param values the values
   * @return a query condition string
   */
  String conditionString(List<Attribute<?>> attributes, List<?> values);
}
