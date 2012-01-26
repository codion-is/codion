/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.Format;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * A default ColumnSearchModel model implementation.
 * @param <K> the type of the column identifier
 */
@SuppressWarnings({"unchecked"})
public class DefaultColumnSearchModel<K> implements ColumnSearchModel<K> {

  private final Event evtUpperBoundChanged = Events.event();
  private final Event evtLowerBoundChanged = Events.event();
  private final Event evtSearchTypeChanged = Events.event();
  private final Event evtSearchStateChanged = Events.event();
  private final Event evtSearchModelCleared = Events.event();
  private final Event evtEnabledChanged = Events.event();

  private final State stLocked = States.state();
  private final State stLowerBoundRequired = States.state();

  private final K columnIdentifier;
  private final int type;
  private final Format format;

  private SearchType searchType = SearchType.LIKE;
  private boolean enabled = false;
  private boolean autoEnable = true;
  private boolean automaticWildcard = false;
  private boolean caseSensitive = true;
  private Object upperBound = null;
  private Object lowerBound = null;
  private String wildcard;

  /**
   * Instantiates a DefaultColumnSearchModel.
   * @param columnIdentifier the column identifier
   * @param type the column data type
   * @param wildcard the string to use as wildcard
   */
  public DefaultColumnSearchModel(final K columnIdentifier, final int type, final String wildcard) {
    this(columnIdentifier, type, wildcard, null);
  }

  /**
   * Instantiates a DefaultColumnSearchModel.
   * @param columnIdentifier the column identifier
   * @param type the column data type
   * @param wildcard the string to use as wildcard
   * @param format the format to use when presenting the values, dates for example
   */
  public DefaultColumnSearchModel(final K columnIdentifier, final int type, final String wildcard,
                                  final Format format) {
    Util.rejectNullValue(columnIdentifier, "searchKey");
    this.columnIdentifier = columnIdentifier;
    this.type = type;
    this.wildcard = wildcard;
    this.format = format;
    bindEvents();
  }

