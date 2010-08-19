/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.awt.event.ActionListener;
import java.sql.Timestamp;
import java.text.Format;
import java.util.Date;

/**
 * Specifies a search model based on a table column, search parameters, search type, upper bound and lower bound,
 * as well as relevant events and states.
 * <K> the type of objects used to identify columns
 */
public interface ColumnSearchModel<K> {

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
   * @param caseSensitive true if this search model should be case sensitive when working with strings
   */
  void setCaseSensitive(final boolean caseSensitive);

  /**
   * @return the Format object to use when formatting input, is any
   */
  Format getFormat();

  /**
   * @param object the object
   * @return true if the object should be included
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
   * @return true if the given value should be included
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
   * @return the data type this search model is based on
   * @see java.sql.Types
   */
  int getType();

  /**
   * @param upper the new upper bound
   */
  void setUpperBound(final Object upper);

  /**
   * A shortcut method for setting the upper bound value, searchType to LIKE
   * and enabling this model.
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
   * @return true if auto enable is enabled
   */
  boolean isAutoEnable();

  /**
   * If set, this model automatically enables itself when a criteria is specified
   * @param autoEnable true to enable, false to disable
   */
  void setAutoEnable(final boolean autoEnable);

  /**
   * @return true if this search model is enabled
   */
  boolean isEnabled();

  /**
   * @param value true to enable, false to disable
   */
  void setEnabled(final boolean value);

  /**
   * Clears the criteria values from this search model
   */
  void clearSearch();

  /**
   * @param value the upper bound
   */
  void setUpperBound(final String value);

  /**
   * @param value the upper bound
   */
  void setUpperBound(final Double value);

  /**
   * @param value the upper bound
   */
  void setUpperBound(final Integer value);

  /**
   * @param value the upper bound
   */
  void setUpperBound(final boolean value);

  /**
   * @param value the upper bound
   */
  void setUpperBound(final char value);

  /**
   * @param value the upper bound
   */
  void setUpperBound(final Boolean value);

  /**
   * @param value the upper bound
   */
  void setUpperBound(final Timestamp value);

  /**
   * @param value the upper bound
   */
  void setUpperBound(final Date value);

  /**
   * @param value the Lower bound
   */
  void setLowerBound(final String value);

  /**
   * @param value the Lower bound
   */
  void setLowerBound(final Double value);

  /**
   * @param value the Lower bound
   */
  void setLowerBound(final Integer value);

  /**
   * @param value the Lower bound
   */
  void setLowerBound(final boolean value);

  /**
   * @param value the Lower bound
   */
  void setLowerBound(final char value);

  /**
   * @param value the Lower bound
   */
  void setLowerBound(final Boolean value);

  /**
   * @param value the Lower bound
   */
  void setLowerBound(final Timestamp value);

  /**
   * @param value the Lower bound
   */
  void setLowerBound(final Date value);

  /**
   * @return an observer for this model's locked state
   */
  StateObserver getLockedState();

  /**
   * @return an observer for this model's enabled state
   */
  EventObserver getEnabledObserver();

  /**
   * @return an observer for this model's lower bound
   */
  EventObserver getLowerBoundObserver();

  /**
   * @return an observer for this model's upper bound
   */
  EventObserver getUpperBoundObserver();

  /**
   * @return an observer for this model's search type
   */
  EventObserver getSearchTypeObserver();

  /**
   * @param listener a listener to be notified each time the enabled state changes
   */
  void addEnabledListener(final ActionListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeEnabledListener(final ActionListener listener);

  /**
   * @param listener a listener to be notified each time the search type changes
   */
  void addSearchTypeListener(final ActionListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeSearchTypeListener(final ActionListener listener);

  /**
   * @param listener a listener to be notified each time the lower bound changes
   */
  void addLowerBoundListener(final ActionListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeLowerBoundListener(final ActionListener listener);

  /**
   * @param listener a listener to be notified each time the upper bound changes
   */
  void addUpperBoundListener(final ActionListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeUpperBoundListener(final ActionListener listener);

  /**
   * @param listener a listener to be notified each time the model is cleared
   */
  void addClearedListener(final ActionListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeClearedListener(final ActionListener listener);

  /**
   * @param listener a listener to be notified each time the search state changes
   */
  void addSearchStateListener(final ActionListener listener);

  /**
   * @param listener the listener to remove
   */
  void removeSearchStateListener(final ActionListener listener);
}
