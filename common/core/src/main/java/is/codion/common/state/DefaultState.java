/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.state;

import is.codion.common.value.Value;
import is.codion.common.value.ValueObserver;

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
  public boolean set(Boolean value) {
    synchronized (this.value) {
      return this.value.set(value);
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
  public void addValidator(Validator<Boolean> validator) {
    this.value.addValidator(validator);
  }

  @Override
  public void removeValidator(Validator<Boolean> validator) {
    this.value.removeValidator(validator);
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
    return observer().removeListener(listener);
  }

  @Override
  public boolean addDataListener(Consumer<Boolean> listener) {
    return observer().addDataListener(listener);
  }

  @Override
  public boolean removeDataListener(Consumer<Boolean> listener) {
    return observer().removeDataListener(listener);
  }

  @Override
  public boolean addWeakListener(Runnable listener) {
    return observer().addWeakListener(listener);
  }

  @Override
  public boolean removeWeakListener(Runnable listener) {
    return observer().removeWeakListener(listener);
  }

  @Override
  public boolean addWeakDataListener(Consumer<Boolean> listener) {
    return observer().addWeakDataListener(listener);
  }

  @Override
  public boolean removeWeakDataListener(Consumer<Boolean> listener) {
    return observer().removeWeakDataListener(listener);
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
