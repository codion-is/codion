/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Encapsulates the property search parameters search type, upper bound and lower bound,
 * as well as relevant events and states.<br>
 * User: Bjorn Darri<br>
 * Date: 26.12.2007<br>
 * Time: 14:48:22<br>
 */
public class DefaultSearchModel<K> implements SearchModel<K> {

  private final Event evtUpperBoundChanged = new Event();
  private final Event evtLowerBoundChanged = new Event();
  private final Event evtSearchTypeChanged = new Event();
  private final Event evtSearchStateChanged = new Event();
  private final Event evtSearchModelCleared = new Event();
  private final Event evtEnabledChanged = new Event();

  private final State stLocked = new State();

  private final K searchKey;
  private final int type;

  private SearchType searchType = SearchType.LIKE;
  private boolean enabled = false;
  private boolean autoEnable = true;
  private boolean automaticWildcard = false;
  private boolean caseSensitive = true;
  private Object upperBound = null;
  private Object lowerBound = null;
  private String wildcard;

  public DefaultSearchModel(final K searchKey, final int type, final String wildcard) {
    Util.rejectNullValue(searchKey, "searchKey");
    this.searchKey = searchKey;
    this.type = type;
    this.wildcard = wildcard;
    bindEvents();
  }

  public final K getSearchKey() {
    return searchKey;
  }

  /**
   * @return true if this filter is be case sensitive
   */
  public final boolean isCaseSensitive() {
    return caseSensitive;
  }

  /**
   * @param caseSensitive true if this search model should be case sensitive when working with strings
   */
  public final void setCaseSensitive(final boolean caseSensitive) {
    this.caseSensitive = caseSensitive;
  }

  /**
   * Locks/unlocks this search model, thus preventing changes to either values or search type
   * @param value the value
   */
  public final void setLocked(final boolean value) {
    stLocked.setActive(value);
  }

  /**
   * @return the data type of the underlying search property.
   * @see Types
   */
  public final int getType() {
    return type;
  }

  public final void setLikeValue(final Comparable value) {
    setSearchType(SearchType.LIKE);
    setUpperBound(value);
    final boolean on = value != null;
    if (isSearchEnabled() != on) {
      setSearchEnabled(on);
    }
    else {
      eventUpperBoundChanged().fire();
    }
  }

  /**
   * @param upper the upper bound
   * @throws IllegalStateException in case this model has been locked
   */
  public final void setUpperBound(final Object upper) {
    checkLock();
    if (!Util.equal(upperBound, upper)) {
      upperBound = upper;
      evtUpperBoundChanged.fire();
    }
  }

  /**
   * @return the upper bound
   */
  public final Object getUpperBound() {
    if (type == Types.VARCHAR && automaticWildcard) {
      return wildcard + upperBound + wildcard;
    }
    else {
      return upperBound;
    }
  }

  /**
   * @param value the lower bound
   * @throws IllegalStateException in case this model has been locked
   */
  public final void setLowerBound(final Object value) {
    checkLock();
    if (!Util.equal(lowerBound, value)) {
      lowerBound = value;
      evtLowerBoundChanged.fire();
    }
  }

  /**
   * @return the lower bound
   */
  public final Object getLowerBound() {
    if (type == Types.VARCHAR && automaticWildcard) {
      return wildcard + lowerBound + wildcard;
    }
    else {
      return lowerBound;
    }
  }

  /**
   * @param value the upper bound
   */
  public final void setUpperBound(final String value) {
    setUpperBound((Object) value);
  }

  /**
   * @param value the upper bound
   */
  public final void setUpperBound(final Double value) {
    setUpperBound((Object) value);
  }

  /**
   * @param value the upper bound
   */
  public final void setUpperBound(final Integer value) {
    setUpperBound((Object) value);
  }

  /**
   * @param value the upper bound
   */
  public final void setUpperBound(final boolean value) {
    setUpperBound(Boolean.valueOf(value));
  }

  /**
   * @param value the upper bound
   */
  public final void setUpperBound(final char value) {
    setUpperBound(Character.valueOf(value));
  }

  /**
   * @param value the upper bound
   */
  public final void setUpperBound(final Boolean value) {
    setUpperBound((Object) value);
  }

  /**
   * @param value the upper bound
   */
  public final void setUpperBound(final Timestamp value) {
    setUpperBound((Object) value);
  }

  /**
   * @param value the upper bound
   */
  public final void setUpperBound(final Date value) {
    setUpperBound((Object) value);
  }

  /**
   * @param value the Lower bound
   */
  public final void setLowerBound(final String value) {
    setLowerBound((Object) value);
  }

