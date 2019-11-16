/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.condition;

import org.jminor.framework.domain.Entity;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static java.util.Objects.requireNonNull;

class DefaultEntityCondition implements EntityCondition {

  private static final long serialVersionUID = 1;

  private static final Condition.EmptyCondition EMPTY_CONDITION = new Condition.EmptyCondition();

  private String entityId;
  private Condition condition;
  private String cachedWhereClause;
  private transient boolean expanded;

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
  public final Condition getCondition(final Entity.Definition definition) {
    expandCondition(requireNonNull(definition, "definition"));

    return condition;
  }

  /** {@inheritDoc} */
  @Override
  public final String getWhereClause(final Entity.Definition definition) {
    if (cachedWhereClause == null) {
      cachedWhereClause = getCondition(definition).getConditionString(definition);
    }

    return cachedWhereClause;
  }

  private void expandCondition(final Entity.Definition definition) {
    if (!expanded) {
      condition = condition.expand(definition);
      expanded = true;
    }
  }

  private void writeObject(final ObjectOutputStream stream) throws IOException {
    stream.writeObject(entityId);
    stream.writeObject(condition);
  }

  private void readObject(final ObjectInputStream stream) throws ClassNotFoundException, IOException {
    entityId = (String) stream.readObject();
    condition = (Condition) stream.readObject();
  }
}
