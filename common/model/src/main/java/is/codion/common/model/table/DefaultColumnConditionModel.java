/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.model.table;

import is.codion.common.Operator;
import is.codion.common.event.Event;
import is.codion.common.event.EventObserver;
import is.codion.common.format.LocaleDateTimePattern;
import is.codion.common.state.State;
import is.codion.common.value.AbstractValue;
import is.codion.common.value.Value;
import is.codion.common.value.Value.Notify;
import is.codion.common.value.ValueSet;

import java.text.Format;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

final class DefaultColumnConditionModel<C, T> implements ColumnConditionModel<C, T> {

	private static final String WILDCARD = "%";
	private static final String REGEX_WILDCARD = ".*";

	private final Runnable autoEnableListener = new AutoEnableListener();
	private final Event<?> conditionChangedEvent = Event.event();
	private final State locked = State.state();
	private final Value.Validator<Object> lockValidator = value -> checkLock();
	private final ValueSet<T> inValues = ValueSet.<T>builder()
					.notify(Notify.WHEN_SET)
					.validator(lockValidator)
					.listener(autoEnableListener)
					.listener(conditionChangedEvent)
					.build();
	private final Value<T> equalValue = new EqualValue();
	private final Value<T> upperBoundValue = Value.builder()
					.<T>nullable()
					.notify(Notify.WHEN_SET)
					.validator(lockValidator)
					.listener(autoEnableListener)
					.listener(conditionChangedEvent)
					.build();
	private final Value<T> lowerBoundValue = Value.builder()
					.<T>nullable()
					.notify(Notify.WHEN_SET)
					.validator(lockValidator)
					.listener(autoEnableListener)
					.listener(conditionChangedEvent)
					.build();
	private final Value<AutomaticWildcard> automaticWildcard = Value.builder()
					.nonNull(AutomaticWildcard.NONE)
					.listener(conditionChangedEvent)
					.build();

	private final Value<Operator> operator;
	private final State caseSensitive;

	private final State autoEnable;
	private final State enabled = State.builder()
					.validator(lockValidator)
					.listener(conditionChangedEvent)
					.build();

	private final C identifier;
	private final Class<T> columnClass;
	private final Format format;
	private final String dateTimePattern;
	private final List<Operator> operators;

	private DefaultColumnConditionModel(DefaultBuilder<C, T> builder) {
		this.identifier = builder.identifier;
		this.operators = unmodifiableList(builder.operators);
		this.operator = Value.builder()
						.nonNull(builder.operator)
						.validator(lockValidator)
						.validator(this::validateOperator)
						.listener(autoEnableListener)
						.listener(conditionChangedEvent)
						.build();
		this.columnClass = builder.columnClass;
		this.format = builder.format;
		this.dateTimePattern = builder.dateTimePattern;
		this.automaticWildcard.set(builder.automaticWildcard);
		this.caseSensitive = State.builder(builder.caseSensitive)
						.listener(conditionChangedEvent)
						.build();
		this.autoEnable = State.builder(builder.autoEnable)
						.listener(autoEnableListener)
						.build();
	}

	@Override
	public C identifier() {
		return identifier;
	}

	@Override
	public State caseSensitive() {
		return caseSensitive;
	}

