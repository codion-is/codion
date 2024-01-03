/*
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db;

import is.codion.framework.db.EntityConnection.Update;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.condition.Condition;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;

final class DefaultUpdate implements Update, Serializable {

  private static final long serialVersionUID = 1;

  private final Condition where;
  private final Map<Column<?>, Object> columnValues;

  private DefaultUpdate(DefaultUpdate.DefaultBuilder builder) {
    this.where = builder.where;
    this.columnValues = unmodifiableMap(builder.columnValues);
  }

  @Override
  public Condition where() {
    return where;
  }

  @Override
  public Map<Column<?>, Object> columnValues() {
    return columnValues;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof DefaultUpdate)) {
      return false;
    }
    DefaultUpdate that = (DefaultUpdate) object;
    return Objects.equals(where, that.where) &&
            Objects.equals(columnValues, that.columnValues);
  }

  @Override
  public int hashCode() {
    return Objects.hash(where, columnValues);
  }

  @Override
  public String toString() {
    return "Update{" +
            "where=" + where +
            ", columnValues=" + columnValues + "}";
  }

  static final class DefaultBuilder implements Update.Builder {

    private final Condition where;
    private final Map<Column<?>, Object> columnValues = new LinkedHashMap<>();

    DefaultBuilder(Condition where) {
      this.where = requireNonNull(where);
    }

    @Override
    public <T> Builder set(Column<?> column, T value) {
      requireNonNull(column, "column");
      if (columnValues.containsKey(column)) {
        throw new IllegalStateException("Update already contains a value for column: " + column);
      }
      columnValues.put(column, value);

      return this;
    }

    @Override
    public Update build() {
      return new DefaultUpdate(this);
    }
  }
}
