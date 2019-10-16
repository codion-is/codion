/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.table;

import org.jminor.common.Event;
import org.jminor.common.EventDataListener;
import org.jminor.common.EventListener;
import org.jminor.common.EventObserver;
import org.jminor.common.Events;
import org.jminor.common.State;
import org.jminor.common.StateObserver;
import org.jminor.common.States;
import org.jminor.common.Value;
import org.jminor.common.Values;
import org.jminor.common.db.condition.Condition;

import java.text.Format;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Locale;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

/**
 * A default ColumnConditionModel model implementation.
 * @param <K> the type of the column identifier
 */
public class DefaultColumnConditionModel<K> implements ColumnConditionModel<K> {

  private final Value upperBoundValue = Values.value();
  private final Value lowerBoundValue = Values.value();
  private final Value<Condition.Type> conditionTypeValue = Values.value(Condition.Type.LIKE);
  private final Event conditionStateChangedEvent = Events.event();
  private final Event conditionModelClearedEvent = Events.event();

  private final State enabledState = States.state();
  private final State lockedState = States.state();
  private final State lowerBoundRequiredState = States.state();

  private final K columnIdentifier;
  private final Class typeClass;
  private final Format format;
  private final String dateTimeFormatPattern;

  private boolean autoEnable = true;
  private AutomaticWildcard automaticWildcard;
  private boolean caseSensitive;
  private String wildcard;

  /**
   * Instantiates a DefaultColumnConditionModel.
   * @param columnIdentifier the column identifier
   * @param typeClass the data type
   * @param wildcard the string to use as wildcard
   */
  public DefaultColumnConditionModel(final K columnIdentifier, final Class typeClass, final String wildcard) {
    this(columnIdentifier, typeClass, wildcard, null, null);
  }

  /**
   * Instantiates a DefaultColumnConditionModel.
   * @param columnIdentifier the column identifier
   * @param typeClass the data type
   * @param wildcard the string to use as wildcard
   * @param format the format to use when presenting the values, numbers for example
   * @param dateTimeFormatPattern the date/time format pattern to use in case of a date/time column
   */
  public DefaultColumnConditionModel(final K columnIdentifier, final Class typeClass, final String wildcard,
                                     final Format format, final String dateTimeFormatPattern) {
    this(columnIdentifier, typeClass, wildcard, format, dateTimeFormatPattern, AUTOMATIC_WILDCARD.get());
  }

  /**
   * Instantiates a DefaultColumnConditionModel.
   * @param columnIdentifier the column identifier
   * @param typeClass the data type
   * @param wildcard the string to use as wildcard
   * @param format the format to use when presenting the values, numbers for example
   * @param dateTimeFormatPattern the date/time format pattern to use in case of a date/time column
   * @param automaticWildcard the automatic wildcard type to use
   */
  public DefaultColumnConditionModel(final K columnIdentifier, final Class typeClass, final String wildcard,
                                     final Format format, final String dateTimeFormatPattern,
                                     final AutomaticWildcard automaticWildcard) {
    this(columnIdentifier, typeClass, wildcard, format, dateTimeFormatPattern, automaticWildcard, CASE_SENSITIVE.get());
  }

