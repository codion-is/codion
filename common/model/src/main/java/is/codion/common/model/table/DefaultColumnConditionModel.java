/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.model.table;

import is.codion.common.Operator;
import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.common.value.ValueSet;

import java.text.Format;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * A default ColumnConditionModel model implementation.
 * @param <C> the type of the column identifier
 * @param <T> the column value type
 */
public class DefaultColumnConditionModel<C, T> implements ColumnConditionModel<C, T> {

  private final ValueSet<T> equalValues = Value.valueSet();
  private final Value<T> upperBoundValue = Value.value();
  private final Value<T> lowerBoundValue = Value.value();
  private final Value<Operator> operatorValue = Value.value(Operator.EQUAL);
  private final Event<?> conditionChangedEvent = Event.event();
  private final Event<?> conditionModelClearedEvent = Event.event();

  private final State enabledState = State.state();
  private final State lockedState = State.state();

  private final C columnIdentifier;
  private final Class<T> typeClass;
  private final Format format;
  private final String dateTimePattern;
  private final List<Operator> operators;

  private boolean autoEnable = true;
  private AutomaticWildcard automaticWildcard;
  private boolean caseSensitive = CASE_SENSITIVE.get();
  private String wildcard;

  /**
   * Instantiates a DefaultColumnConditionModel.
   * @param columnIdentifier the column identifier
   * @param typeClass the data type
   * @param operators the conditional operators available to this condition model
   * @param wildcard the string to use as wildcard
   */
  public DefaultColumnConditionModel(final C columnIdentifier, final Class<T> typeClass, final List<Operator> operators,
                                     final String wildcard) {
    this(columnIdentifier, typeClass, operators, wildcard, null, null);
  }

  /**
   * Instantiates a DefaultColumnConditionModel.
   * @param columnIdentifier the column identifier
   * @param typeClass the data type
   * @param operators the conditional operators available to this condition model
   * @param wildcard the string to use as wildcard
   * @param format the format to use when presenting the values, numbers for example
   * @param dateTimePattern the date/time format pattern to use in case of a date/time column
   */
  public DefaultColumnConditionModel(final C columnIdentifier, final Class<T> typeClass, final List<Operator> operators,
                                     final String wildcard, final Format format, final String dateTimePattern) {
    this(columnIdentifier, typeClass, operators, wildcard, format, dateTimePattern, AUTOMATIC_WILDCARD.get());
  }

  /**
   * Instantiates a DefaultColumnConditionModel.
   * @param columnIdentifier the column identifier
   * @param typeClass the data type
   * @param operators the conditional operators available to this condition model
   * @param wildcard the string to use as wildcard
   * @param format the format to use when presenting the values, numbers for example
   * @param dateTimePattern the date/time format pattern to use in case of a date/time column
   * @param automaticWildcard the automatic wildcard type to use
   */
  public DefaultColumnConditionModel(final C columnIdentifier, final Class<T> typeClass, final List<Operator> operators,
                                     final String wildcard, final Format format, final String dateTimePattern,
                                     final AutomaticWildcard automaticWildcard) {
    if (requireNonNull(operators, "operators").isEmpty()) {
      throw new IllegalArgumentException("One or more operators must be specified");
    }
    this.columnIdentifier = requireNonNull(columnIdentifier, "columnIdentifier");
    this.operators = unmodifiableList(operators);
    this.typeClass = typeClass;
    this.wildcard = wildcard;
    this.format = format;
    this.dateTimePattern = dateTimePattern;
    this.automaticWildcard = automaticWildcard;
    this.enabledState.addValidator(value -> checkLock());
    this.equalValues.addValidator(value -> checkLock());
    this.upperBoundValue.addValidator(value -> checkLock());
    this.lowerBoundValue.addValidator(value -> checkLock());
    this.operatorValue.addValidator(this::validateOperator);
    this.operatorValue.addValidator(value -> checkLock());
    bindEvents();
  }

  @Override
  public final C getColumnIdentifier() {
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
  public final String getDateTimePattern() {
    return dateTimePattern;
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
    return equalValues.get().stream()
            .map(this::getBoundValue)
            .collect(toList());
  }

  @Override
  public final void setUpperBound(final T value) {
    upperBoundValue.set(value);
  }

  @Override
  public final T getUpperBound() {
    return getBoundValue(upperBoundValue.get());
  }

  @Override
  public final void setLowerBound(final T value) {
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
    validateOperator(operator);
    operatorValue.set(operator);
  }

  @Override
  public final void previousOperator() {
    operatorValue.set(operators.get(getPreviousOperatorIndex()));
  }

  @Override
  public final void nextOperator() {
    operatorValue.set(operators.get(getNextOperatorIndex()));
  }

  @Override
  public final List<Operator> getOperators() {
    return operators;
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
    enabledState.set(enabled);
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
  public final State getEnabledState() {
    return enabledState;
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
  public final Value<Operator> getOperatorValue() {
    return operatorValue;
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

  private String addWildcard(final String value) {
    //only use wildcard for EQUAL and NOT_EQUAL
    if (operatorValue.equalTo(Operator.EQUAL) || operatorValue.equalTo(Operator.NOT_EQUAL)) {
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

  private int getNextOperatorIndex() {
    final int currentIndex = operators.indexOf(operatorValue.get());

    return currentIndex == operators.size() - 1 ? 0 : currentIndex + 1;
  }

  private int getPreviousOperatorIndex() {
    final int currentIndex = operators.indexOf(operatorValue.get());

    return currentIndex == 0 ? operators.size() - 1 : currentIndex - 1;
  }

  private void bindEvents() {
    final EventListener autoEnableListener = new AutoEnableListener();
    equalValues.addListener(autoEnableListener);
    upperBoundValue.addListener(autoEnableListener);
    lowerBoundValue.addListener(autoEnableListener);
    operatorValue.addListener(autoEnableListener);
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

  private void validateOperator(final Operator operator) {
    if (!operators.contains(requireNonNull(operator, "operator"))) {
      throw new IllegalArgumentException("Operator " + operator + " not available in this condition model");
    }
  }

  private final class AutoEnableListener implements EventListener {

    @Override
    public void onEvent() {
      if (autoEnable) {
        switch (operatorValue.get()) {
          case EQUAL:
          case NOT_EQUAL:
            setEnabled(equalValues.isNotEmpty());
            break;
          case LESS_THAN:
          case LESS_THAN_OR_EQUAL:
            setEnabled(upperBoundValue.isNotNull());
            break;
          case GREATER_THAN:
          case GREATER_THAN_OR_EQUAL:
            setEnabled(lowerBoundValue.isNotNull());
            break;
          case BETWEEN:
          case BETWEEN_EXCLUSIVE:
          case NOT_BETWEEN:
          case NOT_BETWEEN_EXCLUSIVE:
            setEnabled(lowerBoundValue.isNotNull() && upperBoundValue.isNotNull());
            break;
          default:
            throw new IllegalStateException("Unknown operator: " + operatorValue.get());
        }
      }
    }
  }
}
