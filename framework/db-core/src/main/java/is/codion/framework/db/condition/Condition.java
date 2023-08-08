/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.framework.db.criteria.Criteria;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityType;

import java.util.List;

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
   * @return a list of the values this condition is based on, in the order they appear
   * in the condition clause. An empty list is returned in case no values are specified.
   */
  List<?> values();

  /**
   * @return a list of the attributes this condition is based on, in the same
   * order as their respective values appear in the condition clause.
   * An empty list is returned in case no values are specified.
   */
  List<Attribute<?>> attributes();

  /**
   * @return the underlying Criteria instance
   */
  Criteria criteria();

  /**
   * @return a {@link SelectCondition.Builder} instance based on this condition
   */
  SelectCondition.Builder selectBuilder();

  /**
   * @return a {@link UpdateCondition.Builder} instance based on this condition
   */
  UpdateCondition.Builder updateBuilder();

  /**
   * @param criteria the criteria
   * @return a condition based on the given criteria
   */
  static Condition where(Criteria criteria) {
    return new DefaultCondition(criteria);
  }
}
