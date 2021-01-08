/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.model.table;

import is.codion.common.db.Operator;
import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;
import is.codion.common.event.EventObserver;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.common.value.ValueSet;

import java.text.Format;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * A default ColumnConditionModel model implementation.
 * @param <R> the type of the rows
 * @param <K> the type of the column identifier
 * @param <T> the column value type
 */
public class DefaultColumnConditionModel<R, K, T> implements ColumnConditionModel<R, K, T> {

  private final ValueSet<T> equalValues = Value.valueSet();
  private final Value<T> upperBoundValue = Value.value();
  private final Value<T> lowerBoundValue = Value.value();
  private final Value<Operator> operatorValue = Value.value(Operator.EQUAL);
  private final Event<?> conditionChangedEvent = Event.event();
  private final Event<?> conditionModelClearedEvent = Event.event();

  private final State enabledState = State.state();
  private final State lockedState = State.state();

  private final K columnIdentifier;
  private final Class<T> typeClass;
  private final Format format;
  private final String dateTimeFormatPattern;

  private Function<R, Comparable<T>> comparableFunction = value -> (Comparable<T>) value;
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
  public DefaultColumnConditionModel(final K columnIdentifier, final Class<T> typeClass, final String wildcard) {
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
  public DefaultColumnConditionModel(final K columnIdentifier, final Class<T> typeClass, final String wildcard,
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
  public DefaultColumnConditionModel(final K columnIdentifier, final Class<T> typeClass, final String wildcard,
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
  public final String getDateTimeFormatPattern() {
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
  public final Class<T> getTypeClass() {
    return typeClass;
  }

  @Override
  public final void setComparableFunction(final Function<R, Comparable<T>> comparableFunction) {
    this.comparableFunction = requireNonNull(comparableFunction);
  }

  @Override
  public final void setEqualValue(final T value) {
    equalValues.set(value == null ? Collections.emptySet() : Collections.singleton(value));
  }

  @Override
  public final T getEqualValue() {
    return getBoundValue(equalValues.get().isEmpty() ? null : equalValues.get().iterator().next());
  }

  @Override
  public final void setEqualValues(final Collection<T> values) {
    equalValues.set(values == null ? Collections.emptySet() : new HashSet<>(values));
  }

  @Override
  public final Collection<T> getEqualValues() {
    return equalValues.get().stream().map(this::getBoundValue).collect(Collectors.toList());
  }

  @Override
  public final void setUpperBound(final T value) {
    validateType(value);
    checkLock();
    upperBoundValue.set(value);
  }

  @Override
  public final T getUpperBound() {
    return getBoundValue(upperBoundValue.get());
  }

  @Override
  public final void setLowerBound(final T value) {
    validateType(value);
    checkLock();
    lowerBoundValue.set(value);
  }

  @Override
  public final T getLowerBound() {
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
    setEqualValues(null);
    setUpperBound(null);
    setLowerBound(null);
    setOperator(Operator.EQUAL);
    conditionModelClearedEvent.onEvent();
  }

  @Override
  public final StateObserver getLockedObserver() {
    return lockedState.getObserver();
  }

  @Override
  public final ValueSet<T> getEqualValueSet() {
    return equalValues;
  }

  @Override
  public final Value<T> getLowerBoundValue() {
    return lowerBoundValue;
  }

  @Override
  public final Value<T> getUpperBoundValue() {
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
  public final void addEqualsValueListener(final EventListener listener) {
    equalValues.addListener(listener);
  }

  @Override
  public final void removeEqualsValueListener(final EventListener listener) {
    equalValues.removeListener(listener);
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
    return !enabledState.get() || include(comparableFunction.apply(row));
  }

  @Override
  public final boolean include(final Comparable<T> comparable) {
    if (!enabledState.get()) {
      return true;
    }

    switch (operatorValue.get()) {
      case EQUAL:
        return includeEqual(comparable);
      case NOT_EQUAL:
        return includeNotEqual(comparable);
      case LESS_THAN:
        return includeLessThan(comparable);
      case LESS_THAN_OR_EQUAL:
        return includeLessThanOrEqual(comparable);
      case GREATER_THAN:
        return includeGreaterThan(comparable);
      case GREATER_THAN_OR_EQUAL:
        return includeGreaterThanOrEqual(comparable);
      case BETWEEN_EXCLUSIVE:
        return includeBetweenExclusive(comparable);
      case BETWEEN:
        return includeBetweenInclusive(comparable);
      case NOT_BETWEEN_EXCLUSIVE:
        return includeNotBetweenExclusive(comparable);
      case NOT_BETWEEN:
        return includeNotBetween(comparable);
      default:
        throw new IllegalArgumentException("Undefined operator: " + operatorValue.get());
    }
  }

  private T getBoundValue(final Object bound) {
    if (typeClass.equals(String.class)) {
      if (bound == null || (bound instanceof String && ((String) bound).isEmpty())) {
        return null;
      }
      if (bound instanceof Collection) {
        return (T) bound;
      }

      return (T) addWildcard((String) bound);
    }

    return (T) bound;
  }

  private boolean includeEqual(final Comparable<T> comparable) {
    if (comparable == null) {
      return this.getEqualValue() == null;
    }
    if (this.getEqualValue() == null) {
      return comparable == null;
    }

    if (comparable instanceof String) {//for Entity and String values
      return includeExactWildcard((String) comparable);
    }

    return comparable.compareTo(this.getEqualValue()) == 0;
  }

  private boolean includeNotEqual(final Comparable<T> comparable) {
    if (comparable == null) {
      return this.getEqualValue() != null;
    }
    if (this.getEqualValue() == null) {
      return comparable != null;
    }

    if (comparable instanceof String && ((String) comparable).contains(wildcard)) {
      return !includeExactWildcard((String) comparable);
    }

    return comparable.compareTo(this.getEqualValue()) != 0;
  }

  private boolean includeExactWildcard(final String value) {
    String equalsValue = (String) getEqualValue();
    if (equalsValue == null) {
      equalsValue = "";
    }
    if (equalsValue.equals(wildcard)) {
      return true;
    }
    if (value == null) {
      return false;
    }

    String realValue = value;
    if (!caseSensitive) {
      equalsValue = equalsValue.toUpperCase(Locale.getDefault());
      realValue = realValue.toUpperCase(Locale.getDefault());
    }

    if (!equalsValue.contains(wildcard)) {
      return realValue.compareTo(equalsValue) == 0;
    }

    return Pattern.matches(prepareForRegex(equalsValue), realValue);
  }

  private String prepareForRegex(final String string) {
    //a somewhat dirty fix to get rid of the '$' sign from the pattern, since it interferes with the regular expression parsing
    return string.replace(wildcard, ".*").replace("\\$", ".").replace("]", "\\\\]").replace("\\[", "\\\\[");
  }

  private boolean includeLessThan(final Comparable<T> comparable) {
    return getUpperBound() == null || comparable != null && comparable.compareTo(getUpperBound()) < 0;
  }

  private boolean includeLessThanOrEqual(final Comparable<T> comparable) {
    return getUpperBound() == null || comparable != null && comparable.compareTo(getUpperBound()) <= 0;
  }

  private boolean includeGreaterThan(final Comparable<T> comparable) {
    return getLowerBound() == null || comparable != null && comparable.compareTo(getLowerBound()) > 0;
  }

  private boolean includeGreaterThanOrEqual(final Comparable<T> comparable) {
    return getLowerBound() == null || comparable != null && comparable.compareTo(getLowerBound()) >= 0;
  }

  private boolean includeBetweenExclusive(final Comparable<T> comparable) {
    if (getLowerBound() == null && getUpperBound() == null) {
      return true;
    }

    if (comparable == null) {
      return false;
    }

    if (getLowerBound() == null) {
      return comparable.compareTo(getUpperBound()) < 0;
    }

    if (getUpperBound() == null) {
      return comparable.compareTo(getLowerBound()) > 0;
    }

    final int lowerCompareResult = comparable.compareTo(getLowerBound());
    final int upperCompareResult = comparable.compareTo(getUpperBound());

    return lowerCompareResult > 0 && upperCompareResult < 0;
  }

  private boolean includeBetweenInclusive(final Comparable<T> comparable) {
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

  private boolean includeNotBetweenExclusive(final Comparable<T> comparable) {
    if (getLowerBound() == null && getUpperBound() == null) {
      return true;
    }

    if (comparable == null) {
      return false;
    }

    if (getLowerBound() == null) {
      return comparable.compareTo(getUpperBound()) > 0;
    }

    if (getUpperBound() == null) {
      return comparable.compareTo(getLowerBound()) < 0;
    }

    final int lowerCompareResult = comparable.compareTo(getLowerBound());
    final int upperCompareResult = comparable.compareTo(getUpperBound());

    return lowerCompareResult < 0 || upperCompareResult > 0;
  }

  private boolean includeNotBetween(final Comparable<T> comparable) {
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
    //only use wildcard for EQUAL and NOT_EQUAL
    if (operatorValue.is(Operator.EQUAL) || operatorValue.is(Operator.NOT_EQUAL)) {
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
    final EventListener autoEnableListener = new AutoEnableListener();
    equalValues.addListener(autoEnableListener);
    upperBoundValue.addListener(autoEnableListener);
    lowerBoundValue.addListener(autoEnableListener);
    equalValues.addListener(conditionChangedEvent);
    upperBoundValue.addListener(conditionChangedEvent);
    lowerBoundValue.addListener(conditionChangedEvent);
    operatorValue.addListener(conditionChangedEvent);
    enabledState.addListener(conditionChangedEvent);
  }

  private void checkLock() {
    if (lockedState.get()) {
      throw new IllegalStateException("Condition model for column identified by " + columnIdentifier + " is locked");
    }
  }

  private void validateType(final Object value) {
    if (value != null) {
      if (value instanceof Collection) {
        for (final Object collValue : ((Collection<Object>) value)) {
          validateType(collValue);
        }
      }
      else if (!typeClass.isAssignableFrom(value.getClass())) {
        throw new IllegalArgumentException("Value of type " + typeClass + " expected for condition " + this + ", got: " + value.getClass());
      }
    }
  }

  private final class AutoEnableListener implements EventListener {

    @Override
    public void onEvent() {
      if (autoEnable) {
        if (operatorValue.is(Operator.EQUAL) || operatorValue.is(Operator.NOT_EQUAL)) {
          setEnabled(equalValues.isNotEmpty());
        }
        else {
          if (operatorValue.get().getValues().equals(Operator.Values.TWO)) {
            setEnabled(lowerBoundValue.isNotNull() && upperBoundValue.isNotNull());
          }
          else {
            setEnabled(upperBoundValue.isNotNull());
          }
        }
      }
    }
  }
}
