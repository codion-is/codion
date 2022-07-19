/*
 * Copyright (c) 2021 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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
   * @param row the row
   * @return true if the row should be included or if this model is not enabled
   */
  boolean include(R row);
}
