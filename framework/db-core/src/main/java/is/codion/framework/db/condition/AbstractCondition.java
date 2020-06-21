/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.Conjunction;
import is.codion.framework.domain.entity.EntityType;

import static java.util.Objects.requireNonNull;

abstract class AbstractCondition implements Condition {

  private static final long serialVersionUID = 1;

  private final EntityType<?> entityType;

  AbstractCondition(final EntityType<?> entityType) {
    this.entityType = requireNonNull(entityType);
  }

  @Override
  public final EntityType<?> getEntityType() {
    return entityType;
  }

  @Override
  public final Condition.Combination and(final Condition... conditions) {
    return new DefaultConditionCombination(Conjunction.AND, this).add(requireNonNull(conditions));
  }

  @Override
  public final Condition.Combination or(final Condition... conditions) {
    return new DefaultConditionCombination(Conjunction.OR, this).add(requireNonNull(conditions));
  }
}
