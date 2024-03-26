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
 * Copyright (c) 2019 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.value;

import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

class DefaultValueObserver<T> implements ValueObserver<T> {

  private final Value<T> value;

  DefaultValueObserver(Value<T> value) {
    this.value = requireNonNull(value, "value");
  }

  @Override
  public final T get() {
    return value.get();
  }

  @Override
  public final boolean nullable() {
    return value.nullable();
  }

  @Override
  public final boolean addListener(Runnable listener) {
    return value.addListener(listener);
  }

  @Override
  public final boolean removeListener(Runnable listener) {
    return value.removeListener(listener);
  }

  @Override
  public final boolean addDataListener(Consumer<? super T> listener) {
    return value.addDataListener(listener);
  }

  @Override
  public final boolean removeDataListener(Consumer<? super T> listener) {
    return value.removeDataListener(listener);
  }

  @Override
  public final boolean addWeakListener(Runnable listener) {
    return value.addWeakListener(listener);
  }

  @Override
  public final boolean removeWeakListener(Runnable listener) {
    return value.removeWeakListener(listener);
  }

  @Override
  public final boolean addWeakDataListener(Consumer<? super T> listener) {
    return value.addWeakDataListener(listener);
  }

  @Override
  public final boolean removeWeakDataListener(Consumer<? super T> listener) {
    return value.removeWeakDataListener(listener);
  }

  protected final <V extends Value<T>> V value() {
    return (V) value;
  }
}
