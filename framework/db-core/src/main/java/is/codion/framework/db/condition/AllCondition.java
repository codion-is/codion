/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.framework.db.criteria.Criteria;
import is.codion.framework.domain.entity.EntityType;

import java.io.Serializable;
import java.util.Objects;

public final class AllCondition implements Condition, Serializable {

  private static final long serialVersionUID = 1;

  private final Criteria criteria;

  AllCondition(EntityType entityType) {
    this.criteria = Criteria.all(entityType);
  }

  @Override
  public EntityType entityType() {
    return criteria.entityType();
  }

  @Override
  public Criteria criteria() {
    return criteria;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof AllCondition)) {
      return false;
    }
    AllCondition that = (AllCondition) object;
    return Objects.equals(entityType(), that.entityType());
  }

  @Override
  public int hashCode() {
    return Objects.hash(entityType());
  }
}
