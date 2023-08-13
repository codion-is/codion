/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db;

import is.codion.framework.db.criteria.Criteria;
import is.codion.framework.domain.entity.Column;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;

final class DefaultUpdate implements Update, Serializable {

  private static final long serialVersionUID = 1;

  private final Criteria criteria;
  private final Map<Column<?>, Object> propertyValues;

  private DefaultUpdate(DefaultUpdate.DefaultBuilder builder) {
    this.criteria = builder.criteria;
    this.propertyValues = builder.columnValues;
  }

  @Override
  public Criteria criteria() {
    return criteria;
  }

  @Override
  public Map<Column<?>, Object> columnValues() {
    return unmodifiableMap(propertyValues);
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
    return Objects.equals(criteria, that.criteria) &&
            Objects.equals(propertyValues, that.propertyValues);
  }

  @Override
  public int hashCode() {
    return Objects.hash(criteria, propertyValues);
  }

  static final class DefaultBuilder implements Update.Builder {

    private final Criteria criteria;
    private final Map<Column<?>, Object> columnValues = new LinkedHashMap<>();

    DefaultBuilder(Update update) {
      this(requireNonNull(update).criteria());
      if (update instanceof DefaultUpdate) {
        DefaultUpdate updateCondition = (DefaultUpdate) update;
        columnValues.putAll(updateCondition.propertyValues);
      }
    }

    DefaultBuilder(Criteria criteria) {
      this.criteria = requireNonNull(criteria);
    }

    @Override
    public <T> Builder set(Column<?> column, T value) {
      requireNonNull(column, "column");
      if (columnValues.containsKey(column)) {
        throw new IllegalArgumentException("Update already contains a value for column: " + column);
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
