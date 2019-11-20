/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.event;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

final class DefaultObserver<T> implements EventObserver<T> {

  private final Object lock = new Object();
  private Set<EventListener> listeners;
  private Set<EventDataListener<T>> dataListeners;

  @Override
  public void addDataListener(final EventDataListener<T> listener) {
    synchronized (lock) {
      getDataListeners().add(requireNonNull(listener, "listener"));
    }
  }

  @Override
  public void removeDataListener(final EventDataListener listener) {
    synchronized (lock) {
      getDataListeners().remove(listener);
    }
  }

  @Override
  public void addListener(final EventListener listener) {
    synchronized (lock) {
      getListeners().add(requireNonNull(listener, "listener"));
    }
  }

  @Override
  public void removeListener(final EventListener listener) {
    synchronized (lock) {
      getListeners().remove(listener);
    }
  }

  List<EventListener> getEventListeners() {
    synchronized (lock) {
      if (listeners == null) {
        return emptyList();
      }

      return new ArrayList<>(listeners);
    }
  }

  List<EventDataListener<T>> getEventDataListeners() {
    synchronized (lock) {
      if (dataListeners == null) {
        return emptyList();
      }

      return new ArrayList<>(dataListeners);
    }
  }

  boolean hasListeners() {
    synchronized (lock) {
      return (listeners != null && !listeners.isEmpty()) || (dataListeners != null && !dataListeners.isEmpty());
    }
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
