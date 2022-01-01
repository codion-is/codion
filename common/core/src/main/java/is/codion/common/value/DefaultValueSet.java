/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.value;

import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;

final class DefaultValueSet<T> extends AbstractValue<Set<T>> implements ValueSet<T> {

  private final Set<T> values = new HashSet<>();

  DefaultValueSet(final Set<T> initialValues) {
    super(emptySet(), NotifyOnSet.YES);
    values.addAll(requireNonNull(initialValues, "initialValues"));
  }

  @Override
  public Set<T> get() {
    return unmodifiableSet(values);
  }

  @Override
  public boolean add(final T value) {
    final Set<T> newValues = new HashSet<>(values);
    final boolean added = newValues.add(value);
    set(newValues);

    return added;
  }

  @Override
  public boolean remove(final T value) {
    final Set<T> newValues = new HashSet<>(values);
    final boolean removed = newValues.remove(value);
    set(newValues);

    return removed;
  }

  @Override
  public boolean isEmpty() {
    return values.isEmpty();
  }

  @Override
  public boolean isNotEmpty() {
    return !isEmpty();
  }

  @Override
  public void clear() {
    set(emptySet());
  }

  @Override
  public Value<T> value() {
    return new SingleValueSet<>(this);
  }

  @Override
  protected void setValue(final Set<T> values) {
    this.values.clear();
    this.values.addAll(values);
  }

  private static class SingleValueSet<T> extends AbstractValue<T> {

    private final ValueSet<T> valueSet;

    private SingleValueSet(final ValueSet<T> valueSet) {
      super(null, NotifyOnSet.NO);
      this.valueSet = valueSet;
      valueSet.addListener(this::notifyValueChange);
    }

    @Override
    public T get() {
      final Set<T> set = valueSet.get();

      return set.isEmpty() ? null : set.iterator().next();
    }

    @Override
    protected void setValue(final T value) {
      valueSet.set(value == null ? emptySet() : singleton(value));
    }
  }
}
