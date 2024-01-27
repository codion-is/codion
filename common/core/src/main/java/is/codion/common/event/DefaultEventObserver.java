/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2019 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.event;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

final class DefaultEventObserver<T> implements EventObserver<T> {

  private static final String LISTENER = "listener";

  private final Object lock = new Object();

  private Set<Runnable> listeners;
  private Set<Consumer<? super T>> dataListeners;
  private List<WeakReference<Runnable>> weakListeners;
  private List<WeakReference<Consumer<? super T>>> weakDataListeners;

  @Override
  public boolean addDataListener(Consumer<? super T> listener) {
    requireNonNull(listener, LISTENER);
    synchronized (lock) {
      return initDataListeners().add(listener);
    }
  }

  @Override
  public boolean removeDataListener(Consumer<? super T> listener) {
    requireNonNull(listener, LISTENER);
    synchronized (lock) {
      return initDataListeners().remove(listener);
    }
  }

  @Override
  public boolean addListener(Runnable listener) {
    requireNonNull(listener, LISTENER);
    synchronized (lock) {
      return initListeners().add(listener);
    }
  }

  @Override
  public boolean removeListener(Runnable listener) {
    requireNonNull(listener, LISTENER);
    synchronized (lock) {
      return initListeners().remove(listener);
    }
  }

  @Override
  public boolean addWeakListener(Runnable listener) {
    requireNonNull(listener, LISTENER);
    synchronized (lock) {
      List<WeakReference<Runnable>> references = initWeakListeners();
      for (WeakReference<Runnable> reference : references) {
        if (reference.get() == listener) {
          return false;
        }
      }
      return references.add(new WeakReference<>(listener));
    }
  }

  @Override
  public boolean removeWeakListener(Runnable listener) {
    requireNonNull(listener, LISTENER);
    synchronized (lock) {
      return initWeakListeners().removeIf(reference -> reference.get() == null || reference.get() == listener);
    }
  }

  @Override
  public boolean addWeakDataListener(Consumer<? super T> listener) {
    requireNonNull(listener, LISTENER);
    synchronized (lock) {
      List<WeakReference<Consumer<? super T>>> references = initWeakDataListeners();
      for (WeakReference<Consumer<? super T>> reference : references) {
        if (reference.get() == listener) {
          return false;
        }
      }
      return references.add(new WeakReference<>(listener));
    }
  }

  @Override
  public boolean removeWeakDataListener(Consumer<? super T> listener) {
    requireNonNull(listener, LISTENER);
    synchronized (lock) {
      return initWeakDataListeners().removeIf(reference -> reference.get() == null || reference.get() == listener);
    }
  }

  void notifyListeners(T data) {
    for (Runnable listener : listeners()) {
      listener.run();
    }
    for (Consumer<? super T> dataListener : dataListeners()) {
      dataListener.accept(data);
    }
    for (WeakReference<Runnable> reference : weakListeners()) {
      Runnable weakListener = reference.get();
      if (weakListener != null) {
        weakListener.run();
      }
    }
    for (WeakReference<Consumer<? super T>> reference : weakDataListeners()) {
      Consumer<? super T> weakDataListener = reference.get();
      if (weakDataListener != null) {
        weakDataListener.accept(data);
      }
    }
  }

  private List<Runnable> listeners() {
    synchronized (lock) {
      if (listeners != null && !listeners.isEmpty()) {
        return new ArrayList<>(listeners);
      }
    }

    return emptyList();
  }

  private List<Consumer<? super T>> dataListeners() {
    synchronized (lock) {
      if (dataListeners != null && !dataListeners.isEmpty()) {
        return new ArrayList<>(dataListeners);
      }
    }

    return emptyList();
  }

  private List<WeakReference<Runnable>> weakListeners() {
    synchronized (lock) {
      if (weakListeners != null && !weakListeners.isEmpty()) {
        weakListeners.removeIf(reference -> reference.get() == null);

        return new ArrayList<>(weakListeners);
      }
    }

    return emptyList();
  }

  private List<WeakReference<Consumer<? super T>>> weakDataListeners() {
    synchronized (lock) {
      if (weakDataListeners != null && !weakDataListeners.isEmpty()) {
        weakDataListeners.removeIf(reference -> reference.get() == null);

        return new ArrayList<>(weakDataListeners);
      }
    }

    return emptyList();
  }

  private Set<Runnable> initListeners() {
    if (listeners == null) {
      listeners = new LinkedHashSet<>(1);
    }

    return listeners;
  }

  private Set<Consumer<? super T>> initDataListeners() {
    if (dataListeners == null) {
      dataListeners = new LinkedHashSet<>(1);
    }

    return dataListeners;
  }

  private List<WeakReference<Runnable>> initWeakListeners() {
    if (weakListeners == null) {
      weakListeners = new ArrayList<>(1);
    }

    return weakListeners;
  }

  private List<WeakReference<Consumer<? super T>>> initWeakDataListeners() {
    if (weakDataListeners == null) {
      weakDataListeners = new ArrayList<>(1);
    }

    return weakDataListeners;
  }
}
