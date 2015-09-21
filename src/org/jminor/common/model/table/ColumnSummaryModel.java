/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.table;

import org.jminor.common.model.EventInfoListener;
import org.jminor.common.model.EventListener;

import java.util.Collection;
import java.util.List;

/**
 * A interface defining a class for providing summaries of numerical table columns: sum, average, minimum, maximum and minimum &#38; maximum.<br>
 * @param <K> the type of objects used to identify columns
 */
public interface ColumnSummaryModel {

  /**
   * Specifies a summary provider
   */
  interface Summary {

    /**
     * Returns a String containing the summary information for the given column
     * @param valueProvider the object responsible for providing the values for the summary
     * @return a summary text
     */
    String getSummary(final ColumnValueProvider valueProvider);
  }

  /**
   * @return the value provider
   */
  ColumnValueProvider getValueProvider();

  /**
   * @param summary the type of summary to show
   */
  void setSummary(final Summary summary);

  /**
   * @return the current summary type
   */
  Summary getSummary();

  /**
   * @return true if changing the summary type is disabled
   */
  boolean isLocked();

  /**
   * @param value if true then changing summary type is disable
   */
  void setLocked(final boolean value);

  /**
   * @return a list containing the available summaries
   */
  List<? extends Summary> getAvailableSummaries();

  /**
   * @return a string representing the summary value
   */
  String getSummaryText();

  /**
   * @param listener a listener to be notified each time the summary value changes
   */
  void addSummaryValueListener(final EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeSummaryValueListener(final EventListener listener);

  /**
   * @param listener a listener to be notified each time the summary type changes
   */
  void addSummaryListener(final EventInfoListener<Summary> listener);

  /**
   * @param listener the listener to remove
   */
  void removeSummaryListener(final EventInfoListener<Summary> listener);

  /**
   * Provides the values used when creating the summary value.
   */
  interface ColumnValueProvider {

    /**
     * @param value the value
     * @return the formatted value
     */
    String format(final Object value);

    /**
     * @return true if the column is numerical
     */
    boolean isNumerical();

    /**
     * @return the values to base the summary on
     */
    Collection getValues();

    /**
     * @return true if the values are of type Integer
     */
    boolean isInteger();

    /**
     * @return true if the values are of type Double
     */
    boolean isDouble();

    /**
     * @return true if the values provided by <code>getValues()</code> is a subset of the total available values
     */
    boolean isValueSubset();

    /**
     * @return true if this value provider returns a subset of the data if available
     */
    boolean isUseValueSubset();

    /**
     * @param value true if this value provider should return a subset of the data if available
     */
    void setUseValueSubset(final boolean value);

    /**
     * @param listener the listener to notify of changes to the underlying data which require a summary refresh
     */
    void addValuesChangedListener(final EventListener listener);
  }
}