  /**
   * @param value the Lower bound
   */
  public final void setLowerBound(final Double value) {
    setLowerBound((Object) value);
  }

  /**
   * @param value the Lower bound
   */
  public final void setLowerBound(final Integer value) {
    setLowerBound((Object) value);
  }

  /**
   * @param value the Lower bound
   */
  public final void setLowerBound(final boolean value) {
    setLowerBound(Boolean.valueOf(value));
  }

  /**
   * @param value the Lower bound
   */
  public final void setLowerBound(final char value) {
    setLowerBound(Character.valueOf(value));
  }

  /**
   * @param value the Lower bound
   */
  public final void setLowerBound(final Boolean value) {
    setLowerBound((Object) value);
  }

  /**
   * @param value the Lower bound
   */
  public final void setLowerBound(final Timestamp value) {
    setLowerBound((Object) value);
  }

  /**
   * @param value the Lower bound
   */
  public final void setLowerBound(final Date value) {
    setLowerBound((Object) value);
  }

  /**
   * @return the search type
   */
  public final SearchType getSearchType() {
    return searchType;
  }

  /**
   * @param searchType the search type
   * @throws IllegalStateException in case this model has been locked
   */
  public final void setSearchType(final SearchType searchType) {
    Util.rejectNullValue(searchType, "searchType");
    checkLock();
    if (!this.searchType.equals(searchType)) {
      this.searchType = searchType;
      evtSearchTypeChanged.fire();
    }
  }

  /**
   * @return the wildcard
   */
  public final String getWildcard() {
    return wildcard;
  }

  /**
   * Sets the wildcard to use
   * @param wildcard the wildcard
   */
  public final void setWildcard(final String wildcard) {
    this.wildcard = wildcard;
  }

  public final boolean isAutoEnable() {
    return autoEnable;
  }

  public final void setAutoEnable(final boolean autoEnable) {
    this.autoEnable = autoEnable;
  }

  /**
   * @return true if this search model is enabled
   */
  public final boolean isSearchEnabled() {
    return enabled;
  }

  /**
   * @param value true if this search model should be enabled
   * @throws IllegalStateException in case this model has been locked
   */
  public final void setSearchEnabled(final boolean value) {
    checkLock();
    if (enabled != value) {
      enabled = value;
      evtEnabledChanged.fire();
    }
  }

  /**
   * @param value true if wildcard should automatically be added to strings
   */
  public final void setAutomaticWildcard(final boolean value) {
    automaticWildcard = value;
  }

  /**
   * @return true if wildcard is automatically be added to strings
   */
  public final boolean isAutomaticWildcard() {
    return automaticWildcard;
  }

  public final void clearSearch() {
    setSearchEnabled(false);
    setUpperBound((Object) null);
    setLowerBound((Object) null);
    setSearchType(SearchType.LIKE);
    evtSearchModelCleared.fire();
  }

  public final State stateLocked() {
    return stLocked.getLinkedState();
  }

  public final Event eventEnabledChanged() {
    return evtEnabledChanged;
  }

  public final Event eventLowerBoundChanged() {
    return evtLowerBoundChanged;
  }

  public final Event eventSearchModelCleared() {
    return evtSearchModelCleared;
  }

  public final Event eventSearchStateChanged() {
    return evtSearchStateChanged;
  }

  public final Event eventSearchTypeChanged() {
    return evtSearchTypeChanged;
  }

  public final Event eventUpperBoundChanged() {
    return evtUpperBoundChanged;
  }

  public final boolean include(final Object object) {
    return include(getComparable(object));
  }

  public final boolean include(final Comparable comparable) {
    if (!isSearchEnabled()) {
      return true;
    }

    Comparable toCompare = comparable;
    if (comparable instanceof Timestamp) {//ignore seconds and milliseconds
      toCompare = DateUtil.floorTimestamp((Timestamp) toCompare);
    }

    switch (getSearchType()) {
      case LIKE:
        return includeLike(toCompare);
      case NOT_LIKE:
        return includeNotLike(toCompare);
      case AT_LEAST:
        return includeMax(toCompare);
      case AT_MOST:
        return includeMin(toCompare);
      case WITHIN_RANGE:
        return includeMinMaxInside(toCompare);
      case OUTSIDE_RANGE:
        return includeMinMaxOutside(toCompare);
    }

    throw new RuntimeException("Undefined search type: " + getSearchType());
  }

