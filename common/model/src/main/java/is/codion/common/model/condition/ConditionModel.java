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
 * Copyright (c) 2010 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.model.condition;

import is.codion.common.Configuration;
import is.codion.common.Operator;
import is.codion.common.observer.Mutable;
import is.codion.common.observer.Observer;
import is.codion.common.property.PropertyValue;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.common.value.ValueSet;

import java.text.Format;
import java.util.List;
import java.util.Optional;

import static is.codion.common.resource.MessageBundle.messageBundle;
import static java.util.ResourceBundle.getBundle;

/**
 * Specifies a condition with an operator and operands as well as relevant events and states.
 * For instances create a {@link Builder} via {@link #builder(Class)}.
 * @param <T> the condition value type
 */
public interface ConditionModel<T> {

	/**
	 * Specifies whether wildcards are added to string values
	 * <ul>
	 * <li>Value type: {@link Wildcard}
	 * <li>Default value: {@link Wildcard#POSTFIX}
	 * </ul>
	 */
	PropertyValue<Wildcard> WILDCARD =
					Configuration.enumValue(ConditionModel.class.getName() + ".wildard",
									Wildcard.class, Wildcard.PREFIX_AND_POSTFIX);

	/**
	 * Specifies whether string based conditions are case-sensitive by default
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: false
	 * </ul>
	 */
	PropertyValue<Boolean> CASE_SENSITIVE =
					Configuration.booleanValue(ConditionModel.class.getName() + ".caseSensitive", false);

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
	 * @return the Format object to use when formatting input, an empty Optional in case none is available
	 */
	Optional<Format> format();

	/**
	 * @return the date/time format pattern, if any
	 */
	String dateTimePattern();

	/**
	 * Note that this is only applicable to string based condition models and only used for
	 * operators {@link Operator#EQUAL} and {@link Operator#NOT_EQUAL}
	 * @return the {@link Mutable} controlling whether wildcards are added to strings
	 */
	Mutable<Wildcard> wildcard();

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
	 * Clears this condition model, that is, clears all operands and sets the operator to the initial one.
	 * @see #autoEnable()
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
	 * Returns true if the given value is accepted by this models condition.
	 * @param value the value
	 * @return true if the given value is accepted by this models condition
	 */
	boolean accepts(Comparable<T> value);

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
	 * Provides access to the operands.
	 * @param <T> the value type
	 */
	interface Operands<T> {

		/**
		 * @return a {@link Value} controlling the operand used for the {@link Operator#EQUAL} and {@link Operator#NOT_EQUAL} operators
		 */
		Value<T> equal();

		/**
		 * @return a {@link Value} controlling the operands used for the {@link Operator#IN} and {@link Operator#NOT_IN} operator
		 */
		ValueSet<T> in();

		/**
		 * @return a {@link Value} controlling the upper bound operand used for range based operators
		 */
		Value<T> upperBound();

		/**
		 * @return a {@link Value} controlling the lower bound operand used for range based operators
		 */
		Value<T> lowerBound();
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
		 * @param format the format to use when presenting the values, numbers for example
		 * @return this builder instance
		 */
		Builder<T> format(Format format);

		/**
		 * @param dateTimePattern the date/time format pattern to use in case of a date/time value
		 * @return this builder instance
		 */
		Builder<T> dateTimePattern(String dateTimePattern);

		/**
		 * @param wildcard the wildcards to use
		 * @return this builder instance
		 */
		Builder<T> wildcard(Wildcard wildcard);

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
