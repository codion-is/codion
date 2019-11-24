/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.condition;

import static java.util.Objects.requireNonNull;

class DefaultEntityCondition implements EntityCondition {

  private static final long serialVersionUID = 1;

  private static final Condition.EmptyCondition EMPTY_CONDITION = new Condition.EmptyCondition();

  private final String entityId;
  private final Condition condition;

  /**
   * Instantiates a new empty {@link DefaultEntityCondition}.
   * Using an empty condition means all underlying records should be selected
   * @param entityId the ID of the entity to select
   */
  DefaultEntityCondition(final String entityId) {
    this(entityId, null);
  }

  /**
   * Instantiates a new {@link EntityCondition}
   * @param entityId the ID of the entity to select
   * @param condition the Condition object
   */
  DefaultEntityCondition(final String entityId, final Condition condition) {
    this.entityId = requireNonNull(entityId, "entityId");
    this.condition = condition == null ? EMPTY_CONDITION : condition;
  }

  /** {@inheritDoc} */
  @Override
  public final String getEntityId() {
    return entityId;
  }

  /** {@inheritDoc} */
  @Override
  public final Condition getCondition() {
    return condition;
  }

  /** {@inheritDoc} */
  @Override
  public final String toString() {
    return getClass().getSimpleName() + " [" + entityId + "]";
  }
}
