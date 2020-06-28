/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.Util;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.Key;

import java.util.Collection;
import java.util.List;

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
    return new DefaultAttributeEqualCondition<>(attribute, singletonList(requireNonNull(value)));
  }

  @Override
  public AttributeCondition<T> equalTo(final T... values) {
    return new DefaultAttributeEqualCondition<>(attribute, asList(requireNonNull(values)));
  }

  @Override
  public AttributeCondition<T> equalTo(final Collection<? extends T> values) {
    return new DefaultAttributeEqualCondition<>(attribute, values);
  }

  @Override
  public AttributeCondition<T> notEqualTo(final T value) {
    return new DefaultAttributeEqualCondition<>(attribute, singletonList(requireNonNull(value)), true);
  }

  @Override
  public AttributeCondition<T> notEqualTo(final T... values) {
    return new DefaultAttributeEqualCondition<>(attribute, asList(requireNonNull(values)), true);
  }

  @Override
  public AttributeCondition<T> notEqualTo(final Collection<? extends T> values) {
    return new DefaultAttributeEqualCondition<>(attribute, values, true);
  }

  @Override
  public AttributeCondition<T> lessThan(final T value) {
    return new DefaultAttributeLessThanCondition<>(attribute, value);
  }

  @Override
  public AttributeCondition<T> greaterThan(final T value) {
    return new DefaultAttributeGreaterThanCondition<>(attribute, value);
  }

  @Override
  public AttributeCondition<T> withinRange(final T lowerBound, final T upperBound) {
    return new DefaultAttributeWithinRangeCondition<>(attribute, lowerBound, upperBound);
  }

  @Override
  public AttributeCondition<T> outsideRange(final T lowerBound, final T upperBound) {
    return new DefaultAttributeOutsideRangeCondition<>(attribute, lowerBound, upperBound);
  }

  @Override
  public AttributeCondition<T> isNull() {
    return new DefaultAttributeEqualCondition<>(attribute, emptyList());
  }

  @Override
  public AttributeCondition<T> isNotNull() {
    return new DefaultAttributeEqualCondition<>(attribute, emptyList(), true);
  }

  @Override
  public AttributeCondition<Entity> equalTo(final Key key) {
    return new DefaultAttributeEqualCondition<>((Attribute<Entity>) attribute, singletonList(key));
  }

  @Override
  public AttributeCondition<Entity> equalTo(final List<Key> keys) {
    checkKeysParameter(keys);
    return new DefaultAttributeEqualCondition<>((Attribute<Entity>) attribute, keys);
  }

  private static void checkKeysParameter(final List<Key> keys) {
    if (Util.nullOrEmpty(keys)) {
      throw new IllegalArgumentException("One or more keys must be provided for condition");
    }
  }
}
