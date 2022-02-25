/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.Conjunction;
import is.codion.framework.domain.entity.EntityType;

import java.io.Serializable;

import static java.util.Objects.requireNonNull;

abstract class AbstractCondition implements Condition, Serializable {

  private static final long serialVersionUID = 1;

  private final EntityType entityType;

  AbstractCondition(EntityType entityType) {
    this.entityType = requireNonNull(entityType);
  }

  @Override
  public final EntityType getEntityType() {
    return entityType;
  }

  @Override
  public final Condition.Combination and(Condition... conditions) {
    return new DefaultConditionCombination(Conjunction.AND, this, conditions);
  }

  @Override
  public final Condition.Combination or(Condition... conditions) {
    return new DefaultConditionCombination(Conjunction.OR, this, conditions);
  }

  @Override
  public final SelectCondition toSelectCondition() {
    if (this instanceof SelectCondition) {
      return (SelectCondition) this;
    }

    return new DefaultSelectCondition(this);
  }

  @Override
  public final UpdateCondition toUpdateCondition() {
    if (this instanceof UpdateCondition) {
      return (UpdateCondition) this;
    }

    return new DefaultUpdateCondition(this);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + ": " + getEntityType();
  }
}
