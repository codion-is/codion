/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.model.table;

import is.codion.common.Operator;
import is.codion.common.Text;
import is.codion.common.event.Event;
import is.codion.common.format.LocaleDateTimePattern;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.common.value.Value.Notify;
import is.codion.common.value.ValueSet;

import java.text.Format;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

final class DefaultColumnConditionModel<C, T> implements ColumnConditionModel<C, T> {

  private final ValueSet<T> equalValues = ValueSet.valueSet(Notify.WHEN_SET);
  private final Value<T> equalValue = equalValues.value();
  private final Value<T> upperBoundValue = Value.value(Notify.WHEN_SET);
  private final Value<T> lowerBoundValue = Value.value(Notify.WHEN_SET);
  private final Value<Operator> operator = Value.value(Operator.EQUAL, Operator.EQUAL);
  private final Event<?> conditionChangedEvent = Event.event();

  private final State caseSensitive;
  private final Value<AutomaticWildcard> automaticWildcard;
  private final char wildcard;

  private final State autoEnable;
  private final State enabled = State.state();
  private final State locked = State.state();

  private final C columnIdentifier;
  private final Class<T> columnClass;
  private final Format format;
  private final String dateTimePattern;
  private final List<Operator> operators;

  private DefaultColumnConditionModel(DefaultBuilder<C, T> builder) {
    this.columnIdentifier = builder.columnIdentifier;
    this.operators = unmodifiableList(builder.operators);
    this.columnClass = builder.columnClass;
    this.wildcard = builder.wildcard;
    this.format = builder.format;
    this.dateTimePattern = builder.dateTimePattern;
    this.automaticWildcard = Value.value(builder.automaticWildcard, AutomaticWildcard.NONE);
    this.caseSensitive = State.state(builder.caseSensitive);
    this.autoEnable = State.state(builder.autoEnable);
    this.enabled.addValidator(value -> checkLock());
    this.equalValues.addValidator(value -> checkLock());
    this.upperBoundValue.addValidator(value -> checkLock());
    this.lowerBoundValue.addValidator(value -> checkLock());
    this.operator.addValidator(this::validateOperator);
    this.operator.addValidator(value -> checkLock());
    bindEvents();
  }

  @Override
  public C columnIdentifier() {
    return columnIdentifier;
  }

  @Override
  public State caseSensitive() {
    return caseSensitive;
  }

  @Override
  public Format format() {
    return format;
  }

  @Override
  public String dateTimePattern() {
    return dateTimePattern;
  }

  @Override
  public State locked() {
    return locked;
  }

  @Override
  public Class<T> columnClass() {
    return columnClass;
  }

  @Override
  public void setEqualValue(T value) {
    equalValue.set(value);
  }

  @Override
  public T getEqualValue() {
    return addAutomaticWildcard(equalValue.get());
  }

  @Override
  public void setEqualValues(Collection<T> values) {
    equalValues.set(values == null ? Collections.emptySet() : new HashSet<>(values));
  }

  @Override
  public Collection<T> getEqualValues() {
    return equalValues.get().stream()
            .map(this::addAutomaticWildcard)
            .collect(toList());
  }

  @Override
  public void setUpperBound(T value) {
    upperBoundValue.set(value);
  }

  @Override
  public T getUpperBound() {
    return upperBoundValue.get();
  }

  @Override
  public void setLowerBound(T value) {
    lowerBoundValue.set(value);
  }

  @Override
  public T getLowerBound() {
    return lowerBoundValue.get();
  }

  @Override
  public Value<Operator> operator() {
    return operator;
  }

  @Override
  public List<Operator> operators() {
    return operators;
  }

  @Override
  public char wildcard() {
    return wildcard;
  }

  @Override
  public State enabled() {
    return enabled;
  }

  @Override
  public Value<AutomaticWildcard> automaticWildcard() {
    return automaticWildcard;
  }

  @Override
  public State autoEnable() {
    return autoEnable;
  }

