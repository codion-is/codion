/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.framework.domain.entity.Attribute;

import java.util.Collection;

import static is.codion.common.Operator.*;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
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

    return equalTo(singletonList(value));
  }

  @Override
  public AttributeCondition<T> equalTo(T... values) {
    requireNonNull(values, VALUES_PARAMETER);

    return equalTo(asList(values));
  }

  @Override
  public AttributeCondition<T> equalTo(Collection<? extends T> values) {
    requireNonNull(values, VALUES_PARAMETER);

    return new MultiValueAttributeCondition<>(attribute, values, EQUAL);
  }

  @Override
  public AttributeCondition<T> notEqualTo(T value) {
    if (value == null) {
      return isNotNull();
    }

    return notEqualTo(singletonList(value));
  }

  @Override
  public AttributeCondition<T> notEqualTo(T... values) {
    requireNonNull(values, VALUES_PARAMETER);

    return notEqualTo(asList(values));
  }

  @Override
  public AttributeCondition<T> notEqualTo(Collection<? extends T> values) {
    requireNonNull(values, VALUES_PARAMETER);

    return new MultiValueAttributeCondition<>(attribute, values, NOT_EQUAL);
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
    return new MultiValueAttributeCondition<>(attribute, emptyList(), EQUAL);
  }

  @Override
  public AttributeCondition<T> isNotNull() {
    return new MultiValueAttributeCondition<>(attribute, emptyList(), NOT_EQUAL);
  }
}
