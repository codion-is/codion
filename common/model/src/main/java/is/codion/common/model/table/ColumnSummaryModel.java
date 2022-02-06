/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.model.table;

import is.codion.common.event.EventListener;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.common.value.ValueObserver;

import java.util.Collection;
import java.util.List;

/**
 * A interface defining a class for providing summaries of numerical table columns: sum, average, minimum, maximum and minimum &#38; maximum.<br>
 */
public interface ColumnSummaryModel {

  /**
   * Specifies a summary provider
   */
  interface Summary {

    /**
     * Returns a String containing the summary information for the given column
     * @param valueProvider the object responsible for providing the values for the summary
     * @param <T> the value type
     * @return a summary text
     */
    <T extends Number> String getSummary(ColumnValueProvider<T> valueProvider);
  }

  /**
   * @return the locked state, if true then changing summary type is disabled
   */
  State getLockedState();

  /**
   * @return a list containing the available summaries
   */
  List<Summary> getAvailableSummaries();

  /**
   * @return the value controlling the summary
   */
  Value<Summary> getSummaryValue();

  /**
   * @return an observer for the string representing the summary value
   */
  ValueObserver<String> getSummaryTextObserver();

  /**
   * Provides the values used when creating the summary value.
   * @param <T> the value type
   */
  interface ColumnValueProvider<T extends Number> {

    /**
     * @param value the value
     * @return the formatted value
     */
    String format(Object value);

    /**
     * @return the values to base the summary on
     */
    Collection<T> getValues();

    /**
     * @return true if the values provided by {@code getValues()} is a subset of the total available values
     */
    boolean isValueSubset();

    /**
     * @param listener the listener to notify of changes to the underlying data which require a summary refresh
     */
    void addValuesChangedListener(EventListener listener);
  }
}
