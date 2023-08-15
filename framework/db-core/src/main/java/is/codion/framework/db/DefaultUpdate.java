/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db;

import is.codion.framework.db.EntityConnection.Update;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.domain.entity.attribute.Column;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;

final class DefaultUpdate implements Update, Serializable {

  private static final long serialVersionUID = 1;

  private final Condition condition;
  private final Map<Column<?>, Object> columnValues;

  private DefaultUpdate(DefaultUpdate.DefaultBuilder builder) {
    this.condition = builder.condition;
    this.columnValues = unmodifiableMap(builder.columnValues);
  }

  @Override
  public Condition condition() {
    return condition;
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
    return Objects.equals(condition, that.condition) &&
            Objects.equals(columnValues, that.columnValues);
  }

  @Override
  public int hashCode() {
    return Objects.hash(condition, columnValues);
  }

  static final class DefaultBuilder implements Update.Builder {

    private final Condition condition;
    private final Map<Column<?>, Object> columnValues = new LinkedHashMap<>();

    DefaultBuilder(Condition condition) {
      this.condition = requireNonNull(condition);
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
