/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.framework.domain.entity.Attribute;

import java.util.Collection;

import static is.codion.common.Operator.*;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

final class DefaultAttributeCriteriaBuilder<T> implements AttributeCriteria.Builder<T> {

  private static final String VALUES_PARAMETER = "values";

  private final Attribute<T> attribute;

  DefaultAttributeCriteriaBuilder(Attribute<T> attribute) {
    this.attribute = requireNonNull(attribute, "attribute");
  }

  @Override
  public AttributeCriteria<T> equalTo(T value) {
    if (value == null) {
      return isNull();
    }

    return new SingleValueAttributeCriteria<>(attribute, value, EQUAL);
  }

  @Override
  public AttributeCriteria<T> notEqualTo(T value) {
    if (value == null) {
      return isNotNull();
    }

    return new SingleValueAttributeCriteria<>(attribute, value, NOT_EQUAL);
  }

  @Override
  public AttributeCriteria<String> equalToIgnoreCase(String value) {
    if (value == null) {
      return (AttributeCriteria<String>) isNull();
    }

    return new SingleValueAttributeCriteria<>((Attribute<String>) attribute, value, EQUAL, false);
  }

  @Override
  public AttributeCriteria<String> notEqualToIgnoreCase(String value) {
    if (value == null) {
      return (AttributeCriteria<String>) isNotNull();
    }

    return new SingleValueAttributeCriteria<>((Attribute<String>) attribute, value, NOT_EQUAL, false);
  }

  @Override
  public AttributeCriteria<T> in(T... values) {
    return in(asList(requireNonNull(values, VALUES_PARAMETER)));
  }

  @Override
  public AttributeCriteria<T> notIn(T... values) {
    return notIn(asList(requireNonNull(values, VALUES_PARAMETER)));
  }

  @Override
  public AttributeCriteria<T> in(Collection<? extends T> values) {
    if (singleStringValue(values)) {
      return equalTo(values.iterator().next());
    }

    return new MultiValueAttributeCriteria<>(attribute, values, EQUAL);
  }

  @Override
  public AttributeCriteria<T> notIn(Collection<? extends T> values) {
    if (singleStringValue(values)) {
      return notEqualTo(values.iterator().next());
    }

    return new MultiValueAttributeCriteria<>(attribute, values, NOT_EQUAL);
  }

  @Override
  public AttributeCriteria<String> inIgnoreCase(String... values) {
    return inIgnoreCase(asList(requireNonNull(values, VALUES_PARAMETER)));
  }

  @Override
  public AttributeCriteria<String> notInIgnoreCase(String... values) {
    return notInIgnoreCase(asList(requireNonNull(values, VALUES_PARAMETER)));
  }

  @Override
  public AttributeCriteria<String> inIgnoreCase(Collection<String> values) {
    if (singleStringValue(values)) {
      return equalToIgnoreCase(values.iterator().next());
    }

    return new MultiValueAttributeCriteria<>((Attribute<String>) attribute, values, EQUAL, false);
  }

  @Override
  public AttributeCriteria<String> notInIgnoreCase(Collection<String> values) {
    if (singleStringValue(values)) {
      return notEqualToIgnoreCase(values.iterator().next());
    }

    return new MultiValueAttributeCriteria<>((Attribute<String>) attribute, values, NOT_EQUAL, false);
  }

  @Override
  public AttributeCriteria<T> lessThan(T value) {
    return new SingleValueAttributeCriteria<>(attribute, value, LESS_THAN);
  }

  @Override
  public AttributeCriteria<T> lessThanOrEqualTo(T value) {
    return new SingleValueAttributeCriteria<>(attribute, value, LESS_THAN_OR_EQUAL);
  }

  @Override
  public AttributeCriteria<T> greaterThan(T value) {
    return new SingleValueAttributeCriteria<>(attribute, value, GREATER_THAN);
  }

  @Override
  public AttributeCriteria<T> greaterThanOrEqualTo(T value) {
    return new SingleValueAttributeCriteria<>(attribute, value, GREATER_THAN_OR_EQUAL);
  }

  @Override
  public AttributeCriteria<T> betweenExclusive(T lowerBound, T upperBound) {
    return new DualValueAttributeCondition<>(attribute, lowerBound, upperBound, BETWEEN_EXCLUSIVE);
  }

  @Override
  public AttributeCriteria<T> between(T lowerBound, T upperBound) {
    return new DualValueAttributeCondition<>(attribute, lowerBound, upperBound, BETWEEN);
  }

  @Override
  public AttributeCriteria<T> notBetweenExclusive(T lowerBound, T upperBound) {
    return new DualValueAttributeCondition<>(attribute, lowerBound, upperBound, NOT_BETWEEN_EXCLUSIVE);
  }

  @Override
  public AttributeCriteria<T> notBetween(T lowerBound, T upperBound) {
    return new DualValueAttributeCondition<>(attribute, lowerBound, upperBound, NOT_BETWEEN);
  }

  @Override
  public AttributeCriteria<T> isNull() {
    return new SingleValueAttributeCriteria<>(attribute, null, EQUAL);
  }

  @Override
  public AttributeCriteria<T> isNotNull() {
    return new SingleValueAttributeCriteria<>(attribute, null, NOT_EQUAL);
  }

  private <T> boolean singleStringValue(Collection<T> values) {
    return requireNonNull(values, VALUES_PARAMETER).size() == 1 && attribute.isString();
  }
}
