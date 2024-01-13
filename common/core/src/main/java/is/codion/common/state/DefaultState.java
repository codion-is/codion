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
package is.codion.common.state;

import is.codion.common.value.Value;
import is.codion.common.value.ValueObserver;

import java.util.function.Consumer;
import java.util.function.Function;

final class DefaultState implements State {

  private final Value<Boolean> value;

  private DefaultStateObserver observer;

  DefaultState(boolean value) {
    this.value = Value.value(value, false);
    this.value.addDataListener(new Notifier());
  }

  @Override
  public String toString() {
    return Boolean.toString(value.get());
  }

  @Override
  public Boolean get() {
    synchronized (this.value) {
      return this.value.get();
    }
  }

  @Override
  public boolean set(Boolean value) {
    synchronized (this.value) {
      return this.value.set(value);
    }
  }

  @Override
  public boolean map(Function<Boolean, Boolean> mapper) {
    synchronized (this.value) {
      return this.value.map(mapper);
    }
  }

  @Override
  public StateObserver observer() {
    synchronized (this.value) {
      if (observer == null) {
        observer = new DefaultStateObserver(this, false);
      }

      return observer;
    }
  }

  @Override
  public StateObserver not() {
    return observer().not();
  }

  @Override
  public void link(Value<Boolean> originalValue) {
    this.value.link(originalValue);
  }

  @Override
  public void unlink(Value<Boolean> originalValue) {
    this.value.unlink(originalValue);
  }

  @Override
  public void link(ValueObserver<Boolean> originalValue) {
    this.value.link(originalValue);
  }

  @Override
  public void unlink(ValueObserver<Boolean> originalValue) {
    this.value.unlink(originalValue);
  }

  @Override
  public boolean addValidator(Validator<Boolean> validator) {
    return this.value.addValidator(validator);
  }

  @Override
  public boolean removeValidator(Validator<Boolean> validator) {
    return this.value.removeValidator(validator);
  }

  @Override
  public void validate(Boolean value) {
    this.value.validate(value);
  }

  @Override
  public boolean isNull() {
    return false;
  }

  @Override
  public boolean isNotNull() {
    return true;
  }

  @Override
  public boolean nullable() {
    return false;
  }

  @Override
  public void accept(Boolean data) {
    set(data);
  }

  @Override
  public boolean addListener(Runnable listener) {
    return observer().addListener(listener);
  }

  @Override
  public boolean removeListener(Runnable listener) {
    if (observer != null) {
      return observer.removeListener(listener);
    }

    return false;
  }

  @Override
  public boolean addDataListener(Consumer<Boolean> listener) {
    return observer().addDataListener(listener);
  }

  @Override
  public boolean removeDataListener(Consumer<Boolean> listener) {
    if (observer != null) {
      return observer.removeDataListener(listener);
    }

    return false;
  }

  @Override
  public boolean addWeakListener(Runnable listener) {
    return observer().addWeakListener(listener);
  }

  @Override
  public boolean removeWeakListener(Runnable listener) {
    if (observer != null) {
      return observer.removeWeakListener(listener);
    }

    return false;
  }

  @Override
  public boolean addWeakDataListener(Consumer<Boolean> listener) {
    return observer().addWeakDataListener(listener);
  }

  @Override
  public boolean removeWeakDataListener(Consumer<Boolean> listener) {
    if (observer != null) {
      return observer.removeWeakDataListener(listener);
    }

    return false;
  }

  private final class Notifier implements Consumer<Boolean> {

    @Override
    public void accept(Boolean value) {
      synchronized (DefaultState.this.value) {
        if (observer != null) {
          observer.notifyObservers(value, !value);
        }
      }
    }
  }
}
