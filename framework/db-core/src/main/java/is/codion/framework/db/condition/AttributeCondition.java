/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.Operator;
import is.codion.framework.domain.entity.Attribute;

import java.util.Collection;

/**
 * A Condition based on a single {@link Attribute}.
 * @param <T> the attribute type
 */
public interface AttributeCondition<T> extends Condition {

  /**
   * @return the attribute
   */
  Attribute<T> attribute();

  /**
   * @return the condition operator
   */
  Operator operator();

  /**
   * A builder for {@link AttributeCondition}.
   * @param <T> the attribute value type
   */
  interface Builder<T> {

    /**
     * Returns a 'equalTo' {@link AttributeCondition} or 'isNull' in case the value is null.
     * @param value the value to use in the condition
     * @return a {@link AttributeCondition}
     */
    AttributeCondition<T> equalTo(T value);

    /**
     * Returns a 'in' {@link AttributeCondition} or 'isNull' in case values is empty.
     * @param values the values to use in the condition
     * @return a {@link AttributeCondition}
     * @throws NullPointerException in case {@code values} is null
     */
    AttributeCondition<T> in(T... values);

    /**
     * Returns a 'in' {@link AttributeCondition} or 'isNull' in case values is empty.
     * @param values the values to use in the condition
     * @return a {@link AttributeCondition}
     * @throws NullPointerException in case {@code values} is null
     */
    AttributeCondition<T> in(Collection<? extends T> values);

    /**
     * Returns a 'equalTo' {@link AttributeCondition} or 'isNotNull' in case the value is null.
     * @param value the value to use in the condition
     * @return a {@link AttributeCondition}
     */
    AttributeCondition<T> notEqualTo(T value);

    /**
     * Returns a 'notIn' {@link AttributeCondition} or 'isNotNull' in case values is empty.
     * @param values the values to use in the condition
     * @return a {@link AttributeCondition}
     * @throws NullPointerException in case {@code values} is null
     */
    AttributeCondition<T> notIn(T... values);

    /**
     * Returns a 'notIn' {@link AttributeCondition} or 'isNotNull' in case values is empty.
     * @param values the values to use in the condition
     * @return a {@link AttributeCondition}
     * @throws NullPointerException in case {@code values} is null
     */
    AttributeCondition<T> notIn(Collection<? extends T> values);

    /**
     * Returns a case-insensitive 'equalTo' {@link AttributeCondition} or 'isNull' in case the value is null.
     * @param value the value to use in the condition
     * @return a {@link AttributeCondition}
     */
    AttributeCondition<String> equalToIgnoreCase(String value);

    /**
     * Returns a case-insensitive 'in' {@link AttributeCondition} or 'isNull' in case values is empty.
     * @param values the values to use in the condition
     * @return a {@link AttributeCondition}
     * @throws NullPointerException in case {@code values} is null
     */
    AttributeCondition<String> inIgnoreCase(String... values);

    /**
     * Returns a case-insensitive 'in' {@link AttributeCondition} or 'isNull' in case values is empty.
     * @param values the values to use in the condition
     * @return a {@link AttributeCondition}
     * @throws NullPointerException in case {@code values} is null
     */
    AttributeCondition<String> inIgnoreCase(Collection<String> values);

    /**
     * Returns a case-insensitive 'notEqualTo' {@link AttributeCondition} or 'isNotNull' in case the value is null.
     * @param value the value to use in the condition
     * @return a {@link AttributeCondition}
     */
    AttributeCondition<String> notEqualToIgnoreCase(String value);

    /**
     * Returns a case-insensitive 'notIn' {@link AttributeCondition} or 'isNotNull' in case values is empty.
     * @param values the values to use in the condition
     * @return a {@link AttributeCondition}
     * @throws NullPointerException in case {@code values} is null
     */
    AttributeCondition<String> notInIgnoreCase(String... values);

    /**
     * Returns a case-insensitive 'notIn' {@link AttributeCondition} or 'isNotNull' in case values is empty.
     * @param values the values to use in the condition
     * @return a {@link AttributeCondition}
     * @throws NullPointerException in case {@code values} is null
     */
    AttributeCondition<String> notInIgnoreCase(Collection<String> values);

    /**
     * Returns a 'lessThan' {@link AttributeCondition}.
     * @param value the value to use in the condition
     * @return a {@link AttributeCondition}
     */
    AttributeCondition<T> lessThan(T value);

    /**
     * Returns a 'lessThanOrEqualTo' {@link AttributeCondition}.
     * @param value the value to use in the condition
     * @return a {@link AttributeCondition}
     */
    AttributeCondition<T> lessThanOrEqualTo(T value);

    /**
     * Returns a 'greaterThan' {@link AttributeCondition}.
     * @param value the value to use in the condition
     * @return a {@link AttributeCondition}
     */
    AttributeCondition<T> greaterThan(T value);

    /**
     * Returns a 'greaterThanOrEqualTo' {@link AttributeCondition}.
     * @param value the value to use in the condition
     * @return a {@link AttributeCondition}
     */
    AttributeCondition<T> greaterThanOrEqualTo(T value);

    /**
     * Returns a 'betweenExclusive' {@link AttributeCondition}.
     * @param lowerBound the lower bound
     * @param upperBound the upper bound
     * @return a {@link AttributeCondition}
     */
    AttributeCondition<T> betweenExclusive(T lowerBound, T upperBound);

    /**
     * Returns a 'between' {@link AttributeCondition}.
     * @param lowerBound the lower bound
     * @param upperBound the upper bound
     * @return a {@link AttributeCondition}
     */
    AttributeCondition<T> between(T lowerBound, T upperBound);

    /**
     * Returns a 'notBetweenExclusive' {@link AttributeCondition}.
     * @param lowerBound the lower bound
     * @param upperBound the upper bound
     * @return a {@link AttributeCondition}
     */
    AttributeCondition<T> notBetweenExclusive(T lowerBound, T upperBound);

    /**
     * Returns a 'notBetween' {@link AttributeCondition}.
     * @param lowerBound the lower bound
     * @param upperBound the upper bound
     * @return a {@link AttributeCondition}
     */
    AttributeCondition<T> notBetween(T lowerBound, T upperBound);

    /**
     * Returns a 'isNull' {@link AttributeCondition}.
     * @return a {@link AttributeCondition}
     */
    AttributeCondition<T> isNull();

    /**
     * Returns a 'isNotNull' {@link AttributeCondition}.
     * @return a {@link AttributeCondition}
     */
    AttributeCondition<T> isNotNull();
  }
}
