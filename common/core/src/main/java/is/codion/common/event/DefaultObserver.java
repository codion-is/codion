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
 * Copyright (c) 2019 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.event;

import is.codion.common.observable.Observer;

import org.jspecify.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Consumer;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

final class DefaultObserver<T> implements Observer<T> {

	private final Lock lock = new Lock() {};

	private @Nullable ArrayList<Object> listeners;

	@Override
	public boolean addListener(Runnable runnable) {
		return add(runnable, false);
	}

	@Override
	public boolean removeListener(Runnable runnable) {
		return remove(runnable);
	}

	@Override
	public boolean addConsumer(Consumer<? super T> consumer) {
		return add(consumer, false);
	}

	@Override
	public boolean removeConsumer(Consumer<? super T> consumer) {
		return remove(consumer);
	}

	@Override
	public boolean addWeakListener(Runnable runnable) {
		return add(runnable, true);
	}

	@Override
	public boolean removeWeakListener(Runnable runnable) {
		return remove(runnable);
	}

	@Override
	public boolean addWeakConsumer(Consumer<? super T> consumer) {
		return add(consumer, true);
	}

	@Override
	public boolean removeWeakConsumer(Consumer<? super T> consumer) {
		return remove(consumer);
	}

	void notifyListeners(@Nullable T data) {
		for (Object listener : listeners()) {
			notifyListener(listener, data);
		}
	}

	private List<Object> listeners() {
		synchronized (lock) {
			if (listeners == null) {
				return emptyList();
			}

			return new ArrayList<>(listeners);
		}
	}

	private boolean add(Object listener, boolean weakReference) {
		requireNonNull(listener);
		synchronized (lock) {
			if (contains(listener)) {
				return false;
			}
			if (listeners == null) {
				listeners = new ArrayList<>(1);
			}

			listeners.add(weakReference ? new WeakReference<>(listener) : listener);
			listeners.trimToSize();

			return true;
		}
	}

	private boolean remove(Object listener) {
		requireNonNull(listener);
		synchronized (lock) {
			if (listeners == null) {
				return false;
			}
			if (remove(listener, listeners.listIterator())) {
				if (listeners.isEmpty()) {
					listeners = null;
				}
				else {
					listeners.trimToSize();
				}

				return true;
			}

			return false;
		}
	}

	private static boolean remove(Object listener, ListIterator<Object> iterator) {
		boolean removed = false;
		while (iterator.hasNext()) {
			Object next = iterator.next();
			if (next == listener) {
				iterator.remove();
				removed = true;
			}
			else if (next instanceof WeakReference) {
				Object reference = ((WeakReference<?>) next).get();
				if (reference == listener) {
					iterator.remove();
					removed = true;
				}
				else if (reference == null) {
					iterator.remove();
				}
			}
		}

		return removed;
	}

	private void notifyListener(Object listener, @Nullable T data) {
		if (listener instanceof WeakReference<?>) {
			listener = ((WeakReference<?>) listener).get();
		}
		if (listener instanceof Runnable) {
			((Runnable) listener).run();
		}
		else if (listener instanceof Consumer) {
			((Consumer<@Nullable T>) listener).accept(data);
		}
	}

	private boolean contains(Object object) {
		if (listeners != null) {
			listeners.removeIf(item -> item instanceof WeakReference && ((WeakReference<?>) item).get() == null);
			for (Object listener : listeners) {
				if (listener == object || (listener instanceof WeakReference<?> && ((WeakReference<?>) listener).get() == object)) {
					return true;
				}
			}
		}

		return false;
	}

	private interface Lock {}
}
