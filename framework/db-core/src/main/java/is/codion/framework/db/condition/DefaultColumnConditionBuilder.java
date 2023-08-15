/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.framework.domain.entity.attribute.Column;

import java.util.Collection;

import static is.codion.common.Operator.*;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

final class DefaultColumnConditionBuilder<T> implements ColumnCondition.Builder<T> {

  private static final String VALUES_PARAMETER = "values";

  private final Column<T> column;

  DefaultColumnConditionBuilder(Column<T> column) {
    this.column = requireNonNull(column, "column");
  }

  @Override
  public ColumnCondition<T> equalTo(T value) {
    if (value == null) {
      return isNull();
    }

    return new SingleValueColumnCondition<>(column, value, EQUAL);
  }

  @Override
  public ColumnCondition<T> notEqualTo(T value) {
    if (value == null) {
      return isNotNull();
    }

    return new SingleValueColumnCondition<>(column, value, NOT_EQUAL);
  }

  @Override
  public ColumnCondition<String> equalToIgnoreCase(String value) {
    if (value == null) {
      return (ColumnCondition<String>) isNull();
    }

    return new SingleValueColumnCondition<>((Column<String>) column, value, EQUAL, false, false);
  }

  @Override
  public ColumnCondition<String> notEqualToIgnoreCase(String value) {
    if (value == null) {
      return (ColumnCondition<String>) isNotNull();
    }

    return new SingleValueColumnCondition<>((Column<String>) column, value, NOT_EQUAL, false, false);
  }

  @Override
  public ColumnCondition<String> like(String value) {
    if (value == null) {
      return (ColumnCondition<String>) isNull();
    }

    return new SingleValueColumnCondition<>((Column<String>) column, value, EQUAL, true, true);
  }

  @Override
  public ColumnCondition<String> notLike(String value) {
    if (value == null) {
      return (ColumnCondition<String>) isNotNull();
    }

    return new SingleValueColumnCondition<>((Column<String>) column, value, NOT_EQUAL, true, true);
  }

  @Override
  public ColumnCondition<String> likeIgnoreCase(String value) {
    if (value == null) {
      return (ColumnCondition<String>) isNull();
    }

    return new SingleValueColumnCondition<>((Column<String>) column, value, EQUAL, false, true);
  }

  @Override
  public ColumnCondition<String> notLikeIgnoreCase(String value) {
    if (value == null) {
      return (ColumnCondition<String>) isNotNull();
    }

    return new SingleValueColumnCondition<>((Column<String>) column, value, NOT_EQUAL, false, true);
  }

  @Override
  public ColumnCondition<T> in(T... values) {
    return in(asList(requireNonNull(values, VALUES_PARAMETER)));
  }

  @Override
  public ColumnCondition<T> notIn(T... values) {
    return notIn(asList(requireNonNull(values, VALUES_PARAMETER)));
  }

  @Override
  public ColumnCondition<T> in(Collection<? extends T> values) {
    return new MultiValueColumnCondition<>(column, values, EQUAL);
  }

  @Override
  public ColumnCondition<T> notIn(Collection<? extends T> values) {
    return new MultiValueColumnCondition<>(column, values, NOT_EQUAL);
  }

  @Override
  public ColumnCondition<String> inIgnoreCase(String... values) {
    return inIgnoreCase(asList(requireNonNull(values, VALUES_PARAMETER)));
  }

  @Override
  public ColumnCondition<String> notInIgnoreCase(String... values) {
    return notInIgnoreCase(asList(requireNonNull(values, VALUES_PARAMETER)));
  }

  @Override
  public ColumnCondition<String> inIgnoreCase(Collection<String> values) {
    return new MultiValueColumnCondition<>((Column<String>) column, values, EQUAL, false);
  }

  @Override
  public ColumnCondition<String> notInIgnoreCase(Collection<String> values) {
    return new MultiValueColumnCondition<>((Column<String>) column, values, NOT_EQUAL, false);
  }

  @Override
  public ColumnCondition<T> lessThan(T value) {
    return new SingleValueColumnCondition<>(column, value, LESS_THAN);
  }

  @Override
  public ColumnCondition<T> lessThanOrEqualTo(T value) {
    return new SingleValueColumnCondition<>(column, value, LESS_THAN_OR_EQUAL);
  }

  @Override
  public ColumnCondition<T> greaterThan(T value) {
    return new SingleValueColumnCondition<>(column, value, GREATER_THAN);
  }

  @Override
  public ColumnCondition<T> greaterThanOrEqualTo(T value) {
    return new SingleValueColumnCondition<>(column, value, GREATER_THAN_OR_EQUAL);
  }

  @Override
  public ColumnCondition<T> betweenExclusive(T lowerBound, T upperBound) {
    return new DualValueColumnCondition<>(column, lowerBound, upperBound, BETWEEN_EXCLUSIVE);
  }

  @Override
  public ColumnCondition<T> between(T lowerBound, T upperBound) {
    return new DualValueColumnCondition<>(column, lowerBound, upperBound, BETWEEN);
  }

  @Override
  public ColumnCondition<T> notBetweenExclusive(T lowerBound, T upperBound) {
    return new DualValueColumnCondition<>(column, lowerBound, upperBound, NOT_BETWEEN_EXCLUSIVE);
  }

  @Override
  public ColumnCondition<T> notBetween(T lowerBound, T upperBound) {
    return new DualValueColumnCondition<>(column, lowerBound, upperBound, NOT_BETWEEN);
  }

  @Override
  public ColumnCondition<T> isNull() {
    return new SingleValueColumnCondition<>(column, null, EQUAL);
  }

  @Override
  public ColumnCondition<T> isNotNull() {
    return new SingleValueColumnCondition<>(column, null, NOT_EQUAL);
  }
}
