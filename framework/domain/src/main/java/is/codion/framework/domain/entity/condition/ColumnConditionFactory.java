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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity.condition;

import is.codion.framework.domain.entity.attribute.Column;

import org.jspecify.annotations.Nullable;

import java.util.Collection;

/**
 * Creates {@link ColumnCondition}s.
 * @param <T> the attribute value type
 */
public sealed interface ColumnConditionFactory<T> permits DefaultColumnConditionFactory, Column {

	/**
	 * Returns a 'equalTo' {@link ColumnCondition} or 'isNull' in case {@code value} is null.
	 * @param value the value to use in the condition
	 * @return a {@link ColumnCondition}
	 */
	ColumnCondition<T> equalTo(@Nullable T value);

	/**
	 * Returns a 'equalTo' {@link ColumnCondition} or 'isNotNull' in case {@code value} is null.
	 * @param value the value to use in the condition
	 * @return a {@link ColumnCondition}
	 */
	ColumnCondition<T> notEqualTo(@Nullable T value);

	/**
	 * Returns a case-insensitive 'equalTo' {@link ColumnCondition} or 'isNull' in case {@code value} is null.
	 * @param value the value to use in the condition
	 * @return a {@link ColumnCondition}
	 */
	ColumnCondition<String> equalToIgnoreCase(@Nullable String value);

	/**
	 * Returns a case-insensitive 'equalTo' {@link ColumnCondition} or 'isNull' in case {@code value} is null.
	 * @param value the value to use in the condition
	 * @return a {@link ColumnCondition}
	 */
	ColumnCondition<Character> equalToIgnoreCase(@Nullable Character value);

	/**
	 * Returns a case-insensitive 'notEqualTo' {@link ColumnCondition} or 'isNotNull' in case {@code value} is null.
	 * @param value the value to use in the condition
	 * @return a {@link ColumnCondition}
	 */
	ColumnCondition<String> notEqualToIgnoreCase(@Nullable String value);

	/**
	 * Returns a case-insensitive 'notEqualTo' {@link ColumnCondition} or 'isNotNull' in case {@code value} is null.
	 * @param value the value to use in the condition
	 * @return a {@link ColumnCondition}
	 */
	ColumnCondition<Character> notEqualToIgnoreCase(@Nullable Character value);

	/**
	 * Returns a 'like' {@link ColumnCondition} or 'isNull' in case {@code value} is null.
	 * @param value the value to use in the condition
	 * @return a {@link ColumnCondition}
	 */
	ColumnCondition<String> like(@Nullable String value);

	/**
	 * Returns a 'like' {@link ColumnCondition} or 'isNotNull' in case {@code value} is null.
	 * @param value the value to use in the condition
	 * @return a {@link ColumnCondition}
	 */
	ColumnCondition<String> notLike(@Nullable String value);

	/**
	 * Returns a case-insensitive 'like' {@link ColumnCondition} or 'isNull' in case {@code value} is null.
	 * @param value the value to use in the condition
	 * @return a {@link ColumnCondition}
	 */
	ColumnCondition<String> likeIgnoreCase(@Nullable String value);

	/**
	 * Returns a case-insensitive 'notLike' {@link ColumnCondition} or 'isNotNull' in case {@code value} is null.
	 * @param value the value to use in the condition
	 * @return a {@link ColumnCondition}
	 */
	ColumnCondition<String> notLikeIgnoreCase(@Nullable String value);

	/**
	 * Returns an 'in' {@link ColumnCondition}.
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
	 * Returns an 'in' {@link ColumnCondition}.
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
	 * @param upper the upper bound
	 * @return a {@link ColumnCondition}
	 * @throws IllegalArgumentException in case {@code upper} is null
	 */
	ColumnCondition<T> lessThan(T upper);

	/**
	 * Returns a 'lessThanOrEqualTo' {@link ColumnCondition}.
	 * @param upper the upper bound
	 * @return a {@link ColumnCondition}
	 * @throws IllegalArgumentException in case {@code upper} is null
	 */
	ColumnCondition<T> lessThanOrEqualTo(T upper);

	/**
	 * Returns a 'greaterThan' {@link ColumnCondition}.
	 * @param lower the lower bound
	 * @return a {@link ColumnCondition}
	 * @throws IllegalArgumentException in case {@code lower} is null
	 */
	ColumnCondition<T> greaterThan(T lower);

	/**
	 * Returns a 'greaterThanOrEqualTo' {@link ColumnCondition}.
	 * @param lower the lower bound
	 * @return a {@link ColumnCondition}
	 * @throws IllegalArgumentException in case {@code lower} is null
	 */
	ColumnCondition<T> greaterThanOrEqualTo(T lower);

	/**
	 * Returns a 'betweenExclusive' {@link ColumnCondition} if both bound values are non-null, 'greaterThan' if the upper bound is null and 'lessThan' if the lower bound is null.
	 * @param lower the lower bound
	 * @param upper the upper bound
	 * @return a {@link ColumnCondition}
	 * @throws IllegalArgumentException in case either {@code lower} or {@code upper} is null
	 */
	ColumnCondition<T> betweenExclusive(T lower, T upper);

	/**
	 * Returns a 'between' {@link ColumnCondition}.
	 * @param lower the lower bound
	 * @param upper the upper bound
	 * @return a {@link ColumnCondition}
	 * @throws IllegalArgumentException in case either {@code lower} or {@code upper} is null
	 */
	ColumnCondition<T> between(T lower, T upper);

	/**
	 * Returns a 'notBetweenExclusive' {@link ColumnCondition}.
	 * @param lower the lower bound
	 * @param upper the upper bound
	 * @return a {@link ColumnCondition}
	 * @throws IllegalArgumentException in case either {@code lower} or {@code upper} is null
	 */
	ColumnCondition<T> notBetweenExclusive(T lower, T upper);

	/**
	 * Returns a 'notBetween' {@link ColumnCondition}.
	 * @param lower the lower bound
	 * @param upper the upper bound
	 * @return a {@link ColumnCondition}
	 * @throws IllegalArgumentException in case either {@code lower} or {@code upper} is null
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

	/**
	 * Instantiates a new {@link ColumnConditionFactory} instance
	 * @param column the column
	 * @param <T> the column type
	 * @return a new {@link ColumnConditionFactory} instance
	 */
	static <T> ColumnConditionFactory<T> factory(Column<T> column) {
		return new DefaultColumnConditionFactory<>(column);
	}
}
