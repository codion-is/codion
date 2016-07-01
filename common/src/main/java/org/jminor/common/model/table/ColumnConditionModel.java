/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.table;

import org.jminor.common.EventInfoListener;
import org.jminor.common.EventListener;
import org.jminor.common.EventObserver;
import org.jminor.common.StateObserver;
import org.jminor.common.Value;
import org.jminor.common.db.condition.ConditionType;

import java.text.Format;

/**
 * Specifies a condition model based on a table column, parameters, operator, upper bound and lower bound,
 * as well as relevant events and states.
 * @param <K> the type of objects used to identify columns
 */
public interface ColumnConditionModel<K> {

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
  void setCaseSensitive(final boolean caseSensitive);

  /**
   * @return the Format object to use when formatting input, if any
   */
  Format getFormat();

  /**
   * @param object the object
   * @return true if the object should be included or if this model is not enabled
   */
  boolean include(final Object object);

  /**
   * @param value true if wildcard should automatically be added to strings
   */
  void setAutomaticWildcard(final boolean value);

  /**
   * @return true if wildcard is automatically be added to strings
   */
  boolean isAutomaticWildcard();

  /**
   * @param comparable the value to check
   * @return true if the given value should be included or if this model is not enabled
   */
  boolean include(final Comparable comparable);

  /**
   * @param value true to lock this model, false to unlock
   */
  void setLocked(final boolean value);

  /**
   * @return true if this model is locked
   */
  boolean isLocked();

  /**
   * @return the data type this condition model is based on
   * @see java.sql.Types
   */
  int getType();

  /**
   * @param upper the new upper bound
   */
  void setUpperBound(final Object upper);

  /**
   * A shortcut method for setting the upper bound value, conditionType to LIKE
   * and enabling this model in case of a non-null value.
   * @param value the value to use as condition
   */
  void setLikeValue(final Comparable value);

  /**
   * @return the upper bound
   */
  Object getUpperBound();

  /**
   * @param value the lower bound
   */
  void setLowerBound(final Object value);

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
  void setConditionType(final ConditionType conditionType);

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
  void setAutoEnable(final boolean autoEnable);

  /**
   * @return true if this condition model is enabled
   */
  boolean isEnabled();

  /**
   * @param value true to enable, false to disable
   */
  void setEnabled(final boolean value);

  /**
   * Clears this condition model
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
  void addEnabledListener(final EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeEnabledListener(final EventListener listener);

  /**
   * @param listener a listener to be notified each time the condition type changes
   */
  void addConditionTypeListener(final EventInfoListener<ConditionType> listener);

  /**
   * @param listener the listener to remove
   */
  void removeConditionTypeListener(final EventInfoListener listener);

  /**
   * @param listener a listener to be notified each time the lower bound changes
   */
  void addLowerBoundListener(final EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeLowerBoundListener(final EventListener listener);

  /**
   * @param listener a listener to be notified each time the lower bound required attribute changes
   */
  void addLowerBoundRequiredListener(final EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeLowerBoundRequiredListener(final EventListener listener);

  /**
   * @param listener a listener to be notified each time the upper bound changes
   */
  void addUpperBoundListener(final EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeUpperBoundListener(final EventListener listener);

  /**
   * @param listener a listener to be notified each time the model is cleared
   */
  void addClearedListener(final EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeClearedListener(final EventListener listener);

  /**
   * @param listener a listener to be notified each time the condition state changes
   */
  void addConditionStateListener(final EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeConditionStateListener(final EventListener listener);
}
