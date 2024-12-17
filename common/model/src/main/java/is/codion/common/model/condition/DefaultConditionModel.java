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
package is.codion.common.model.condition;

import is.codion.common.Operator;
import is.codion.common.event.Event;
import is.codion.common.format.LocaleDateTimePattern;
import is.codion.common.observable.Observable;
import is.codion.common.observable.Observer;
import is.codion.common.state.State;
import is.codion.common.value.AbstractValue;
import is.codion.common.value.Value;
import is.codion.common.value.Value.Notify;
import is.codion.common.value.ValueSet;

import org.jspecify.annotations.Nullable;

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

final class DefaultConditionModel<T> implements ConditionModel<T> {

	private static final String WILDCARD_CHARACTER = "%";
	private static final String REGEX_WILDCARD = ".*";

	private final Runnable autoEnableListener = new AutoEnableListener();
	private final Event<?> conditionChanged = Event.event();
	private final State locked = State.state();
	private final Value.Validator<Object> lockValidator = value -> checkLock();

	private final Value<Operator> operator;
	private final DefaultOperands<T> operands;
	private final State caseSensitive;

	private final State autoEnable;
	private final State enabled = State.builder()
					.validator(lockValidator)
					.listener(conditionChanged)
					.build();

	private final Class<T> valueClass;
	private final @Nullable Format format;
	private final @Nullable String dateTimePattern;
	private final List<Operator> operators;