  @Override
  public void clear() {
    enabled.set(false);
    setEqualValues(null);
    setUpperBound(null);
    setLowerBound(null);
    operator.set(Operator.EQUAL);
  }

  @Override
  public boolean accepts(Comparable<T> columnValue) {
    return valueAccepted(columnValue);
  }

  @Override
  public ValueSet<T> equalValues() {
    return equalValues;
  }

  @Override
  public Value<T> lowerBoundValue() {
    return lowerBoundValue;
  }

  @Override
  public Value<T> upperBoundValue() {
    return upperBoundValue;
  }

  @Override
  public void addChangeListener(Runnable listener) {
    conditionChangedEvent.addListener(listener);
  }

  @Override
  public void removeChangeListener(Runnable listener) {
    conditionChangedEvent.removeListener(listener);
  }

  private boolean valueAccepted(Comparable<T> comparable) {
    switch (operator.get()) {
      case EQUAL:
        return isEqual(comparable);
      case NOT_EQUAL:
        return isNotEqual(comparable);
      case LESS_THAN:
        return isLessThan(comparable);
      case LESS_THAN_OR_EQUAL:
        return isLessThanOrEqual(comparable);
      case GREATER_THAN:
        return isGreaterThan(comparable);
      case GREATER_THAN_OR_EQUAL:
        return isGreaterThanOrEqual(comparable);
      case BETWEEN_EXCLUSIVE:
        return isBetweenExclusive(comparable);
      case BETWEEN:
        return isBetween(comparable);
      case NOT_BETWEEN_EXCLUSIVE:
        return isNotBetweenExclusive(comparable);
      case NOT_BETWEEN:
        return isNotBetween(comparable);
      default:
        throw new IllegalArgumentException("Undefined operator: " + operator.get());
    }
  }

  private boolean isEqual(Comparable<T> comparable) {
    T equalValue = getEqualValue();
    if (comparable == null) {
      return equalValue == null;
    }
    if (equalValue == null) {
      return comparable == null;
    }

    if (comparable instanceof String && ((String) equalValue).contains(String.valueOf(wildcard))) {
      return isEqualWildcard((String) comparable);
    }

    return comparable.compareTo(equalValue) == 0;
  }

  private boolean isNotEqual(Comparable<T> comparable) {
    T equalValue = getEqualValue();
    if (comparable == null) {
      return equalValue != null;
    }
    if (equalValue == null) {
      return comparable != null;
    }

    if (comparable instanceof String && ((String) equalValue).contains(String.valueOf(wildcard))) {
      return !isEqualWildcard((String) comparable);
    }

    return comparable.compareTo(equalValue) != 0;
  }

  private boolean isEqualWildcard(String value) {
    String equalValue = (String) getEqualValue();
    if (equalValue == null) {
      equalValue = "";
    }
    if (equalValue.equals(String.valueOf(wildcard))) {
      return true;
    }

    String valueToTest = value;
    if (!caseSensitive.get()) {
      equalValue = equalValue.toUpperCase();
      valueToTest = valueToTest.toUpperCase();
    }

    if (!equalValue.contains(String.valueOf(wildcard))) {
      return valueToTest.compareTo(equalValue) == 0;
    }

    return Pattern.matches(prepareForRegex(equalValue), valueToTest);
  }

  private String prepareForRegex(String string) {
    //a somewhat dirty fix to get rid of the '$' sign from the pattern, since it interferes with the regular expression parsing
    return string.replace(String.valueOf(wildcard), ".*").replace("\\$", ".").replace("]", "\\\\]").replace("\\[", "\\\\[");
  }

  private boolean isLessThan(Comparable<T> comparable) {
    T upperBound = getUpperBound();

    return upperBound == null || comparable != null && comparable.compareTo(upperBound) < 0;
  }

  private boolean isLessThanOrEqual(Comparable<T> comparable) {
    T upperBound = getUpperBound();

    return upperBound == null || comparable != null && comparable.compareTo(upperBound) <= 0;
  }

