/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson.
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
            "columnValues=" + columnValues +
            '}';
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
