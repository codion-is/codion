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
  public boolean addDataListener(Consumer<T> listener) {
    return observer().addDataListener(listener);
  }

  @Override
  public boolean removeDataListener(Consumer<T> listener) {
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
  public boolean addWeakDataListener(Consumer<T> listener) {
    return observer().addWeakDataListener(listener);
  }

  @Override
  public boolean removeWeakDataListener(Consumer<T> listener) {
    return observer().removeWeakDataListener(listener);
  }
}
