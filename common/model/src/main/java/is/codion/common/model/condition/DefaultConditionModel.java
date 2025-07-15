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
 * Copyright (c) 2008 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.model.condition;

import is.codion.common.Operator;
import is.codion.common.event.Event;
import is.codion.common.format.LocaleDateTimePattern;
import is.codion.common.observable.Observer;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.common.value.ValueSet;

import org.jspecify.annotations.Nullable;

import java.text.Format;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

final class DefaultConditionModel<T> implements ConditionModel<T> {

	private static final String REGEX_WILDCARD = ".*";

	private final Runnable autoEnableListener = new AutoEnableListener();
	private final Event<?> conditionChanged = Event.event();
	private final State locked = State.state();
	private final Value.Validator<Object> lockValidator = value -> checkLock();
	private final Value.Validator<Set<T>> inOperandsValidator = new InOperandsValidator();

	private final Value<Operator> operator;
	private final DefaultOperands<T> operands;
	private final SetCondition<T> setCondition = new DefaultSetCondition();
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
		this.operands = new DefaultOperands<>(builder.operands);
		this.operands.equal.addValidator(lockValidator);
		this.operands.equal.addListener(autoEnableListener);
		this.operands.equal.addListener(conditionChanged);
		this.operands.wildcard.addListener(conditionChanged);
		this.operands.in.addValidator(lockValidator);
		this.operands.in.addValidator(inOperandsValidator);
		this.operands.in.addListener(autoEnableListener);
		this.operands.in.addListener(conditionChanged);
		this.operands.upper.addValidator(lockValidator);
		this.operands.upper.addListener(autoEnableListener);
		this.operands.upper.addListener(conditionChanged);
		this.operands.lower.addValidator(lockValidator);
		this.operands.lower.addListener(autoEnableListener);
		this.operands.lower.addListener(conditionChanged);
		this.valueClass = builder.valueClass;
		this.format = builder.format;
		this.dateTimePattern = builder.dateTimePattern;
		this.caseSensitive = State.builder()
						.value(builder.caseSensitive)
						.listener(conditionChanged)
						.build();
		this.autoEnable = State.builder()
						.value(builder.autoEnable)
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
	public SetCondition<T> set() {
		return setCondition;
	}

	@Override
	public State enabled() {
		return enabled;
	}

	@Override
	public State autoEnable() {
		return autoEnable;
	}

