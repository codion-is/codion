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
    super(emptySet(), NotifyOnSet.YES);
    values.addAll(requireNonNull(initialValues, "initialValues"));
  }

  @Override
  public Set<V> get() {
    return unmodifiableSet(values);
  }

  @Override
  public boolean add(final V value) {
    final Set<V> newValues = new HashSet<>(values);
    final boolean added = newValues.add(value);
    set(newValues);

    return added;
  }

  @Override
  public boolean remove(final V value) {
    final Set<V> newValues = new HashSet<>(values);
    final boolean removed = newValues.remove(value);
    set(newValues);

    return removed;
  }

  @Override
  public void clear() {
    set(emptySet());
  }

  @Override
  public Value<V> value() {
    return new SingleValueSet<>(this);
  }

  @Override
  protected void doSet(final Set<V> values) {
    this.values.clear();
    this.values.addAll(values);
  }

  private static class SingleValueSet<V> extends AbstractValue<V> {

    private final ValueSet<V> valueSet;

    private SingleValueSet(final ValueSet<V> valueSet) {
      super(null, NotifyOnSet.YES);
      this.valueSet = valueSet;
      valueSet.addDataListener(set -> set(set.isEmpty() ? null : set.iterator().next()));
    }

    @Override
    public V get() {
      final Set<V> set = valueSet.get();

      return set.isEmpty() ? null : set.iterator().next();
    }

    @Override
    protected void doSet(final V value) {
      valueSet.set(value == null ? emptySet() : singleton(value));
    }
  }
}
