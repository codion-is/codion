/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.OrderBy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

final class DefaultEntitySelectCondition extends DefaultEntityCondition implements EntitySelectCondition {

  private static final long serialVersionUID = 1;

  private HashMap<Attribute<?>, Integer> foreignKeyFetchDepths;
  private List<Attribute<?>> selectAttributes = emptyList();

  private OrderBy orderBy;
  private Integer foreignKeyFetchDepth;
  private int fetchCount = -1;
  private boolean forUpdate;
  private int limit;
  private int offset;

  /**
   * Instantiates a new {@link DefaultEntitySelectCondition}, which includes all the underlying entities
   * @param entityType the type of the entity to select
   */
  DefaultEntitySelectCondition(final EntityType entityType) {
    super(entityType);
  }

  /**
   * Instantiates a new {@link DefaultEntitySelectCondition}
   * @param entityType the type of the entity to select
   * @param condition the Condition object
   * @see DefaultPropertyCondition
   * @see EntityKeyCondition
   */
  DefaultEntitySelectCondition(final EntityType entityType, final Condition condition) {
    super(entityType, condition);
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
  public EntitySelectCondition setForeignKeyFetchDepth(final Attribute<?> foreignKeyAttribute, final int fetchDepth) {
    if (foreignKeyFetchDepths == null) {
      foreignKeyFetchDepths = new HashMap<>();
    }
    this.foreignKeyFetchDepths.put(foreignKeyAttribute, fetchDepth);
    return this;
  }

  @Override
  public Integer getForeignKeyFetchDepth(final Attribute<?> foreignKeyAttribute) {
    if (foreignKeyFetchDepths != null && foreignKeyFetchDepths.containsKey(foreignKeyAttribute)) {
      return foreignKeyFetchDepths.get(foreignKeyAttribute);
    }

    return foreignKeyFetchDepth;
  }

  @Override
  public EntitySelectCondition setForeignKeyFetchDepth(final int fetchDepth) {
    this.foreignKeyFetchDepth = fetchDepth;
    return this;
  }

  @Override
  public EntitySelectCondition setSelectAttributes(final Attribute<?>... attributes) {
    this.selectAttributes = new ArrayList<>(asList(attributes));
    return this;
  }

  @Override
  public List<Attribute<?>> getSelectAttributes() {
    return selectAttributes;
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
