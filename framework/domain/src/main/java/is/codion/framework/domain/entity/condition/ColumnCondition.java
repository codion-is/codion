/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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
     * Returns a 'equalTo' {@link ColumnCondition} or 'isNull' in case the value is null.
     * @param value the value to use in the condition
     * @return a {@link ColumnCondition}
     */
    ColumnCondition<T> equalTo(T value);

    /**
     * Returns a 'equalTo' {@link ColumnCondition} or 'isNotNull' in case the value is null.
     * @param value the value to use in the condition
     * @return a {@link ColumnCondition}
     */
    ColumnCondition<T> notEqualTo(T value);

    /**
     * Returns a case-insensitive 'equalTo' {@link ColumnCondition} or 'isNull' in case the value is null.
     * @param value the value to use in the condition
     * @return a {@link ColumnCondition}
     */
    ColumnCondition<String> equalToIgnoreCase(String value);

    /**
     * Returns a case-insensitive 'equalTo' {@link ColumnCondition} or 'isNull' in case the value is null.
     * @param value the value to use in the condition
     * @return a {@link ColumnCondition}
     */
    ColumnCondition<Character> equalToIgnoreCase(Character value);

    /**
     * Returns a case-insensitive 'notEqualTo' {@link ColumnCondition} or 'isNotNull' in case the value is null.
     * @param value the value to use in the condition
     * @return a {@link ColumnCondition}
     */
    ColumnCondition<String> notEqualToIgnoreCase(String value);

    /**
     * Returns a case-insensitive 'notEqualTo' {@link ColumnCondition} or 'isNotNull' in case the value is null.
     * @param value the value to use in the condition
     * @return a {@link ColumnCondition}
     */
    ColumnCondition<Character> notEqualToIgnoreCase(Character value);

    /**
     * Returns a 'like' {@link ColumnCondition} or 'isNull' in case the value is null.
     * @param value the value to use in the condition
     * @return a {@link ColumnCondition}
     */
    ColumnCondition<String> like(String value);

    /**
     * Returns a 'like' {@link ColumnCondition} or 'isNotNull' in case the value is null.
     * @param value the value to use in the condition
     * @return a {@link ColumnCondition}
     */
    ColumnCondition<String> notLike(String value);

    /**
     * Returns a case-insensitive 'like' {@link ColumnCondition} or 'isNull' in case the value is null.
     * @param value the value to use in the condition
     * @return a {@link ColumnCondition}
     */
    ColumnCondition<String> likeIgnoreCase(String value);

    /**
     * Returns a case-insensitive 'notLike' {@link ColumnCondition} or 'isNotNull' in case the value is null.
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
     * @param lowerBound the lower bound
     * @param upperBound the upper bound
     * @return a {@link ColumnCondition}
     */
    ColumnCondition<T> betweenExclusive(T lowerBound, T upperBound);

    /**
     * Returns a 'between' {@link ColumnCondition}.
     * @param lowerBound the lower bound
     * @param upperBound the upper bound
     * @return a {@link ColumnCondition}
     */
    ColumnCondition<T> between(T lowerBound, T upperBound);

    /**
     * Returns a 'notBetweenExclusive' {@link ColumnCondition}.
     * @param lowerBound the lower bound
     * @param upperBound the upper bound
     * @return a {@link ColumnCondition}
     */
    ColumnCondition<T> notBetweenExclusive(T lowerBound, T upperBound);

    /**
     * Returns a 'notBetween' {@link ColumnCondition}.
     * @param lowerBound the lower bound
     * @param upperBound the upper bound
     * @return a {@link ColumnCondition}
     */
    ColumnCondition<T> notBetween(T lowerBound, T upperBound);

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
   * @return a new {@link Factory} instance
   * @param <T> the column type
   */
  static <T> ColumnCondition.Factory<T> factory(Column<T> column) {
    return new DefaultColumnConditionFactory<>(column);
  }
}
