/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import java.util.List;

/**
 * Provides criteria strings for where clauses
 */
public interface CriteriaProvider {

  /**
   * Creates a query criteria string for the given values
   * @param attributes the criteria attributes
   * @param values the values
   * @return a query criteria string
   */
  String toString(List<Attribute<?>> attributes, List<?> values);
}
