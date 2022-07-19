/*
 * Copyright (c) 2020 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.value;

import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;

final class DefaultValueSet<T> extends AbstractValue<Set<T>> implements ValueSet<T> {

  private final Set<T> values = new HashSet<>();

  DefaultValueSet(Set<T> initialValues) {
    super(emptySet(), true);
    values.addAll(requireNonNull(initialValues, "initialValues"));
  }

  @Override
  public Set<T> get() {
    return unmodifiableSet(new HashSet<>(values));
  }

  @Override
  public boolean add(T value) {
    Set<T> newValues = new HashSet<>(values);
    boolean added = newValues.add(value);
    set(newValues);

    return added;
  }

  @Override
  public boolean remove(T value) {
    Set<T> newValues = new HashSet<>(values);
    boolean removed = newValues.remove(value);
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
  protected void setValue(Set<T> values) {
    this.values.clear();
    this.values.addAll(values);
  }

  private static class SingleValueSet<T> extends AbstractValue<T> {

    private final ValueSet<T> valueSet;

    private SingleValueSet(ValueSet<T> valueSet) {
      super(null, false);
      this.valueSet = valueSet;
      valueSet.addListener(this::notifyValueChange);
    }

    @Override
    public T get() {
      Set<T> set = valueSet.get();

      return set.isEmpty() ? null : set.iterator().next();
    }

    @Override
    protected void setValue(T value) {
      valueSet.set(value == null ? emptySet() : singleton(value));
    }
  }
}
