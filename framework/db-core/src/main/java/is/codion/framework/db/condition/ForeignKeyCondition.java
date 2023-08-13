/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.framework.domain.entity.Entity;

import java.util.Collection;

/**
 * A ForeignKey based condition.
 */
public interface ForeignKeyCondition extends Condition {

  /**
   * Builds a ForeignKey based condition.
   */
  interface Builder {

    /**
     * Returns a 'equalTo' {@link Condition} or 'isNull' in case the value is null.
     * @param value the value to use in the condition
     * @return a {@link Condition}
     */
    Condition equalTo(Entity value);

    /**
     * Returns a 'notEqualTo' {@link Condition} or 'isNotNull' in case the value is null.
     * @param value the value to use in the condition
     * @return a {@link Condition}
     */
    Condition notEqualTo(Entity value);

    /**
     * Returns a 'in' {@link Condition}.
     * @param values the values to use in the condition
     * @return a {@link Condition}
     * @throws NullPointerException in case {@code values} is null
     */
    Condition in(Entity... values);

    /**
     * Returns a 'notIn' {@link Condition}.
     * @param values the values to use in the condition
     * @return a {@link Condition}
     * @throws NullPointerException in case {@code values} is null
     */
    Condition notIn(Entity... values);

    /**
     * Returns a 'in' {@link Condition}.
     * @param values the values to use in the condition
     * @return a {@link Condition}
     * @throws NullPointerException in case {@code values} is null
     */
    Condition in(Collection<? extends Entity> values);

    /**
     * Returns a 'notIn' condition.
     * @param values the values to use in the condition
     * @return a {@link Condition}
     * @throws IllegalArgumentException in case {@code values} is null
     */
    Condition notIn(Collection<? extends Entity> values);

    /**
     * Returns a 'isNull' {@link Condition}.
     * @return a {@link Condition}
     */
    Condition isNull();

    /**
     * Returns a 'isNotNull' {@link Condition}.
     * @return a {@link Condition}
     */
    Condition isNotNull();
  }
}