  private boolean isGreaterThan(Comparable<T> comparable) {
    T lowerBound = getLowerBound();

    return lowerBound == null || comparable != null && comparable.compareTo(lowerBound) > 0;
  }

  private boolean isGreaterThanOrEqual(Comparable<T> comparable) {
    T lowerBound = getLowerBound();

    return lowerBound == null || comparable != null && comparable.compareTo(lowerBound) >= 0;
  }

  private boolean isBetweenExclusive(Comparable<T> comparable) {
    T lowerBound = getLowerBound();
    T upperBound = getUpperBound();
    if (lowerBound == null && upperBound == null) {
      return true;
    }

    if (comparable == null) {
      return false;
    }

    if (lowerBound == null) {
      return comparable.compareTo(upperBound) < 0;
    }

    if (upperBound == null) {
      return comparable.compareTo(lowerBound) > 0;
    }

    int lowerCompareResult = comparable.compareTo(lowerBound);
    int upperCompareResult = comparable.compareTo(upperBound);

    return lowerCompareResult > 0 && upperCompareResult < 0;
  }

  private boolean isBetween(Comparable<T> comparable) {
    T lowerBound = getLowerBound();
    T upperBound = getUpperBound();
    if (lowerBound == null && upperBound == null) {
      return true;
    }

    if (comparable == null) {
      return false;
    }

    if (lowerBound == null) {
      return comparable.compareTo(upperBound) <= 0;
    }

    if (upperBound == null) {
      return comparable.compareTo(lowerBound) >= 0;
    }

    int lowerCompareResult = comparable.compareTo(lowerBound);
    int upperCompareResult = comparable.compareTo(upperBound);

    return lowerCompareResult >= 0 && upperCompareResult <= 0;
  }

  private boolean isNotBetweenExclusive(Comparable<T> comparable) {
    T lowerBound = getLowerBound();
    T upperBound = getUpperBound();
    if (lowerBound == null && upperBound == null) {
      return true;
    }

    if (comparable == null) {
      return false;
    }

    if (lowerBound == null) {
      return comparable.compareTo(upperBound) > 0;
    }

    if (upperBound == null) {
      return comparable.compareTo(lowerBound) < 0;
    }

    int lowerCompareResult = comparable.compareTo(lowerBound);
    int upperCompareResult = comparable.compareTo(upperBound);

    return lowerCompareResult < 0 || upperCompareResult > 0;
  }

  private boolean isNotBetween(Comparable<T> comparable) {
    T lowerBound = getLowerBound();
    T upperBound = getUpperBound();
    if (lowerBound == null && upperBound == null) {
      return true;
    }

    if (comparable == null) {
      return false;
    }

    if (lowerBound == null) {
      return comparable.compareTo(upperBound) >= 0;
    }

    if (upperBound == null) {
      return comparable.compareTo(lowerBound) <= 0;
    }

    int lowerCompareResult = comparable.compareTo(lowerBound);
    int upperCompareResult = comparable.compareTo(upperBound);

    return lowerCompareResult <= 0 || upperCompareResult >= 0;
  }

  private T addAutomaticWildcard(T bound) {
    if (!(bound instanceof String)) {
      return bound;
    }
    switch (operator.get()) {
      //wildcard only used for EQUAL and NOT_EQUAL
      case EQUAL:
      case NOT_EQUAL:
        return (T) addAutomaticWildcard((String) bound);
      default:
        return bound;
    }
  }

