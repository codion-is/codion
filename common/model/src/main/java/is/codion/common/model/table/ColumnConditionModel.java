/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.model.table;

import is.codion.common.Configuration;
import is.codion.common.db.Operator;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;
import is.codion.common.event.EventObserver;
import is.codion.common.state.StateObserver;
import is.codion.common.value.PropertyValue;
import is.codion.common.value.Value;
import is.codion.common.value.ValueSet;

import java.text.Format;
import java.util.Collection;

/**
 * Specifies a condition model based on a table column, parameters, operator, upper bound and lower bound,
 * as well as relevant events and states.
 * @param <R> the type of rows
 * @param <K> the type of objects used to identify columns
 */
public interface ColumnConditionModel<R, K> {

  /**
   * Specifies whether wildcards are automatically added to string conditions<br>
   * Value type: {@link AutomaticWildcard}<br>
   * Default value: {@link AutomaticWildcard#NONE}
   */
  PropertyValue<AutomaticWildcard> AUTOMATIC_WILDCARD = Configuration.value(
          "is.codion.common.model.table.ColumnConditionModel.automaticWildard",
          AutomaticWildcard.NONE, AutomaticWildcard::valueOf);

  /**
   * Specifies whether string based conditions are case sensitive or not
   * Value type: Boolean<br>
   * Default value: true
   */
  PropertyValue<Boolean> CASE_SENSITIVE = Configuration.booleanValue(
          "is.codion.common.model.table.ColumnConditionModel.caseSensitive", true);

  /**
   * The possible automatic wildcard types
   */
  enum AutomaticWildcard {
    /**
     * No wildcard
     */
    NONE,
    /**
     * Wildard added at front
     */
    PREFIX,
    /**
     * Wildcard added at end
     */
    POSTFIX,
    /**
     * Wildcard added at front and at end
     */
    PREFIX_AND_POSTFIX
  }

  /**
   * @return the column identifier
   */
  K getColumnIdentifier();

  /**
   * @return true if this filter is be case sensitive
   */
  boolean isCaseSensitive();

  /**
   * @param caseSensitive true if this condition model should be case sensitive when working with strings
   */
  void setCaseSensitive(boolean caseSensitive);

  /**
   * @return the Format object to use when formatting input, if any
   */
  Format getFormat();

  /**
   * @return the date/time format pattern, if any
   */
  String getDateTimeFormatPattern();

  /**
   * @param row the row
   * @return true if the row should be included or if this model is not enabled
   */
  boolean include(R row);

  /**
   * Sets the automatic wildcard type.
   * Note that this is only applicable to string based condition models and only used for
   * operators {@link Operator#EQUALS} and {@link Operator#NOT_EQUALS}
   * @param automaticWildcard the automatic wildcard type to use
   */
  void setAutomaticWildcard(AutomaticWildcard automaticWildcard);

  /**
   * @return the automatic wildcard type being used by this model
   */
  AutomaticWildcard getAutomaticWildcard();

  /**
   * @param comparable the value to check
   * @return true if the given value should be included or if this model is not enabled
   */
  boolean include(Comparable<?> comparable);

  /**
   * @param locked true to lock this model, false to unlock
   */
  void setLocked(boolean locked);

  /**
   * @return true if this model is locked
   */
  boolean isLocked();

  /**
   * @return the data type this condition model is based on
   */
  Class<?> getTypeClass();

  /**
   * Sets the values used when the {@link Operator#EQUALS} is enabled.
   * @param value the value to use as condition
   * @param <T> the value type
   */
  <T> void setEqualsValue(T value);

  /**
   * @param <T> the value type
   * @return the equals value, possibly null
   */
  <T> T getEqualsValue();

  /**
   * @param values the values to set
   * @param <T> the value type
   */
  <T> void setEqualsValues(Collection<T> values);

  /**
   * @param <T> the value type
   * @return the equals values, never null
   */
  <T> Collection<T> getEqualsValues();

  /**
   * @param upper the new upper bound
   * @param <T> the value type
   */
  <T> void setUpperBound(T upper);

  /**
   * @return the upper bound
   * @param <T> the value type
   */
  <T> T getUpperBound();

  /**
   * @param value the lower bound
   * @param <T> the value type
   */
  <T> void setLowerBound(T value);

  /**
   * @return the lower bound
   * @param <T> the value type
   */
  <T> T getLowerBound();

  /**
   * @return the search operator
   */
  Operator getOperator();

  /**
   * @param operator the search operator
   */
  void setOperator(Operator operator);

  /**
   * @return true if the current operator requires a lower bound value to be specified,
   * such as within and outside range.
   */
  boolean isLowerBoundRequired();

  /**
   * @return true if auto enable is enabled
   */
  boolean isAutoEnable();

  /**
   * If set, this model automatically enables itself when a condition is specified
   * @param autoEnable true to enable, false to disable
   */
  void setAutoEnable(boolean autoEnable);

  /**
   * @return true if this condition model is enabled
   */
  boolean isEnabled();

  /**
   * @param enabled true to enable, false to disable
   */
  void setEnabled(boolean enabled);

  /**
   * Disables and clears this condition model, that is, sets the upper and lower bounds to null
   * and the operator to the default value {@link Operator#EQUALS}
   */
  void clearCondition();

  /**
   * @param <T> the value type
   * @return a ValueSet based on the equals values of this condition model
   */
  <T> ValueSet<T> getEqualsValueSet();

  /**
   * @param <T> the value type
   * @return a Value based on the upper bound value of this condition model
   */
  <T> Value<T> getUpperBoundValue();

  /**
   * @param <T> the value type
   * @return a Value based on the lower bound value of this condition model
   */
  <T> Value<T> getLowerBoundValue();

  /**
   * @return an observer for this models locked state
   */
  StateObserver getLockedObserver();

  /**
   * @return an observer notified each time the enabled state changes
   */
  EventObserver<Boolean> getEnabledObserver();

  /**
   * @return an observer notified each time the operator changes
   */
  EventObserver<Operator> getOperatorObserver();

  /**
   * @param listener a listener to be notified each time the enabled state changes
   */
  void addEnabledListener(EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeEnabledListener(EventListener listener);

  /**
   * @param listener a listener to be notified each time the operator changes
   */
  void addOperatorListener(EventDataListener<Operator> listener);

  /**
   * @param listener the listener to remove
   */
  void removeOperatorListener(EventDataListener<Operator> listener);

  /**
   * @param listener a listener to be notified each time the lower bound changes
   */
  void addEqualsValueListener(EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeEqualsValueListener(EventListener listener);

  /**
   * @param listener a listener to be notified each time the lower bound changes
   */
  void addLowerBoundListener(EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeLowerBoundListener(EventListener listener);

  /**
   * @param listener a listener to be notified each time the lower bound required attribute changes
   */
  void addLowerBoundRequiredListener(EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeLowerBoundRequiredListener(EventListener listener);

  /**
   * @param listener a listener to be notified each time the upper bound changes
   */
  void addUpperBoundListener(EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeUpperBoundListener(EventListener listener);

  /**
   * @param listener a listener to be notified each time the model is cleared
   */
  void addClearedListener(EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeClearedListener(EventListener listener);

  /**
   * @param listener a listener to be notified each time the condition state changes
   */
  void addConditionChangedListener(EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeConditionChangedListener(EventListener listener);
}
