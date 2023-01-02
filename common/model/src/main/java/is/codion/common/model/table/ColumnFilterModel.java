/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.model.table;

import java.util.function.Function;

/**
 * Specifies a filter model based on a table column, parameters, operator, upper bound and lower bound,
 * as well as relevant events and states.
 * @param <R> the type of rows
 * @param <C> the type of objects used to identify columns
 * @param <T> the column value type
 */
public interface ColumnFilterModel<R, C, T> extends ColumnConditionModel<C, T> {

  /**
   * The default implementation simply returns the row, assuming it is a Comparable instance.
   * @param comparableFunction the function converting from a Row object to a Comparable for the underlying column
   */
  void setComparableFunction(Function<R, Comparable<T>> comparableFunction);

  /**
   * Returns true if the criteria in this filter model accepts the given input row.
   * Note that this is independent of the enabled state of this model.
   * @param row the row
   * @return true if the row should be included according to this filter model criteria
   */
  boolean include(R row);
}
