/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.event;

final class DefaultEvent<T> implements Event<T> {

  private final Object lock = new Object();
  private DefaultObserver<T> observer;

  @Override
  public void onEvent() {
    onEvent(null);
  }

  @Override
  public void onEvent(final T data) {
    if (observer != null && observer.hasListeners()) {
      for (final EventListener listener : observer.getEventListeners()) {
        listener.onEvent();
      }
      for (final EventDataListener<T> dataListener : observer.getEventDataListeners()) {
        dataListener.onEvent(data);
      }
    }

  }

  @Override
  public EventObserver<T> getObserver() {
    synchronized (lock) {
      if (observer == null) {
        observer = new DefaultObserver<>();
      }

      return observer;
    }
  }

  @Override
  public void addListener(final EventListener listener) {
    getObserver().addListener(listener);
  }

  @Override
  public void removeListener(final EventListener listener) {
    getObserver().removeListener(listener);
  }

  @Override
  public void addDataListener(final EventDataListener<T> listener) {
    getObserver().addDataListener(listener);
  }

  @Override
  public void removeDataListener(final EventDataListener listener) {
    getObserver().removeDataListener(listener);
  }
}
