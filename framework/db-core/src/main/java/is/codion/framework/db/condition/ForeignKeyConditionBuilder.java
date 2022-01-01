/*
 * Copyright (c) 2020 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.framework.domain.entity.Entity;

import java.util.Collection;

/**
 * A builder for {@link Condition}s based on foreign keys.
 */
public interface ForeignKeyConditionBuilder {

  /**
   * Returns a 'equalTo' {@link Condition} or 'isNull' in case the value is null.
   * @param value the value to use in the condition
   * @return a {@link Condition}
   */
  Condition equalTo(Entity value);

  /**
   * Returns a 'equalTo' {@link Condition} or 'isNull' in case values is empty.
   * @param values the values to use in the condition
   * @return a {@link Condition}
   * @throws NullPointerException in case {@code values} is null
   */
  Condition equalTo(Entity... values);

  /**
   * Returns a 'equalTo' {@link Condition} or 'isNull' in case values is empty.
   * @param values the values to use in the condition
   * @return a {@link Condition}
   * @throws NullPointerException in case {@code values} is null
   */
  Condition equalTo(Collection<? extends Entity> values);

  /**
   * Returns a 'notEqualTo' {@link Condition} or 'isNotNull' in case the value is null.
   * @param value the value to use in the condition
   * @return a {@link Condition}
   */
  Condition notEqualTo(Entity value);

  /**
   * Returns a 'notEqualTo' {@link Condition} or 'isNotNull' in case values is empty.
   * @param values the values to use in the condition
   * @return a {@link Condition}
   * @throws NullPointerException in case {@code values} is null
   */
  Condition notEqualTo(Entity... values);

  /**
   * Returns a 'notEqualTo' condition or 'isNotNull' in case values is empty.
   * @param values the values to use in the condition
   * @return a {@link Condition}
   * @throws IllegalArgumentException in case {@code values} is null
   */
  Condition notEqualTo(Collection<? extends Entity> values);

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