  /**
   * Instantiates a DefaultColumnConditionModel.
   * @param columnIdentifier the column identifier
   * @param typeClass the data type
   * @param wildcard the string to use as wildcard
   * @param format the format to use when presenting the values, numbers for example
   * @param dateTimeFormatPattern the date/time format pattern to use in case of a date/time column
   * @param automaticWildcard the automatic wildcard type to use
   * @param caseSensitive true if string based conditions should be case sensitive
   */
  public DefaultColumnConditionModel(final K columnIdentifier, final Class typeClass, final String wildcard,
                                     final Format format, final String dateTimeFormatPattern,
                                     final AutomaticWildcard automaticWildcard, final boolean caseSensitive) {
    this.columnIdentifier = requireNonNull(columnIdentifier, "columnIdentifier");
    this.typeClass = typeClass;
    this.wildcard = wildcard;
    this.format = format;
    this.dateTimeFormatPattern = dateTimeFormatPattern;
    this.automaticWildcard = automaticWildcard;
    this.caseSensitive = caseSensitive;
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
  public String getDateTimeFormatPattern() {
    return dateTimeFormatPattern;
  }

  /** {@inheritDoc} */
  @Override
  public final void setLocked(final boolean value) {
    lockedState.set(value);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isLocked() {
    return lockedState.get();
  }

  /** {@inheritDoc} */
  @Override
  public final Class getTypeClass() {
    return typeClass;
  }

  /** {@inheritDoc} */
  @Override
  public final void setLikeValue(final Object value) {
    setConditionType(Condition.Type.LIKE);
    setUpperBound(value);
    final boolean enableSearch = value != null;
    if (enabledState.get() != enableSearch) {
      setEnabled(enableSearch);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void setUpperBound(final Object value) {
    validateType(value);
    checkLock();
    upperBoundValue.set(value);
  }

  /** {@inheritDoc} */
  @Override
  public final Object getUpperBound() {
    return getBoundValue(upperBoundValue.get());
  }

  /** {@inheritDoc} */
  @Override
  public final void setLowerBound(final Object value) {
    validateType(value);
    checkLock();
    lowerBoundValue.set(value);
  }

  /** {@inheritDoc} */
  @Override
  public final Object getLowerBound() {
    return getBoundValue(lowerBoundValue.get());
  }

  /** {@inheritDoc} */
  @Override
  public final Condition.Type getConditionType() {
    return conditionTypeValue.get();
  }

  /** {@inheritDoc} */
  @Override
  public final void setConditionType(final Condition.Type conditionType) {
    checkLock();
    conditionTypeValue.set(requireNonNull(conditionType, "conditionType"));
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isLowerBoundRequired() {
    return lowerBoundRequiredState.get();
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
    return enabledState.get();
  }

  /** {@inheritDoc} */
  @Override
  public final void setEnabled(final boolean enabled) {
    checkLock();
    if (enabledState.get() != enabled) {
      enabledState.set(enabled);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void setAutomaticWildcard(final AutomaticWildcard automaticWildcard) {
    this.automaticWildcard = requireNonNull(automaticWildcard);
  }

  /** {@inheritDoc} */
  @Override
  public final AutomaticWildcard getAutomaticWildcard() {
    return automaticWildcard;
  }

  /** {@inheritDoc} */
  @Override
  public final void clearCondition() {
    setEnabled(false);
    setUpperBound(null);
    setLowerBound(null);
    setConditionType(Condition.Type.LIKE);
    conditionModelClearedEvent.fire();
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
    upperBoundValue.getChangeObserver().addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeUpperBoundListener(final EventListener listener) {
    upperBoundValue.getChangeObserver().removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addLowerBoundListener(final EventListener listener) {
    lowerBoundValue.getChangeObserver().addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeLowerBoundListener(final EventListener listener) {
    lowerBoundValue.getChangeObserver().removeListener(listener);
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
    conditionModelClearedEvent.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeClearedListener(final EventListener listener) {
    conditionModelClearedEvent.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addConditionStateListener(final EventListener listener) {
    conditionStateChangedEvent.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeConditionStateListener(final EventListener listener) {
    conditionStateChangedEvent.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addConditionTypeListener(final EventDataListener<Condition.Type> listener) {
    conditionTypeValue.getChangeObserver().addDataListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeConditionTypeListener(final EventDataListener listener) {
    conditionTypeValue.getChangeObserver().removeDataListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final EventObserver<Condition.Type> getConditionTypeObserver() {
    return conditionTypeValue.getChangeObserver();
  }

  /** {@inheritDoc} */
  @Override
  public final boolean include(final Object object) {
    return !enabledState.get() || include(getComparable(object));
  }

  /** {@inheritDoc} */
  @Override
  public final boolean include(final Comparable comparable) {
    if (!enabledState.get()) {
      return true;
    }

    Comparable toCompare = comparable;
    if (comparable instanceof LocalDateTime) {
      toCompare = ((LocalDateTime) comparable).truncatedTo(ChronoUnit.MINUTES);
    }

    switch (conditionTypeValue.get()) {
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
        throw new IllegalArgumentException("Undefined search type: " + conditionTypeValue.get());
    }
  }

  /**
   * @param object the object
   * @return a Comparable representing the given object
   */
  protected Comparable getComparable(final Object object) {
    return (Comparable) object;
  }

  private Object getBoundValue(final Object upperBound) {
    if (typeClass.equals(String.class)) {
      if (upperBound == null || (upperBound instanceof String && ((String) upperBound).length() == 0)) {
        return null;
      }
      if (upperBound instanceof Collection) {
        return upperBound;
      }

      return addWildcard((String) upperBound);
    }

    return upperBound;
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
    if (upperBoundString == null) {
      upperBoundString = "";
    }
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

  private String addWildcard(final String value) {
    //only use wildcard for LIKE and NOT_LIKE
    if (conditionTypeValue.get().equals(Condition.Type.LIKE) || conditionTypeValue.get().equals(Condition.Type.NOT_LIKE)) {
      switch (automaticWildcard) {
        case PREFIX_AND_POSTFIX:
          return wildcard + value + wildcard;
        case PREFIX:
          return wildcard + value;
        case POSTFIX:
          return value + wildcard;
        default:
          return value;
      }
    }

    return value;
  }

  private void bindEvents() {
    final EventListener autoEnableListener = () -> {
      if (autoEnable) {
        final boolean upperBoundNull = upperBoundValue.get() == null;
        final boolean lowerBoundNull = lowerBoundValue.get() == null;
        if (conditionTypeValue.get().getValues().equals(Condition.Type.Values.TWO)) {
          setEnabled(!lowerBoundNull && !upperBoundNull);
        }
        else {
          setEnabled(!upperBoundNull);
        }
      }
    };
    upperBoundValue.getChangeObserver().addListener(autoEnableListener);
    lowerBoundValue.getChangeObserver().addListener(autoEnableListener);
    upperBoundValue.getChangeObserver().addListener(conditionStateChangedEvent);
    lowerBoundValue.getChangeObserver().addListener(conditionStateChangedEvent);
    conditionTypeValue.getChangeObserver().addListener(conditionStateChangedEvent);
    enabledState.addListener(conditionStateChangedEvent);
    conditionTypeValue.getChangeObserver().addListener(() ->
            lowerBoundRequiredState.set(getConditionType().getValues().equals(Condition.Type.Values.TWO)));
  }

  private void checkLock() {
    if (lockedState.get()) {
      throw new IllegalStateException("Condition model for column identified by " + columnIdentifier + " is locked");
    }
  }

  private void validateType(final Object value) {
    if (value != null) {
      if (value instanceof Collection) {
        for (final Object collValue : ((Collection) value)) {
          validateType(collValue);
        }
      }
      else if (!typeClass.isAssignableFrom(value.getClass())) {
        throw new IllegalArgumentException("Value of type " + typeClass + " expected for condition " + this + ", got: " + value.getClass());
      }
    }
  }
}