	@Override
	public void clear() {
		operands.clear();
		operator.clear();
		enabled.set(false);
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
		if (equalOperand instanceof String) {
			equalOperand = (T) operands.equalWithWildcards();
		}
		if (comparable instanceof String && ((String) equalOperand).contains(WILDCARD_CHARACTER)) {
			return isEqualWildcard((String) comparable, (String) equalOperand);
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
		if (equalOperand instanceof String) {
			equalOperand = (T) operands.equalWithWildcards();
		}
		if (comparable instanceof String && ((String) equalOperand).contains(WILDCARD_CHARACTER)) {
			return !isEqualWildcard((String) comparable, (String) equalOperand);
		}

		return comparable.compareTo(equalOperand) != 0;
	}

	static String addWildcard(String operand, Wildcard wildcard) {
		requireNonNull(operand);
		switch (wildcard) {
			case PREFIX:
				operand = addWildcardPrefix(operand);
				break;
			case POSTFIX:
				operand = addWildcardPostfix(operand);
				break;
			case PREFIX_AND_POSTFIX:
				operand = addWildcardPrefix(operand);
				operand = addWildcardPostfix(operand);
				break;
			default:
				break;
		}

		return operand;
	}

	static String addWildcardPrefix(String operand) {
		if (!operand.startsWith(WILDCARD_CHARACTER)) {
			return WILDCARD_CHARACTER + operand;
		}

		return operand;
	}

	static String addWildcardPostfix(String operand) {
		if (!operand.endsWith(WILDCARD_CHARACTER)) {
			return operand + WILDCARD_CHARACTER;
		}

		return operand;
	}

	private static boolean isEqualWildcard(String value, String equalOperand) {
		if (equalOperand.equals(WILDCARD_CHARACTER)) {
			return true;
		}

		return Pattern.matches(Stream.of(equalOperand.split(WILDCARD_CHARACTER))
						.map(Pattern::quote)
						.collect(joining(REGEX_WILDCARD, "", equalOperand.endsWith(WILDCARD_CHARACTER) ? REGEX_WILDCARD : "")), value);
	}

	private boolean isLessThan(@Nullable Comparable<T> comparable) {
		T upper = operands.upper.get();

		return upper == null || comparable != null && comparable.compareTo(upper) < 0;
	}

	private boolean isLessThanOrEqual(@Nullable Comparable<T> comparable) {
		T upper = operands.upper.get();

		return upper == null || comparable != null && comparable.compareTo(upper) <= 0;
	}

	private boolean isGreaterThan(@Nullable Comparable<T> comparable) {
		T lower = operands.lower.get();

		return lower == null || comparable != null && comparable.compareTo(lower) > 0;
	}

	private boolean isGreaterThanOrEqual(@Nullable Comparable<T> comparable) {
		T lower = operands.lower.get();

		return lower == null || comparable != null && comparable.compareTo(lower) >= 0;
	}

	private boolean isBetweenExclusive(@Nullable Comparable<T> comparable) {
		T lower = operands.lower.get();
		T upper = operands.upper.get();
		if (lower == null && upper == null) {
			return true;
		}

		if (comparable == null) {
			return false;
		}

		if (lower == null) {
			return comparable.compareTo(upper) < 0;
		}

		if (upper == null) {
			return comparable.compareTo(lower) > 0;
		}

		int lowerCompareResult = comparable.compareTo(lower);
		int upperCompareResult = comparable.compareTo(upper);

		return lowerCompareResult > 0 && upperCompareResult < 0;
	}

	private boolean isBetween(@Nullable Comparable<T> comparable) {
		T lower = operands.lower.get();
		T upper = operands.upper.get();
		if (lower == null && upper == null) {
			return true;
		}

		if (comparable == null) {
			return false;
		}

		if (lower == null) {
			return comparable.compareTo(upper) <= 0;
		}

		if (upper == null) {
			return comparable.compareTo(lower) >= 0;
		}

		int lowerCompareResult = comparable.compareTo(lower);
		int upperCompareResult = comparable.compareTo(upper);

		return lowerCompareResult >= 0 && upperCompareResult <= 0;
	}

	private boolean isNotBetweenExclusive(@Nullable Comparable<T> comparable) {
		T lower = operands.lower.get();
		T upper = operands.upper.get();
		if (lower == null && upper == null) {
			return true;
		}

		if (comparable == null) {
			return false;
		}

		if (lower == null) {
			return comparable.compareTo(upper) > 0;
		}

		if (upper == null) {
			return comparable.compareTo(lower) < 0;
		}

		int lowerCompareResult = comparable.compareTo(lower);
		int upperCompareResult = comparable.compareTo(upper);

		return lowerCompareResult < 0 || upperCompareResult > 0;
	}

	private boolean isNotBetween(@Nullable Comparable<T> comparable) {
		T lower = operands.lower.get();
		T upper = operands.upper.get();
		if (lower == null && upper == null) {
			return true;
		}

		if (comparable == null) {
			return false;
		}

		if (lower == null) {
			return comparable.compareTo(upper) >= 0;
		}

		if (upper == null) {
			return comparable.compareTo(lower) <= 0;
		}

		int lowerCompareResult = comparable.compareTo(lower);
		int upperCompareResult = comparable.compareTo(upper);

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

	/**
	 * Converts String or Character values to lowercase, leaving other types unchanged.
	 * @param value the value to potentially convert to lowercase
	 * @param <V> the value type
	 * @return the value converted to lowercase if it's a String or Character, otherwise unchanged
	 */
	private static <V> @Nullable V stringOrCharacterToLowerCase(@Nullable V value) {
		if (value instanceof String) {
			return (V) ((String) value).toLowerCase();
		}
		if (value instanceof Character) {
			return (V) Character.valueOf(Character.toLowerCase((Character) value));
		}

		return value;
	}

	private final class AutoEnableListener implements Runnable {

		@Override
		public void run() {
			if (autoEnable.get()) {
				switch (operator.getOrThrow()) {
					case EQUAL:
					case NOT_EQUAL:
						enabled.set(!operands.equal.isNull());
						break;
					case LESS_THAN:
					case LESS_THAN_OR_EQUAL:
						enabled.set(!operands.upper.isNull());
						break;
					case GREATER_THAN:
					case GREATER_THAN_OR_EQUAL:
						enabled.set(!operands.lower.isNull());
						break;
					case BETWEEN:
					case BETWEEN_EXCLUSIVE:
					case NOT_BETWEEN:
					case NOT_BETWEEN_EXCLUSIVE:
						enabled.set(!operands.lower.isNull() && !operands.upper.isNull());
						break;
					case IN:
					case NOT_IN:
						enabled.set(!operands.in.isEmpty());
						break;
					default:
						throw new IllegalStateException("Unknown operator: " + operator.get());
				}
			}
		}
	}

	private final class InOperandsValidator implements Value.Validator<Set<T>> {

		@Override
		public void validate(Set<T> values) {
			for (T value : values) {
				requireNonNull(value, "In operands must not be null");
			}
		}
	}

	private static final class DefaultOperands<T> implements Operands<T> {

		private final Value<Wildcard> wildcard;
		private final Value<T> equal;
		private final ValueSet<T> in;
		private final Value<T> upper;
		private final Value<T> lower;

		private DefaultOperands(Operands<T> operands) {
			wildcard = operands.wildcard();
			equal = operands.equal();
			in = operands.in();
			upper = operands.upper();
			lower = operands.lower();
		}

		@Override
		public Value<Wildcard> wildcard() {
			return wildcard;
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
		public Value<T> upper() {
			return upper;
		}

		@Override
		public Value<T> lower() {
			return lower;
		}

		private void clear() {
			equal.clear();
			in.clear();
			upper.clear();
			lower.clear();
		}
	}

	private static final class DefaultValueClassStep implements Builder.ValueClassStep {

		@Override
		public <T> Builder<T> valueClass(Class<T> valueClass) {
			return new DefaultBuilder<>(valueClass);
		}
	}

	static final class DefaultBuilder<T> implements Builder<T> {

		static final Builder.ValueClassStep VALUE_CLASS = new DefaultValueClassStep();

		private static final List<Operator> DEFAULT_OPERATORS = asList(Operator.values());

		private final Class<T> valueClass;

		private List<Operator> operators;
		private Operator operator = Operator.EQUAL;
		private Operands<T> operands = new Operands<T>() {};
		private @Nullable Format format;
		private @Nullable String dateTimePattern = LocaleDateTimePattern.builder()
						.delimiterDash()
						.yearFourDigits()
						.hoursMinutesSeconds()
						.build()
						.dateTimePattern();
		private boolean caseSensitive = CASE_SENSITIVE.getOrThrow();
		private boolean autoEnable = true;

		private DefaultBuilder(Class<T> valueClass) {
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
		public Builder<T> operands(Operands<T> operands) {
			this.operands = requireNonNull(operands);
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

	private final class DefaultSetCondition implements SetCondition<T> {

		@Override
		public boolean isNull() {
			boolean changed = set(null, operands.equal);
			changed = set(Operator.EQUAL, operator) || changed;
			enabled.set(true);

			return changed;
		}

		@Override
		public boolean isNotNull() {
			boolean changed = set(null, operands.equal);
			changed = set(Operator.NOT_IN, operator) || changed;
			enabled.set(true);

			return changed;
		}

		@Override
		public boolean equalTo(@Nullable T value) {
			boolean changed = set(value, operands.equal);
			changed = set(Operator.EQUAL, operator) || changed;
			enabled.set(value != null);

			return changed;
		}

		@Override
		public boolean notEqualTo(@Nullable T value) {
			boolean changed = set(value, operands.equal);
			changed = set(Operator.NOT_EQUAL, operator) || changed;
			enabled.set(value != null);

			return changed;
		}

		@Override
		public boolean greaterThan(@Nullable T value) {
			boolean changed = set(value, operands.lower);
			changed = set(Operator.GREATER_THAN, operator) || changed;
			enabled.set(value != null);

			return changed;
		}

		@Override
		public boolean greaterThanOrEqualTo(@Nullable T value) {
			boolean changed = set(value, operands.lower);
			changed = set(Operator.GREATER_THAN_OR_EQUAL, operator) || changed;
			enabled.set(value != null);

			return changed;
		}

		@Override
		public boolean lessThan(@Nullable T value) {
			boolean changed = set(value, operands.upper);
			changed = set(Operator.LESS_THAN, operator) || changed;
			enabled.set(value != null);

			return changed;
		}

		@Override
		public boolean lessThanOrEqualTo(@Nullable T value) {
			boolean changed = set(value, operands.upper);
			changed = set(Operator.LESS_THAN_OR_EQUAL, operator) || changed;
			enabled.set(value != null);

			return changed;
		}

		@Override
		public boolean in(T... values) {
			return in(asList(requireNonNull(values)));
		}

		@Override
		public boolean in(Collection<T> values) {
			boolean changed = set(values, operands.in);
			changed = set(Operator.IN, operator) || changed;
			enabled.set(!values.isEmpty());

			return changed;
		}

		@Override
		public boolean notIn(T... values) {
			return notIn(asList(requireNonNull(values)));
		}

		@Override
		public boolean notIn(Collection<T> values) {
			boolean changed = set(values, operands.in);
			changed = set(Operator.NOT_IN, operator) || changed;
			enabled.set(!values.isEmpty());

			return changed;
		}

		@Override
		public boolean betweenExclusive(@Nullable T lower, @Nullable T upper) {
			boolean changed = set(lower, operands.lower);
			changed = set(upper, operands.upper) || changed;
			changed = set(Operator.BETWEEN_EXCLUSIVE, operator) || changed;
			enabled.set(lower != null && upper != null);

			return changed;
		}

		@Override
		public boolean notBetweenExclusive(@Nullable T lower, @Nullable T upper) {
			boolean changed = set(lower, operands.lower);
			changed = set(upper, operands.upper) || changed;
			changed = set(Operator.NOT_BETWEEN_EXCLUSIVE, operator) || changed;
			enabled.set(lower != null && upper != null);

			return changed;
		}

		@Override
		public boolean between(@Nullable T lower, @Nullable T upper) {
			boolean changed = set(lower, operands.lower);
			changed = set(upper, operands.upper) || changed;
			changed = set(Operator.BETWEEN, operator) || changed;
			enabled.set(lower != null && upper != null);

			return changed;
		}

		@Override
		public boolean notBetween(@Nullable T lower, @Nullable T upper) {
			boolean changed = set(lower, operands.lower);
			changed = set(upper, operands.upper) || changed;
			changed = set(Operator.NOT_BETWEEN, operator) || changed;
			enabled.set(lower != null && upper != null);

			return changed;
		}

		private static boolean set(Operator operator, Value<Operator> value) {
			boolean changed = value.isNotEqualTo(operator);
			value.set(operator);

			return changed;
		}

		private static <T> boolean set(Collection<T> value, ValueSet<T> operand) {
			boolean changed = operand.isNotEqualTo(new HashSet<>(requireNonNull(value)));
			operand.set(value);

			return changed;
		}

		private static <T> boolean set(@Nullable T value, Value<T> operand) {
			boolean changed = operand.isNotEqualTo(value);
			operand.set(value);

			return changed;
		}
	}
}
