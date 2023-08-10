/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.framework.db.criteria.Criteria;
import is.codion.framework.domain.entity.EntityType;

/**
 * Specifies a query condition, based on a {@link Criteria}.
 * A factory class for {@link Condition} via {@link #all(EntityType)} and {@link #where(Criteria)}.
 * For {@link Criteria} instances use the factory methods provided by the {@link Criteria} class.
 * @see #all(EntityType)
 * @see #where(Criteria)
 */
public interface Condition {

  /**
   * @return the entity type
   */
  EntityType entityType();

  /**
   * @return the underlying Criteria instance
   */
  Criteria criteria();

  /**
   * Creates a condition specifying all entities of the given type
   * @param entityType the entity type
   * @return a condition for all entities
   */
  static Condition all(EntityType entityType) {
    return where(Criteria.all(entityType));
  }

  /**
   * @param criteria the criteria
   * @return a condition based on the given criteria
   */
  static Condition where(Criteria criteria) {
    return new DefaultCondition(criteria);
  }
}
