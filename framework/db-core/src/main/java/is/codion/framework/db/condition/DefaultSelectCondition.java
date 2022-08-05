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
  private final Map<ForeignKey, Integer> foreignKeyFetchDepths;
  private final Collection<Attribute<?>> selectAttributes;
  private final OrderBy orderBy;
  private final Integer fetchDepth;
  private final boolean forUpdate;
  private final int limit;
  private final int offset;
  private final int queryTimeout;

  private DefaultSelectCondition(DefaultBuilder builder) {
    super(builder.condition.getEntityType());
    this.condition = builder.condition;
    this.foreignKeyFetchDepths = builder.foreignKeyFetchDepths;
    this.selectAttributes = builder.selectAttributes;
    this.orderBy = builder.orderBy;
    this.fetchDepth = builder.fetchDepth;
    this.forUpdate = builder.forUpdate;
    this.limit = builder.limit;
    this.offset = builder.offset;
    this.queryTimeout = builder.queryTimeout;
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

  static final class DefaultBuilder implements SelectCondition.Builder {

    private final Condition condition;

    private Map<ForeignKey, Integer> foreignKeyFetchDepths;
    private Collection<Attribute<?>> selectAttributes = emptyList();

    private OrderBy orderBy;
    private Integer fetchDepth;
    private boolean forUpdate;
    private int limit = -1;
    private int offset = -1;
    private int queryTimeout = DEFAULT_QUERY_TIMEOUT_SECONDS;

    DefaultBuilder(Condition condition) {
      this.condition = requireNonNull(condition);
      if (condition instanceof DefaultSelectCondition) {
        DefaultSelectCondition selectCondition = (DefaultSelectCondition) condition;
        foreignKeyFetchDepths = selectCondition.foreignKeyFetchDepths;
        selectAttributes = selectCondition.selectAttributes;
        orderBy = selectCondition.orderBy;
        fetchDepth = selectCondition.fetchDepth;
        forUpdate = selectCondition.forUpdate;
        limit = selectCondition.limit;
        offset = selectCondition.offset;
        queryTimeout = selectCondition.queryTimeout;
      }
    }

    @Override
    public Builder orderBy(OrderBy orderBy) {
      this.orderBy = orderBy;
      return this;
    }

    @Override
    public Builder limit(int limit) {
      this.limit = limit;
      return this;
    }

    @Override
    public Builder offset(int offset) {
      this.offset = offset;
      return this;
    }

    @Override
    public Builder forUpdate() {
      this.forUpdate = true;
      return this;
    }

    @Override
    public Builder fetchDepth(int fetchDepth) {
      this.fetchDepth = fetchDepth;
      return this;
    }

    @Override
    public Builder fetchDepth(ForeignKey foreignKey, int fetchDepth) {
      requireNonNull(foreignKey);
      if (foreignKeyFetchDepths == null) {
        foreignKeyFetchDepths = new HashMap<>();
      }
      foreignKeyFetchDepths.put(foreignKey, fetchDepth);
      return this;
    }

    @Override
    public Builder selectAttributes(Attribute<?>... attributes) {
      selectAttributes = requireNonNull(attributes).length == 0 ? emptyList() : unmodifiableList(asList(attributes));
      return this;
    }

    @Override
    public Builder selectAttributes(Collection<Attribute<?>> attributes) {
      selectAttributes = requireNonNull(attributes).isEmpty() ? emptyList() : unmodifiableList(new ArrayList<>(attributes));
      return this;
    }

    @Override
    public Builder queryTimeout(int queryTimeout) {
      this.queryTimeout = queryTimeout;
      return this;
    }

    @Override
    public SelectCondition build() {
      return new DefaultSelectCondition(this);
    }
  }
}
