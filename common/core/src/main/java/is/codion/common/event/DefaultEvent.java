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
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson.
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
  public void addListener(Runnable listener) {
    observer().addListener(listener);
  }

  @Override
  public void removeListener(Runnable listener) {
    observer().removeListener(listener);
  }

  @Override
  public void addDataListener(Consumer<T> listener) {
    observer().addDataListener(listener);
  }

  @Override
  public void removeDataListener(Consumer<T> listener) {
    observer().removeDataListener(listener);
  }

  @Override
  public void addWeakListener(Runnable listener) {
    observer().addWeakListener(listener);
  }

  @Override
  public void removeWeakListener(Runnable listener) {
    observer().removeWeakListener(listener);
  }

  @Override
  public void addWeakDataListener(Consumer<T> listener) {
    observer().addWeakDataListener(listener);
  }

  @Override
  public void removeWeakDataListener(Consumer<T> listener) {
    observer().removeWeakDataListener(listener);
  }
}
