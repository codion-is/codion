/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.model.table;

import is.codion.common.Operator;
import is.codion.common.Text;
import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.common.value.ValueSet;

import java.text.Format;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

final class DefaultColumnConditionModel<C, T> implements ColumnConditionModel<C, T> {

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

  private DefaultColumnConditionModel(DefaultBuilder<C, T> builder) {
    this.columnIdentifier = builder.columnIdentifier;
    this.operators = unmodifiableList(builder.operators);
    this.columnClass = builder.columnClass;
    this.wildcardValue.set(builder.wildcard);
    this.format = builder.format;
    this.dateTimePattern = builder.dateTimePattern;
    this.automaticWildcardValue.set(builder.automaticWildcard);
    this.enabledState.addValidator(value -> checkLock());
    this.equalValues.addValidator(value -> checkLock());
    this.upperBoundValue.addValidator(value -> checkLock());
    this.lowerBoundValue.addValidator(value -> checkLock());
    this.operatorValue.addValidator(this::validateOperator);
    this.operatorValue.addValidator(value -> checkLock());
    bindEvents();
  }

  @Override
  public C columnIdentifier() {
    return columnIdentifier;
  }

  @Override
  public State caseSensitiveState() {
    return caseSensitiveState;
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
  public void setLocked(boolean locked) {
    lockedState.set(locked);
  }

  @Override
  public boolean isLocked() {
    return lockedState.get();
  }

  @Override
  public Class<T> columnClass() {
    return columnClass;
  }

  @Override
  public void setEqualValue(T value) {
    equalValues.set(value == null ? Collections.emptySet() : Collections.singleton(value));
  }

  @Override
  public T getEqualValue() {
    return boundValue(equalValues.get().isEmpty() ? null : equalValues.get().iterator().next());
  }

  @Override
  public void setEqualValues(Collection<T> values) {
    equalValues.set(values == null ? Collections.emptySet() : new HashSet<>(values));
  }

  @Override
  public Collection<T> getEqualValues() {
    return equalValues.get().stream()
            .map(this::boundValue)
            .collect(toList());
  }

  @Override
  public void setUpperBound(T value) {
    upperBoundValue.set(value);
  }

  @Override
  public T getUpperBound() {
    return boundValue(upperBoundValue.get());
  }

  @Override
  public void setLowerBound(T value) {
    lowerBoundValue.set(value);
  }

  @Override
  public T getLowerBound() {
    return boundValue(lowerBoundValue.get());
  }

  @Override
  public Operator getOperator() {
    return operatorValue.get();
  }

  @Override
  public void setOperator(Operator operator) {
    validateOperator(operator);
    operatorValue.set(operator);
  }

  @Override
  public List<Operator> operators() {
    return operators;
  }

  @Override
  public Value<Character> wildcardValue() {
    return wildcardValue;
  }

  @Override
  public boolean isEnabled() {
    return enabledState.get();
  }

  @Override
  public void setEnabled(boolean enabled) {
    enabledState.set(enabled);
  }

  @Override
  public Value<AutomaticWildcard> automaticWildcardValue() {
    return automaticWildcardValue;
  }

  @Override
  public State autoEnableState() {
    return autoEnableState;
  }

  @Override
  public void clearCondition() {
    setEnabled(false);
    setEqualValues(null);
    setUpperBound(null);
    setLowerBound(null);
    setOperator(Operator.EQUAL);
    conditionModelClearedEvent.onEvent();
  }

  @Override
  public boolean accepts(Comparable<T> columnValue) {
    return !enabledState.get() || include(columnValue);
  }

  @Override
  public StateObserver lockedObserver() {
    return lockedState.observer();
  }

  @Override
  public ValueSet<T> equalValueSet() {
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
  public State enabledState() {
    return enabledState;
  }

  @Override
  public void addEnabledListener(EventListener listener) {
    enabledState.addListener(listener);
  }

  @Override
  public void removeEnabledListener(EventListener listener) {
    enabledState.removeListener(listener);
  }

  @Override
  public void addEqualsValueListener(EventListener listener) {
    equalValues.addListener(listener);
  }

  @Override
  public void removeEqualsValueListener(EventListener listener) {
    equalValues.removeListener(listener);
  }

  @Override
  public void addUpperBoundListener(EventListener listener) {
    upperBoundValue.addListener(listener);
  }

  @Override
  public void removeUpperBoundListener(EventListener listener) {
    upperBoundValue.removeListener(listener);
  }

  @Override
  public void addLowerBoundListener(EventListener listener) {
    lowerBoundValue.addListener(listener);
  }

  @Override
  public void removeLowerBoundListener(EventListener listener) {
    lowerBoundValue.removeListener(listener);
  }

  @Override
  public void addClearedListener(EventListener listener) {
    conditionModelClearedEvent.addListener(listener);
  }

  @Override
  public void removeClearedListener(EventListener listener) {
    conditionModelClearedEvent.removeListener(listener);
  }

  @Override
  public void addConditionChangedListener(EventListener listener) {
    conditionChangedEvent.addListener(listener);
  }

  @Override
  public void removeConditionChangedListener(EventListener listener) {
    conditionChangedEvent.removeListener(listener);
  }

  @Override
  public void addOperatorListener(EventDataListener<Operator> listener) {
    operatorValue.addDataListener(listener);
  }

  @Override
  public void removeOperatorListener(EventDataListener<Operator> listener) {
    operatorValue.removeDataListener(listener);
  }

  @Override
  public Value<Operator> operatorValue() {
    return operatorValue;
  }

  private boolean include(Comparable<T> comparable) {
    switch (getOperator()) {
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
        throw new IllegalArgumentException("Undefined operator: " + getOperator());
    }
  }

  private boolean includeEqual(Comparable<T> comparable) {
    T equalValue = getEqualValue();
    if (comparable == null) {
      return equalValue == null;
    }
    if (equalValue == null) {
      return comparable == null;
    }

    if (comparable instanceof String && ((String) equalValue).contains(String.valueOf(wildcardValue().get()))) {
      return includeExactWildcard((String) comparable);
    }

    return comparable.compareTo(equalValue) == 0;
  }

  private boolean includeNotEqual(Comparable<T> comparable) {
    T equalValue = getEqualValue();
    if (comparable == null) {
      return equalValue != null;
    }
    if (equalValue == null) {
      return comparable != null;
    }

    if (comparable instanceof String && ((String) equalValue).contains(String.valueOf(wildcardValue().get()))) {
      return !includeExactWildcard((String) comparable);
    }

    return comparable.compareTo(equalValue) != 0;
  }

  private boolean includeExactWildcard(String value) {
    String equalsValue = (String) getEqualValue();
    if (equalsValue == null) {
      equalsValue = "";
    }
    if (equalsValue.equals(String.valueOf(wildcardValue().get()))) {
      return true;
    }

    String realValue = value;
    if (!caseSensitiveState().get()) {
      equalsValue = equalsValue.toUpperCase(Locale.getDefault());
      realValue = realValue.toUpperCase(Locale.getDefault());
    }

    if (!equalsValue.contains(String.valueOf(wildcardValue().get()))) {
      return realValue.compareTo(equalsValue) == 0;
    }

    return Pattern.matches(prepareForRegex(equalsValue), realValue);
  }

  private String prepareForRegex(String string) {
    //a somewhat dirty fix to get rid of the '$' sign from the pattern, since it interferes with the regular expression parsing
    return string.replace(String.valueOf(wildcardValue().get()), ".*").replace("\\$", ".").replace("]", "\\\\]").replace("\\[", "\\\\[");
  }

  private boolean includeLessThan(Comparable<T> comparable) {
    T upperBound = getUpperBound();

    return upperBound == null || comparable != null && comparable.compareTo(upperBound) < 0;
  }

  private boolean includeLessThanOrEqual(Comparable<T> comparable) {
    T upperBound = getUpperBound();

    return upperBound == null || comparable != null && comparable.compareTo(upperBound) <= 0;
  }

  private boolean includeGreaterThan(Comparable<T> comparable) {
    T lowerBound = getLowerBound();

    return lowerBound == null || comparable != null && comparable.compareTo(lowerBound) > 0;
  }

  private boolean includeGreaterThanOrEqual(Comparable<T> comparable) {
    T lowerBound = getLowerBound();

    return lowerBound == null || comparable != null && comparable.compareTo(lowerBound) >= 0;
  }

  private boolean includeBetweenExclusive(Comparable<T> comparable) {
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

  private boolean includeBetweenInclusive(Comparable<T> comparable) {
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

  private boolean includeNotBetweenExclusive(Comparable<T> comparable) {
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

  private boolean includeNotBetween(Comparable<T> comparable) {
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

  static final class DefaultBuilder<C, T> implements Builder<C, T> {

    private final C columnIdentifier;
    private final Class<T> columnClass;

    private List<Operator> operators = Arrays.asList(Operator.values());
    private char wildcard = Text.WILDCARD_CHARACTER.get();
    private Format format;
    private String dateTimePattern;
    private AutomaticWildcard automaticWildcard = ColumnConditionModel.AUTOMATIC_WILDCARD.get();

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
    public ColumnConditionModel<C, T> build() {
      return new DefaultColumnConditionModel<>(this);
    }
  }
}
