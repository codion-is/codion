/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.event;

import java.util.function.Consumer;

final class DefaultEvent<T> implements Event<T> {

  private final Object lock = new Object();
  private DefaultEventObserver<T> observer;

  @Override
  public void run() {
    accept(null);
  }

  @Override
  public void accept(T data) {
    if (observer != null) {
      observer.notifyListeners(data);
    }
  }

  @Override
  public EventObserver<T> observer() {
    synchronized (lock) {
      if (observer == null) {
        observer = new DefaultEventObserver<>();
      }

      return observer;
    }
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
  public void addDataListener(Consumer<T> listener) {
    observer().addDataListener(listener);
  }

  @Override
  public void removeDataListener(Consumer<T> listener) {
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
  public void addWeakDataListener(Consumer<T> listener) {
    observer().addWeakDataListener(listener);
  }

  @Override
  public void removeWeakDataListener(Consumer<T> listener) {
    observer().removeWeakDataListener(listener);
  }
}
