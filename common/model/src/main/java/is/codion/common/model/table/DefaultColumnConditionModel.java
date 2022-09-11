/*
 * Copyright (c) 2008 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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

  private final ValueSet<T> equalValues = ValueSet.valueSet();
  private final Value<T> upperBoundValue = Value.value();
  private final Value<T> lowerBoundValue = Value.value();
  private final Value<Operator> operatorValue = Value.value(Operator.EQUAL);
  private final Event<?> conditionChangedEvent = Event.event();
  private final Event<?> conditionModelClearedEvent = Event.event();

  private final State caseSensitiveState = State.state(CASE_SENSITIVE.get());
  private final Value<AutomaticWildcard> automaticWildcardValue = Value.value(AUTOMATIC_WILDCARD.get(), AutomaticWildcard.NONE);
  private final Value<Character> wildcardValue = Value.value('%', '%');

  private final State autoEnableState = State.state(true);
  private final State enabledState = State.state();
  private final State lockedState = State.state();

  private final C columnIdentifier;
  private final Class<T> columnClass;
  private final Format format;
  private final String dateTimePattern;
  private final List<Operator> operators;

  /**
   * Instantiates a DefaultColumnConditionModel.
   * @param columnIdentifier the column identifier
   * @param columnClass the column class
   * @param operators the conditional operators available to this condition model
   * @param wildcard the character to use as wildcard
   */
  public DefaultColumnConditionModel(C columnIdentifier, Class<T> columnClass, List<Operator> operators,
                                     char wildcard) {
    this(columnIdentifier, columnClass, operators, wildcard, null, null);
  }

  /**
   * Instantiates a DefaultColumnConditionModel.
   * @param columnIdentifier the column identifier
   * @param columnClass the column class
   * @param operators the conditional operators available to this condition model
   * @param wildcard the character to use as wildcard
   * @param format the format to use when presenting the values, numbers for example
   * @param dateTimePattern the date/time format pattern to use in case of a date/time column
   */
  public DefaultColumnConditionModel(C columnIdentifier, Class<T> columnClass, List<Operator> operators,
                                     char wildcard, Format format, String dateTimePattern) {
    this(columnIdentifier, columnClass, operators, wildcard, format, dateTimePattern, AUTOMATIC_WILDCARD.get());
  }

  /**
   * Instantiates a DefaultColumnConditionModel.
   * @param columnIdentifier the column identifier
   * @param columnClass the column class
   * @param operators the conditional operators available to this condition model
   * @param wildcard the character to use as wildcard
   * @param format the format to use when presenting the values, numbers for example
   * @param dateTimePattern the date/time format pattern to use in case of a date/time column
   * @param automaticWildcard the automatic wildcard type to use
   */
  public DefaultColumnConditionModel(C columnIdentifier, Class<T> columnClass, List<Operator> operators,
                                     char wildcard, Format format, String dateTimePattern,
                                     AutomaticWildcard automaticWildcard) {
    if (requireNonNull(operators, "operators").isEmpty()) {
      throw new IllegalArgumentException("One or more operators must be specified");
    }
    this.columnIdentifier = requireNonNull(columnIdentifier, "columnIdentifier");
    this.operators = unmodifiableList(operators);
    this.columnClass = columnClass;
    this.wildcardValue.set(wildcard);
    this.format = format;
    this.dateTimePattern = dateTimePattern;
    this.automaticWildcardValue.set(automaticWildcard);
    this.enabledState.addValidator(value -> checkLock());
    this.equalValues.addValidator(value -> checkLock());
    this.upperBoundValue.addValidator(value -> checkLock());
    this.lowerBoundValue.addValidator(value -> checkLock());
    this.operatorValue.addValidator(this::validateOperator);
    this.operatorValue.addValidator(value -> checkLock());
    bindEvents();
  }

  @Override
  public final C columnIdentifier() {
    return columnIdentifier;
  }

  @Override
  public final State caseSensitiveState() {
    return caseSensitiveState;
  }

  @Override
  public final Format format() {
    return format;
  }

  @Override
  public final String dateTimePattern() {
    return dateTimePattern;
  }

  @Override
  public final void setLocked(boolean locked) {
    lockedState.set(locked);
  }

  @Override
  public final boolean isLocked() {
    return lockedState.get();
  }

  @Override
  public final Class<T> columnClass() {
    return columnClass;
  }

  @Override
  public final void setEqualValue(T value) {
    equalValues.set(value == null ? Collections.emptySet() : Collections.singleton(value));
  }

  @Override
  public final T getEqualValue() {
    return boundValue(equalValues.get().isEmpty() ? null : equalValues.get().iterator().next());
  }

  @Override
  public final void setEqualValues(Collection<T> values) {
    equalValues.set(values == null ? Collections.emptySet() : new HashSet<>(values));
  }

  @Override
  public final Collection<T> getEqualValues() {
    return equalValues.get().stream()
            .map(this::boundValue)
            .collect(toList());
  }

  @Override
  public final void setUpperBound(T value) {
    upperBoundValue.set(value);
  }

  @Override
  public final T getUpperBound() {
    return boundValue(upperBoundValue.get());
  }

  @Override
  public final void setLowerBound(T value) {
    lowerBoundValue.set(value);
  }

  @Override
  public final T getLowerBound() {
    return boundValue(lowerBoundValue.get());
  }

  @Override
  public final Operator getOperator() {
    return operatorValue.get();
  }

  @Override
  public final void setOperator(Operator operator) {
    validateOperator(operator);
    operatorValue.set(operator);
  }

  @Override
  public final List<Operator> operators() {
    return operators;
  }

  @Override
  public final Value<Character> wildcardValue() {
    return wildcardValue;
  }

  @Override
  public final boolean isEnabled() {
    return enabledState.get();
  }

  @Override
  public final void setEnabled(boolean enabled) {
    enabledState.set(enabled);
  }

  @Override
  public final Value<AutomaticWildcard> automaticWildcardValue() {
    return automaticWildcardValue;
  }

  @Override
  public final State autoEnableState() {
    return autoEnableState;
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
  public final StateObserver lockedObserver() {
    return lockedState.observer();
  }

  @Override
  public final ValueSet<T> equalValueSet() {
    return equalValues;
  }

  @Override
  public final Value<T> lowerBoundValue() {
    return lowerBoundValue;
  }

  @Override
  public final Value<T> upperBoundValue() {
    return upperBoundValue;
  }

  @Override
  public final State enabledState() {
    return enabledState;
  }

  @Override
  public final void addEnabledListener(EventListener listener) {
    enabledState.addListener(listener);
  }

  @Override
  public final void removeEnabledListener(EventListener listener) {
    enabledState.removeListener(listener);
  }

  @Override
  public final void addEqualsValueListener(EventListener listener) {
    equalValues.addListener(listener);
  }

  @Override
  public final void removeEqualsValueListener(EventListener listener) {
    equalValues.removeListener(listener);
  }

  @Override
  public final void addUpperBoundListener(EventListener listener) {
    upperBoundValue.addListener(listener);
  }

  @Override
  public final void removeUpperBoundListener(EventListener listener) {
    upperBoundValue.removeListener(listener);
  }

  @Override
  public final void addLowerBoundListener(EventListener listener) {
    lowerBoundValue.addListener(listener);
  }

  @Override
  public final void removeLowerBoundListener(EventListener listener) {
    lowerBoundValue.removeListener(listener);
  }

  @Override
  public final void addClearedListener(EventListener listener) {
    conditionModelClearedEvent.addListener(listener);
  }

  @Override
  public final void removeClearedListener(EventListener listener) {
    conditionModelClearedEvent.removeListener(listener);
  }

  @Override
  public final void addConditionChangedListener(EventListener listener) {
    conditionChangedEvent.addListener(listener);
  }

  @Override
  public final void removeConditionChangedListener(EventListener listener) {
    conditionChangedEvent.removeListener(listener);
  }

  @Override
  public final void addOperatorListener(EventDataListener<Operator> listener) {
    operatorValue.addDataListener(listener);
  }

  @Override
  public final void removeOperatorListener(EventDataListener<Operator> listener) {
    operatorValue.removeDataListener(listener);
  }

  @Override
  public final Value<Operator> operatorValue() {
    return operatorValue;
  }

  private T boundValue(Object bound) {
    if (columnClass.equals(String.class)) {
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

  private String addWildcard(String value) {
    //only use wildcard for EQUAL and NOT_EQUAL
    if (operatorValue.equalTo(Operator.EQUAL) || operatorValue.equalTo(Operator.NOT_EQUAL)) {
      switch (automaticWildcardValue.get()) {
        case PREFIX_AND_POSTFIX:
          return wildcardValue.get() + value + wildcardValue.get();
        case PREFIX:
          return wildcardValue.get() + value;
        case POSTFIX:
          return value + wildcardValue.get();
        default:
          return value;
      }
    }

    return value;
  }

  private void bindEvents() {
    EventListener autoEnableListener = new AutoEnableListener();
    equalValues.addListener(autoEnableListener);
    upperBoundValue.addListener(autoEnableListener);
    lowerBoundValue.addListener(autoEnableListener);
    operatorValue.addListener(autoEnableListener);
    autoEnableState.addListener(autoEnableListener);
    equalValues.addListener(conditionChangedEvent);
    upperBoundValue.addListener(conditionChangedEvent);
    lowerBoundValue.addListener(conditionChangedEvent);
    operatorValue.addListener(conditionChangedEvent);
    enabledState.addListener(conditionChangedEvent);
    caseSensitiveState.addListener(conditionChangedEvent);
    automaticWildcardValue.addListener(conditionChangedEvent);
  }

  private void checkLock() {
    if (lockedState.get()) {
      throw new IllegalStateException("Condition model for column identified by " + columnIdentifier + " is locked");
    }
  }

  private void validateOperator(Operator operator) {
    if (!operators.contains(requireNonNull(operator, "operator"))) {
      throw new IllegalArgumentException("Operator " + operator + " not available in this condition model");
    }
  }

  private final class AutoEnableListener implements EventListener {

    @Override
    public void onEvent() {
      if (autoEnableState.get()) {
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
