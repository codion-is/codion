/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.model.table;

import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;

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
   * @param summary the type of summary to show
   */
  void setSummary(Summary summary);

  /**
   * @return the current summary type
   */
  Summary getSummary();

  /**
   * @return true if changing the summary type is disabled
   */
  boolean isLocked();

  /**
   * @param locked if true then changing summary type is disable
   */
  void setLocked(boolean locked);

  /**
   * @return a list containing the available summaries
   */
  List<Summary> getAvailableSummaries();

  /**
   * @return a string representing the summary value
   */
  String getSummaryText();

  /**
   * @param listener a listener to be notified each time the summary value changes
   */
  void addSummaryValueListener(EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeSummaryValueListener(EventListener listener);

  /**
   * @param listener a listener to be notified each time the summary type changes
   */
  void addSummaryListener(EventDataListener<Summary> listener);

  /**
   * @param listener the listener to remove
   */
  void removeSummaryListener(EventDataListener<Summary> listener);

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