	@Override
	public Optional<Format> format() {
		return Optional.ofNullable(format);
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
	public Value<Operator> operator() {
		return operator;
	}

	@Override
	public List<Operator> operators() {
		return operators;
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
		equalValue.clear();
		upperBoundValue.clear();
		lowerBoundValue.clear();
		inValues.clear();
		operator.clear();
	}

	@Override
	public boolean accepts(Comparable<T> columnValue) {
		return valueAccepted(columnValue);
	}

	@Override
	public Value<T> equalValue() {
		return equalValue;
	}

	@Override
	public ValueSet<T> inValues() {
		return inValues;
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
	public EventObserver<?> conditionChangedEvent() {
		return conditionChangedEvent.observer();
	}

	private boolean valueAccepted(Comparable<T> comparable) {
		if (!caseSensitive.get()) {
			comparable = stringOrCharacterToLowerCase(comparable);
		}
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
			case IN:
				return isIn(comparable);
			case NOT_IN:
				return isNotIn(comparable);
			default:
				throw new IllegalArgumentException("Unknown operator: " + operator.get());
		}
	}

	private boolean isEqual(Comparable<T> comparable) {
		T equalValue = equalValue().get();
		if (!caseSensitive.get()) {
			equalValue = stringOrCharacterToLowerCase(equalValue);
		}
		if (comparable == null) {
			return equalValue == null;
		}
		if (equalValue == null) {
			return comparable == null;
		}
		if (comparable instanceof String && ((String) equalValue).contains(WILDCARD)) {
			return isEqualWildcard((String) comparable);
		}

		return comparable.compareTo(equalValue) == 0;
	}

	private boolean isNotEqual(Comparable<T> comparable) {
		T equalValue = equalValue().get();
		if (!caseSensitive.get()) {
			equalValue = stringOrCharacterToLowerCase(equalValue);
		}
		if (comparable == null) {
			return equalValue != null;
		}
		if (equalValue == null) {
			return comparable != null;
		}
		if (comparable instanceof String && ((String) equalValue).contains(WILDCARD)) {
			return !isEqualWildcard((String) comparable);
		}

		return comparable.compareTo(equalValue) != 0;
	}

	private boolean isEqualWildcard(String value) {
		String equalValue = (String) equalValue().get();
		if (equalValue == null) {
			equalValue = "";
		}
		if (equalValue.equals(WILDCARD)) {
			return true;
		}
		if (!caseSensitive.get()) {
			equalValue = equalValue.toLowerCase();
		}
		if (!equalValue.contains(WILDCARD)) {
			return value.compareTo(equalValue) == 0;
		}

		return Pattern.matches(Stream.of(equalValue.split(WILDCARD))
						.map(Pattern::quote)
						.collect(joining(REGEX_WILDCARD, "", equalValue.endsWith(WILDCARD) ? REGEX_WILDCARD : "")), value);
	}

	private boolean isLessThan(Comparable<T> comparable) {
		T upperBound = upperBoundValue.get();

		return upperBound == null || comparable != null && comparable.compareTo(upperBound) < 0;
	}

	private boolean isLessThanOrEqual(Comparable<T> comparable) {
		T upperBound = upperBoundValue.get();

		return upperBound == null || comparable != null && comparable.compareTo(upperBound) <= 0;
	}

	private boolean isGreaterThan(Comparable<T> comparable) {
		T lowerBound = lowerBoundValue.get();

		return lowerBound == null || comparable != null && comparable.compareTo(lowerBound) > 0;
	}

	private boolean isGreaterThanOrEqual(Comparable<T> comparable) {
		T lowerBound = lowerBoundValue.get();

		return lowerBound == null || comparable != null && comparable.compareTo(lowerBound) >= 0;
	}

	private boolean isBetweenExclusive(Comparable<T> comparable) {
		T lowerBound = lowerBoundValue.get();
		T upperBound = upperBoundValue.get();
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
		T lowerBound = lowerBoundValue.get();
		T upperBound = upperBoundValue.get();
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
		T lowerBound = lowerBoundValue.get();
		T upperBound = upperBoundValue.get();
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
		T lowerBound = lowerBoundValue.get();
		T upperBound = upperBoundValue.get();
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

	private boolean isIn(Comparable<T> comparable) {
		return inValues.get().contains(comparable);
	}

	private boolean isNotIn(Comparable<T> comparable) {
		return !isIn(comparable);
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
			case PREFIX:
				value = addWildcardPrefix(value);
				break;
			case POSTFIX:
				value = addWildcardPostfix(value);
				break;
			case PREFIX_AND_POSTFIX:
				value = addWildcardPrefix(value);
				value = addWildcardPostfix(value);
				break;
			default:
				break;
		}

		return value;
	}

	private static String addWildcardPrefix(String value) {
		if (!value.startsWith(WILDCARD)) {
			return WILDCARD + value;
		}

		return value;
	}

	private static String addWildcardPostfix(String value) {
		if (!value.endsWith(WILDCARD)) {
			return value + WILDCARD;
		}

		return value;
	}

	private void checkLock() {
		if (locked.get()) {
			throw new IllegalStateException("Condition model for column identified by " + identifier + " is locked");
		}
	}

	private void validateOperator(Operator operator) {
		if (operators != null && !operators.contains(requireNonNull(operator, "operator"))) {
			throw new IllegalArgumentException("Operator " + operator + " not available in condition model: " + identifier);
		}
	}

	private static <T> T stringOrCharacterToLowerCase(T value) {
		if (value instanceof String) {
			return (T) ((String) value).toLowerCase();
		}
		if (value instanceof Character) {
			return (T) Character.valueOf(Character.toLowerCase((Character) value));
		}

		return value;
	}

	private static <T> Comparable<T> stringOrCharacterToLowerCase(Comparable<T> comparable) {
		if (comparable instanceof String) {
			return (Comparable<T>) ((String) comparable).toLowerCase();
		}
		if (comparable instanceof Character) {
			return (Comparable<T>) Character.valueOf(Character.toLowerCase((Character) comparable));
		}

		return comparable;
	}

	private final class EqualValue extends AbstractValue<T> {

		private T value;

		public EqualValue() {
			super(null, Notify.WHEN_SET);
			addValidator(lockValidator);
			addListener(autoEnableListener);
			addListener(conditionChangedEvent);
		}

		@Override
		protected T getValue() {
			return addAutomaticWildcard(value);
		}

		@Override
		protected void setValue(T value) {
			this.value = value;
		}
	}

	private final class AutoEnableListener implements Runnable {

		@Override
		public void run() {
			if (autoEnable.get()) {
				switch (operator.get()) {
					case EQUAL:
					case NOT_EQUAL:
						enabled.set(equalValue.isNotNull());
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
					case IN:
					case NOT_IN:
						enabled.set(inValues.notEmpty());
						break;
					default:
						throw new IllegalStateException("Unknown operator: " + operator.get());
				}
			}
		}
	}

	static final class DefaultBuilder<C, T> implements Builder<C, T> {

		private static final List<Operator> DEFAULT_OPERATORS = asList(Operator.values());

		private final C identifier;
		private final Class<T> columnClass;

		private List<Operator> operators;
		private Operator operator = Operator.EQUAL;
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

		DefaultBuilder(C identifier, Class<T> columnClass) {
			this.identifier = requireNonNull(identifier);
			this.columnClass = requireNonNull(columnClass);
			this.operators = columnClass.equals(Boolean.class) ? singletonList(Operator.EQUAL) : DEFAULT_OPERATORS;
		}

		@Override
		public Builder<C, T> operators(List<Operator> operators) {
			if (requireNonNull(operators).isEmpty()) {
				throw new IllegalArgumentException("One or more operators must be specified");
			}
			validateOperators(operators, operator);
			this.operators = operators;
			return this;
		}

		@Override
		public Builder<C, T> operator(Operator operator) {
			validateOperators(operators, operator);
			this.operator = operator;
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

		private static void validateOperators(List<Operator> operators, Operator operator) {
			if (!operators.contains(operator)) {
				throw new IllegalArgumentException("Available operators do no not contain the selected operator: " + operator);
			}
		}
	}
}
