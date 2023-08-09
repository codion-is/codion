/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.framework.db.criteria.Criteria;
import is.codion.framework.domain.entity.EntityType;

import java.io.Serializable;

import static java.util.Objects.requireNonNull;

/**
 * A base class for Condition.
 * Remember to override {@link #equals(Object)} and {@link #hashCode()} if query caching is being used.
 */
class DefaultCondition implements Condition, Serializable {

  private static final long serialVersionUID = 1;

  private final Criteria criteria;

  protected DefaultCondition(Criteria criteria) {
    this.criteria = requireNonNull(criteria);
  }

  @Override
  public final EntityType entityType() {
    return criteria.entityType();
  }

  @Override
  public final Criteria criteria() {
    return criteria;
  }

  @Override
  public final String toString() {
    return getClass().getSimpleName() + ": " + entityType();
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof DefaultCondition)) {
      return false;
    }
    DefaultCondition that = (DefaultCondition) object;
    return criteria.equals(that.criteria);
  }

  @Override
  public int hashCode() {
    return criteria.hashCode();
  }
}
