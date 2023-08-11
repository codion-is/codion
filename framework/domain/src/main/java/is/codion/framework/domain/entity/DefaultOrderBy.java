/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

final class DefaultOrderBy implements OrderBy, Serializable {

  private static final long serialVersionUID = 1;

  private final List<OrderByAttribute> orderByAttributes;

  private DefaultOrderBy(DefaultOrderByBuilder builder) {
    this.orderByAttributes = unmodifiableList(builder.orderByAttributes);
  }

  @Override
  public List<OrderByAttribute> orderByAttributes() {
    return orderByAttributes;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof DefaultOrderBy)) {
      return false;
    }
    DefaultOrderBy that = (DefaultOrderBy) object;
    return orderByAttributes.equals(that.orderByAttributes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(orderByAttributes);
  }

  private static final class DefaultOrderByAttribute implements OrderByAttribute, Serializable {

    private static final long serialVersionUID = 1;

    private final Column<?> attribute;
    private final NullOrder nullOrder;
    private final boolean ascending;

    private DefaultOrderByAttribute(Column<?> attribute, NullOrder nullOrder, boolean ascending) {
      this.attribute = requireNonNull(attribute, "attribute");
      this.nullOrder = requireNonNull(nullOrder, "nullOrder");
      this.ascending = ascending;
    }

    @Override
    public Column<?> attribute() {
      return attribute;
    }

    @Override
    public NullOrder nullOrder() {
      return nullOrder;
    }

    @Override
    public boolean isAscending() {
      return ascending;
    }

    @Override
    public boolean equals(Object object) {
      if (this == object) {
        return true;
      }
      if (object == null || getClass() != object.getClass()) {
        return false;
      }
      DefaultOrderByAttribute that = (DefaultOrderByAttribute) object;
      return attribute.equals(that.attribute) &&
              //backwards compatability, nullOrder could be null after deserialization from older version
              Objects.equals(nullOrder, that.nullOrder) &&
              ascending == that.ascending;
    }

    @Override
    public int hashCode() {
      return Objects.hash(attribute, nullOrder, ascending);
    }
  }

  static final class DefaultOrderByBuilder implements Builder {

    private final List<OrderByAttribute> orderByAttributes = new ArrayList<>(1);

    @Override
    public Builder ascending(Column<?>... attributes) {
      add(true, NullOrder.DEFAULT, requireNonNull(attributes));
      return this;
    }

    @Override
    public Builder ascendingNullsFirst(Column<?>... attributes) {
      add(true, NullOrder.NULLS_FIRST, requireNonNull(attributes));
      return this;
    }

    @Override
    public Builder ascendingNullsLast(Column<?>... attributes) {
      add(true, NullOrder.NULLS_LAST, requireNonNull(attributes));
      return this;
    }

    @Override
    public Builder descending(Column<?>... attributes) {
      add(false, NullOrder.DEFAULT, requireNonNull(attributes));
      return this;
    }

    @Override
    public Builder descendingNullsFirst(Column<?>... attributes) {
      add(false, NullOrder.NULLS_FIRST, requireNonNull(attributes));
      return this;
    }

    @Override
    public Builder descendingNullsLast(Column<?>... attributes) {
      add(false, NullOrder.NULLS_LAST, requireNonNull(attributes));
      return this;
    }

    @Override
    public OrderBy build() {
      return new DefaultOrderBy(this);
    }

    private void add(boolean ascending, NullOrder nullOrder, Column<?>... attributes) {
      if (attributes.length == 0) {
        throw new IllegalArgumentException("One or more attributes required for order by");
      }
      for (Column<?> attribute : attributes) {
        for (OrderByAttribute orderByColumn : orderByAttributes) {
          if (requireNonNull(attribute).equals(orderByColumn.attribute())) {
            throw new IllegalArgumentException("Order by already contains attribute: " + attribute);
          }
        }
        orderByAttributes.add(new DefaultOrderByAttribute(attribute, nullOrder, ascending));
      }
    }
  }
}
