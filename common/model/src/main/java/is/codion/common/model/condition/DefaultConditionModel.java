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
import is.codion.common.observable.Observable;
import is.codion.common.observable.Observer;
import is.codion.common.state.State;
import is.codion.common.value.AbstractValue;
import is.codion.common.value.Value;
import is.codion.common.value.Value.Notify;
import is.codion.common.value.ValueSet;

import org.jspecify.annotations.Nullable;

import java.text.Format;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

final class DefaultConditionModel<T> implements ConditionModel<T> {

	private static final String WILDCARD_CHARACTER = "%";
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
		this.operands = new DefaultOperands<>(builder.wildcard, operator, builder.operands);
		this.operands.equal.addValidator(lockValidator);
		this.operands.equal.addListener(autoEnableListener);
		this.operands.equal.addListener(conditionChanged);
		this.operands.equal.wildcard.addListener(conditionChanged);
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
	public SetCondition<T> set() {
		return setCondition;
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
		public void validate(@Nullable Set<T> values) {
			for (T value : values) {
				requireNonNull(value, "In operands must not be null");
			}
		}
	}

	private static final class DefaultOperands<T> implements Operands<T> {

		private final EqualOperand<T> equal;
		private final ValueSet<T> in = ValueSet.<T>builder()
						.notify(Notify.WHEN_SET)
						.build();
		private final Value<T> upper = Value.builder()
						.<T>nullable()
						.notify(Notify.WHEN_SET)
						.build();
		private final Value<T> lower = Value.builder()
						.<T>nullable()
						.notify(Notify.WHEN_SET)
						.build();

		private DefaultOperands(Wildcard wildcard, Observable<Operator> operatorObserver, DefaultInitialOperands<T> initial) {
			equal = new EqualOperand<>(wildcard, operatorObserver);
			equal.set(initial.equal);
			in.set(initial.in);
			upper.set(initial.upper);
			lower.set(initial.lower);
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

		private static final class EqualOperand<T> extends AbstractValue<T> {

			private final Value<Wildcard> wildcard;
			private final Observable<Operator> operatorObserver;

			private @Nullable T value;

			private EqualOperand(Wildcard wildcard, Observable<Operator> operatorObserver) {
				super(null, Notify.WHEN_SET);
				this.wildcard = Value.nonNull(wildcard);
				this.operatorObserver = operatorObserver;
			}

			@Override
			protected @Nullable T getValue() {
				return addWildcard(value);
			}

			@Override
			protected void setValue(@Nullable T value) {
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
		private final DefaultInitialOperands<T> operands = new DefaultInitialOperands<>();

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
		public Builder<T> operands(Consumer<InitialOperands<T>> operands) {
			requireNonNull(operands).accept(this.operands);
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

	private static final class DefaultInitialOperands<T> implements InitialOperands<T> {

		private Set<T> in = emptySet();
		private @Nullable T equal;
		private @Nullable T upper;
		private @Nullable T lower;

		@Override
		public InitialOperands<T> equal(T equal) {
			this.equal = requireNonNull(equal);
			return this;
		}

		@Override
		public InitialOperands<T> in(Set<T> in) {
			this.in = requireNonNull(in);
			return this;
		}

		@Override
		public InitialOperands<T> upper(T upper) {
			this.upper = requireNonNull(upper);
			return this;
		}

		@Override
		public InitialOperands<T> lower(T lower) {
			this.lower = requireNonNull(lower);
			return this;
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