  /** {@inheritDoc} */
  @Override
  public final K getColumnIdentifier() {
    return columnIdentifier;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isCaseSensitive() {
    return caseSensitive;
  }

  /** {@inheritDoc} */
  @Override
  public final void setCaseSensitive(final boolean caseSensitive) {
    this.caseSensitive = caseSensitive;
  }

  /** {@inheritDoc} */
  @Override
  public final Format getFormat() {
    return format;
  }

  /** {@inheritDoc} */
  @Override
  public final void setLocked(final boolean value) {
    stLocked.setActive(value);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isLocked() {
    return stLocked.isActive();
  }

  /** {@inheritDoc} */
  @Override
  public final int getType() {
    return type;
  }

  /** {@inheritDoc} */
  @Override
  public final void setLikeValue(final Comparable value) {
    setSearchType(SearchType.LIKE);
    setUpperBound(value);
    final boolean enableSearch = value != null;
    if (enabled != enableSearch) {
      setEnabled(enableSearch);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void setUpperBound(final Object upper) {
    checkLock();
    if (!Util.equal(upperBound, upper)) {
      upperBound = upper;
      evtUpperBoundChanged.fire();
    }
  }

  /** {@inheritDoc} */
  @Override
  public final Object getUpperBound() {
    if (type == Types.VARCHAR) {
      if (upperBound == null || (upperBound instanceof String && ((String) upperBound).isEmpty())) {
        return null;
      }
      if (automaticWildcard) {
        return wildcard + upperBound + wildcard;
      }
      else {
        return upperBound;
      }
    }
    else {
      return upperBound;
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void setLowerBound(final Object value) {
    checkLock();
    if (!Util.equal(lowerBound, value)) {
      lowerBound = value;
      evtLowerBoundChanged.fire();
    }
  }

  /** {@inheritDoc} */
  @Override
  public final Object getLowerBound() {
    if (type == Types.VARCHAR) {
      if (lowerBound == null || (lowerBound instanceof String && ((String) lowerBound).isEmpty())) {
        return null;
      }
      if (automaticWildcard) {
        return wildcard + lowerBound + wildcard;
      }
      else {
        return lowerBound;
      }
    }
    else {
      return lowerBound;
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void setUpperBound(final String value) {
    setUpperBound((Object) value);
  }

  /** {@inheritDoc} */
  @Override
  public final void setUpperBound(final Double value) {
    setUpperBound((Object) value);
  }

  /** {@inheritDoc} */
  @Override
  public final void setUpperBound(final Integer value) {
    setUpperBound((Object) value);
  }

  /** {@inheritDoc} */
  @Override
  public final void setUpperBound(final boolean value) {
    setUpperBound(Boolean.valueOf(value));
  }

  /** {@inheritDoc} */
  @Override
  public final void setUpperBound(final char value) {
    setUpperBound(Character.valueOf(value));
  }

  /** {@inheritDoc} */
  @Override
  public final void setUpperBound(final Boolean value) {
    setUpperBound((Object) value);
  }

  /** {@inheritDoc} */
  @Override
  public final void setUpperBound(final Timestamp value) {
    setUpperBound((Object) value);
  }

  /** {@inheritDoc} */
  @Override
  public final void setUpperBound(final Date value) {
    setUpperBound((Object) value);
  }

  /** {@inheritDoc} */
  @Override
  public final void setLowerBound(final String value) {
    setLowerBound((Object) value);
  }

  /** {@inheritDoc} */
  @Override
  public final void setLowerBound(final Double value) {
    setLowerBound((Object) value);
  }

  /** {@inheritDoc} */
  @Override
  public final void setLowerBound(final Integer value) {
    setLowerBound((Object) value);
  }

  /** {@inheritDoc} */
  @Override
  public final void setLowerBound(final boolean value) {
    setLowerBound(Boolean.valueOf(value));
  }

  /** {@inheritDoc} */
  @Override
  public final void setLowerBound(final char value) {
    setLowerBound(Character.valueOf(value));
  }

  /** {@inheritDoc} */
  @Override
  public final void setLowerBound(final Boolean value) {
    setLowerBound((Object) value);
  }

  /** {@inheritDoc} */
  @Override
  public final void setLowerBound(final Timestamp value) {
    setLowerBound((Object) value);
  }

  /** {@inheritDoc} */
  @Override
  public final void setLowerBound(final Date value) {
    setLowerBound((Object) value);
  }

  /** {@inheritDoc} */
  @Override
  public final SearchType getSearchType() {
    return searchType;
  }

  /** {@inheritDoc} */
  @Override
  public final void setSearchType(final SearchType searchType) {
    Util.rejectNullValue(searchType, "searchType");
    checkLock();
    if (!this.searchType.equals(searchType)) {
      this.searchType = searchType;
      evtSearchTypeChanged.fire();
    }
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isLowerBoundRequired() {
    return stLowerBoundRequired.isActive();
  }

  /**
   * @return the search wildcard
   */
  public final String getWildcard() {
    return wildcard;
  }

  /**
   * @param wildcard the search wildcard
   */
  public final void setWildcard(final String wildcard) {
    this.wildcard = wildcard;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isAutoEnable() {
    return autoEnable;
  }

  /** {@inheritDoc} */
  @Override
  public final void setAutoEnable(final boolean autoEnable) {
    this.autoEnable = autoEnable;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isEnabled() {
    return enabled;
  }

  /** {@inheritDoc} */
  @Override
  public final void setEnabled(final boolean value) {
    checkLock();
    if (enabled != value) {
      enabled = value;
      evtEnabledChanged.fire();
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void setAutomaticWildcard(final boolean value) {
    automaticWildcard = value;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isAutomaticWildcard() {
    return automaticWildcard;
  }

  /** {@inheritDoc} */
  @Override
  public final void clearSearch() {
    setEnabled(false);
    setUpperBound((Object) null);
    setLowerBound((Object) null);
    setSearchType(SearchType.LIKE);
    evtSearchModelCleared.fire();
  }

  /** {@inheritDoc} */
  @Override
  public final StateObserver getLockedObserver() {
    return stLocked.getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final EventObserver getEnabledObserver() {
    return evtEnabledChanged.getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final EventObserver getLowerBoundObserver() {
    return evtLowerBoundChanged.getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final EventObserver getUpperBoundObserver() {
    return evtUpperBoundChanged.getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final void addEnabledListener(final ActionListener listener) {
    evtEnabledChanged.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeEnabledListener(final ActionListener listener) {
    evtEnabledChanged.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addUpperBoundListener(final ActionListener listener) {
    evtUpperBoundChanged.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeUpperBoundListener(final ActionListener listener) {
    evtUpperBoundChanged.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addLowerBoundListener(final ActionListener listener) {
    evtLowerBoundChanged.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeLowerBoundListener(final ActionListener listener) {
    evtLowerBoundChanged.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addLowerBoundRequiredListener(final ActionListener listener) {
    stLowerBoundRequired.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeLowerBoundRequiredListener(final ActionListener listener) {
    stLowerBoundRequired.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addClearedListener(final ActionListener listener) {
    evtSearchModelCleared.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeClearedListener(final ActionListener listener) {
    evtSearchModelCleared.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addSearchStateListener(final ActionListener listener) {
    evtSearchStateChanged.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeSearchStateListener(final ActionListener listener) {
    evtSearchStateChanged.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addSearchTypeListener(final ActionListener listener) {
    evtSearchTypeChanged.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeSearchTypeListener(final ActionListener listener) {
    evtSearchTypeChanged.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final EventObserver getSearchTypeObserver() {
    return evtSearchTypeChanged.getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean include(final Object object) {
    return include(getComparable(object));
  }

  /** {@inheritDoc} */
  @Override
  public final boolean include(final Comparable comparable) {
    if (!enabled) {
      return true;
    }

    Comparable toCompare = comparable;
    if (comparable instanceof Timestamp) {//ignore seconds and milliseconds
      toCompare = DateUtil.floorTimestamp((Timestamp) toCompare);
    }

    switch (searchType) {
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

    throw new IllegalArgumentException("Undefined search type: " + searchType);
  }

  /**
   * @param searchType the search type
   * @return the number of input values required for the given search type
   */
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

  /**
   * @param object the object
   * @return a Comparable representing the given object
   */
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

    if (comparable instanceof String && ((String) comparable).contains(wildcard)) {
      return !includeExactWildcard((String) comparable);
    }

    return comparable.compareTo(getUpperBound()) != 0;
  }

  private boolean includeExactWildcard(final String value) {
    String upperBoundString = (String) getUpperBound();
    if (upperBoundString.equals(wildcard)) {
      return true;
    }
    if (value == null) {
      return false;
    }

    String realValue = value;
    if (!caseSensitive) {
      upperBoundString = upperBoundString.toUpperCase(Locale.getDefault());
      realValue = realValue.toUpperCase(Locale.getDefault());
    }

    if (!upperBoundString.contains(wildcard)) {
      return realValue.compareTo(upperBoundString) == 0;
    }

    return Pattern.matches(prepareForRegex(upperBoundString), realValue);
  }

  private String prepareForRegex(final String string) {
    //a somewhat dirty fix to get rid of the '$' sign from the pattern, since it interferes with the regular expression parsing
    return string.replaceAll(wildcard, ".*").replaceAll("\\$", ".").replaceAll("\\]", "\\\\]").replaceAll("\\[", "\\\\[");
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
      /** {@inheritDoc} */
      @Override
      public void actionPerformed(final ActionEvent e) {
        if (autoEnable) {
          final boolean upperBoundNull = upperBound == null;
          final boolean lowerBoundNull = lowerBound == null;
          if (getValueCount(searchType) == 2) {
            setEnabled(!lowerBoundNull && !upperBoundNull);
          }
          else {
            setEnabled(!upperBoundNull);
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
    evtSearchTypeChanged.addListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        stLowerBoundRequired.setActive(getSearchType() == SearchType.WITHIN_RANGE || getSearchType() == SearchType.OUTSIDE_RANGE);
      }
    });
  }

  private void checkLock() {
    if (stLocked.isActive()) {
      throw new IllegalStateException("Search model for key " + columnIdentifier + " is locked");
    }
  }
}
