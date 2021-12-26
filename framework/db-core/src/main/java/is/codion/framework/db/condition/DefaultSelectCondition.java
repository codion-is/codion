/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.entity.OrderBy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

final class DefaultSelectCondition extends AbstractCondition implements SelectCondition {

  private static final long serialVersionUID = 1;

  private final Condition condition;
  private Map<ForeignKey, Integer> foreignKeyFetchDepths;
  private List<Attribute<?>> selectAttributes = emptyList();

  private OrderBy orderBy;
  private Integer fetchDepth;
  private int fetchCount = -1;
  private boolean forUpdate;
  private int limit = -1;
  private int offset = -1;

  /**
   * Instantiates a new {@link DefaultSelectCondition}
   * @param condition the Condition object
   */
  DefaultSelectCondition(final Condition condition) {
    super(requireNonNull(condition, "condition").getEntityType());
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
  public String getConditionString(final EntityDefinition definition) {
    return condition.getConditionString(definition);
  }

  @Override
  public int getFetchCount() {
    return fetchCount;
  }

  @Override
  public SelectCondition fetchCount(final int fetchCount) {
    this.fetchCount = fetchCount;
    return this;
  }

  @Override
  public OrderBy getOrderBy() {
    return orderBy;
  }

  @Override
  public SelectCondition orderBy(final OrderBy orderBy) {
    this.orderBy = orderBy;
    return this;
  }

  @Override
  public int getLimit() {
    return limit;
  }

  @Override
  public SelectCondition limit(final int limit) {
    this.limit = limit;
    return this;
  }

  @Override
  public int getOffset() {
    return offset;
  }

  @Override
  public SelectCondition offset(final int offset) {
    this.offset = offset;
    return this;
  }

  @Override
  public SelectCondition fetchDepth(final ForeignKey foreignKey, final int fetchDepth) {
    if (foreignKeyFetchDepths == null) {
      foreignKeyFetchDepths = new HashMap<>();
    }
    this.foreignKeyFetchDepths.put(foreignKey, fetchDepth);
    return this;
  }

  @Override
  public Integer getFetchDepth(final ForeignKey foreignKey) {
    if (foreignKeyFetchDepths != null && foreignKeyFetchDepths.containsKey(foreignKey)) {
      return foreignKeyFetchDepths.get(foreignKey);
    }

    return fetchDepth;
  }

  @Override
  public SelectCondition fetchDepth(final int fetchDepth) {
    this.fetchDepth = fetchDepth;
    return this;
  }

  @Override
  public Integer getFetchDepth() {
    return fetchDepth;
  }

  @Override
  public SelectCondition selectAttributes(final Attribute<?>... attributes) {
    this.selectAttributes = unmodifiableList(asList(requireNonNull(attributes)));
    return this;
  }

  @Override
  public SelectCondition selectAttributes(final Collection<Attribute<?>> attributes) {
    this.selectAttributes = requireNonNull(attributes).isEmpty() ? emptyList() : unmodifiableList(new ArrayList<>(attributes));
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
  public SelectCondition forUpdate() {
    this.forUpdate = true;
    return this;
  }
}
