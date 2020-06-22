/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.OrderBy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

final class DefaultSelectCondition extends AbstractCondition implements SelectCondition {

  private static final long serialVersionUID = 1;

  private final Condition condition;
  private HashMap<Attribute<?>, Integer> foreignKeyFetchDepths;
  private List<Attribute<?>> selectAttributes = emptyList();

  private OrderBy orderBy;
  private Integer foreignKeyFetchDepth;
  private int fetchCount = -1;
  private boolean forUpdate;
  private int limit;
  private int offset;

  /**
   * Instantiates a new {@link DefaultSelectCondition}
   * @param condition the Condition object
   * @see DefaultAttributeCondition
   * @see EntityKeyCondition
   */
  DefaultSelectCondition(final Condition condition) {
    super(condition.getEntityType());
    this.condition = condition;
  }

  @Override
  public Condition getCondition() {
    return condition;
  }

  @Override
  public List<?> getValues() {
    return condition.getValues();
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return condition.getAttributes();
  }

  @Override
  public String getWhereClause(final EntityDefinition definition) {
    return condition.getWhereClause(definition);
  }

  @Override
  public int getFetchCount() {
    return fetchCount;
  }

  @Override
  public SelectCondition setFetchCount(final int fetchCount) {
    this.fetchCount = fetchCount;
    return this;
  }

  @Override
  public OrderBy getOrderBy() {
    return orderBy;
  }

  @Override
  public SelectCondition setOrderBy(final OrderBy orderBy) {
    this.orderBy = orderBy;
    return this;
  }

  @Override
  public int getLimit() {
    return limit;
  }

  @Override
  public SelectCondition setLimit(final int limit) {
    this.limit = limit;
    return this;
  }

  @Override
  public int getOffset() {
    return offset;
  }

  @Override
  public SelectCondition setOffset(final int offset) {
    this.offset = offset;
    return this;
  }

  @Override
  public SelectCondition setForeignKeyFetchDepth(final Attribute<Entity> foreignKeyAttribute, final int fetchDepth) {
    if (foreignKeyFetchDepths == null) {
      foreignKeyFetchDepths = new HashMap<>();
    }
    this.foreignKeyFetchDepths.put(foreignKeyAttribute, fetchDepth);
    return this;
  }

  @Override
  public Integer getForeignKeyFetchDepth(final Attribute<Entity> foreignKeyAttribute) {
    if (foreignKeyFetchDepths != null && foreignKeyFetchDepths.containsKey(foreignKeyAttribute)) {
      return foreignKeyFetchDepths.get(foreignKeyAttribute);
    }

    return foreignKeyFetchDepth;
  }

  @Override
  public SelectCondition setForeignKeyFetchDepth(final int fetchDepth) {
    this.foreignKeyFetchDepth = fetchDepth;
    return this;
  }

  @Override
  public SelectCondition setSelectAttributes(final Attribute<?>... attributes) {
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
  public SelectCondition setForUpdate(final boolean forUpdate) {
    this.forUpdate = forUpdate;
    return this;
  }
}
