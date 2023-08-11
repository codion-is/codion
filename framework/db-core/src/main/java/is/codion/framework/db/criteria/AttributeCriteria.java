/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.criteria;

import is.codion.common.Operator;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Column;

import java.util.Collection;

/**
 * A criteria based on a single {@link Attribute}.
 * @param <T> the attribute type
 */
public interface AttributeCriteria<T> extends Criteria {

  /**
   * @return the attribute
   */
  Column<T> attribute();

  /**
   * @return the condition operator
   */
  Operator operator();

  /**
   * @return true if this condition is case sensitive, only applies to String based conditions
   */
  boolean caseSensitive();

  /**
   * A builder for {@link AttributeCriteria}.
   * @param <T> the attribute value type
   */
  interface Builder<T> {

    /**
     * Returns a 'equalTo' {@link AttributeCriteria} or 'isNull' in case the value is null.
     * @param value the value to use in the condition
     * @return a {@link AttributeCriteria}
     */
    AttributeCriteria<T> equalTo(T value);

    /**
     * Returns a 'equalTo' {@link AttributeCriteria} or 'isNotNull' in case the value is null.
     * @param value the value to use in the condition
     * @return a {@link AttributeCriteria}
     */
    AttributeCriteria<T> notEqualTo(T value);

    /**
     * Returns a case-insensitive 'equalTo' {@link AttributeCriteria} or 'isNull' in case the value is null.
     * @param value the value to use in the condition
     * @return a {@link AttributeCriteria}
     */
    AttributeCriteria<String> equalToIgnoreCase(String value);

    /**
     * Returns a case-insensitive 'notEqualTo' {@link AttributeCriteria} or 'isNotNull' in case the value is null.
     * @param value the value to use in the condition
     * @return a {@link AttributeCriteria}
     */
    AttributeCriteria<String> notEqualToIgnoreCase(String value);

    /**
     * Returns a 'in' {@link AttributeCriteria}.
     * @param values the values to use in the condition
     * @return a {@link AttributeCriteria}
     * @throws NullPointerException in case {@code values} is null
     */
    AttributeCriteria<T> in(T... values);

    /**
     * Returns a 'notIn' {@link AttributeCriteria}.
     * @param values the values to use in the condition
     * @return a {@link AttributeCriteria}
     * @throws NullPointerException in case {@code values} is null
     */
    AttributeCriteria<T> notIn(T... values);

    /**
     * Returns a 'in' {@link AttributeCriteria}.
     * @param values the values to use in the condition
     * @return a {@link AttributeCriteria}
     * @throws NullPointerException in case {@code values} is null
     */
    AttributeCriteria<T> in(Collection<? extends T> values);

    /**
     * Returns a 'notIn' {@link AttributeCriteria}.
     * @param values the values to use in the condition
     * @return a {@link AttributeCriteria}
     * @throws NullPointerException in case {@code values} is null
     */
    AttributeCriteria<T> notIn(Collection<? extends T> values);

    /**
     * Returns a case-insensitive 'in' {@link AttributeCriteria}.
     * @param values the values to use in the condition
     * @return a {@link AttributeCriteria}
     * @throws NullPointerException in case {@code values} is null
     */
    AttributeCriteria<String> inIgnoreCase(String... values);

    /**
     * Returns a case-insensitive 'notIn' {@link AttributeCriteria}.
     * @param values the values to use in the condition
     * @return a {@link AttributeCriteria}
     * @throws NullPointerException in case {@code values} is null
     */
    AttributeCriteria<String> notInIgnoreCase(String... values);

    /**
     * Returns a case-insensitive 'in' {@link AttributeCriteria}.
     * @param values the values to use in the condition
     * @return a {@link AttributeCriteria}
     * @throws NullPointerException in case {@code values} is null
     */
    AttributeCriteria<String> inIgnoreCase(Collection<String> values);

    /**
     * Returns a case-insensitive 'notIn' {@link AttributeCriteria}.
     * @param values the values to use in the condition
     * @return a {@link AttributeCriteria}
     * @throws NullPointerException in case {@code values} is null
     */
    AttributeCriteria<String> notInIgnoreCase(Collection<String> values);

    /**
     * Returns a 'lessThan' {@link AttributeCriteria}.
     * @param value the value to use in the condition
     * @return a {@link AttributeCriteria}
     */
    AttributeCriteria<T> lessThan(T value);

    /**
     * Returns a 'lessThanOrEqualTo' {@link AttributeCriteria}.
     * @param value the value to use in the condition
     * @return a {@link AttributeCriteria}
     */
    AttributeCriteria<T> lessThanOrEqualTo(T value);

    /**
     * Returns a 'greaterThan' {@link AttributeCriteria}.
     * @param value the value to use in the condition
     * @return a {@link AttributeCriteria}
     */
    AttributeCriteria<T> greaterThan(T value);

    /**
     * Returns a 'greaterThanOrEqualTo' {@link AttributeCriteria}.
     * @param value the value to use in the condition
     * @return a {@link AttributeCriteria}
     */
    AttributeCriteria<T> greaterThanOrEqualTo(T value);

    /**
     * Returns a 'betweenExclusive' {@link AttributeCriteria}.
     * @param lowerBound the lower bound
     * @param upperBound the upper bound
     * @return a {@link AttributeCriteria}
     */
    AttributeCriteria<T> betweenExclusive(T lowerBound, T upperBound);

    /**
     * Returns a 'between' {@link AttributeCriteria}.
     * @param lowerBound the lower bound
     * @param upperBound the upper bound
     * @return a {@link AttributeCriteria}
     */
    AttributeCriteria<T> between(T lowerBound, T upperBound);

    /**
     * Returns a 'notBetweenExclusive' {@link AttributeCriteria}.
     * @param lowerBound the lower bound
     * @param upperBound the upper bound
     * @return a {@link AttributeCriteria}
     */
    AttributeCriteria<T> notBetweenExclusive(T lowerBound, T upperBound);

    /**
     * Returns a 'notBetween' {@link AttributeCriteria}.
     * @param lowerBound the lower bound
     * @param upperBound the upper bound
     * @return a {@link AttributeCriteria}
     */
    AttributeCriteria<T> notBetween(T lowerBound, T upperBound);

    /**
     * Returns a 'isNull' {@link AttributeCriteria}.
     * @return a {@link AttributeCriteria}
     */
    AttributeCriteria<T> isNull();

    /**
     * Returns a 'isNotNull' {@link AttributeCriteria}.
     * @return a {@link AttributeCriteria}
     */
    AttributeCriteria<T> isNotNull();
  }
}