	private DefaultConditionModel(DefaultBuilder<T> builder) {
		this.operators = unmodifiableList(builder.operators);
		this.operator = Value.builder()
						.nonNull(builder.operator)
						.validator(lockValidator)
						.validator(this::validateOperator)
						.listener(autoEnableListener)
						.listener(conditionChanged)
						.build();
		this.operands = new DefaultOperands<>(builder.wildcard, operator);
		this.operands.equal.addValidator(lockValidator);
		this.operands.equal.addListener(autoEnableListener);
		this.operands.equal.addListener(conditionChanged);
		this.operands.equal.wildcard.addListener(conditionChanged);
		this.operands.in.addValidator(lockValidator);
		this.operands.in.addListener(autoEnableListener);
		this.operands.in.addListener(conditionChanged);
		this.operands.upperBound.addValidator(lockValidator);
		this.operands.upperBound.addListener(autoEnableListener);
		this.operands.upperBound.addListener(conditionChanged);
		this.operands.lowerBound.addValidator(lockValidator);
		this.operands.lowerBound.addListener(autoEnableListener);
		this.operands.lowerBound.addListener(conditionChanged);
		this.valueClass = builder.valueClass;
		this.format = builder.format;
		this.dateTimePattern = builder.dateTimePattern;
		this.caseSensitive = State.builder(builder.caseSensitive)
						.listener(conditionChanged)
						.build();
		this.autoEnable = State.builder(builder.autoEnable)
						.listener(autoEnableListener)
						.build();
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
	public Optional<String> dateTimePattern() {
		return Optional.ofNullable(dateTimePattern);
	}

	@Override
	public State locked() {
		return locked;
	}

	@Override
	public Class<T> valueClass() {
		return valueClass;
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
	public Operands<T> operands() {
		return operands;
	}

	@Override
	public State enabled() {
		return enabled;
	}

	@Override
	public Value<Wildcard> wildcard() {
		return operands.equal.wildcard;
	}

	@Override
	public State autoEnable() {
		return autoEnable;
	}

	@Override
	public void clear() {
		operands.clear();
		operator.clear();
	}

	@Override
	public boolean accepts(@Nullable Comparable<T> value) {
		return valueAccepted(value);
	}

	@Override
	public Observer<?> changed() {
		return conditionChanged.observer();
	}

	private boolean valueAccepted(@Nullable Comparable<T> comparable) {
		if (!caseSensitive.get()) {
			comparable = stringOrCharacterToLowerCase(comparable);
		}
		switch (operator.getOrThrow()) {
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

	private boolean isEqual(@Nullable Comparable<T> comparable) {
		T equalOperand = operands.equal().get();
		if (!caseSensitive.get()) {
			equalOperand = stringOrCharacterToLowerCase(equalOperand);
		}
		if (comparable == null) {
			return equalOperand == null;
		}
		if (equalOperand == null) {
			return comparable == null;
		}
		if (comparable instanceof String && ((String) equalOperand).contains(WILDCARD_CHARACTER)) {
			return isEqualWildcard((String) comparable);
		}

		return comparable.compareTo(equalOperand) == 0;
	}

	private boolean isNotEqual(@Nullable Comparable<T> comparable) {
		T equalOperand = operands.equal().get();
		if (!caseSensitive.get()) {
			equalOperand = stringOrCharacterToLowerCase(equalOperand);
		}
		if (comparable == null) {
			return equalOperand != null;
		}
		if (equalOperand == null) {
			return comparable != null;
		}
		if (comparable instanceof String && ((String) equalOperand).contains(WILDCARD_CHARACTER)) {
			return !isEqualWildcard((String) comparable);
		}

		return comparable.compareTo(equalOperand) != 0;
	}

	private boolean isEqualWildcard(String value) {
		String equalOperand = (String) operands.equal().get();
		if (equalOperand == null) {
			equalOperand = "";
		}
		if (equalOperand.equals(WILDCARD_CHARACTER)) {
			return true;
		}
		if (!caseSensitive.get()) {
			equalOperand = equalOperand.toLowerCase();
		}
		if (!equalOperand.contains(WILDCARD_CHARACTER)) {
			return value.compareTo(equalOperand) == 0;
		}

		return Pattern.matches(Stream.of(equalOperand.split(WILDCARD_CHARACTER))
						.map(Pattern::quote)
						.collect(joining(REGEX_WILDCARD, "", equalOperand.endsWith(WILDCARD_CHARACTER) ? REGEX_WILDCARD : "")), value);
	}

	private boolean isLessThan(@Nullable Comparable<T> comparable) {
		T upperBound = operands.upperBound.get();

		return upperBound == null || comparable != null && comparable.compareTo(upperBound) < 0;
	}

	private boolean isLessThanOrEqual(@Nullable Comparable<T> comparable) {
		T upperBound = operands.upperBound.get();

		return upperBound == null || comparable != null && comparable.compareTo(upperBound) <= 0;
	}

	private boolean isGreaterThan(@Nullable Comparable<T> comparable) {
		T lowerBound = operands.lowerBound.get();

		return lowerBound == null || comparable != null && comparable.compareTo(lowerBound) > 0;
	}

	private boolean isGreaterThanOrEqual(@Nullable Comparable<T> comparable) {
		T lowerBound = operands.lowerBound.get();

		return lowerBound == null || comparable != null && comparable.compareTo(lowerBound) >= 0;
	}

	private boolean isBetweenExclusive(@Nullable Comparable<T> comparable) {
		T lowerBound = operands.lowerBound.get();
		T upperBound = operands.upperBound.get();
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

	private boolean isBetween(@Nullable Comparable<T> comparable) {
		T lowerBound = operands.lowerBound.get();
		T upperBound = operands.upperBound.get();
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

	private boolean isNotBetweenExclusive(@Nullable Comparable<T> comparable) {
		T lowerBound = operands.lowerBound.get();
		T upperBound = operands.upperBound.get();
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

	private boolean isNotBetween(@Nullable Comparable<T> comparable) {
		T lowerBound = operands.lowerBound.get();
		T upperBound = operands.upperBound.get();
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

	private boolean isIn(@Nullable Comparable<T> comparable) {
		return operands.in.get().contains(comparable);
	}

	private boolean isNotIn(@Nullable Comparable<T> comparable) {
		return !isIn(comparable);
	}

	private void checkLock() {
		if (locked.get()) {
			throw new IllegalStateException("Condition model is locked");
		}
	}

	private void validateOperator(@Nullable Operator operator) {
		if (operators != null && !operators.contains(requireNonNull(operator))) {
			throw new IllegalArgumentException("Operator " + operator + " not available");
		}
	}

	private static <T> @Nullable T stringOrCharacterToLowerCase(@Nullable T value) {
		if (value instanceof String) {
			return (T) ((String) value).toLowerCase();
		}
		if (value instanceof Character) {
			return (T) Character.valueOf(Character.toLowerCase((Character) value));
		}

		return value;
	}

	private static <T> @Nullable Comparable<T> stringOrCharacterToLowerCase(@Nullable Comparable<T> comparable) {
		if (comparable instanceof String) {
			return (Comparable<T>) ((String) comparable).toLowerCase();
		}
		if (comparable instanceof Character) {
			return (Comparable<T>) Character.valueOf(Character.toLowerCase((Character) comparable));
		}

		return comparable;
	}

	private final class AutoEnableListener implements Runnable {

		@Override
		public void run() {
			if (autoEnable.get()) {
				switch (operator.getOrThrow()) {
					case EQUAL:
					case NOT_EQUAL:
						enabled.set(operands.equal.isNotNull());
						break;
					case LESS_THAN:
					case LESS_THAN_OR_EQUAL:
						enabled.set(operands.upperBound.isNotNull());
						break;
					case GREATER_THAN:
					case GREATER_THAN_OR_EQUAL:
						enabled.set(operands.lowerBound.isNotNull());
						break;
					case BETWEEN:
					case BETWEEN_EXCLUSIVE:
					case NOT_BETWEEN:
					case NOT_BETWEEN_EXCLUSIVE:
						enabled.set(operands.lowerBound.isNotNull() && operands.upperBound.isNotNull());
						break;
					case IN:
					case NOT_IN:
						enabled.set(operands.in.notEmpty());
						break;
					default:
						throw new IllegalStateException("Unknown operator: " + operator.get());
				}
			}
		}
	}

	private static final class DefaultOperands<T> implements Operands<T> {

		private final EqualOperand<T> equal;
		private final ValueSet<T> in = ValueSet.<T>builder()
						.notify(Notify.WHEN_SET)
						.build();
		private final Value<T> upperBound = Value.builder()
						.<T>nullable()
						.notify(Notify.WHEN_SET)
						.build();
		private final Value<T> lowerBound = Value.builder()
						.<T>nullable()
						.notify(Notify.WHEN_SET)
						.build();

		private DefaultOperands(Wildcard wildcard, Observable<Operator> operatorObserver) {
			equal = new EqualOperand<>(wildcard, operatorObserver);
		}

		@Override
		public Value<T> equal() {
			return equal;
		}

		@Override
		public ValueSet<T> in() {
			return in;
		}

		@Override
		public Value<T> upperBound() {
			return upperBound;
		}

		@Override
		public Value<T> lowerBound() {
			return lowerBound;
		}

		private void clear() {
			equal.clear();
			in.clear();
			upperBound.clear();
			lowerBound.clear();
		}

		private static final class EqualOperand<T> extends AbstractValue<T> {

			private final Value<Wildcard> wildcard;
			private final Observable<Operator> operatorObserver;

			private @Nullable T value;

			private EqualOperand(Wildcard wildcard, Observable<Operator> operatorObserver) {
				super(null, Notify.WHEN_SET);
				this.wildcard = Value.builder()
								.nonNull(wildcard)
								.build();
				this.operatorObserver = operatorObserver;
			}

			@Override
			protected @Nullable T getValue() {
				return addWildcard(value);
			}

			@Override
			protected void setValue(T value) {
				this.value = value;
			}

			private @Nullable T addWildcard(@Nullable T operand) {
				if (!(operand instanceof String)) {
					return operand;
				}
				switch (operatorObserver.getOrThrow()) {
					//wildcard only used for EQUAL and NOT_EQUAL
					case EQUAL:
					case NOT_EQUAL:
						return (T) addWildcard((String) operand);
					default:
						return operand;
				}
			}

			private String addWildcard(String operand) {
				String operandWithWildcards = operand;
				switch (wildcard.getOrThrow()) {
					case PREFIX:
						operandWithWildcards = addWildcardPrefix(operandWithWildcards);
						break;
					case POSTFIX:
						operandWithWildcards = addWildcardPostfix(operandWithWildcards);
						break;
					case PREFIX_AND_POSTFIX:
						operandWithWildcards = addWildcardPrefix(operandWithWildcards);
						operandWithWildcards = addWildcardPostfix(operandWithWildcards);
						break;
					default:
						break;
				}

				return operandWithWildcards;
			}

			private static String addWildcardPrefix(String operand) {
				if (!operand.startsWith(WILDCARD_CHARACTER)) {
					return WILDCARD_CHARACTER + operand;
				}

				return operand;
			}

			private static String addWildcardPostfix(String operand) {
				if (!operand.endsWith(WILDCARD_CHARACTER)) {
					return operand + WILDCARD_CHARACTER;
				}

				return operand;
			}
		}
	}

	static final class DefaultBuilder<T> implements Builder<T> {

		private static final List<Operator> DEFAULT_OPERATORS = asList(Operator.values());

		private final Class<T> valueClass;

		private List<Operator> operators;
		private Operator operator = Operator.EQUAL;
		private @Nullable Format format;
		private @Nullable String dateTimePattern = LocaleDateTimePattern.builder()
						.delimiterDash()
						.yearFourDigits()
						.hoursMinutesSeconds()
						.build()
						.dateTimePattern();
		private Wildcard wildcard = WILDCARD.getOrThrow();
		private boolean caseSensitive = CASE_SENSITIVE.getOrThrow();
		private boolean autoEnable = true;

		DefaultBuilder(Class<T> valueClass) {
			this.valueClass = requireNonNull(valueClass);
			this.operators = valueClass.equals(Boolean.class) ? singletonList(Operator.EQUAL) : DEFAULT_OPERATORS;
		}

		@Override
		public Builder<T> operators(List<Operator> operators) {
			if (requireNonNull(operators).isEmpty()) {
				throw new IllegalArgumentException("One or more operators must be specified");
			}
			validateOperators(operators, operator);
			this.operators = operators;
			return this;
		}

		@Override
		public Builder<T> operator(Operator operator) {
			validateOperators(operators, operator);
			this.operator = operator;
			return this;
		}

		@Override
		public Builder<T> format(@Nullable Format format) {
			this.format = format;
			return this;
		}

		@Override
		public Builder<T> dateTimePattern(@Nullable String dateTimePattern) {
			this.dateTimePattern = dateTimePattern;
			return this;
		}

		@Override
		public Builder<T> wildcard(Wildcard wildcard) {
			this.wildcard = requireNonNull(wildcard);
			return this;
		}

		@Override
		public Builder<T> caseSensitive(boolean caseSensitive) {
			this.caseSensitive = caseSensitive;
			return this;
		}

		@Override
		public Builder<T> autoEnable(boolean autoEnable) {
			this.autoEnable = autoEnable;
			return this;
		}

		@Override
		public ConditionModel<T> build() {
			return new DefaultConditionModel<>(this);
		}

		private static void validateOperators(List<Operator> operators, Operator operator) {
			if (!operators.contains(operator)) {
				throw new IllegalArgumentException("Available operators do no not contain the selected operator: " + operator);
			}
		}
	}
}
