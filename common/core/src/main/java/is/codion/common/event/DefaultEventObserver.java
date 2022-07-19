/*
 * Copyright (c) 2019 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.event;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

final class DefaultEventObserver<T> implements EventObserver<T> {

  private final Object lock = new Object();
  private Set<EventListener> listeners;
  private Set<EventDataListener<T>> dataListeners;

  @Override
  public void addDataListener(EventDataListener<T> listener) {
    synchronized (lock) {
      getDataListeners().add(requireNonNull(listener, "listener"));
    }
  }

  @Override
  public void removeDataListener(EventDataListener<T> listener) {
    synchronized (lock) {
      getDataListeners().remove(listener);
    }
  }

  @Override
  public void addListener(EventListener listener) {
    synchronized (lock) {
      getListeners().add(requireNonNull(listener, "listener"));
    }
  }

  @Override
  public void removeListener(EventListener listener) {
    synchronized (lock) {
      getListeners().remove(listener);
    }
  }

  void notifyListeners(T data) {
    for (EventListener listener : getEventListeners()) {
      listener.onEvent();
    }
    for (EventDataListener<T> dataListener : getEventDataListeners()) {
      dataListener.onEvent(data);
    }
  }

  private List<EventListener> getEventListeners() {
    synchronized (lock) {
      if (listeners != null && !listeners.isEmpty()) {
        return new ArrayList<>(listeners);
      }
    }

    return emptyList();
  }

  private List<EventDataListener<T>> getEventDataListeners() {
    synchronized (lock) {
      if (dataListeners != null && !dataListeners.isEmpty()) {
        return new ArrayList<>(dataListeners);
      }
    }

    return emptyList();
  }

  private Set<EventListener> getListeners() {
    if (listeners == null) {
      listeners = new LinkedHashSet<>(1);
    }

    return listeners;
  }

  private Set<EventDataListener<T>> getDataListeners() {
    if (dataListeners == null) {
      dataListeners = new LinkedHashSet<>(1);
    }

    return dataListeners;
  }
}
