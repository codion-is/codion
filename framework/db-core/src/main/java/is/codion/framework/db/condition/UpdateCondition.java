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
 * {@link UpdateCondition#builder(Condition)}.
 */
public interface UpdateCondition extends Condition {

  /**
   * @return an unmodifiable view of the new values mapped to their respective attributes
   */
  Map<Column<?>, Object> attributeValues();

  /**
   * Builds an {@link UpdateCondition}.
   */
  interface Builder {

    /**
     * Adds a attribute value to update
     * @param attribute the attribute
     * @param value the new value
     * @param <T> the value type
     * @return this builder
     */
    <T> Builder set(Column<?> attribute, T value);

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
  static Builder builder(Condition condition) {
    return new DefaultUpdateCondition.DefaultBuilder(condition);
  }
}
