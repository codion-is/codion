/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.condition;

import org.jminor.framework.domain.entity.OrderBy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

final class DefaultEntitySelectCondition extends DefaultEntityCondition implements EntitySelectCondition {

  private static final long serialVersionUID = 1;

  private HashMap<String, Integer> foreignKeyFetchDepthLimits;
  private List<String> selectPropertyIds = emptyList();

  private OrderBy orderBy;
  private Integer foreignKeyFetchDepthLimit;
  private int fetchCount = -1;
  private boolean forUpdate;
  private int limit;
  private int offset;

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

  @Override
  public int getFetchCount() {
    return fetchCount;
  }

  @Override
  public EntitySelectCondition setFetchCount(final int fetchCount) {
    this.fetchCount = fetchCount;
    return this;
  }

  @Override
  public OrderBy getOrderBy() {
    return orderBy;
  }

  @Override
  public EntitySelectCondition setOrderBy(final OrderBy orderBy) {
    this.orderBy = orderBy;
    return this;
  }

  @Override
  public int getLimit() {
    return limit;
  }

  @Override
  public EntitySelectCondition setLimit(final int limit) {
    this.limit = limit;
    return this;
  }

  @Override
  public int getOffset() {
    return offset;
  }

  @Override
  public EntitySelectCondition setOffset(final int offset) {
    this.offset = offset;
    return this;
  }

  @Override
  public EntitySelectCondition setForeignKeyFetchDepthLimit(final String foreignKeyPropertyId, final int fetchDepthLimit) {
    if (foreignKeyFetchDepthLimits == null) {
      foreignKeyFetchDepthLimits = new HashMap<>();
    }
    this.foreignKeyFetchDepthLimits.put(foreignKeyPropertyId, fetchDepthLimit);
    return this;
  }

  @Override
  public Integer getForeignKeyFetchDepthLimit(final String foreignKeyPropertyId) {
    if (foreignKeyFetchDepthLimits != null && foreignKeyFetchDepthLimits.containsKey(foreignKeyPropertyId)) {
      return foreignKeyFetchDepthLimits.get(foreignKeyPropertyId);
    }

    return foreignKeyFetchDepthLimit;
  }

  @Override
  public EntitySelectCondition setForeignKeyFetchDepthLimit(final int fetchDepthLimit) {
    this.foreignKeyFetchDepthLimit = fetchDepthLimit;
    return this;
  }

  @Override
  public EntitySelectCondition setSelectPropertyIds(final String... propertyIds) {
    this.selectPropertyIds = new ArrayList<>(asList(propertyIds));
    return this;
  }

  @Override
  public List<String> getSelectPropertyIds() {
    return selectPropertyIds;
  }

  @Override
  public boolean isForUpdate() {
    return forUpdate;
  }

  @Override
  public EntitySelectCondition setForUpdate(final boolean forUpdate) {
    this.forUpdate = forUpdate;
    return this;
  }
}