  private String addAutomaticWildcard(String value) {
    switch (automaticWildcard.get()) {
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

  private void bindEvents() {
    Runnable autoEnableListener = new AutoEnableListener();
    equalValues.addListener(autoEnableListener);
    upperBoundValue.addListener(autoEnableListener);
    lowerBoundValue.addListener(autoEnableListener);
    operator.addListener(autoEnableListener);
    autoEnable.addListener(autoEnableListener);
    equalValues.addListener(conditionChangedEvent);
    upperBoundValue.addListener(conditionChangedEvent);
    lowerBoundValue.addListener(conditionChangedEvent);
    operator.addListener(conditionChangedEvent);
    enabled.addListener(conditionChangedEvent);
    caseSensitive.addListener(conditionChangedEvent);
    automaticWildcard.addListener(conditionChangedEvent);
  }

  private void checkLock() {
    if (locked.get()) {
      throw new IllegalStateException("Condition model for column identified by " + columnIdentifier + " is locked");
    }
  }

  private void validateOperator(Operator operator) {
    if (!operators.contains(requireNonNull(operator, "operator"))) {
      throw new IllegalArgumentException("Operator " + operator + " not available in this condition model");
    }
  }

  private final class AutoEnableListener implements Runnable {

    @Override
    public void run() {
      if (autoEnable.get()) {
        switch (operator.get()) {
          case EQUAL:
          case NOT_EQUAL:
            enabled.set(equalValues.notEmpty());
            break;
          case LESS_THAN:
          case LESS_THAN_OR_EQUAL:
            enabled.set(upperBoundValue.isNotNull());
            break;
          case GREATER_THAN:
          case GREATER_THAN_OR_EQUAL:
            enabled.set(lowerBoundValue.isNotNull());
            break;
          case BETWEEN:
          case BETWEEN_EXCLUSIVE:
          case NOT_BETWEEN:
          case NOT_BETWEEN_EXCLUSIVE:
            enabled.set(lowerBoundValue.isNotNull() && upperBoundValue.isNotNull());
            break;
          default:
            throw new IllegalStateException("Unknown operator: " + operator.get());
        }
      }
    }
  }

  static final class DefaultBuilder<C, T> implements Builder<C, T> {

    private final C columnIdentifier;
    private final Class<T> columnClass;

    private List<Operator> operators = Arrays.asList(Operator.values());
    private char wildcard = Text.WILDCARD_CHARACTER.get();
    private Format format;
    private String dateTimePattern = LocaleDateTimePattern.builder()
          .delimiterDash()
          .yearFourDigits()
          .hoursMinutesSeconds()
          .build()
          .dateTimePattern();
    private AutomaticWildcard automaticWildcard = ColumnConditionModel.AUTOMATIC_WILDCARD.get();
    private boolean caseSensitive = CASE_SENSITIVE.get();
    private boolean autoEnable = true;

    DefaultBuilder(C columnIdentifier, Class<T> columnClass) {
      this.columnIdentifier = requireNonNull(columnIdentifier);
      this.columnClass = requireNonNull(columnClass);
    }

    @Override
    public Builder<C, T> operators(List<Operator> operators) {
      if (requireNonNull(operators).isEmpty()) {
        throw new IllegalArgumentException("One or more operators must be specified");
      }

      this.operators = operators;
      return this;
    }

    @Override
    public Builder<C, T> wildcard(char wildcard) {
      this.wildcard = wildcard;
      return this;
    }

    @Override
    public Builder<C, T> format(Format format) {
      this.format = format;
      return this;
    }

    @Override
    public Builder<C, T> dateTimePattern(String dateTimePattern) {
      this.dateTimePattern = dateTimePattern;
      return this;
    }

    @Override
    public Builder<C, T> automaticWildcard(AutomaticWildcard automaticWildcard) {
      this.automaticWildcard = requireNonNull(automaticWildcard);
      return this;
    }

    @Override
    public Builder<C, T> caseSensitive(boolean caseSensitive) {
      this.caseSensitive = caseSensitive;
      return this;
    }

    @Override
    public Builder<C, T> autoEnable(boolean autoEnable) {
      this.autoEnable = autoEnable;
      return this;
    }

    @Override
    public ColumnConditionModel<C, T> build() {
      return new DefaultColumnConditionModel<>(this);
    }
  }
}
