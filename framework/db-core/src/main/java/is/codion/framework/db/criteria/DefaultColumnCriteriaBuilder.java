/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.criteria;

import is.codion.framework.domain.entity.Column;

import java.util.Collection;

import static is.codion.common.Operator.*;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

final class DefaultColumnCriteriaBuilder<T> implements ColumnCriteria.Builder<T> {

  private static final String VALUES_PARAMETER = "values";

  private final Column<T> column;

  DefaultColumnCriteriaBuilder(Column<T> column) {
    this.column = requireNonNull(column, "column");
  }

  @Override
  public ColumnCriteria<T> equalTo(T value) {
    if (value == null) {
      return isNull();
    }

    return new SingleValueColumnCriteria<>(column, value, EQUAL);
  }

  @Override
  public ColumnCriteria<T> notEqualTo(T value) {
    if (value == null) {
      return isNotNull();
    }

    return new SingleValueColumnCriteria<>(column, value, NOT_EQUAL);
  }

  @Override
  public ColumnCriteria<String> equalToIgnoreCase(String value) {
    if (value == null) {
      return (ColumnCriteria<String>) isNull();
    }

    return new SingleValueColumnCriteria<>((Column<String>) column, value, EQUAL, false, false);
  }

  @Override
  public ColumnCriteria<String> notEqualToIgnoreCase(String value) {
    if (value == null) {
      return (ColumnCriteria<String>) isNotNull();
    }

    return new SingleValueColumnCriteria<>((Column<String>) column, value, NOT_EQUAL, false, false);
  }

  @Override
  public ColumnCriteria<String> like(String value) {
    if (value == null) {
      return (ColumnCriteria<String>) isNotNull();
    }

    return new SingleValueColumnCriteria<>((Column<String>) column, value, EQUAL, true, true);
  }

  @Override
  public ColumnCriteria<String> notLike(String value) {
    if (value == null) {
      return (ColumnCriteria<String>) isNotNull();
    }

    return new SingleValueColumnCriteria<>((Column<String>) column, value, NOT_EQUAL, true, true);
  }

  @Override
  public ColumnCriteria<String> likeIgnoreCase(String value) {
    if (value == null) {
      return (ColumnCriteria<String>) isNull();
    }

    return new SingleValueColumnCriteria<>((Column<String>) column, value, EQUAL, false, true);
  }

  @Override
  public ColumnCriteria<String> notLikeIgnoreCase(String value) {
    if (value == null) {
      return (ColumnCriteria<String>) isNotNull();
    }

    return new SingleValueColumnCriteria<>((Column<String>) column, value, NOT_EQUAL, false, true);
  }

  @Override
  public ColumnCriteria<T> in(T... values) {
    return in(asList(requireNonNull(values, VALUES_PARAMETER)));
  }

  @Override
  public ColumnCriteria<T> notIn(T... values) {
    return notIn(asList(requireNonNull(values, VALUES_PARAMETER)));
  }

  @Override
  public ColumnCriteria<T> in(Collection<? extends T> values) {
    if (singleStringValue(values)) {
      return equalTo(values.iterator().next());
    }

    return new MultiValueColumnCriteria<>(column, values, EQUAL);
  }

  @Override
  public ColumnCriteria<T> notIn(Collection<? extends T> values) {
    if (singleStringValue(values)) {
      return notEqualTo(values.iterator().next());
    }

    return new MultiValueColumnCriteria<>(column, values, NOT_EQUAL);
  }

  @Override
  public ColumnCriteria<String> inIgnoreCase(String... values) {
    return inIgnoreCase(asList(requireNonNull(values, VALUES_PARAMETER)));
  }

  @Override
  public ColumnCriteria<String> notInIgnoreCase(String... values) {
    return notInIgnoreCase(asList(requireNonNull(values, VALUES_PARAMETER)));
  }

  @Override
  public ColumnCriteria<String> inIgnoreCase(Collection<String> values) {
    if (singleStringValue(values)) {
      return equalToIgnoreCase(values.iterator().next());
    }

    return new MultiValueColumnCriteria<>((Column<String>) column, values, EQUAL, false);
  }

  @Override
  public ColumnCriteria<String> notInIgnoreCase(Collection<String> values) {
    if (singleStringValue(values)) {
      return notEqualToIgnoreCase(values.iterator().next());
    }

    return new MultiValueColumnCriteria<>((Column<String>) column, values, NOT_EQUAL, false);
  }

  @Override
  public ColumnCriteria<T> lessThan(T value) {
    return new SingleValueColumnCriteria<>(column, value, LESS_THAN);
  }

  @Override
  public ColumnCriteria<T> lessThanOrEqualTo(T value) {
    return new SingleValueColumnCriteria<>(column, value, LESS_THAN_OR_EQUAL);
  }

  @Override
  public ColumnCriteria<T> greaterThan(T value) {
    return new SingleValueColumnCriteria<>(column, value, GREATER_THAN);
  }

  @Override
  public ColumnCriteria<T> greaterThanOrEqualTo(T value) {
    return new SingleValueColumnCriteria<>(column, value, GREATER_THAN_OR_EQUAL);
  }

  @Override
  public ColumnCriteria<T> betweenExclusive(T lowerBound, T upperBound) {
    return new DualValueColumnCriteria<>(column, lowerBound, upperBound, BETWEEN_EXCLUSIVE);
  }

  @Override
  public ColumnCriteria<T> between(T lowerBound, T upperBound) {
    return new DualValueColumnCriteria<>(column, lowerBound, upperBound, BETWEEN);
  }

  @Override
  public ColumnCriteria<T> notBetweenExclusive(T lowerBound, T upperBound) {
    return new DualValueColumnCriteria<>(column, lowerBound, upperBound, NOT_BETWEEN_EXCLUSIVE);
  }

  @Override
  public ColumnCriteria<T> notBetween(T lowerBound, T upperBound) {
    return new DualValueColumnCriteria<>(column, lowerBound, upperBound, NOT_BETWEEN);
  }

  @Override
  public ColumnCriteria<T> isNull() {
    return new SingleValueColumnCriteria<>(column, null, EQUAL);
  }

  @Override
  public ColumnCriteria<T> isNotNull() {
    return new SingleValueColumnCriteria<>(column, null, NOT_EQUAL);
  }

  private <T> boolean singleStringValue(Collection<T> values) {
    return requireNonNull(values, VALUES_PARAMETER).size() == 1 && column.isString();
  }
}
