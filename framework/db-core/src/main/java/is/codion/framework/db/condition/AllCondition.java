/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.framework.db.criteria.AbstractCriteria;
import is.codion.framework.db.criteria.Criteria;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;

import java.io.Serializable;
import java.util.Objects;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

public final class AllCondition implements Condition, Serializable {

  private static final long serialVersionUID = 1;

  private final Criteria criteria;

  AllCondition(EntityType entityType) {
    this.criteria = new AllCriteria(entityType);
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
  public SelectCondition.Builder selectBuilder() {
    return new DefaultSelectCondition.DefaultBuilder(this);
  }

  @Override
  public UpdateCondition.Builder updateBuilder() {
    return new DefaultUpdateCondition.DefaultBuilder(this);
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

  private static final class AllCriteria extends AbstractCriteria {

    private static final long serialVersionUID = 1;

    private AllCriteria(EntityType entityType) {
      super(entityType, emptyList(), emptyList());
    }

    @Override
    public String toString(EntityDefinition definition) {
      requireNonNull(definition);
      return "";
    }
  }
}
