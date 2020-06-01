/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.framework.domain.entity.EntityIdentity;

import static java.util.Objects.requireNonNull;

class DefaultEntityCondition implements EntityCondition {

  private static final long serialVersionUID = 1;

  private static final Condition.EmptyCondition EMPTY_CONDITION = new Condition.EmptyCondition();

  private final EntityIdentity entityId;
  private final Condition condition;

  /**
   * Instantiates a new empty {@link DefaultEntityCondition}.
   * Using an empty condition means all underlying records should be selected
   * @param entityId the id of the entity to select
   */
  DefaultEntityCondition(final EntityIdentity entityId) {
    this(entityId, null);
  }

  /**
   * Instantiates a new {@link EntityCondition}
   * @param entityId the id of the entity to select
   * @param condition the Condition object
   */
  DefaultEntityCondition(final EntityIdentity entityId, final Condition condition) {
    this.entityId = requireNonNull(entityId, "entityId");
    this.condition = condition == null ? EMPTY_CONDITION : condition;
  }

  @Override
  public final EntityIdentity getEntityId() {
    return entityId;
  }

  @Override
  public final Condition getCondition() {
    return condition;
  }

  @Override
  public final String toString() {
    return getClass().getSimpleName() + " [" + entityId + "]";
  }
}
