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
  private final Map<Column<?>, Object> values;

  private DefaultUpdate(DefaultBuilder builder) {
    this.where = builder.where;
    this.values = unmodifiableMap(builder.values);
  }

  @Override
  public Condition where() {
    return where;
  }

  @Override
  public Map<Column<?>, Object> values() {
    return values;
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
            Objects.equals(values, that.values);
  }

  @Override
  public int hashCode() {
    return Objects.hash(where, values);
  }

  @Override
  public String toString() {
    return "Update{" +
            "where=" + where +
            ", values=" + values + "}";
  }

  static final class DefaultBuilder implements Update.Builder {

    private final Condition where;
    private final Map<Column<?>, Object> values = new LinkedHashMap<>();

    DefaultBuilder(Condition where) {
      this.where = requireNonNull(where);
    }

    @Override
    public <T> Builder set(Column<?> column, T value) {
      requireNonNull(column, "column");
      if (values.containsKey(column)) {
        throw new IllegalStateException("Update already contains a value for column: " + column);
      }
      values.put(column, value);

      return this;
    }

    @Override
    public Update build() {
      if (values.isEmpty()) {
        throw new IllegalStateException("No values provided for update");
      }

      return new DefaultUpdate(this);
    }
  }
}
