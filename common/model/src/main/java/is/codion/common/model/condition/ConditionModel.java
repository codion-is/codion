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
 * Copyright (c) 2010 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.model.condition;

import is.codion.common.Operator;
import is.codion.common.observable.Observer;
import is.codion.common.property.PropertyValue;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.common.value.Value.Notify;
import is.codion.common.value.ValueSet;

import org.jspecify.annotations.Nullable;

import java.text.Format;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static is.codion.common.Configuration.booleanValue;
import static is.codion.common.Configuration.enumValue;
import static is.codion.common.model.condition.DefaultConditionModel.addWildcard;
import static is.codion.common.resource.MessageBundle.messageBundle;
import static java.util.ResourceBundle.getBundle;

/**
 * Specifies a condition with an operator and operands as well as relevant events and states.
 * For instances create a {@link Builder} via {@link #builder(Class)}.
 * @param <T> the condition value type
 */
public interface ConditionModel<T> {

	/**
	 * The wildcard character
	 */
	String WILDCARD_CHARACTER = "%";

	/**
	 * Specifies whether wildcards are added to string values
	 * <ul>
	 * <li>Value type: {@link Wildcard}
	 * <li>Default value: {@link Wildcard#POSTFIX}
	 * </ul>
	 */
	PropertyValue<Wildcard> WILDCARD =
					enumValue(ConditionModel.class.getName() + ".wildcard",
									Wildcard.class, Wildcard.PREFIX_AND_POSTFIX);

	/**
	 * Specifies whether string based conditions are case-sensitive by default
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: false
	 * </ul>
	 */
	PropertyValue<Boolean> CASE_SENSITIVE =
					booleanValue(ConditionModel.class.getName() + ".caseSensitive", false);

	/**
	 * The possible wildcard types
	 */
	enum Wildcard {
		/**
		 * No wildcard
		 */
		NONE,
		/**
		 * Wildard added at front
		 */
		PREFIX,
		/**
		 * Wildcard added at end
		 */
		POSTFIX,
		/**
		 * Wildcard added at front and at end
		 */
		PREFIX_AND_POSTFIX;

		private final String description;

		Wildcard() {
			this.description = messageBundle(Wildcard.class, getBundle(Wildcard.class.getName())).getString(this.toString());
		}

		/**
		 * @return a description
		 */
		public String description() {
			return description;
		}
	}

	/**
	 * @return the {@link State} controlling whether this model is case-sensitive, when working with strings
	 */
	State caseSensitive();

	/**
	 * @return the {@link Format} to use when presenting a value, an empty {@link Optional} in case none is available
	 */
	Optional<Format> format();

	/**
	 * @return the date/time format pattern to use when presenting a temporal value, an empty {@link Optional} in case none is available
	 */
	Optional<String> dateTimePattern();

	/**
	 * @return the {@link State} controlling whether this model is enabled automatically when a condition value is specified
	 */
	State autoEnable();

	/**
	 * @return the {@link State} controlling the locked status
	 */
	State locked();

	/**
	 * @return the value class this condition model is based on
	 */
	Class<T> valueClass();

	/**
	 * @return the operators available in this condition model
	 */
	List<Operator> operators();

	/**
	 * @return a state controlling the enabled status
	 */
	State enabled();

	/**
	 * Clears this condition model, that is, disables it, clears all operands and sets the operator to the initial one.
	 * @see #operators()
	 * @see Builder#operator(Operator)
	 */
	void clear();

	/**
	 * @return a {@link Value} controlling on the operator
	 */
	Value<Operator> operator();

	/**
	 * @return the operands
	 */
	Operands<T> operands();

	/**
	 * @return the {@link SetCondition} instance
	 */
	SetCondition<T> set();

	/**
	 * Returns true if the given value is accepted by this models condition.
	 * @param value the value
	 * @return true if the given value is accepted by this models condition
	 */
	boolean accepts(@Nullable Comparable<T> value);

	/**
	 * @return an observer notified each time the condition changes
	 */
	Observer<?> changed();

	/**
	 * Returns a new {@link Builder} instance.
	 * @param valueClass the value class
	 * @param <T> the condition value type
	 * @return a new {@link Builder} instance
	 */
	static <T> Builder<T> builder(Class<T> valueClass) {
		return new DefaultConditionModel.DefaultBuilder<>(valueClass);
	}

