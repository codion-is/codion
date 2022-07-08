/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.state;

import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;
import is.codion.common.value.Value;
import is.codion.common.value.ValueObserver;

import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

final class DefaultState implements State {

  private final Value<Boolean> value;

  private DefaultStateObserver observer;

  DefaultState(boolean value) {
    this.value = Value.value(value, false);
    this.value.addDataListener(new Notifier());
  }

  DefaultState(Supplier<Boolean> getter, Consumer<Boolean> setter) {
    this.value = Value.value(getter, setter, false);
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
  public StateObserver getObserver() {
    synchronized (this.value) {
      if (observer == null) {
        observer = new DefaultStateObserver(this, false);
      }

      return observer;
    }
  }

  @Override
  public StateObserver getReversedObserver() {
    return getObserver().getReversedObserver();
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
  public void link(ValueObserver<Boolean> originalValueObserver) {
    this.value.link(originalValueObserver);
  }

  @Override
  public void unlink(ValueObserver<Boolean> originalValueObserver) {
    this.value.unlink(originalValueObserver);
  }

  @Override
  public Set<Value<Boolean>> getLinkedValues() {
    return this.value.getLinkedValues();
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
  public Collection<Validator<Boolean>> getValidators() {
    return this.value.getValidators();
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
  public boolean isNullable() {
    return false;
  }

  @Override
  public void onEvent(Boolean data) {
    set(data);
  }

  @Override
  public void addListener(EventListener listener) {
    getObserver().addListener(listener);
  }

  @Override
  public void removeListener(EventListener listener) {
    getObserver().removeListener(listener);
  }

  @Override
  public void addDataListener(EventDataListener<Boolean> listener) {
    getObserver().addDataListener(listener);
  }

  @Override
  public void removeDataListener(EventDataListener<Boolean> listener) {
    getObserver().removeDataListener(listener);
  }

  private final class Notifier implements EventDataListener<Boolean> {

    @Override
    public void onEvent(Boolean value) {
      synchronized (DefaultState.this.value) {
        if (observer != null) {
          observer.notifyObservers(value, !value);
        }
      }
    }
  }
}
