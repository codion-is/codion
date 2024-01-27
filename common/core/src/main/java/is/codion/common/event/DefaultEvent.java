/*
 * Copyright (c) 2019 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
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
  public boolean addListener(Runnable listener) {
    return observer().addListener(listener);
  }

  @Override
  public boolean removeListener(Runnable listener) {
    return observer().removeListener(listener);
  }

  @Override
  public boolean addDataListener(Consumer<? super T> listener) {
    return observer().addDataListener(listener);
  }

  @Override
  public boolean removeDataListener(Consumer<? super T> listener) {
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
  public boolean addWeakDataListener(Consumer<? super T> listener) {
    return observer().addWeakDataListener(listener);
  }

  @Override
  public boolean removeWeakDataListener(Consumer<? super T> listener) {
    return observer().removeWeakDataListener(listener);
  }
}
