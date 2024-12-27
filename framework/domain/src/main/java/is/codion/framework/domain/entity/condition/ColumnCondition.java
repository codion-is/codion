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
 * Copyright (c) 2019 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity.condition;

import is.codion.common.Operator;
import is.codion.framework.domain.entity.attribute.Column;

import java.util.Collection;

/**
 * A condition based on a single {@link Column}.
 * @param <T> the attribute type
 */
public interface ColumnCondition<T> extends Condition {

	/**
	 * @return the attribute
	 */
	Column<T> column();

	/**
	 * @return the condition operator
	 */
	Operator operator();

	/**
	 * @return true if this condition is case sensitive, only applies to String based conditions
	 */
	boolean caseSensitive();

	/**
	 * Creates {@link ColumnCondition}s.
	 * @param <T> the attribute value type
	 */
	interface Factory<T> {

		/**
		 * Returns a 'equalTo' {@link ColumnCondition} or 'isNull' in case {@code value} is null.
		 * @param value the value to use in the condition
		 * @return a {@link ColumnCondition}
		 */
		ColumnCondition<T> equalTo(T value);

		/**
		 * Returns a 'equalTo' {@link ColumnCondition} or 'isNotNull' in case {@code value} is null.
		 * @param value the value to use in the condition
		 * @return a {@link ColumnCondition}
		 */
		ColumnCondition<T> notEqualTo(T value);

		/**
		 * Returns a case-insensitive 'equalTo' {@link ColumnCondition} or 'isNull' in case {@code value} is null.
		 * @param value the value to use in the condition
		 * @return a {@link ColumnCondition}
		 */
		ColumnCondition<String> equalToIgnoreCase(String value);

		/**
		 * Returns a case-insensitive 'equalTo' {@link ColumnCondition} or 'isNull' in case {@code value} is null.
		 * @param value the value to use in the condition
		 * @return a {@link ColumnCondition}
		 */
		ColumnCondition<Character> equalToIgnoreCase(Character value);

		/**
		 * Returns a case-insensitive 'notEqualTo' {@link ColumnCondition} or 'isNotNull' in case {@code value} is null.
		 * @param value the value to use in the condition
		 * @return a {@link ColumnCondition}
		 */
		ColumnCondition<String> notEqualToIgnoreCase(String value);

		/**
		 * Returns a case-insensitive 'notEqualTo' {@link ColumnCondition} or 'isNotNull' in case {@code value} is null.
		 * @param value the value to use in the condition
		 * @return a {@link ColumnCondition}
		 */
		ColumnCondition<Character> notEqualToIgnoreCase(Character value);

		/**
		 * Returns a 'like' {@link ColumnCondition} or 'isNull' in case {@code value} is null.
		 * @param value the value to use in the condition
		 * @return a {@link ColumnCondition}
		 */
		ColumnCondition<String> like(String value);

		/**
		 * Returns a 'like' {@link ColumnCondition} or 'isNotNull' in case {@code value} is null.
		 * @param value the value to use in the condition
		 * @return a {@link ColumnCondition}
		 */
		ColumnCondition<String> notLike(String value);

		/**
		 * Returns a case-insensitive 'like' {@link ColumnCondition} or 'isNull' in case {@code value} is null.
		 * @param value the value to use in the condition
		 * @return a {@link ColumnCondition}
		 */
		ColumnCondition<String> likeIgnoreCase(String value);

		/**
		 * Returns a case-insensitive 'notLike' {@link ColumnCondition} or 'isNotNull' in case {@code value} is null.
		 * @param value the value to use in the condition
		 * @return a {@link ColumnCondition}
		 */
		ColumnCondition<String> notLikeIgnoreCase(String value);

		/**
		 * Returns a 'in' {@link ColumnCondition}.
		 * @param values the values to use in the condition
		 * @return a {@link ColumnCondition}
		 * @throws NullPointerException in case {@code values} is null
		 */
		ColumnCondition<T> in(T... values);

		/**
		 * Returns a 'notIn' {@link ColumnCondition}.
		 * @param values the values to use in the condition
		 * @return a {@link ColumnCondition}
		 * @throws NullPointerException in case {@code values} is null
		 */
		ColumnCondition<T> notIn(T... values);

		/**
		 * Returns a 'in' {@link ColumnCondition}.
		 * @param values the values to use in the condition
		 * @return a {@link ColumnCondition}
		 * @throws NullPointerException in case {@code values} is null
		 */
		ColumnCondition<T> in(Collection<? extends T> values);

