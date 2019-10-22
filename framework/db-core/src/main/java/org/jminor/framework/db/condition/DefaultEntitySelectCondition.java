/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.condition;

import org.jminor.framework.domain.Entity;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

final class DefaultEntitySelectCondition extends DefaultEntityCondition implements EntitySelectCondition {

  private static final long serialVersionUID = 1;

  private HashMap<String, Integer> foreignKeyFetchDepthLimits;
  private ArrayList<String> selectPropertyIds;

  private Entity.OrderBy orderBy;
  private Integer foreignKeyFetchDepthLimit;
  private int fetchCount = -1;
  private boolean forUpdate;
  private int limit;
  private int offset;

  /**
   * Instantiates a new {@link DefaultEntityCondition}.
   * @param key the key of the Entity to select
   */
  DefaultEntitySelectCondition(final Entity.Key key) {
    super(key);
  }

  /**
   * Instantiates a new {@link DefaultEntityCondition}.
   * @param keys the keys of the Entity to select
   */
  DefaultEntitySelectCondition(final List<Entity.Key> keys) {
    super(keys);
  }

  /**
   * Instantiates a new {@link DefaultEntitySelectCondition}, which includes all the underlying entities
   * @param domain the domain model
   * @param entityId the ID of the entity to select
   */
  DefaultEntitySelectCondition(final String entityId) {
    super(entityId);
  }

  /**
   * Instantiates a new {@link DefaultEntitySelectCondition}
   * @param domain the domain model
   * @param entityId the ID of the entity to select
   * @param condition the Condition object
   * @see DefaultPropertyCondition
   * @see EntityKeyCondition
   */
  DefaultEntitySelectCondition(final String entityId, final Condition condition) {
    super(entityId, condition);
  }

  /** {@inheritDoc} */
  @Override
  public int getFetchCount() {
    return fetchCount;
  }

  /** {@inheritDoc} */
  @Override
  public EntitySelectCondition setFetchCount(final int fetchCount) {
    this.fetchCount = fetchCount;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public Entity.OrderBy getOrderBy() {
    return orderBy;
  }

  /** {@inheritDoc} */
  @Override
  public EntitySelectCondition setOrderBy(final Entity.OrderBy orderBy) {
    this.orderBy = orderBy;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public int getLimit() {
    return limit;
  }

  /** {@inheritDoc} */
  @Override
  public EntitySelectCondition setLimit(final int limit) {
    this.limit = limit;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public int getOffset() {
    return offset;
  }

  /** {@inheritDoc} */
  @Override
  public EntitySelectCondition setOffset(final int offset) {
    this.offset = offset;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public EntitySelectCondition setForeignKeyFetchDepthLimit(final String foreignKeyPropertyId, final int fetchDepthLimit) {
    if (foreignKeyFetchDepthLimits == null) {
      foreignKeyFetchDepthLimits = new HashMap<>();
    }
    this.foreignKeyFetchDepthLimits.put(foreignKeyPropertyId, fetchDepthLimit);
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public Integer getForeignKeyFetchDepthLimit(final String foreignKeyPropertyId) {
    if (foreignKeyFetchDepthLimits != null && foreignKeyFetchDepthLimits.containsKey(foreignKeyPropertyId)) {
      return foreignKeyFetchDepthLimits.get(foreignKeyPropertyId);
    }

    return foreignKeyFetchDepthLimit;
  }

  /** {@inheritDoc} */
  @Override
  public EntitySelectCondition setForeignKeyFetchDepthLimit(final int fetchDepthLimit) {
    this.foreignKeyFetchDepthLimit = fetchDepthLimit;
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public EntitySelectCondition setSelectPropertyIds(final String... propertyIds) {
    this.selectPropertyIds = new ArrayList<>(asList(propertyIds));
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public List<String> getSelectPropertyIds() {
    return selectPropertyIds == null ? emptyList() : selectPropertyIds;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isForUpdate() {
    return forUpdate;
  }

  /** {@inheritDoc} */
  @Override
  public EntitySelectCondition setForUpdate(final boolean forUpdate) {
    this.forUpdate = forUpdate;
    return this;
  }

  private void writeObject(final ObjectOutputStream stream) throws IOException {
    stream.writeObject(orderBy);
    stream.writeInt(fetchCount);
    stream.writeBoolean(forUpdate);
    stream.writeObject(foreignKeyFetchDepthLimit);
    stream.writeObject(foreignKeyFetchDepthLimits);
    stream.writeObject(selectPropertyIds);
    stream.writeInt(limit);
    stream.writeInt(offset);
  }

  private void readObject(final ObjectInputStream stream) throws ClassNotFoundException, IOException {
    orderBy = (Entity.OrderBy) stream.readObject();
    fetchCount = stream.readInt();
    forUpdate = stream.readBoolean();
    foreignKeyFetchDepthLimit = (Integer) stream.readObject();
    foreignKeyFetchDepthLimits = (HashMap) stream.readObject();
    selectPropertyIds = (ArrayList) stream.readObject();
    limit = stream.readInt();
    offset = stream.readInt();
  }
}
