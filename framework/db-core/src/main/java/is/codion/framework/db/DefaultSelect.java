/*
 * Copyright (c) 2019 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db;

import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.condition.Condition;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;

final class DefaultSelect implements Select, Serializable {

  private static final long serialVersionUID = 1;

  private final Condition where;
  private final Condition having;
  private final Map<ForeignKey, Integer> foreignKeyFetchDepths;
  private final Collection<Attribute<?>> attributes;
  private final OrderBy orderBy;
  private final Integer fetchDepth;
  private final boolean forUpdate;
  private final Integer limit;
  private final Integer offset;
  private final int queryTimeout;

  private DefaultSelect(DefaultBuilder builder) {
    this.where = builder.where;
    this.having = builder.having;
    this.foreignKeyFetchDepths = builder.foreignKeyFetchDepths == null ?
            null :
            unmodifiableMap(builder.foreignKeyFetchDepths);
    this.attributes = builder.attributes;
    this.orderBy = builder.orderBy;
    this.fetchDepth = builder.fetchDepth;
    this.forUpdate = builder.forUpdate;
    this.limit = builder.limit;
    this.offset = builder.offset;
    this.queryTimeout = builder.queryTimeout;
  }

  @Override
  public Condition where() {
    return where;
  }

  @Override
  public Condition having() {
    return having;
  }

  @Override
  public Optional<OrderBy> orderBy() {
    return Optional.ofNullable(orderBy);
  }

  @Override
  public OptionalInt limit() {
    return limit == null ? OptionalInt.empty() : OptionalInt.of(limit);
  }

  @Override
  public OptionalInt offset() {
    return offset == null ? OptionalInt.empty() : OptionalInt.of(offset);
  }

  @Override
  public boolean forUpdate() {
    return forUpdate;
  }

  @Override
  public OptionalInt fetchDepth() {
    return fetchDepth == null ? OptionalInt.empty() : OptionalInt.of(fetchDepth);
  }

  @Override
  public OptionalInt fetchDepth(ForeignKey foreignKey) {
    requireNonNull(foreignKey);
    if (foreignKeyFetchDepths().containsKey(foreignKey)) {
      return OptionalInt.of(foreignKeyFetchDepths.get(foreignKey));
    }

    return fetchDepth();
  }

  @Override
  public Map<ForeignKey, Integer> foreignKeyFetchDepths() {
    return foreignKeyFetchDepths == null ? emptyMap() : foreignKeyFetchDepths;
  }

  @Override
  public int queryTimeout() {
    return queryTimeout;
  }

  @Override
  public Collection<Attribute<?>> attributes() {
    return attributes;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof DefaultSelect)) {
      return false;
    }
    DefaultSelect that = (DefaultSelect) object;
    return forUpdate == that.forUpdate &&
            limit == that.limit &&
            offset == that.offset &&
            where.equals(that.where) &&
            Objects.equals(having, that.having) &&
            Objects.equals(foreignKeyFetchDepths, that.foreignKeyFetchDepths) &&
            attributes.equals(that.attributes) &&
            Objects.equals(orderBy, that.orderBy) &&
            Objects.equals(fetchDepth, that.fetchDepth);
  }

  @Override
  public int hashCode() {
    return Objects.hash(forUpdate, limit, offset, where, foreignKeyFetchDepths, attributes, orderBy, fetchDepth);
  }

  @Override
  public String toString() {
    return "Select{" +
            "where=" + where +
            ", having=" + having +
            ", foreignKeyFetchDepths=" + foreignKeyFetchDepths +
            ", attributes=" + attributes +
            ", orderBy=" + orderBy +
            ", fetchDepth=" + fetchDepth +
            ", forUpdate=" + forUpdate +
            ", limit=" + limit +
            ", offset=" + offset +
            ", queryTimeout=" + queryTimeout + "}";
  }

  static final class DefaultBuilder implements Select.Builder {

    private final Condition where;

    private Map<ForeignKey, Integer> foreignKeyFetchDepths;
    private Collection<Attribute<?>> attributes = emptyList();

    private Condition having;
    private OrderBy orderBy;
    private Integer fetchDepth;
    private boolean forUpdate;
    private Integer limit;
    private Integer offset;
    private int queryTimeout = EntityConnection.DEFAULT_QUERY_TIMEOUT_SECONDS;

    DefaultBuilder(Condition where) {
      this.where = requireNonNull(where);
      this.having = Condition.all(where.entityType());
    }

    @Override
    public Builder orderBy(OrderBy orderBy) {
      this.orderBy = orderBy;
      return this;
    }

    @Override
    public Builder limit(Integer limit) {
      this.limit = limit;
      return this;
    }

    @Override
    public Builder offset(Integer offset) {
      this.offset = offset;
      return this;
    }

    @Override
    public Builder forUpdate() {
      this.forUpdate = true;
      this.fetchDepth = 0;
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
    public <T extends Attribute<?>> Builder attributes(T... attributes) {
      this.attributes = requireNonNull(attributes).length == 0 ? emptyList() : unmodifiableList(asList(attributes));
      return this;
    }

    @Override
    public Builder attributes(Collection<? extends Attribute<?>> attributes) {
      this.attributes = requireNonNull(attributes).isEmpty() ? emptyList() : unmodifiableList(new ArrayList<>(attributes));
      return this;
    }

    @Override
    public Builder queryTimeout(int queryTimeout) {
      if (queryTimeout < 0) {
        throw new IllegalArgumentException("Query timeout must be greater than or equal to 0");
      }
      this.queryTimeout = queryTimeout;
      return this;
    }

    @Override
    public Builder having(Condition having) {
      this.having = requireNonNull(having);
      return this;
    }

    @Override
    public Select build() {
      return new DefaultSelect(this);
    }
  }
}