		/**
		 * Returns a 'notIn' {@link ColumnCondition}.
		 * @param values the values to use in the condition
		 * @return a {@link ColumnCondition}
		 * @throws NullPointerException in case {@code values} is null
		 */
		ColumnCondition<T> notIn(Collection<? extends T> values);

		/**
		 * Returns a case-insensitive 'in' {@link ColumnCondition}.
		 * @param values the values to use in the condition
		 * @return a {@link ColumnCondition}
		 * @throws NullPointerException in case {@code values} is null
		 */
		ColumnCondition<String> inIgnoreCase(String... values);

		/**
		 * Returns a case-insensitive 'notIn' {@link ColumnCondition}.
		 * @param values the values to use in the condition
		 * @return a {@link ColumnCondition}
		 * @throws NullPointerException in case {@code values} is null
		 */
		ColumnCondition<String> notInIgnoreCase(String... values);

		/**
		 * Returns a case-insensitive 'in' {@link ColumnCondition}.
		 * @param values the values to use in the condition
		 * @return a {@link ColumnCondition}
		 * @throws NullPointerException in case {@code values} is null
		 */
		ColumnCondition<String> inIgnoreCase(Collection<String> values);

		/**
		 * Returns a case-insensitive 'notIn' {@link ColumnCondition}.
		 * @param values the values to use in the condition
		 * @return a {@link ColumnCondition}
		 * @throws NullPointerException in case {@code values} is null
		 */
		ColumnCondition<String> notInIgnoreCase(Collection<String> values);

		/**
		 * Returns a 'lessThan' {@link ColumnCondition}.
		 * @param value the value to use in the condition
		 * @return a {@link ColumnCondition}
		 */
		ColumnCondition<T> lessThan(T value);

		/**
		 * Returns a 'lessThanOrEqualTo' {@link ColumnCondition}.
		 * @param value the value to use in the condition
		 * @return a {@link ColumnCondition}
		 */
		ColumnCondition<T> lessThanOrEqualTo(T value);

		/**
		 * Returns a 'greaterThan' {@link ColumnCondition}.
		 * @param value the value to use in the condition
		 * @return a {@link ColumnCondition}
		 */
		ColumnCondition<T> greaterThan(T value);

		/**
		 * Returns a 'greaterThanOrEqualTo' {@link ColumnCondition}.
		 * @param value the value to use in the condition
		 * @return a {@link ColumnCondition}
		 */
		ColumnCondition<T> greaterThanOrEqualTo(T value);

		/**
		 * Returns a 'betweenExclusive' {@link ColumnCondition}.
		 * @param lower the lower bound
		 * @param upper the upper bound
		 * @return a {@link ColumnCondition}
		 */
		ColumnCondition<T> betweenExclusive(T lower, T upper);

		/**
		 * Returns a 'between' {@link ColumnCondition}.
		 * @param lower the lower bound
		 * @param upper the upper bound
		 * @return a {@link ColumnCondition}
		 */
		ColumnCondition<T> between(T lower, T upper);

		/**
		 * Returns a 'notBetweenExclusive' {@link ColumnCondition}.
		 * @param lower the lower bound
		 * @param upper the upper bound
		 * @return a {@link ColumnCondition}
		 */
		ColumnCondition<T> notBetweenExclusive(T lower, T upper);

		/**
		 * Returns a 'notBetween' {@link ColumnCondition}.
		 * @param lower the lower bound
		 * @param upper the upper bound
		 * @return a {@link ColumnCondition}
		 */
		ColumnCondition<T> notBetween(T lower, T upper);

		/**
		 * Returns a 'isNull' {@link ColumnCondition}.
		 * @return a {@link ColumnCondition}
		 */
		ColumnCondition<T> isNull();

		/**
		 * Returns a 'isNotNull' {@link ColumnCondition}.
		 * @return a {@link ColumnCondition}
		 */
		ColumnCondition<T> isNotNull();
	}

	/**
	 * Instantiates a new {@link Factory} instance
	 * @param column the column
	 * @param <T> the column type
	 * @return a new {@link Factory} instance
	 */
	static <T> ColumnCondition.Factory<T> factory(Column<T> column) {
		return new DefaultColumnConditionFactory<>(column);
	}
}
