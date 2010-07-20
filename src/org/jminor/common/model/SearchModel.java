/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.sql.Timestamp;
import java.util.Date;

/**
 * User: Björn Darri
 * Date: 19.7.2010
 * Time: 16:56:36
 */
public interface SearchModel<K> {

  String UPPER_BOUND_PROPERTY = "upperBound";
  String LOWER_BOUND_PROPERTY = "lowerBound";

  K getSearchKey();

  /**
   * @return true if this filter is be case sensitive
   */
  boolean isCaseSensitive();

  /**
   * @param caseSensitive true if this search model should be case sensitive when working with strings
   */
  void setCaseSensitive(final boolean caseSensitive);

  /**
   * @param object the object
   * @return true if the object should be included
   */
  boolean include(Object object);

  /**
   * @param value true if wildcard should automatically be added to strings
   */
  void setAutomaticWildcard(final boolean value);

  /**
   * @return true if wildcard is automatically be added to strings
   */
  boolean isAutomaticWildcard();

  void setLocked(boolean value);

  void setUpperBound(Object upper);

  int getType();

  Object getUpperBound();

  void setLowerBound(Object value);

  Object getLowerBound();

  SearchType getSearchType();

  void setSearchType(SearchType searchType);

  boolean isAutoEnable();

  void setAutoEnable(boolean autoEnable);

  boolean isSearchEnabled();

  void setSearchEnabled(boolean value);

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

  State stateLocked();

  Event eventEnabledChanged();

  Event eventLowerBoundChanged();

  Event eventSearchModelCleared();

  Event eventSearchStateChanged();

  Event eventSearchTypeChanged();

  Event eventUpperBoundChanged();
}
