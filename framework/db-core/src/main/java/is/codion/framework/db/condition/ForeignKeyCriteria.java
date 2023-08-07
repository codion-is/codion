/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.framework.domain.entity.Entity;

import java.util.Collection;

/**
 * A ForeignKey based criteria.
 */
public interface ForeignKeyCriteria extends Criteria {

  /**
   * Builds a ForeignKey based criteria.
   */
  interface Builder {

    /**
     * Returns a 'equalTo' {@link Criteria} or 'isNull' in case the value is null.
     * @param value the value to use in the Criteria
     * @return a {@link Criteria}
     */
    Criteria equalTo(Entity value);

    /**
     * Returns a 'notEqualTo' {@link Criteria} or 'isNotNull' in case the value is null.
     * @param value the value to use in the Criteria
     * @return a {@link Criteria}
     */
    Criteria notEqualTo(Entity value);

    /**
     * Returns a 'in' {@link Criteria}.
     * @param values the values to use in the Criteria
     * @return a {@link Criteria}
     * @throws NullPointerException in case {@code values} is null
     */
    Criteria in(Entity... values);

    /**
     * Returns a 'notIn' {@link Criteria}.
     * @param values the values to use in the Criteria
     * @return a {@link Criteria}
     * @throws NullPointerException in case {@code values} is null
     */
    Criteria notIn(Entity... values);

    /**
     * Returns a 'in' {@link Criteria}.
     * @param values the values to use in the Criteria
     * @return a {@link Criteria}
     * @throws NullPointerException in case {@code values} is null
     */
    Criteria in(Collection<? extends Entity> values);

    /**
     * Returns a 'notIn' Criteria.
     * @param values the values to use in the Criteria
     * @return a {@link Criteria}
     * @throws IllegalArgumentException in case {@code values} is null
     */
    Criteria notIn(Collection<? extends Entity> values);

    /**
     * Returns a 'isNull' {@link Criteria}.
     * @return a {@link Criteria}
     */
    Criteria isNull();

    /**
     * Returns a 'isNotNull' {@link Criteria}.
     * @return a {@link Criteria}
     */
    Criteria isNotNull();
  }
}
