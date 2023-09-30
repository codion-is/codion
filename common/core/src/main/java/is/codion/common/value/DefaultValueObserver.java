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
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.common.value;

import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

final class DefaultValueObserver<T> implements ValueObserver<T> {

  private final Value<T> value;

  DefaultValueObserver(Value<T> value) {
    this.value = requireNonNull(value, "value");
  }

  @Override
  public T get() {
    return value.get();
  }

  @Override
  public boolean nullable() {
    return value.nullable();
  }

  @Override
  public void addListener(Runnable listener) {
    value.addListener(listener);
  }

  @Override
  public void removeListener(Runnable listener) {
    value.removeListener(listener);
  }

  @Override
  public void addDataListener(Consumer<T> listener) {
    value.addDataListener(listener);
  }

  @Override
  public void removeDataListener(Consumer<T> listener) {
    value.removeDataListener(listener);
  }

  @Override
  public void addWeakListener(Runnable listener) {
    value.addWeakListener(listener);
  }

  @Override
  public void removeWeakListener(Runnable listener) {
    value.removeWeakListener(listener);
  }

  @Override
  public void addWeakDataListener(Consumer<T> listener) {
    value.addWeakDataListener(listener);
  }

  @Override
  public void removeWeakDataListener(Consumer<T> listener) {
    value.removeWeakDataListener(listener);
  }
}
