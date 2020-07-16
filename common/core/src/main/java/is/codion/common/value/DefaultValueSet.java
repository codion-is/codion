/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.value;

import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;

final class DefaultValueSet<V> extends AbstractValue<Set<V>> implements ValueSet<V> {

  private final Set<V> values = new HashSet<>();

  DefaultValueSet(final Set<V> initialValues) {
    values.addAll(requireNonNull(initialValues, "initialValues"));
  }

  @Override
  public void set(final Set<V> values) {
    this.values.clear();
    if (values != null) {
      this.values.addAll(values);
    }
    notifyValueChange();
  }

  @Override
  public Set<V> get() {
    return unmodifiableSet(values);
  }

  @Override
  public boolean add(final V value) {
    final boolean added = values.add(value);
    if (added) {
      notifyValueChange();
    }

    return added;
  }

  @Override
  public boolean remove(final V value) {
    final boolean removed = values.remove(value);
    if (removed) {
      notifyValueChange();
    }

    return removed;
  }

  @Override
  public void clear() {
    values.clear();
    notifyValueChange();
  }

  @Override
  public boolean isNullable() {
    return false;
  }

  @Override
  public Value<V> value() {
    return new SingleValueSet<>(this);
  }

  private static class SingleValueSet<V> extends AbstractValue<V> {

    private final ValueSet<V> valueSet;

    private SingleValueSet(final ValueSet<V> valueSet) {
      this.valueSet = valueSet;
    }

    @Override
    public void set(final V value) {
      valueSet.set(value == null ? emptySet() : singleton(value));
      notifyValueChange();
    }

    @Override
    public V get() {
      final Set<V> set = valueSet.get();

      return set.isEmpty() ? null : set.iterator().next();
    }

    @Override
    public boolean isNullable() {
      return true;
    }
  }
}
