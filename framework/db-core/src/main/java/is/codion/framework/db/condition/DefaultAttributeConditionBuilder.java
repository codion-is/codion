/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.framework.domain.entity.Attribute;

import java.util.Collection;

import static is.codion.common.Operator.*;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

final class DefaultAttributeConditionBuilder<T> implements AttributeCondition.Builder<T> {

  private static final String VALUES_PARAMETER = "values";

  private final Attribute<T> attribute;

  DefaultAttributeConditionBuilder(Attribute<T> attribute) {
    this.attribute = requireNonNull(attribute, "attribute");
  }

  @Override
  public AttributeCondition<T> equalTo(T value) {
    if (value == null) {
      return isNull();
    }

    return new SingleValueAttributeCondition<>(attribute, value, EQUAL);
  }

  @Override
  public AttributeCondition<T> notEqualTo(T value) {
    if (value == null) {
      return isNotNull();
    }

    return new SingleValueAttributeCondition<>(attribute, value, NOT_EQUAL);
  }

  @Override
  public AttributeCondition<String> equalToIgnoreCase(String value) {
    if (value == null) {
      return (AttributeCondition<String>) isNull();
    }

    return new SingleValueAttributeCondition<>((Attribute<String>) attribute, value, EQUAL, false);
  }

  @Override
  public AttributeCondition<String> notEqualToIgnoreCase(String value) {
    if (value == null) {
      return (AttributeCondition<String>) isNotNull();
    }

    return new SingleValueAttributeCondition<>((Attribute<String>) attribute, value, NOT_EQUAL, false);
  }

  @Override
  public AttributeCondition<T> in(T... values) {
    requireNonNull(values, VALUES_PARAMETER);

    return in(asList(values));
  }

  @Override
  public AttributeCondition<T> notIn(T... values) {
    requireNonNull(values, VALUES_PARAMETER);

    return notIn(asList(values));
  }

  @Override
  public AttributeCondition<T> in(Collection<? extends T> values) {
    requireNonNull(values, VALUES_PARAMETER);
    if (values.isEmpty()) {
      return isNull();
    }
    if (values.size() == 1) {
      return equalTo(values.iterator().next());
    }

    return new MultiValueAttributeCondition<>(attribute, values, EQUAL);
  }

  @Override
  public AttributeCondition<T> notIn(Collection<? extends T> values) {
    requireNonNull(values, VALUES_PARAMETER);
    if (values.isEmpty()) {
      return isNotNull();
    }
    if (values.size() == 1) {
      return notEqualTo(values.iterator().next());
    }

    return new MultiValueAttributeCondition<>(attribute, values, NOT_EQUAL);
  }

  @Override
  public AttributeCondition<String> inIgnoreCase(String... values) {
    requireNonNull(values, VALUES_PARAMETER);

    return inIgnoreCase(asList(values));
  }

  @Override
  public AttributeCondition<String> notInIgnoreCase(String... values) {
    requireNonNull(values, VALUES_PARAMETER);

    return notInIgnoreCase(asList(values));
  }

  @Override
  public AttributeCondition<String> inIgnoreCase(Collection<String> values) {
    requireNonNull(values, VALUES_PARAMETER);
    if (values.isEmpty()) {
      return (AttributeCondition<String>) isNull();
    }
    if (values.size() == 1) {
      return equalToIgnoreCase(values.iterator().next());
    }

    return new MultiValueAttributeCondition<>((Attribute<String>) attribute, values, EQUAL, false);
  }

  @Override
  public AttributeCondition<String> notInIgnoreCase(Collection<String> values) {
    requireNonNull(values, VALUES_PARAMETER);
    if (values.isEmpty()) {
      return (AttributeCondition<String>) isNotNull();
    }
    if (values.size() == 1) {
      return notEqualToIgnoreCase(values.iterator().next());
    }

    return new MultiValueAttributeCondition<>((Attribute<String>) attribute, values, NOT_EQUAL, false);
  }

  @Override
  public AttributeCondition<T> lessThan(T value) {
    return new SingleValueAttributeCondition<>(attribute, value, LESS_THAN);
  }

  @Override
  public AttributeCondition<T> lessThanOrEqualTo(T value) {
    return new SingleValueAttributeCondition<>(attribute, value, LESS_THAN_OR_EQUAL);
  }

  @Override
  public AttributeCondition<T> greaterThan(T value) {
    return new SingleValueAttributeCondition<>(attribute, value, GREATER_THAN);
  }

  @Override
  public AttributeCondition<T> greaterThanOrEqualTo(T value) {
    return new SingleValueAttributeCondition<>(attribute, value, GREATER_THAN_OR_EQUAL);
  }

  @Override
  public AttributeCondition<T> betweenExclusive(T lowerBound, T upperBound) {
    return new DualValueAttributeCondition<>(attribute, lowerBound, upperBound, BETWEEN_EXCLUSIVE);
  }

  @Override
  public AttributeCondition<T> between(T lowerBound, T upperBound) {
    return new DualValueAttributeCondition<>(attribute, lowerBound, upperBound, BETWEEN);
  }

  @Override
  public AttributeCondition<T> notBetweenExclusive(T lowerBound, T upperBound) {
    return new DualValueAttributeCondition<>(attribute, lowerBound, upperBound, NOT_BETWEEN_EXCLUSIVE);
  }

  @Override
  public AttributeCondition<T> notBetween(T lowerBound, T upperBound) {
    return new DualValueAttributeCondition<>(attribute, lowerBound, upperBound, NOT_BETWEEN);
  }

  @Override
  public AttributeCondition<T> isNull() {
    return new SingleValueAttributeCondition<>(attribute, null, EQUAL);
  }

  @Override
  public AttributeCondition<T> isNotNull() {
    return new SingleValueAttributeCondition<>(attribute, null, NOT_EQUAL);
  }
}
