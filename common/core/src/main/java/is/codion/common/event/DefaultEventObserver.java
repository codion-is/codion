/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.event;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

final class DefaultEventObserver<T> implements EventObserver<T> {

  private static final String LISTENER = "listener";

  private final Object lock = new Object();

  private Set<EventListener> listeners;
  private Set<EventDataListener<T>> dataListeners;
  private Set<WeakReference<EventListener>> weakListeners;
  private Set<WeakReference<EventDataListener<T>>> weakDataListeners;

  @Override
  public void addDataListener(EventDataListener<T> listener) {
    synchronized (lock) {
      dataListeners().add(requireNonNull(listener, LISTENER));
    }
  }

  @Override
  public void removeDataListener(EventDataListener<T> listener) {
    synchronized (lock) {
      dataListeners().remove(listener);
    }
  }

  @Override
  public void addListener(EventListener listener) {
    synchronized (lock) {
      listeners().add(requireNonNull(listener, LISTENER));
    }
  }

  @Override
  public void removeListener(EventListener listener) {
    synchronized (lock) {
      listeners().remove(listener);
    }
  }

  @Override
  public void addWeakDataListener(EventDataListener<T> listener) {
    synchronized (lock) {
      weakDataListeners().add(new WeakReference<>(requireNonNull(listener, LISTENER)));
    }
  }

  @Override
  public void removeWeakDataListener(EventDataListener<T> listener) {
    synchronized (lock) {
      weakDataListeners().removeIf(reference -> reference.get() == null || reference.get() == listener);
    }
  }

  @Override
  public void addWeakListener(EventListener listener) {
    synchronized (lock) {
      weakListeners().add(new WeakReference<>(requireNonNull(listener, LISTENER)));
    }
  }

  @Override
  public void removeWeakListener(EventListener listener) {
    synchronized (lock) {
      weakListeners().removeIf(reference -> reference.get() == null || reference.get() == listener);
    }
  }

  void notifyListeners(T data) {
    for (EventListener listener : eventListeners()) {
      listener.onEvent();
    }
    for (EventDataListener<T> dataListener : eventDataListeners()) {
      dataListener.onEvent(data);
    }
    for (WeakReference<EventListener> reference : weakEventListeners()) {
      EventListener weakListener = reference.get();
      if (weakListener != null) {
        weakListener.onEvent();
      }
    }
    for (WeakReference<EventDataListener<T>> reference : weakEventDataListeners()) {
      EventDataListener<T> weakDataListener = reference.get();
      if (weakDataListener != null) {
        weakDataListener.onEvent(data);
      }
    }
  }

  private List<EventListener> eventListeners() {
    synchronized (lock) {
      if (listeners != null && !listeners.isEmpty()) {
        return new ArrayList<>(listeners);
      }
    }

    return emptyList();
  }

  private List<EventDataListener<T>> eventDataListeners() {
    synchronized (lock) {
      if (dataListeners != null && !dataListeners.isEmpty()) {
        return new ArrayList<>(dataListeners);
      }
    }

    return emptyList();
  }

  private Set<EventListener> listeners() {
    if (listeners == null) {
      listeners = new LinkedHashSet<>(1);
    }

    return listeners;
  }

  private Set<EventDataListener<T>> dataListeners() {
    if (dataListeners == null) {
      dataListeners = new LinkedHashSet<>(1);
    }

    return dataListeners;
  }

  private List<WeakReference<EventListener>> weakEventListeners() {
    synchronized (lock) {
      if (weakListeners != null && !weakListeners.isEmpty()) {
        weakListeners.removeIf(reference -> reference.get() == null);

        return new ArrayList<>(weakListeners);
      }
    }

    return emptyList();
  }

  private List<WeakReference<EventDataListener<T>>> weakEventDataListeners() {
    synchronized (lock) {
      if (weakDataListeners != null && !weakDataListeners.isEmpty()) {
        weakDataListeners.removeIf(reference -> reference.get() == null);

        return new ArrayList<>(weakDataListeners);
      }
    }

    return emptyList();
  }

  private Set<WeakReference<EventListener>> weakListeners() {
    if (weakListeners == null) {
      weakListeners = new LinkedHashSet<>(1);
    }

    return weakListeners;
  }

  private Set<WeakReference<EventDataListener<T>>> weakDataListeners() {
    if (weakDataListeners == null) {
      weakDataListeners = new LinkedHashSet<>(1);
    }

    return weakDataListeners;
  }
}
