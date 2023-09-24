/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2009 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.common.model.table;

import is.codion.common.model.table.DefaultColumnSummaryModel.DefaultSummaryValues;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.common.value.ValueObserver;

import java.text.Format;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

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
  State locked();

  /**
   * @return a list containing the available summaries
   */
  List<Summary> summaries();

  /**
   * @return the value controlling the summary
   */
  Value<Summary> summary();

  /**
   * @return an observer for the string representing the summary value
   */
  ValueObserver<String> summaryText();

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
     * @param listener the listener notified the underlying data changes, requiring a summary refresh
     */
    void addListener(Runnable listener);

    /**
     * @param <C> the column identifier type
     */
    interface Factory<C> {

      /**
       * @param columnIdentifier the column identifier
       * @param format the format to use
       * @param <T> the column type
       * @return a summary value provider or an empty Optional, if no summary is available for the column
       */
      <T extends Number> Optional<SummaryValueProvider<T>> createSummaryValueProvider(C columnIdentifier, Format format);
    }
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
