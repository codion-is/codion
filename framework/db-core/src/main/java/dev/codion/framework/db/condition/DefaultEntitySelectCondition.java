/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.db.condition;

import dev.codion.framework.domain.entity.OrderBy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

final class DefaultEntitySelectCondition extends DefaultEntityCondition implements EntitySelectCondition {

  private static final long serialVersionUID = 1;

  private HashMap<String, Integer> foreignKeyFetchDepths;
  private List<String> selectPropertyIds = emptyList();

  private OrderBy orderBy;
  private Integer foreignKeyFetchDepth;
  private int fetchCount = -1;
  private boolean forUpdate;
  private int limit;
  private int offset;

  /**
   * Instantiates a new {@link DefaultEntitySelectCondition}, which includes all the underlying entities
   * @param entityId the id of the entity to select
   */
  DefaultEntitySelectCondition(final String entityId) {
    super(entityId);
  }

  /**
   * Instantiates a new {@link DefaultEntitySelectCondition}
   * @param entityId the id of the entity to select
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
  public EntitySelectCondition setForeignKeyFetchDepth(final String foreignKeyPropertyId, final int fetchDepth) {
    if (foreignKeyFetchDepths == null) {
      foreignKeyFetchDepths = new HashMap<>();
    }
    this.foreignKeyFetchDepths.put(foreignKeyPropertyId, fetchDepth);
    return this;
  }

  @Override
  public Integer getForeignKeyFetchDepth(final String foreignKeyPropertyId) {
    if (foreignKeyFetchDepths != null && foreignKeyFetchDepths.containsKey(foreignKeyPropertyId)) {
      return foreignKeyFetchDepths.get(foreignKeyPropertyId);
    }

    return foreignKeyFetchDepth;
  }

  @Override
  public EntitySelectCondition setForeignKeyFetchDepth(final int fetchDepth) {
    this.foreignKeyFetchDepth = fetchDepth;
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
