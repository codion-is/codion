/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.framework.db.criteria.Criteria;
import is.codion.framework.domain.entity.Column;
import is.codion.framework.domain.entity.EntityType;

import java.util.Map;

/**
 * A condition specifying a where clause along with attributes and their associated values for update.
 * A factory class for {@link UpdateCondition.Builder} instances via
 * {@link UpdateCondition#all(EntityType)}, {@link UpdateCondition#where(Criteria)} and
 * {@link UpdateCondition#builder(UpdateCondition)}.
 */
public interface UpdateCondition {

  /**
   * @return the underlying Criteria instance
   */
  Criteria criteria();

  /**
   * @return an unmodifiable view of the new values mapped to their respective columns
   */
  Map<Column<?>, Object> columnValues();

  /**
   * Builds an {@link UpdateCondition}.
   */
  interface Builder {

    /**
     * Adds a column value to update
     * @param column the column
     * @param value the new value
     * @param <T> the value type
     * @return this builder
     */
    <T> Builder set(Column<?> column, T value);

    /**
     * @return a new {@link UpdateCondition} instance based on this builder
     */
    UpdateCondition build();
  }

  /**
   * @param entityType the entity type
   * @return a {@link UpdateCondition.Builder} instance
   */
  static Builder all(EntityType entityType) {
    return new DefaultUpdateCondition.DefaultBuilder(Criteria.all(entityType));
  }

  /**
   * @param criteria the criteria
   * @return a {@link UpdateCondition.Builder} instance
   */
  static Builder where(Criteria criteria) {
    return new DefaultUpdateCondition.DefaultBuilder(criteria);
  }

  /**
   * @param condition the condition
   * @return a {@link UpdateCondition.Builder} instance
   */
  static Builder builder(UpdateCondition condition) {
    return new DefaultUpdateCondition.DefaultBuilder(condition);
  }
}
