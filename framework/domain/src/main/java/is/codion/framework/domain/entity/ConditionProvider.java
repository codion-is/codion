/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.domain.entity;

import java.util.List;

/**
 * Provides condition strings for where clauses
 */
public interface ConditionProvider {

  /**
   * Creates a query condition string for the given values
   * @param propertyIds the condition propertyIds
   * @param values the values
   * @return a query condition string
   */
  String getConditionString(List<String> propertyIds, List values);
}
