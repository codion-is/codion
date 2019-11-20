/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.event;

final class DefaultEvent<T> implements Event<T> {

  private final Object lock = new Object();
  private DefaultObserver<T> observer;

  @Override
  public void fire() {
    fire(null);
  }

  @Override
  public void fire(final T data) {
    synchronized (lock) {
      if (observer != null && observer.hasListeners()) {
        for (final EventListener listener : observer.getEventListeners()) {
          listener.eventOccurred();
        }
        for (final EventDataListener<T> dataListener : observer.getEventDataListeners()) {
          dataListener.eventOccurred(data);
        }
      }
    }
  }

  @Override
  public void eventOccurred() {
    eventOccurred(null);
  }

  @Override
  public void eventOccurred(final T data) {
    fire(data);
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
