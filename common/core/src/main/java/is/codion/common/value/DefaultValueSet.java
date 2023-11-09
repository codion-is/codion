/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.common.value;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;

final class DefaultValueSet<T> extends AbstractValue<Set<T>> implements ValueSet<T> {

  private final Set<T> values = new LinkedHashSet<>();
  private final Value<T> value = new SingleValue();

  DefaultValueSet(Set<T> initialValues, Notify notify) {
    super(emptySet(), notify);
    set(requireNonNull(initialValues, "initialValues"));
  }

  @Override
  public void set(Collection<T> values) {
    set(values == null ? null : new LinkedHashSet<>(values));
  }

  @Override
  public Set<T> get() {
    return unmodifiableSet(new LinkedHashSet<>(values));
  }

  @Override
  public boolean add(T value) {
    Set<T> newValues = new LinkedHashSet<>(values);
    boolean added = newValues.add(value);
    set(newValues);

    return added;
  }

  @Override
  public boolean addAll(T... values) {
    requireNonNull(values);
    Set<T> newValues = new LinkedHashSet<>(this.values);
    boolean added = false;
    for (T value : values) {
      added = newValues.add(value) || added;
    }
    set(newValues);

    return added;
  }

  @Override
  public boolean remove(T value) {
    Set<T> newValues = new LinkedHashSet<>(values);
    boolean removed = newValues.remove(value);
    set(newValues);

    return removed;
  }

  @Override
  public boolean removeAll(T... values) {
    requireNonNull(values);
    Set<T> newValues = new LinkedHashSet<>(this.values);
    boolean removed = false;
    for (T value : values) {
      removed = newValues.remove(value) || removed;
    }
    set(newValues);

    return removed;
  }

  @Override
  public boolean empty() {
    return values.isEmpty();
  }

  @Override
  public boolean notEmpty() {
    return !empty();
  }

  @Override
  public void clear() {
    set(emptySet());
  }

  @Override
  public Value<T> value() {
    return value;
  }

  @Override
  public boolean isNull() {
    return false;
  }

  @Override
  protected void setValue(Set<T> values) {
    this.values.clear();
    this.values.addAll(values);
  }

  private class SingleValue extends AbstractValue<T> {

    private SingleValue() {
      super(null);
      DefaultValueSet.this.addListener(this::notifyListeners);
    }

    @Override
    public T get() {
      Set<T> set = DefaultValueSet.this.get();

      return set.isEmpty() ? null : set.iterator().next();
    }

    @Override
    protected void setValue(T value) {
      DefaultValueSet.this.set(value == null ? emptySet() : singleton(value));
    }
  }
}
