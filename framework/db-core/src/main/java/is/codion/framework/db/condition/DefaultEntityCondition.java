/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.framework.domain.entity.EntityType;

import static java.util.Objects.requireNonNull;

class DefaultEntityCondition implements EntityCondition {

  private static final long serialVersionUID = 1;

  private static final Condition.EmptyCondition EMPTY_CONDITION = new Condition.EmptyCondition();

  private final EntityType entityType;
  private final Condition condition;

  /**
   * Instantiates a new empty {@link DefaultEntityCondition}.
   * Using an empty condition means all underlying records should be selected
   * @param entityType the type of the entity to select
   */
  DefaultEntityCondition(final EntityType entityType) {
    this(entityType, null);
  }

  /**
   * Instantiates a new {@link EntityCondition}
   * @param entityType the type of the entity to select
   * @param condition the Condition object
   */
  DefaultEntityCondition(final EntityType entityType, final Condition condition) {
    this.entityType = requireNonNull(entityType, "entityType");
    this.condition = condition == null ? EMPTY_CONDITION : condition;
  }

  @Override
  public final EntityType getEntityType() {
    return entityType;
  }

  @Override
  public final Condition getCondition() {
    return condition;
  }

  @Override
  public final String toString() {
    return getClass().getSimpleName() + " [" + entityType + "]";
  }
}
