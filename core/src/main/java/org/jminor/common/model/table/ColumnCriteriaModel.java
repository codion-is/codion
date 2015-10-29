/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.table;

import org.jminor.common.model.EventInfoListener;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.EventObserver;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.Value;

import java.text.Format;

/**
 * Specifies a criteria model based on a table column, parameters, operator, upper bound and lower bound,
 * as well as relevant events and states.
 * @param <K> the type of objects used to identify columns
 */
public interface ColumnCriteriaModel<K> {

  String UPPER_BOUND_PROPERTY = "upperBound";
  String LOWER_BOUND_PROPERTY = "lowerBound";

  /**
   * @return the column identifier
   */
  K getColumnIdentifier();

  /**
   * @return true if this filter is be case sensitive
   */
  boolean isCaseSensitive();

  /**
   * @param caseSensitive true if this criteria model should be case sensitive when working with strings
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
   * @return the data type this criteria model is based on
   * @see java.sql.Types
   */
  int getType();

  /**
   * @param upper the new upper bound
   */
  void setUpperBound(final Object upper);

  /**
   * A shortcut method for setting the upper bound value, searchType to LIKE
   * and enabling this model in case of a non-null value.
   * @param value the value to use as criteria
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
  SearchType getSearchType();

  /**
   * @param searchType the search type
   */
  void setSearchType(final SearchType searchType);

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
   * If set, this model automatically enables itself when a criteria is specified
   * @param autoEnable true to enable, false to disable
   */
  void setAutoEnable(final boolean autoEnable);

  /**
   * @return true if this criteria model is enabled
   */
  boolean isEnabled();

  /**
   * @param value true to enable, false to disable
   */
  void setEnabled(final boolean value);

  /**
   * Clears this criteria model
   */
  void clearCriteria();

  /**
   * @return a Value based on the upper bound value of this criteria model
   */
  Value getUpperBoundValue();

  /**
   * @return a Value based on the lower bound value of this criteria model
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
   * @return an observer notified each time the search type changes
   */
  EventObserver<SearchType> getSearchTypeObserver();

  /**
   * @param listener a listener to be notified each time the enabled state changes
   */
  void addEnabledListener(final EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeEnabledListener(final EventListener listener);

  /**
   * @param listener a listener to be notified each time the search type changes
   */
  void addSearchTypeListener(final EventInfoListener<SearchType> listener);

  /**
   * @param listener the listener to remove
   */
  void removeSearchTypeListener(final EventInfoListener listener);

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
   * @param listener a listener to be notified each time the criteria state changes
   */
  void addCriteriaStateListener(final EventListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeCriteriaStateListener(final EventListener listener);
}
