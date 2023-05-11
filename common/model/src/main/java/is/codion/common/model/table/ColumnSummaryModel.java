/*
 * Copyright (c) 2009 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.model.table;

import is.codion.common.event.EventListener;
import is.codion.common.model.table.DefaultColumnSummaryModel.DefaultSummaryValues;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.common.value.ValueObserver;

import java.util.Collection;
import java.util.List;

import static java.util.Collections.unmodifiableCollection;

/**
 * A interface defining a class for providing summaries of numerical table columns: sum, average, minimum, maximum and minimum &#38; maximum.<br>
 * For instances use the {@link #columnSummaryModel(SummaryValueProvider)} factory method.
 * @see #columnSummaryModel(SummaryValueProvider)
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
    <T extends Number> String summary(SummaryValueProvider<T> valueProvider);
  }

  /**
   * @return the locked state, if true then changing summary type is disabled
   */
  State lockedState();

  /**
   * @return a list containing the available summaries
   */
  List<Summary> availableSummaries();

  /**
   * @return the value controlling the summary
   */
  Value<Summary> summaryValue();

  /**
   * @return an observer for the string representing the summary value
   */
  ValueObserver<String> summaryTextObserver();

  /**
   * Instantiates a new {@link ColumnSummaryModel}
   * @param valueProvider the value provider
   * @param <T> the value type
   * @return a new {@link ColumnSummaryModel} instance
   */
  static <T extends Number> ColumnSummaryModel columnSummaryModel(SummaryValueProvider<T> valueProvider) {
    return new DefaultColumnSummaryModel<>(valueProvider);
  }

  /**
   * Instantiates a new {@link SummaryValues}.
   * @param values the values
   * @param subset true if the values are a subset of the available values
   * @param <T> the value type
   * @return a new {@link SummaryValues} instance.
   */
  static <T extends Number> SummaryValues<T> summaryValues(Collection<T> values, boolean subset) {
    return new DefaultSummaryValues<>(unmodifiableCollection(values), subset);
  }

  /**
   * @param <C> the column identifier type
   */
  interface Factory<C> {

    /**
     * @param columnIdentifier the column identifier
     * @return a summary model, or null if none is available
     */
    ColumnSummaryModel createSummaryModel(C columnIdentifier);
  }

  /**
   * Provides the values on which to base the summary .
   * @param <T> the value type
   */
  interface SummaryValueProvider<T extends Number> {

    /**
     * @param value the value
     * @return the formatted value
     */
    String format(Object value);

    /**
     * @return the values to base the summary on
     */
    SummaryValues<T> values();

    /**
     * @param listener the listener to notify of changes to the underlying data which require a summary refresh
     */
    void addValuesListener(EventListener listener);
  }

  /**
   * The values to base a summary on.
   * For instances use the {@link ColumnSummaryModel#summaryValues(Collection, boolean)} factory method.
   * @see ColumnSummaryModel#summaryValues(Collection, boolean)
   */
  interface SummaryValues<T extends Number> {

    /**
     * @return the values to base the summary on
     */
    Collection<T> values();

    /**
     * @return true if the values provided by {@link #values()} is a subset of the total available values
     */
    boolean isSubset();
  }
}
