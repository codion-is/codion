/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.framework.domain.entity.Attribute;

import java.util.Collection;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

final class DefaultAttributeConditionBuilder<T> implements AttributeCondition.Builder<T> {

  private final Attribute<T> attribute;

  DefaultAttributeConditionBuilder(final Attribute<T> attribute) {
    this.attribute = requireNonNull(attribute, "attribute");
  }

  @Override
  public AttributeCondition<T> equalTo(final T value) {
    if (value == null) {
      throw new IllegalArgumentException("equalTo condition value can not be null");
    }

    return equalTo(singletonList(value));
  }

  @Override
  public AttributeCondition<T> equalTo(final T... values) {
    if (values == null) {
      throw new IllegalArgumentException("equalTo condition values can not be null");
    }

    return equalTo(asList(values));
  }

  @Override
  public AttributeCondition<T> equalTo(final Collection<? extends T> values) {
    if (values == null || values.isEmpty()) {
      throw new IllegalArgumentException("equalTo condition must contain at least one value");
    }

    return new DefaultAttributeEqualCondition<>(attribute, values);
  }

  @Override
  public AttributeCondition<T> notEqualTo(final T value) {
    if (value == null) {
      throw new IllegalArgumentException("notEqualTo condition value can not be null");
    }

    return notEqualTo(singletonList(value));
  }

  @Override
  public AttributeCondition<T> notEqualTo(final T... values) {
    if (values == null) {
      throw new IllegalArgumentException("notEqualTo condition values can not be null");
    }

    return notEqualTo(asList(values));
  }

  @Override
  public AttributeCondition<T> notEqualTo(final Collection<? extends T> values) {
    if (values == null || values.isEmpty()) {
      throw new IllegalArgumentException("notEqualTo condition must contain at least one value");
    }

    return new DefaultAttributeEqualCondition<>(attribute, values, true);
  }

  @Override
  public AttributeCondition<T> lessThan(final T value) {
    return new DefaultAttributeLessThanCondition<>(attribute, value, false);
  }

  @Override
  public AttributeCondition<T> lessThanOrEqualTo(final T value) {
    return new DefaultAttributeLessThanCondition<>(attribute, value, true);
  }

  @Override
  public AttributeCondition<T> greaterThan(final T value) {
    return new DefaultAttributeGreaterThanCondition<>(attribute, value, false);
  }

  @Override
  public AttributeCondition<T> greaterThanOrEqualTo(final T value) {
    return new DefaultAttributeGreaterThanCondition<>(attribute, value, true);
  }

  @Override
  public AttributeCondition<T> betweenExclusive(final T lowerBound, final T upperBound) {
    return new DefaultAttributeBetweenCondition<>(attribute, lowerBound, upperBound, true);
  }

  @Override
  public AttributeCondition<T> between(final T lowerBound, final T upperBound) {
    return new DefaultAttributeBetweenCondition<>(attribute, lowerBound, upperBound, false);
  }

  @Override
  public AttributeCondition<T> notBetweenExclusive(final T lowerBound, final T upperBound) {
    return new DefaultAttributeNotBetweenCondition<>(attribute, lowerBound, upperBound, true);
  }

  @Override
  public AttributeCondition<T> notBetween(final T lowerBound, final T upperBound) {
    return new DefaultAttributeNotBetweenCondition<>(attribute, lowerBound, upperBound, false);
  }

  @Override
  public AttributeCondition<T> isNull() {
    return new DefaultAttributeEqualCondition<>(attribute, emptyList());
  }

  @Override
  public AttributeCondition<T> isNotNull() {
    return new DefaultAttributeEqualCondition<>(attribute, emptyList(), true);
  }
}