	/**
	 * Provides condition operands.
	 * @param <T> the value type
	 */
	interface Operands<T> {

		/**
		 * @return the {@link Value} controlling how wildcards are added to a String {@link #equal()} operand when accessed via {@link #equalWildcards()}
		 */
		default Value<Wildcard> wildcard() {
			return Value.builder()
							.nonNull(WILDCARD.getOrThrow())
							.build();
		}

		/**
		 * @return the {@link #equal()} operand, assuming it is a String, with wildcards added
		 * @see #wildcard()
		 */
		default @Nullable String equalWildcards() {
			String equalOperand = (String) equal().get();
			if (equalOperand != null) {
				return addWildcard(equalOperand, wildcard().getOrThrow());
			}

			return null;
		}

		/**
		 * @return a {@link Value} controlling the operand used for the {@link Operator#EQUAL} and {@link Operator#NOT_EQUAL} operators
		 */
		default Value<T> equal() {
			return Value.builder()
							.<T>nullable()
							.notify(Notify.SET)
							.build();
		}

		/**
		 * @return a {@link Value} controlling the upper bound operand used for range based operators
		 */
		default Value<T> upper() {
			return Value.builder()
							.<T>nullable()
							.notify(Notify.SET)
							.build();
		}

		/**
		 * @return a {@link Value} controlling the lower bound operand used for range based operators
		 */
		default Value<T> lower() {
			return Value.builder()
							.<T>nullable()
							.notify(Notify.SET)
							.build();
		}

		/**
		 * @return a {@link Value} controlling the operands used for the {@link Operator#IN} and {@link Operator#NOT_IN} operator
		 */
		default ValueSet<T> in() {
			return ValueSet.<T>builder()
							.notify(Notify.SET)
							.build();
		}
	}

	/**
	 * Provides a way to set the condition.
	 * @param <T> the value type
	 */
	interface SetCondition<T> {

		/**
		 * <p>Sets the operator to {@link Operator#EQUAL}, the operand to {@code null} and enables this condition.
		 * @return true if the condition state changed
		 */
		boolean isNull();

		/**
		 * <p>Sets the operator to {@link Operator#NOT_EQUAL}, the operand to {@code null} and enables this condition.
		 * @return true if the condition state changed
		 */
		boolean isNotNull();

		/**
		 * <p>Sets the operator to {@link Operator#EQUAL} and the operand to {@code value}.
		 * <p>Enables the condition if {@code value} is non-null, otherwise disables it.
		 * @param value the operand
		 * @return true if the condition state changed
		 */
		boolean equalTo(@Nullable T value);

		/**
		 * <p>Sets the operator to {@link Operator#NOT_EQUAL} and the operand to {@code value}.
		 * <p>Enables the condition if {@code value} is non-null, otherwise disables it.
		 * @param value the operand
		 * @return true if the condition state changed
		 */
		boolean notEqualTo(@Nullable T value);

		/**
		 * <p>Sets the operator to {@link Operator#GREATER_THAN} and the operand to {@code value}.
		 * <p>Enables the condition if {@code value} is non-null, otherwise disables it.
		 * @param value the operand
		 * @return true if the condition state changed
		 */
		boolean greaterThan(@Nullable T value);

		/**
		 * <p>Sets the operator to {@link Operator#GREATER_THAN_OR_EQUAL} and the operand to {@code value}.
		 * <p>Enables the condition if {@code value} is non-null, otherwise disables it.
		 * @param value the operand
		 * @return true if the condition state changed
		 */
		boolean greaterThanOrEqualTo(@Nullable T value);

		/**
		 * <p>Sets the operator to {@link Operator#LESS_THAN} and the operand to {@code value}.
		 * <p>Enables the condition if {@code value} is non-null, otherwise disables it.
		 * @param value the operand
		 * @return true if the condition state changed
		 */
		boolean lessThan(@Nullable T value);

		/**
		 * <p>Sets the operator to {@link Operator#LESS_THAN_OR_EQUAL} and the operand to {@code value}.
		 * <p>Enables the condition if {@code value} is non-null, otherwise disables it.
		 * @param value the operand
		 * @return true if the condition state changed
		 */
		boolean lessThanOrEqualTo(@Nullable T value);

