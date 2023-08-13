/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.framework.db.criteria.Criteria;
import is.codion.framework.domain.entity.EntityType;

/**
 * Specifies a query condition, based on a {@link Criteria}.
 * A factory class for {@link Condition} via {@link #where(Criteria)}.
 * For {@link Criteria} instances use the factory methods provided by the {@link Criteria} class.
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
   * @param criteria the criteria
   * @return a condition based on the given criteria
   */
  static Condition where(Criteria criteria) {
    return new DefaultCondition(criteria);
  }
}
