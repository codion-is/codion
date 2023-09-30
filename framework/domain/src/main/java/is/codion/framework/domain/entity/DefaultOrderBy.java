/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.entity.attribute.Column;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

final class DefaultOrderBy implements OrderBy, Serializable {

  private static final long serialVersionUID = 1;

  private final List<OrderByColumn> orderByColumns;

  private DefaultOrderBy(DefaultOrderByBuilder builder) {
    this.orderByColumns = unmodifiableList(builder.orderByColumns);
  }

  @Override
  public List<OrderByColumn> orderByColumns() {
    return orderByColumns;
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
    return orderByColumns.equals(that.orderByColumns);
  }

  @Override
  public int hashCode() {
    return orderByColumns.hashCode();
  }

  @Override
  public String toString() {
    return "OrderBy{" +
            "orderByColumns=" + orderByColumns +
            '}';
  }

  private static final class DefaultOrderByColumn implements OrderByColumn, Serializable {

    private static final long serialVersionUID = 1;

    private final Column<?> column;
    private final NullOrder nullOrder;
    private final boolean ascending;

    private DefaultOrderByColumn(Column<?> column, NullOrder nullOrder, boolean ascending) {
      this.column = requireNonNull(column, "column");
      this.nullOrder = requireNonNull(nullOrder, "nullOrder");
      this.ascending = ascending;
    }

    @Override
    public Column<?> column() {
      return column;
    }

    @Override
    public NullOrder nullOrder() {
      return nullOrder;
    }

    @Override
    public boolean ascending() {
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
      DefaultOrderByColumn that = (DefaultOrderByColumn) object;
      return column.equals(that.column) &&
              nullOrder.equals(that.nullOrder) &&
              ascending == that.ascending;
    }

    @Override
    public int hashCode() {
      return Objects.hash(column, nullOrder, ascending);
    }

    @Override
    public String toString() {
      return "OrderByColumn{" +
              "column=" + column +
              ", nullOrder=" + nullOrder +
              ", ascending=" + ascending +
              '}';
    }
  }

  static final class DefaultOrderByBuilder implements Builder {

    private final List<OrderByColumn> orderByColumns = new ArrayList<>(1);

    @Override
    public Builder ascending(Column<?>... columns) {
      add(true, NullOrder.DEFAULT, requireNonNull(columns));
      return this;
    }

    @Override
    public Builder ascendingNullsFirst(Column<?>... columns) {
      add(true, NullOrder.NULLS_FIRST, requireNonNull(columns));
      return this;
    }

    @Override
    public Builder ascendingNullsLast(Column<?>... columns) {
      add(true, NullOrder.NULLS_LAST, requireNonNull(columns));
      return this;
    }

    @Override
    public Builder descending(Column<?>... columns) {
      add(false, NullOrder.DEFAULT, requireNonNull(columns));
      return this;
    }

    @Override
    public Builder descendingNullsFirst(Column<?>... columns) {
      add(false, NullOrder.NULLS_FIRST, requireNonNull(columns));
      return this;
    }

    @Override
    public Builder descendingNullsLast(Column<?>... columns) {
      add(false, NullOrder.NULLS_LAST, requireNonNull(columns));
      return this;
    }

    @Override
    public OrderBy build() {
      return new DefaultOrderBy(this);
    }

    private void add(boolean ascending, NullOrder nullOrder, Column<?>... columns) {
      if (columns.length == 0) {
        throw new IllegalArgumentException("One or more columns required for order by");
      }
      for (Column<?> column : columns) {
        for (OrderByColumn orderByColumn : orderByColumns) {
          if (requireNonNull(column).equals(orderByColumn.column())) {
            throw new IllegalArgumentException("Order by already contains column: " + column);
          }
        }
        orderByColumns.add(new DefaultOrderByColumn(column, nullOrder, ascending));
      }
    }
  }
}
