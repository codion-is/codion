/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.table;

import org.jminor.common.db.Operator;
import org.jminor.common.event.Event;
import org.jminor.common.event.EventDataListener;
import org.jminor.common.event.EventListener;
import org.jminor.common.event.EventObserver;
import org.jminor.common.event.Events;
import org.jminor.common.state.State;
import org.jminor.common.state.StateObserver;
import org.jminor.common.state.States;
import org.jminor.common.value.Value;
import org.jminor.common.value.Values;

import java.text.Format;
import java.util.Collection;
import java.util.Locale;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

/**
 * A default ColumnConditionModel model implementation.
 * @param <R> the type of the rows
 * @param <K> the type of the column identifier
 */
public class DefaultColumnConditionModel<R, K> implements ColumnConditionModel<R, K> {

  private final Value<Object> upperBoundValue = Values.value();
  private final Value<Object> lowerBoundValue = Values.value();
  private final Value<Operator> operatorValue = Values.value(Operator.LIKE);
  private final Event conditionChangedEvent = Events.event();
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
  private boolean caseSensitive = CASE_SENSITIVE.get();
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
    this.columnIdentifier = requireNonNull(columnIdentifier, "columnIdentifier");
    this.typeClass = typeClass;
    this.wildcard = wildcard;
    this.format = format;
    this.dateTimeFormatPattern = dateTimeFormatPattern;
    this.automaticWildcard = automaticWildcard;
    bindEvents();
  }

  @Override
  public final K getColumnIdentifier() {
    return columnIdentifier;
  }

  @Override
  public final boolean isCaseSensitive() {
    return caseSensitive;
  }

  @Override
  public final void setCaseSensitive(final boolean caseSensitive) {
    this.caseSensitive = caseSensitive;
  }

  @Override
  public final Format getFormat() {
    return format;
  }

  @Override
  public String getDateTimeFormatPattern() {
    return dateTimeFormatPattern;
  }

  @Override
  public final void setLocked(final boolean locked) {
    lockedState.set(locked);
  }

  @Override
  public final boolean isLocked() {
    return lockedState.get();
  }

  @Override
  public final Class getTypeClass() {
    return typeClass;
  }

  @Override
  public final void setLikeValue(final Object value) {
    setOperator(Operator.LIKE);
    setUpperBound(value);
    final boolean enableSearch = value != null;
    if (enabledState.get() != enableSearch) {
      setEnabled(enableSearch);
    }
  }

  @Override
  public final void setUpperBound(final Object value) {
    validateType(value);
    checkLock();
    upperBoundValue.set(value);
  }

  @Override
  public final Object getUpperBound() {
    return getBoundValue(upperBoundValue.get());
  }

  @Override
  public final void setLowerBound(final Object value) {
    validateType(value);
    checkLock();
    lowerBoundValue.set(value);
  }

  @Override
  public final Object getLowerBound() {
    return getBoundValue(lowerBoundValue.get());
  }

  @Override
  public final Operator getOperator() {
    return operatorValue.get();
  }

  @Override
  public final void setOperator(final Operator operator) {
    checkLock();
    operatorValue.set(requireNonNull(operator, "operator"));
  }

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

  @Override
  public final boolean isAutoEnable() {
    return autoEnable;
  }

  @Override
  public final void setAutoEnable(final boolean autoEnable) {
    this.autoEnable = autoEnable;
  }

  @Override
  public final boolean isEnabled() {
    return enabledState.get();
  }

  @Override
  public final void setEnabled(final boolean enabled) {
    checkLock();
    if (enabledState.get() != enabled) {
      enabledState.set(enabled);
    }
  }

  @Override
  public final void setAutomaticWildcard(final AutomaticWildcard automaticWildcard) {
    this.automaticWildcard = requireNonNull(automaticWildcard);
  }

  @Override
  public final AutomaticWildcard getAutomaticWildcard() {
    return automaticWildcard;
  }

  @Override
  public final void clearCondition() {
    setEnabled(false);
    setUpperBound(null);
    setLowerBound(null);
    setOperator(Operator.LIKE);
    conditionModelClearedEvent.onEvent();
  }

  @Override
  public final StateObserver getLockedObserver() {
    return lockedState.getObserver();
  }

  @Override
  public Value getLowerBoundValue() {
    return lowerBoundValue;
  }

  @Override
  public Value getUpperBoundValue() {
    return upperBoundValue;
  }

  @Override
  public final EventObserver<Boolean> getEnabledObserver() {
    return enabledState.getObserver();
  }

  @Override
  public final void addEnabledListener(final EventListener listener) {
    enabledState.addListener(listener);
  }

  @Override
  public final void removeEnabledListener(final EventListener listener) {
    enabledState.removeListener(listener);
  }

  @Override
  public final void addUpperBoundListener(final EventListener listener) {
    upperBoundValue.addListener(listener);
  }

  @Override
  public final void removeUpperBoundListener(final EventListener listener) {
    upperBoundValue.removeListener(listener);
  }

  @Override
  public final void addLowerBoundListener(final EventListener listener) {
    lowerBoundValue.addListener(listener);
  }

  @Override
  public final void removeLowerBoundListener(final EventListener listener) {
    lowerBoundValue.removeListener(listener);
  }

  @Override
  public final void addLowerBoundRequiredListener(final EventListener listener) {
    lowerBoundRequiredState.addListener(listener);
  }

  @Override
  public final void removeLowerBoundRequiredListener(final EventListener listener) {
    lowerBoundRequiredState.removeListener(listener);
  }

  @Override
  public final void addClearedListener(final EventListener listener) {
    conditionModelClearedEvent.addListener(listener);
  }

  @Override
  public final void removeClearedListener(final EventListener listener) {
    conditionModelClearedEvent.removeListener(listener);
  }

  @Override
  public final void addConditionChangedListener(final EventListener listener) {
    conditionChangedEvent.addListener(listener);
  }

  @Override
  public final void removeConditionChangedListener(final EventListener listener) {
    conditionChangedEvent.removeListener(listener);
  }

  @Override
  public final void addOperatorListener(final EventDataListener<Operator> listener) {
    operatorValue.addDataListener(listener);
  }

  @Override
  public final void removeOperatorListener(final EventDataListener<Operator> listener) {
    operatorValue.removeDataListener(listener);
  }

  @Override
  public final EventObserver<Operator> getOperatorObserver() {
    return operatorValue;
  }

  @Override
  public final boolean include(final R row) {
    return !enabledState.get() || include(getComparable(row));
  }

  @Override
  public final boolean include(final Comparable comparable) {
    if (!enabledState.get()) {
      return true;
    }

    switch (operatorValue.get()) {
      case LIKE:
        return includeLike(comparable);
      case NOT_LIKE:
        return includeNotLike(comparable);
      case LESS_THAN:
        return includeLessThan(comparable);
      case GREATER_THAN:
        return includeGreaterThan(comparable);
      case WITHIN_RANGE:
        return includeWithinRange(comparable);
      case OUTSIDE_RANGE:
        return includeOutsideRange(comparable);
      default:
        throw new IllegalArgumentException("Undefined operator: " + operatorValue.get());
    }
  }

  /**
   * This default implementation simply returns the row, assuming it is a Comparable instance.
   * @param row the row
   * @return a Comparable from the given row to compare with this condition model's value.
   */
  protected Comparable getComparable(final R row) {
    return (Comparable) row;
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
    if (comparable == null) {
      return getUpperBound() == null;
    }
    if (getUpperBound() == null) {
      return comparable == null;
    }

    if (comparable instanceof String) {//for Entity and String values
      return includeExactWildcard((String) comparable);
    }

    return comparable.compareTo(getUpperBound()) == 0;
  }

  private boolean includeNotLike(final Comparable comparable) {
    if (comparable == null) {
      return getUpperBound() != null;
    }
    if (getUpperBound() == null) {
      return comparable != null;
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
    if (operatorValue.get().equals(Operator.LIKE) || operatorValue.get().equals(Operator.NOT_LIKE)) {
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
        if (operatorValue.get().getValues().equals(Operator.Values.TWO)) {
          setEnabled(!lowerBoundNull && !upperBoundNull);
        }
        else {
          setEnabled(!upperBoundNull);
        }
      }
    };
    upperBoundValue.addListener(autoEnableListener);
    lowerBoundValue.addListener(autoEnableListener);
    upperBoundValue.addListener(conditionChangedEvent);
    lowerBoundValue.addListener(conditionChangedEvent);
    operatorValue.addListener(conditionChangedEvent);
    enabledState.addListener(conditionChangedEvent);
    operatorValue.addListener(() ->
            lowerBoundRequiredState.set(getOperator().getValues().equals(Operator.Values.TWO)));
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
