/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.db.Operator;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.Key;

import java.util.Collection;
import java.util.List;

/**
 * A Condition based on a single {@link Attribute}.
 * @param <T> the attribute type
 */
public interface AttributeCondition<T> extends Condition {

  /**
   * @return the attribute
   */
  Attribute<T> getAttribute();

  /**
   * @return the condition operator
   */
  Operator getOperator();

  /**
   * @param caseSensitive false if this condition should not be case-sensitive
   * @return this condition
   * @throws IllegalArgumentException in case the underlying attribute is not String based
   */
  AttributeCondition<String> setCaseSensitive(boolean caseSensitive);

  /**
   * @return true if this condition is case sensitive, only applicable to conditions based on String attributes.
   */
  boolean isCaseSensitive();

  /**
   * A builder for {@link AttributeCondition}.
   * @param <T> the attribute value type
   */
  interface Builder<T> {

    /**
     * Returns a 'equalTo' {@link AttributeCondition}.
     * @param value the value to use in the condition
     * @return a {@link AttributeCondition}
     */
    AttributeCondition<T> equalTo(T value);

    /**
     * Returns a 'equalTo' {@link AttributeCondition}.
     * @param values the values to use in the condition
     * @return a {@link AttributeCondition}
     */
    AttributeCondition<T> equalTo(T... values);

    /**
     * Returns a 'equalTo' {@link AttributeCondition}.
     * @param values the values to use in the condition
     * @return a {@link AttributeCondition}
     */
    AttributeCondition<T> equalTo(Collection<? extends T> values);

    /**
     * Returns a 'notEqualTo' {@link AttributeCondition}.
     * @param value the value to use in the condition
     * @return a {@link AttributeCondition}
     */
    AttributeCondition<T> notEqualTo(T value);

    /**
     * Returns a 'notEqualTo' {@link AttributeCondition}.
     * @param values the values to use in the condition
     * @return a {@link AttributeCondition}
     */
    AttributeCondition<T> notEqualTo(T... values);

    /**
     * Returns a 'notEqualTo' {@link AttributeCondition}.
     * @param values the values to use in the condition
     * @return a {@link AttributeCondition}
     */
    AttributeCondition<T> notEqualTo(Collection<? extends T> values);

    /**
     * Returns a 'lessThan' {@link AttributeCondition}.
     * @param value the value to use in the condition
     * @return a {@link AttributeCondition}
     */
    AttributeCondition<T> lessThan(T value);

    /**
     * Returns a 'greaterThan' {@link AttributeCondition}.
     * @param value the value to use in the condition
     * @return a {@link AttributeCondition}
     */
    AttributeCondition<T> greaterThan(T value);

    /**
     * Returns a 'withinRange' {@link AttributeCondition}.
     * @param lowerBound the lower bound
     * @param upperBound the upper bound
     * @return a {@link AttributeCondition}
     */
    AttributeCondition<T> withinRange(T lowerBound, T upperBound);

    /**
     * Returns a 'outsideRange' {@link AttributeCondition}.
     * @param lowerBound the lower bound
     * @param upperBound the upper bound
     * @return a {@link AttributeCondition}
     */
    AttributeCondition<T> outsideRange(T lowerBound, T upperBound);

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

    /**
     * Returns a 'equalTo' {@link AttributeCondition}.
     * @param key the key to use in the condition
     * @return a {@link AttributeCondition}
     */
    AttributeCondition<Entity> equalTo(Key key);

    /**
     * Returns a 'equalTo' {@link AttributeCondition}.
     * @param keys the keys to use in the condition
     * @return a {@link AttributeCondition}
     */
    AttributeCondition<Entity> equalTo(Key... keys);

    /**
     * Returns a 'equalTo' {@link AttributeCondition}.
     * @param keys the keys to use in the condition
     * @return a {@link AttributeCondition}
     */
    AttributeCondition<Entity> equalTo(List<Key> keys);
  }
}
