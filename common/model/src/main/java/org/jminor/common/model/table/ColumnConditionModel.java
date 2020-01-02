/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.table;

import org.jminor.common.Configuration;
import org.jminor.common.db.ConditionType;
import org.jminor.common.event.EventDataListener;
import org.jminor.common.event.EventListener;
import org.jminor.common.event.EventObserver;
import org.jminor.common.state.StateObserver;
import org.jminor.common.value.PropertyValue;
import org.jminor.common.value.Value;

import java.text.Format;

/**
 * Specifies a condition model based on a table column, parameters, operator, upper bound and lower bound,
 * as well as relevant events and states.
 * @param <C> the type of objects used to identify columns
 */
public interface ColumnConditionModel<C> {

  /**
   * Specifies whether wildcards are automatically added to string conditions<br>
   * Value type: {@link AutomaticWildcard}<br>
   * Default value: {@link AutomaticWildcard#NONE}
   */
  PropertyValue<AutomaticWildcard> AUTOMATIC_WILDCARD = Configuration.value(
          "org.jminor.common.model.table.ColumnConditionModel.automaticWildard",
          AutomaticWildcard.NONE, AutomaticWildcard::valueOf);

  /**
   * Specifies whether string based conditions are case sensitive or not
   * Value type: Boolean<br>
   * Default value: true
   */
  PropertyValue<Boolean> CASE_SENSITIVE = Configuration.booleanValue(
          "org.jminor.common.model.table.ColumnConditionModel.caseSensitive", true);

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
  C getColumnIdentifier();

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
   * @param object the object
   * @return true if the object should be included or if this model is not enabled
   */
  boolean include(Object object);

  /**
   * Sets the automatic wildcard type.
   * Note that this is only applicable to string based condition models and only used for
   * condition types {@link ConditionType#LIKE} and {@link ConditionType#NOT_LIKE}
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
  boolean include(Comparable comparable);

  /**
   * @param value true to lock this model, false to unlock
   */
  void setLocked(boolean value);

  /**
   * @return true if this model is locked
   */
  boolean isLocked();

  /**
   * @return the data type this condition model is based on
   */
  Class getTypeClass();

  /**
   * @param upper the new upper bound
   */
  void setUpperBound(Object upper);

  /**
   * A shortcut method for setting the upper bound value, conditionType to LIKE
   * and enabling this model in case of a non-null value.
   * @param value the value to use as condition
   */
  void setLikeValue(Object value);

  /**
   * @return the upper bound
   */
  Object getUpperBound();

  /**
   * @param value the lower bound
   */
  void setLowerBound(Object value);

  /**
   * @return the lower bound
   */
  Object getLowerBound();

  /**
   * @return the search type
   */
  ConditionType getConditionType();

  /**
   * @param conditionType the search type
   */
  void setConditionType(ConditionType conditionType);

  /**
   * @return true if the current search type requires a lower bound value to be specified,
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
   * @param value true to enable, false to disable
   */
  void setEnabled(boolean value);

  /**
   * Disables and clears this condition model, that is, sets the upper and lower bounds to null
   * and the condition type to the default value {@link ConditionType#LIKE}
   */
  void clearCondition();

  /**
   * @return a Value based on the upper bound value of this condition model
   */
  Value getUpperBoundValue();

  /**
   * @return a Value based on the lower bound value of this condition model
   */
  Value getLowerBoundValue();

  /**
   * @return an observer for this models locked state
   */
  StateObserver getLockedObserver();

  /**
   * @return an observer notified each time the enabled state changes
   */
  EventObserver<Boolean> getEnabledObserver();

  /**
   * @return an observer notified each time the condition type changes
   */
  EventObserver<ConditionType> getConditionTypeObserver();

  /**
   * @param listener a listener to be notified each time the enabled state changes
   */
  void addEnabledListener(EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeEnabledListener(EventListener listener);

  /**
   * @param listener a listener to be notified each time the condition type changes
   */
  void addConditionTypeListener(EventDataListener<ConditionType> listener);

  /**
   * @param listener the listener to remove
   */
  void removeConditionTypeListener(EventDataListener listener);

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
  void addConditionStateListener(EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeConditionStateListener(EventListener listener);
}
