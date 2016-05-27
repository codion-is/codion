/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.table;

import org.jminor.common.Event;
import org.jminor.common.EventInfoListener;
import org.jminor.common.EventListener;
import org.jminor.common.EventObserver;
import org.jminor.common.Events;
import org.jminor.common.State;
import org.jminor.common.StateObserver;
import org.jminor.common.States;
import org.jminor.common.Value;
import org.jminor.common.Values;
import org.jminor.common.model.DateUtil;
import org.jminor.common.model.SearchType;

import java.sql.Timestamp;
import java.sql.Types;
import java.text.Format;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A default ColumnCriteriaModel model implementation.
 * @param <K> the type of the column identifier
 */
public class DefaultColumnCriteriaModel<K> implements ColumnCriteriaModel<K> {

  private final Value upperBoundValue = Values.value();
  private final Value lowerBoundValue = Values.value();
  private final Value<SearchType> searchTypeValue = Values.value(SearchType.LIKE);
  private final Event criteriaStateChangedEvent = Events.event();
  private final Event criteriaModelClearedEvent = Events.event();

  private final State enabledState = States.state();
  private final State lockedState = States.state();
  private final State lowerBoundRequiredState = States.state();

  private final K columnIdentifier;
  private final int type;
  private final Format format;

  private boolean autoEnable = true;
  private boolean automaticWildcard = false;
  private boolean caseSensitive = true;
  private String wildcard;

  /**
   * Instantiates a DefaultColumnCriteriaModel.
   * @param columnIdentifier the column identifier
   * @param type the column data type
   * @param wildcard the string to use as wildcard
   */
  public DefaultColumnCriteriaModel(final K columnIdentifier, final int type, final String wildcard) {
    this(columnIdentifier, type, wildcard, null);
  }

