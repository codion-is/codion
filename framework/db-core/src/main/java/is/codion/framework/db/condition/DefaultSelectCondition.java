/*
 * Copyright (c) 2019 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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
import java.util.Objects;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

final class DefaultSelectCondition extends AbstractCondition implements SelectCondition {

  private static final long serialVersionUID = 1;

  private final Condition condition;
  private Map<ForeignKey, Integer> foreignKeyFetchDepths;
  private Collection<Attribute<?>> selectAttributes = emptyList();

  private OrderBy orderBy;
  private Integer fetchDepth;
  private boolean forUpdate;
  private int limit = -1;
  private int offset = -1;
  private int queryTimeout = DEFAULT_QUERY_TIMEOUT_SECONDS;

  /**
   * Instantiates a new {@link DefaultSelectCondition}
   * @param condition the Condition object
   */
  DefaultSelectCondition(Condition condition) {
    super(requireNonNull(condition, "condition").getEntityType());
    this.condition = condition;
  }

  private DefaultSelectCondition(DefaultSelectCondition selectCondition) {
    super(selectCondition.getEntityType());
    this.condition = selectCondition.condition;
    this.foreignKeyFetchDepths = selectCondition.foreignKeyFetchDepths;
    this.selectAttributes = selectCondition.selectAttributes;
    this.orderBy = selectCondition.orderBy;
    this.fetchDepth = selectCondition.fetchDepth;
    this.forUpdate = selectCondition.forUpdate;
    this.limit = selectCondition.limit;
    this.offset = selectCondition.offset;
    this.queryTimeout = selectCondition.queryTimeout;
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
  public String getConditionString(EntityDefinition definition) {
    return condition.getConditionString(definition);
  }

  @Override
  public Condition getCondition() {
    return condition;
  }

  @Override
  public Optional<OrderBy> getOrderBy() {
    return Optional.ofNullable(orderBy);
  }

  @Override
  public int getLimit() {
    return limit;
  }

  @Override
  public int getOffset() {
    return offset;
  }

  @Override
  public boolean isForUpdate() {
    return forUpdate;
  }

  @Override
  public Optional<Integer> getFetchDepth() {
    return Optional.ofNullable(fetchDepth);
  }

  @Override
  public Optional<Integer> getFetchDepth(ForeignKey foreignKey) {
    requireNonNull(foreignKey);
    if (foreignKeyFetchDepths != null && foreignKeyFetchDepths.containsKey(foreignKey)) {
      return Optional.of(foreignKeyFetchDepths.get(foreignKey));
    }

    return getFetchDepth();
  }

  @Override
  public int getQueryTimeout() {
    return queryTimeout;
  }

  @Override
  public Collection<Attribute<?>> getSelectAttributes() {
    return selectAttributes;
  }

  @Override
  public SelectCondition orderBy(OrderBy orderBy) {
    DefaultSelectCondition selectCondition = new DefaultSelectCondition(this);
    selectCondition.orderBy = orderBy;

    return selectCondition;
  }

  @Override
  public SelectCondition limit(int limit) {
    DefaultSelectCondition selectCondition = new DefaultSelectCondition(this);
    selectCondition.limit = limit;

    return selectCondition;
  }

  @Override
  public SelectCondition offset(int offset) {
    DefaultSelectCondition selectCondition = new DefaultSelectCondition(this);
    selectCondition.offset = offset;

    return selectCondition;
  }

  @Override
  public SelectCondition forUpdate() {
    DefaultSelectCondition selectCondition = new DefaultSelectCondition(this);
    selectCondition.forUpdate = true;

    return selectCondition;
  }

  @Override
  public SelectCondition fetchDepth(int fetchDepth) {
    DefaultSelectCondition selectCondition = new DefaultSelectCondition(this);
    selectCondition.fetchDepth = fetchDepth;

    return selectCondition;
  }

  @Override
  public SelectCondition fetchDepth(ForeignKey foreignKey, int fetchDepth) {
    requireNonNull(foreignKey);
    DefaultSelectCondition selectCondition = new DefaultSelectCondition(this);
    if (selectCondition.foreignKeyFetchDepths == null) {
      selectCondition.foreignKeyFetchDepths = new HashMap<>();
    }
    selectCondition.foreignKeyFetchDepths.put(foreignKey, fetchDepth);

    return selectCondition;
  }

  @Override
  public SelectCondition selectAttributes(Attribute<?>... attributes) {
    DefaultSelectCondition selectCondition = new DefaultSelectCondition(this);
    selectCondition.selectAttributes = requireNonNull(attributes).length == 0 ? emptyList() : unmodifiableList(asList(attributes));

    return selectCondition;
  }

  @Override
  public SelectCondition selectAttributes(Collection<Attribute<?>> attributes) {
    DefaultSelectCondition selectCondition = new DefaultSelectCondition(this);
    selectCondition.selectAttributes = requireNonNull(attributes).isEmpty() ? emptyList() : unmodifiableList(new ArrayList<>(attributes));

    return selectCondition;
  }

  @Override
  public SelectCondition queryTimeout(int queryTimeout) {
    DefaultSelectCondition selectCondition = new DefaultSelectCondition(this);
    selectCondition.queryTimeout = queryTimeout;

    return selectCondition;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof DefaultSelectCondition)) {
      return false;
    }
    if (!super.equals(object)) {
      return false;
    }
    DefaultSelectCondition that = (DefaultSelectCondition) object;
    return forUpdate == that.forUpdate &&
            limit == that.limit &&
            offset == that.offset &&
            condition.equals(that.condition) &&
            Objects.equals(foreignKeyFetchDepths, that.foreignKeyFetchDepths) &&
            selectAttributes.equals(that.selectAttributes) &&
            Objects.equals(orderBy, that.orderBy) &&
            Objects.equals(fetchDepth, that.fetchDepth);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), condition, foreignKeyFetchDepths,
            selectAttributes, orderBy, fetchDepth, forUpdate, limit, offset);
  }
}
