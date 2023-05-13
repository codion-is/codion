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

  private Set<EventListener> eventListeners;
  private Set<EventDataListener<T>> eventDataListeners;
  private List<WeakReference<EventListener>> weakEventListeners;
  private List<WeakReference<EventDataListener<T>>> weakEventDataListeners;

  @Override
  public void addDataListener(EventDataListener<T> listener) {
    requireNonNull(listener, LISTENER);
    synchronized (lock) {
      initDataListeners().add(listener);
    }
  }

  @Override
  public void removeDataListener(EventDataListener<T> listener) {
    requireNonNull(listener, LISTENER);
    synchronized (lock) {
      initDataListeners().remove(listener);
    }
  }

  @Override
  public void addListener(EventListener listener) {
    requireNonNull(listener, LISTENER);
    synchronized (lock) {
      initListeners().add(listener);
    }
  }

  @Override
  public void removeListener(EventListener listener) {
    requireNonNull(listener, LISTENER);
    synchronized (lock) {
      initListeners().remove(listener);
    }
  }

  @Override
  public void addWeakListener(EventListener listener) {
    requireNonNull(listener, LISTENER);
    synchronized (lock) {
      List<WeakReference<EventListener>> references = initWeakListeners();
      for (WeakReference<EventListener> reference : references) {
        if (reference.get() == listener) {
          return;
        }
      }
      references.add(new WeakReference<>(listener));
    }
  }

  @Override
  public void removeWeakListener(EventListener listener) {
    requireNonNull(listener, LISTENER);
    synchronized (lock) {
      initWeakListeners().removeIf(reference -> reference.get() == null || reference.get() == listener);
    }
  }

  @Override
  public void addWeakDataListener(EventDataListener<T> listener) {
    requireNonNull(listener, LISTENER);
    synchronized (lock) {
      List<WeakReference<EventDataListener<T>>> references = initWeakDataListeners();
      for (WeakReference<EventDataListener<T>> reference : references) {
        if (reference.get() == listener) {
          return;
        }
      }
      references.add(new WeakReference<>(listener));
    }
  }

  @Override
  public void removeWeakDataListener(EventDataListener<T> listener) {
    requireNonNull(listener, LISTENER);
    synchronized (lock) {
      initWeakDataListeners().removeIf(reference -> reference.get() == null || reference.get() == listener);
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
      if (eventListeners != null && !eventListeners.isEmpty()) {
        return new ArrayList<>(eventListeners);
      }
    }

    return emptyList();
  }

  private List<EventDataListener<T>> eventDataListeners() {
    synchronized (lock) {
      if (eventDataListeners != null && !eventDataListeners.isEmpty()) {
        return new ArrayList<>(eventDataListeners);
      }
    }

    return emptyList();
  }

  private List<WeakReference<EventListener>> weakEventListeners() {
    synchronized (lock) {
      if (weakEventListeners != null && !weakEventListeners.isEmpty()) {
        weakEventListeners.removeIf(reference -> reference.get() == null);

        return new ArrayList<>(weakEventListeners);
      }
    }

    return emptyList();
  }

  private List<WeakReference<EventDataListener<T>>> weakEventDataListeners() {
    synchronized (lock) {
      if (weakEventDataListeners != null && !weakEventDataListeners.isEmpty()) {
        weakEventDataListeners.removeIf(reference -> reference.get() == null);

        return new ArrayList<>(weakEventDataListeners);
      }
    }

    return emptyList();
  }

  private Set<EventListener> initListeners() {
    if (eventListeners == null) {
      eventListeners = new LinkedHashSet<>(1);
    }

    return eventListeners;
  }

  private Set<EventDataListener<T>> initDataListeners() {
    if (eventDataListeners == null) {
      eventDataListeners = new LinkedHashSet<>(1);
    }

    return eventDataListeners;
  }

  private List<WeakReference<EventListener>> initWeakListeners() {
    if (weakEventListeners == null) {
      weakEventListeners = new ArrayList<>(1);
    }

    return weakEventListeners;
  }

  private List<WeakReference<EventDataListener<T>>> initWeakDataListeners() {
    if (weakEventDataListeners == null) {
      weakEventDataListeners = new ArrayList<>(1);
    }

    return weakEventDataListeners;
  }
}