  /**
   * Instantiates a DefaultColumnCriteriaModel.
   * @param columnIdentifier the column identifier
   * @param type the column data type
   * @param wildcard the string to use as wildcard
   * @param format the format to use when presenting the values, dates for example
   */
  public DefaultColumnCriteriaModel(final K columnIdentifier, final int type, final String wildcard,
                                    final Format format) {
    Objects.requireNonNull(columnIdentifier, "columnIdentifier");
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
    lockedState.setActive(value);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isLocked() {
    return lockedState.isActive();
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
    if (enabledState.isActive() != enableSearch) {
      setEnabled(enableSearch);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void setUpperBound(final Object upper) {
    checkLock();
    upperBoundValue.set(upper);
  }

  /** {@inheritDoc} */
  @Override
  public final Object getUpperBound() {
    final Object upperBound = upperBoundValue.get();
    if (type == Types.VARCHAR) {
      if (upperBound == null || (upperBound instanceof String && ((String) upperBound).length() == 0)) {
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
    lowerBoundValue.set(value);
  }

  /** {@inheritDoc} */
  @Override
  public final Object getLowerBound() {
    final Object lowerBound = lowerBoundValue.get();
    if (type == Types.VARCHAR) {
      if (lowerBound == null || (lowerBound instanceof String && ((String) lowerBound).length() == 0)) {
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
  public final SearchType getSearchType() {
    return searchTypeValue.get();
  }

  /** {@inheritDoc} */
  @Override
  public final void setSearchType(final SearchType searchType) {
    Objects.requireNonNull(searchType, "searchType");
    checkLock();
    searchTypeValue.set(searchType);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isLowerBoundRequired() {
    return lowerBoundRequiredState.isActive();
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
    return enabledState.isActive();
  }

  /** {@inheritDoc} */
  @Override
  public final void setEnabled(final boolean enabled) {
    checkLock();
    if (enabledState.isActive() != enabled) {
      enabledState.setActive(enabled);
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
  public final void clearCriteria() {
    setEnabled(false);
    setUpperBound(null);
    setLowerBound(null);
    setSearchType(SearchType.LIKE);
    criteriaModelClearedEvent.fire();
  }

  /** {@inheritDoc} */
  @Override
  public final StateObserver getLockedObserver() {
    return lockedState.getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public Value getLowerBoundValue() {
    return lowerBoundValue;
  }

  /** {@inheritDoc} */
  @Override
  public Value getUpperBoundValue() {
    return upperBoundValue;
  }

  /** {@inheritDoc} */
  @Override
  public final EventObserver<Boolean> getEnabledObserver() {
    return enabledState.getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final void addEnabledListener(final EventListener listener) {
    enabledState.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeEnabledListener(final EventListener listener) {
    enabledState.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addUpperBoundListener(final EventListener listener) {
    upperBoundValue.getObserver().addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeUpperBoundListener(final EventListener listener) {
    upperBoundValue.getObserver().removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addLowerBoundListener(final EventListener listener) {
    lowerBoundValue.getObserver().addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeLowerBoundListener(final EventListener listener) {
    lowerBoundValue.getObserver().removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addLowerBoundRequiredListener(final EventListener listener) {
    lowerBoundRequiredState.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeLowerBoundRequiredListener(final EventListener listener) {
    lowerBoundRequiredState.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addClearedListener(final EventListener listener) {
    criteriaModelClearedEvent.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeClearedListener(final EventListener listener) {
    criteriaModelClearedEvent.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addCriteriaStateListener(final EventListener listener) {
    criteriaStateChangedEvent.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeCriteriaStateListener(final EventListener listener) {
    criteriaStateChangedEvent.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addSearchTypeListener(final EventInfoListener<SearchType> listener) {
    searchTypeValue.getObserver().addInfoListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeSearchTypeListener(final EventInfoListener listener) {
    searchTypeValue.getObserver().removeInfoListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final EventObserver<SearchType> getSearchTypeObserver() {
    return searchTypeValue.getObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean include(final Object object) {
    return !enabledState.isActive() || include(getComparable(object));
  }

  /** {@inheritDoc} */
  @Override
  public final boolean include(final Comparable comparable) {
    if (!enabledState.isActive()) {
      return true;
    }

    Comparable toCompare = comparable;
    if (comparable instanceof Timestamp) {//ignore seconds and milliseconds
      toCompare = DateUtil.floorTimestamp((Timestamp) toCompare);
    }

    switch (searchTypeValue.get()) {
      case LIKE:
        return includeLike(toCompare);
      case NOT_LIKE:
        return includeNotLike(toCompare);
      case LESS_THAN:
        return includeLessThan(toCompare);
      case GREATER_THAN:
        return includeGreaterThan(toCompare);
      case WITHIN_RANGE:
        return includeWithinRange(toCompare);
      case OUTSIDE_RANGE:
        return includeOutsideRange(toCompare);
      default:
        throw new IllegalArgumentException("Undefined search type: " + searchTypeValue.get());
    }
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

  private boolean includeLessThan(final Comparable comparable) {
    return getUpperBound() == null || comparable != null && comparable.compareTo(getUpperBound()) <= 0;
  }

  private boolean includeGreaterThan(final Comparable comparable) {
    return getUpperBound() == null || comparable != null && comparable.compareTo(getUpperBound()) >= 0;
  }

  private boolean includeWithinRange(final Comparable comparable) {
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

  private boolean includeOutsideRange(final Comparable comparable) {
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
    final EventListener autoEnableListener = new EventListener() {
      @Override
      public void eventOccurred() {
        if (autoEnable) {
          final boolean upperBoundNull = upperBoundValue.get() == null;
          final boolean lowerBoundNull = lowerBoundValue.get() == null;
          if (searchTypeValue.get().getValues().equals(SearchType.Values.TWO)) {
            setEnabled(!lowerBoundNull && !upperBoundNull);
          }
          else {
            setEnabled(!upperBoundNull);
          }
        }
      }
    };
    upperBoundValue.getObserver().addListener(autoEnableListener);
    lowerBoundValue.getObserver().addListener(autoEnableListener);
    upperBoundValue.getObserver().addListener(criteriaStateChangedEvent);
    lowerBoundValue.getObserver().addListener(criteriaStateChangedEvent);
    searchTypeValue.getObserver().addListener(criteriaStateChangedEvent);
    enabledState.addListener(criteriaStateChangedEvent);
    searchTypeValue.getObserver().addListener(new EventListener() {
      @Override
      public void eventOccurred() {
        lowerBoundRequiredState.setActive(getSearchType().getValues().equals(SearchType.Values.TWO));
      }
    });
  }

  private void checkLock() {
    if (lockedState.isActive()) {
      throw new IllegalStateException("Criteria model for column identified by " + columnIdentifier + " is locked");
    }
  }
}
