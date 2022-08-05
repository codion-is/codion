/*
 * Copyright (c) 2019 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.event;

final class DefaultEvent<T> implements Event<T> {

  private final Object lock = new Object();
  private DefaultEventObserver<T> observer;

  @Override
  public void onEvent() {
    onEvent(null);
  }

  @Override
  public void onEvent(T data) {
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
  public void addListener(EventListener listener) {
    observer().addListener(listener);
  }

  @Override
  public void removeListener(EventListener listener) {
    observer().removeListener(listener);
  }

  @Override
  public void addDataListener(EventDataListener<T> listener) {
    observer().addDataListener(listener);
  }

  @Override
  public void removeDataListener(EventDataListener<T> listener) {
    observer().removeDataListener(listener);
  }
}
