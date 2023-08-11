/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.criteria;

import is.codion.common.Operator;
import is.codion.framework.domain.entity.Column;

import java.util.Collection;

/**
 * A criteria based on a single {@link Column}.
 * @param <T> the attribute type
 */
public interface ColumnCriteria<T> extends Criteria {

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
   * A builder for {@link ColumnCriteria}.
   * @param <T> the attribute value type
   */
  interface Builder<T> {

    /**
     * Returns a 'equalTo' {@link ColumnCriteria} or 'isNull' in case the value is null.
     * @param value the value to use in the condition
     * @return a {@link ColumnCriteria}
     */
    ColumnCriteria<T> equalTo(T value);

    /**
     * Returns a 'equalTo' {@link ColumnCriteria} or 'isNotNull' in case the value is null.
     * @param value the value to use in the condition
     * @return a {@link ColumnCriteria}
     */
    ColumnCriteria<T> notEqualTo(T value);

    /**
     * Returns a case-insensitive 'equalTo' {@link ColumnCriteria} or 'isNull' in case the value is null.
     * @param value the value to use in the condition
     * @return a {@link ColumnCriteria}
     */
    ColumnCriteria<String> equalToIgnoreCase(String value);

    /**
     * Returns a case-insensitive 'notEqualTo' {@link ColumnCriteria} or 'isNotNull' in case the value is null.
     * @param value the value to use in the condition
     * @return a {@link ColumnCriteria}
     */
    ColumnCriteria<String> notEqualToIgnoreCase(String value);

    /**
     * Returns a 'in' {@link ColumnCriteria}.
     * @param values the values to use in the condition
     * @return a {@link ColumnCriteria}
     * @throws NullPointerException in case {@code values} is null
     */
    ColumnCriteria<T> in(T... values);

    /**
     * Returns a 'notIn' {@link ColumnCriteria}.
     * @param values the values to use in the condition
     * @return a {@link ColumnCriteria}
     * @throws NullPointerException in case {@code values} is null
     */
    ColumnCriteria<T> notIn(T... values);

    /**
     * Returns a 'in' {@link ColumnCriteria}.
     * @param values the values to use in the condition
     * @return a {@link ColumnCriteria}
     * @throws NullPointerException in case {@code values} is null
     */
    ColumnCriteria<T> in(Collection<? extends T> values);

    /**
     * Returns a 'notIn' {@link ColumnCriteria}.
     * @param values the values to use in the condition
     * @return a {@link ColumnCriteria}
     * @throws NullPointerException in case {@code values} is null
     */
    ColumnCriteria<T> notIn(Collection<? extends T> values);

    /**
     * Returns a case-insensitive 'in' {@link ColumnCriteria}.
     * @param values the values to use in the condition
     * @return a {@link ColumnCriteria}
     * @throws NullPointerException in case {@code values} is null
     */
    ColumnCriteria<String> inIgnoreCase(String... values);

    /**
     * Returns a case-insensitive 'notIn' {@link ColumnCriteria}.
     * @param values the values to use in the condition
     * @return a {@link ColumnCriteria}
     * @throws NullPointerException in case {@code values} is null
     */
    ColumnCriteria<String> notInIgnoreCase(String... values);

    /**
     * Returns a case-insensitive 'in' {@link ColumnCriteria}.
     * @param values the values to use in the condition
     * @return a {@link ColumnCriteria}
     * @throws NullPointerException in case {@code values} is null
     */
    ColumnCriteria<String> inIgnoreCase(Collection<String> values);

    /**
     * Returns a case-insensitive 'notIn' {@link ColumnCriteria}.
     * @param values the values to use in the condition
     * @return a {@link ColumnCriteria}
     * @throws NullPointerException in case {@code values} is null
     */
    ColumnCriteria<String> notInIgnoreCase(Collection<String> values);

    /**
     * Returns a 'lessThan' {@link ColumnCriteria}.
     * @param value the value to use in the condition
     * @return a {@link ColumnCriteria}
     */
    ColumnCriteria<T> lessThan(T value);

    /**
     * Returns a 'lessThanOrEqualTo' {@link ColumnCriteria}.
     * @param value the value to use in the condition
     * @return a {@link ColumnCriteria}
     */
    ColumnCriteria<T> lessThanOrEqualTo(T value);

    /**
     * Returns a 'greaterThan' {@link ColumnCriteria}.
     * @param value the value to use in the condition
     * @return a {@link ColumnCriteria}
     */
    ColumnCriteria<T> greaterThan(T value);

    /**
     * Returns a 'greaterThanOrEqualTo' {@link ColumnCriteria}.
     * @param value the value to use in the condition
     * @return a {@link ColumnCriteria}
     */
    ColumnCriteria<T> greaterThanOrEqualTo(T value);

    /**
     * Returns a 'betweenExclusive' {@link ColumnCriteria}.
     * @param lowerBound the lower bound
     * @param upperBound the upper bound
     * @return a {@link ColumnCriteria}
     */
    ColumnCriteria<T> betweenExclusive(T lowerBound, T upperBound);

    /**
     * Returns a 'between' {@link ColumnCriteria}.
     * @param lowerBound the lower bound
     * @param upperBound the upper bound
     * @return a {@link ColumnCriteria}
     */
    ColumnCriteria<T> between(T lowerBound, T upperBound);

    /**
     * Returns a 'notBetweenExclusive' {@link ColumnCriteria}.
     * @param lowerBound the lower bound
     * @param upperBound the upper bound
     * @return a {@link ColumnCriteria}
     */
    ColumnCriteria<T> notBetweenExclusive(T lowerBound, T upperBound);

    /**
     * Returns a 'notBetween' {@link ColumnCriteria}.
     * @param lowerBound the lower bound
     * @param upperBound the upper bound
     * @return a {@link ColumnCriteria}
     */
    ColumnCriteria<T> notBetween(T lowerBound, T upperBound);

    /**
     * Returns a 'isNull' {@link ColumnCriteria}.
     * @return a {@link ColumnCriteria}
     */
    ColumnCriteria<T> isNull();

    /**
     * Returns a 'isNotNull' {@link ColumnCriteria}.
     * @return a {@link ColumnCriteria}
     */
    ColumnCriteria<T> isNotNull();
  }
}