  public static int getValueCount(final SearchType searchType) {
    switch(searchType) {
      case LIKE:
      case AT_LEAST:
      case AT_MOST:
      case NOT_LIKE:
        return 1;
      case WITHIN_RANGE:
      case OUTSIDE_RANGE:
        return 2;
    }

    throw new IllegalArgumentException("Undefined search type " + searchType);
  }

  protected Comparable getComparable(final Object object) {
    return (Comparable) object;
  }

  private boolean includeLike(final Comparable comparable) {
    if (getUpperBound() == null) {
      return true;
    }

    if (comparable == null) {
      return false;
    }

    if (comparable instanceof String) {//for Entity and String values
      return includeExactWildcard((String) comparable);
    }

    return comparable.compareTo(getUpperBound()) == 0;
  }

  private boolean includeNotLike(final Comparable comparable) {
    if (getUpperBound() == null) {
      return true;
    }

    if (comparable == null) {
      return false;
    }

    if (comparable instanceof String && ((String) comparable).contains(getWildcard())) {
      return !includeExactWildcard((String) comparable);
    }

    return comparable.compareTo(getUpperBound()) != 0;
  }

  private boolean includeExactWildcard(final String value) {
    String upperBound = (String) getUpperBound();
    if (upperBound.equals(getWildcard())) {
      return true;
    }
    if (value == null) {
      return false;
    }

    String realValue = value;
    if (!isCaseSensitive()) {
      upperBound = upperBound.toUpperCase(Locale.getDefault());
      realValue = realValue.toUpperCase(Locale.getDefault());
    }

    if (upperBound.indexOf(getWildcard()) < 0) {
      return realValue.compareTo(upperBound) == 0;
    }

    return Pattern.matches(prepareForRegex(upperBound), realValue);
  }

  private String prepareForRegex(final String string) {
    //a somewhat dirty fix to get rid of the '$' sign from the pattern, since it interferes with the regular expression parsing
    return string.replaceAll(getWildcard(), ".*").replaceAll("\\$", ".").replaceAll("\\]", "\\\\]").replaceAll("\\[", "\\\\[");
  }

  private boolean includeMax(final Comparable comparable) {
    return getUpperBound() == null || comparable != null && comparable.compareTo(getUpperBound()) <= 0;
  }

  private boolean includeMin(final Comparable comparable) {
    return getUpperBound() == null || comparable != null && comparable.compareTo(getUpperBound()) >= 0;
  }

  private boolean includeMinMaxInside(final Comparable comparable) {
    if (getLowerBound() == null && getUpperBound() == null) {
      return true;
    }

    if (comparable == null) {
      return false;
    }

    if (getLowerBound() == null) {
      return comparable.compareTo(getUpperBound()) <= 0;
    }

    if (getUpperBound() == null) {
      return comparable.compareTo(getLowerBound()) >= 0;
    }

    final int lowerCompareResult = comparable.compareTo(getLowerBound());
    final int upperCompareResult = comparable.compareTo(getUpperBound());

    return lowerCompareResult >= 0 && upperCompareResult <= 0;
  }

  private boolean includeMinMaxOutside(final Comparable comparable) {
    if (getLowerBound() == null && getUpperBound() == null) {
      return true;
    }

    if (comparable == null) {
      return false;
    }

    if (getLowerBound() == null) {
      return comparable.compareTo(getUpperBound()) >= 0;
    }

    if (getUpperBound() == null) {
      return comparable.compareTo(getLowerBound()) <= 0;
    }

    final int lowerCompareResult = comparable.compareTo(getLowerBound());
    final int upperCompareResult = comparable.compareTo(getUpperBound());

    return lowerCompareResult <= 0 || upperCompareResult >= 0;
  }

  private void bindEvents() {
    final ActionListener autoEnableListener = new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        if (autoEnable) {
          final boolean upperBoundNull = upperBound == null;
          final boolean lowerBoundNull = lowerBound == null;
          if (getValueCount(searchType) == 2) {
            setSearchEnabled(!lowerBoundNull && !upperBoundNull);
          }
          else {
            setSearchEnabled(!upperBoundNull);
          }
        }
      }
    };
    evtUpperBoundChanged.addListener(autoEnableListener);
    evtLowerBoundChanged.addListener(autoEnableListener);
    evtUpperBoundChanged.addListener(evtSearchStateChanged);
    evtLowerBoundChanged.addListener(evtSearchStateChanged);
    evtSearchTypeChanged.addListener(evtSearchStateChanged);
    evtEnabledChanged.addListener(evtSearchStateChanged);
  }

  private void checkLock() {
    if (stLocked.isActive()) {
      throw new IllegalStateException("Search model for key " + searchKey + " is locked");
    }
  }
}
