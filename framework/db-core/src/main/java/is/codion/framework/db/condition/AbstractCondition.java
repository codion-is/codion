/*
 * Copyright (c) 2020 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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
  public final SelectCondition.Builder selectBuilder() {
    return SelectCondition.builder(this);
  }

  @Override
  public final UpdateCondition.Builder updateBuilder() {
    return UpdateCondition.builder(this);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + ": " + getEntityType();
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof AbstractCondition)) {
      return false;
    }
    AbstractCondition that = (AbstractCondition) object;
    return entityType.equals(that.entityType);
  }

  @Override
  public int hashCode() {
    return entityType.hashCode();
  }
}