		/**
		 * <p>Sets the operator to {@link Operator#IN} and the operands to {@code values}.
		 * <p>Enables the condition if {@code values} is not empty, otherwise disables it.
		 * @param values the operands
		 * @return true if the condition state changed
		 */
		boolean in(T... values);

		/**
		 * <p>Sets the operator to {@link Operator#IN} and the operands to {@code values}.
		 * <p>Enables the condition if {@code values} is not empty, otherwise disables it.
		 * @param values the operands
		 * @return true if the condition state changed
		 */
		boolean in(Collection<T> values);

		/**
		 * <p>Sets the operator to {@link Operator#IN} and the operands to {@code values}.
		 * <p>Enables the condition if {@code values} is not empty, otherwise disables it.
		 * @param values the operands
		 * @return true if the condition state changed
		 */
		boolean notIn(T... values);

		/**
		 * <p>Sets the operator to {@link Operator#NOT_IN} and the operands to {@code values}.
		 * <p>Enables the condition if {@code values} is not empty, otherwise disables it.
		 * @param values the operands
		 * @return true if the condition state changed
		 */
		boolean notIn(Collection<T> values);

		/**
		 * <p>Sets the operator to {@link Operator#BETWEEN_EXCLUSIVE} and the operands to {@code lower} and {@code upper}.
		 * <p>Enables the condition if both operands are non-null, otherwise disables it.
		 * @param lower the lower bound
		 * @param upper the upper bound
		 * @return true if the condition state changed
		 */
		boolean betweenExclusive(@Nullable T lower, @Nullable T upper);

		/**
		 * <p>Sets the operator to {@link Operator#NOT_BETWEEN_EXCLUSIVE} and the operands to {@code lower} and {@code upper}.
		 * <p>Enables the condition if both operands are non-null, otherwise disables it.
		 * @param lower the lower bound
		 * @param upper the upper bound
		 * @return true if the condition state changed
		 */
		boolean notBetweenExclusive(@Nullable T lower, @Nullable T upper);

		/**
		 * <p>Sets the operator to {@link Operator#BETWEEN} and the operands to {@code lower} and {@code upper}.
		 * <p>Enables the condition if both operands are non-null, otherwise disables it.
		 * @param lower the lower bound
		 * @param upper the upper bound
		 * @return true if the condition state changed
		 */
		boolean between(@Nullable T lower, @Nullable T upper);

		/**
		 * <p>Sets the operator to {@link Operator#NOT_BETWEEN} and the operands to {@code lower} and {@code upper}.
		 * <p>Enables the condition if both operands are non-null, otherwise disables it.
		 * @param lower the lower bound
		 * @param upper the upper bound
		 * @return true if the condition state changed
		 */
		boolean notBetween(@Nullable T lower, @Nullable T upper);
	}

	/**
	 * Builds a {@link ConditionModel} instance.
	 */
	interface Builder<T> {

		/**
		 * @param operators the conditional operators available to this condition model
		 * @return this builder instance
		 * @throws IllegalArgumentException in case operators don't contain the selected operator
		 * @see #operator(Operator)
		 */
		Builder<T> operators(List<Operator> operators);

		/**
		 * @param operator the initial operator
		 * @return this builder instance
		 * @throws IllegalArgumentException in case the model operators don't contain the given operator
		 * @see #operators(List)
		 */
		Builder<T> operator(Operator operator);

		/**
		 * @param operands provides the operands
		 * @return this builder instance
		 */
		Builder<T> operands(Operands<T> operands);

		/**
		 * @param format the format to use when presenting the values, numbers for example
		 * @return this builder instance
		 */
		Builder<T> format(@Nullable Format format);

		/**
		 * @param dateTimePattern the date/time format pattern to use in case of a date/time value
		 * @return this builder instance
		 */
		Builder<T> dateTimePattern(@Nullable String dateTimePattern);

		/**
		 * @param caseSensitive true if the model should be case-sensitive
		 * @return this builder instance
		 */
		Builder<T> caseSensitive(boolean caseSensitive);

		/**
		 * @param autoEnable true if the model should auto-enable
		 * @return this builder instance
		 */
		Builder<T> autoEnable(boolean autoEnable);

		/**
		 * @return a new {@link ConditionModel} instance based on this builder
		 */
		ConditionModel<T> build();
	}
}
