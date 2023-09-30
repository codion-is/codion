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
package is.codion.common.state;

import is.codion.common.value.Value;
import is.codion.common.value.ValueObserver;

import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;

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
  public void set(Boolean value) {
    synchronized (this.value) {
      this.value.set(value);
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
  public Set<Value<Boolean>> linkedValues() {
    return this.value.linkedValues();
  }

  @Override
  public void addValidator(Validator<Boolean> validator) {
    this.value.addValidator(validator);
  }

  @Override
  public void removeValidator(Validator<Boolean> validator) {
    this.value.removeValidator(validator);
  }

  @Override
  public Collection<Validator<Boolean>> validators() {
    return this.value.validators();
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
  public void addListener(Runnable listener) {
    observer().addListener(listener);
  }

  @Override
  public void removeListener(Runnable listener) {
    observer().removeListener(listener);
  }

  @Override
  public void addDataListener(Consumer<Boolean> listener) {
    observer().addDataListener(listener);
  }

  @Override
  public void removeDataListener(Consumer<Boolean> listener) {
    observer().removeDataListener(listener);
  }

  @Override
  public void addWeakListener(Runnable listener) {
    observer().addWeakListener(listener);
  }

  @Override
  public void removeWeakListener(Runnable listener) {
    observer().removeWeakListener(listener);
  }

  @Override
  public void addWeakDataListener(Consumer<Boolean> listener) {
    observer().addWeakDataListener(listener);
  }

  @Override
  public void removeWeakDataListener(Consumer<Boolean> listener) {
    observer().removeWeakDataListener(listener);
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
