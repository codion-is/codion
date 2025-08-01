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

import is.codion.common.observer.Observer;

import org.jspecify.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Consumer;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

/**
 * Thread-safe implementation of Observer.
 * All listener management operations are synchronized using an internal lock.
 * Dead weak references are cleaned up during add/remove operations.
 */
final class DefaultObserver<T> implements Observer<T> {

	private final Lock lock = new Lock() {};

	private @Nullable ArrayList<Listener<?>> listeners;

	@Override
	public boolean addListener(Runnable runnable) {
		return add(new RunnableListener(requireNonNull(runnable)));
	}

	@Override
	public boolean removeListener(Runnable runnable) {
		return remove(runnable);
	}

	@Override
	public boolean addConsumer(Consumer<? super T> consumer) {
		return add(new ConsumerListener<>(requireNonNull(consumer)));
	}

	@Override
	public boolean removeConsumer(Consumer<? super T> consumer) {
		return remove(consumer);
	}

	@Override
	public boolean addWeakListener(Runnable runnable) {
		return add(new WeakRunnableListener(requireNonNull(runnable)));
	}

	@Override
	public boolean removeWeakListener(Runnable runnable) {
		return remove(runnable);
	}

	@Override
	public boolean addWeakConsumer(Consumer<? super T> consumer) {
		return add(new WeakConsumerListener<>(requireNonNull(consumer)));
	}

	@Override
	public boolean removeWeakConsumer(Consumer<? super T> consumer) {
		return remove(consumer);
	}

	void notifyListeners(@Nullable T data) {
		for (Listener<?> listener : listeners()) {
			notifyListener(listener, data);
		}
	}

	private List<Listener<?>> listeners() {
		synchronized (lock) {
			if (listeners == null) {
				return emptyList();
			}

			return new ArrayList<>(listeners);
		}
	}

	private boolean add(Listener<?> listener) {
		synchronized (lock) {
			if (contains(listener)) {
				return false;
			}
			if (listeners == null) {
				listeners = new ArrayList<>(1);
			}

			listeners.add(listener);
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

	private static boolean remove(Object listenerToRemove, ListIterator<Listener<?>> iterator) {
		boolean removed = false;
		while (iterator.hasNext()) {
			Object listener = iterator.next().get();
			if (listener == listenerToRemove) {
				iterator.remove();
				removed = true;
			}
			else if (listener == null) {
				iterator.remove();
			}
		}

		return removed;
	}

	private void notifyListener(Listener<?> listener, @Nullable T data) {
		if (listener instanceof RunnableListener) {
			((RunnableListener) listener).get().run();
		}
		else if (listener instanceof ConsumerListener) {
			((ConsumerListener<@Nullable T>) listener).get().accept(data);
		}
		else if (listener instanceof WeakRunnableListener) {
			Runnable runnable = ((WeakRunnableListener) listener).get();
			if (runnable != null) {
				runnable.run();
			}
		}
		else if (listener instanceof WeakConsumerListener<?>) {
			Consumer<@Nullable T> consumer = ((WeakConsumerListener<T>) listener).get();
			if (consumer != null) {
				consumer.accept(data);
			}
		}
	}

	private boolean contains(Listener<?> reference) {
		if (listeners != null) {
			listeners.removeIf(listener -> listener.get() == null);
			for (Listener<?> listener : listeners) {
				if (listener.get() == reference.get()) {
					return true;
				}
			}
		}

		return false;
	}

	private interface Listener<T> {

		T get();
	}

	private static final class RunnableListener implements Listener<Runnable> {

		private final Runnable runnable;

		private RunnableListener(Runnable runnable) {
			this.runnable = runnable;
		}

		@Override
		public Runnable get() {
			return runnable;
		}
	}

	private static final class WeakRunnableListener implements Listener<Runnable> {

		private final WeakReference<Runnable> weakReference;

		private WeakRunnableListener(Runnable runnable) {
			this.weakReference = new WeakReference<>(runnable);
		}

		@Override
		public @Nullable Runnable get() {
			return weakReference.get();
		}
	}

	private static final class ConsumerListener<T> implements Listener<Consumer<T>> {

		private final Consumer<T> consumer;

		private ConsumerListener(Consumer<T> consumer) {
			this.consumer = consumer;
		}

		@Override
		public Consumer<T> get() {
			return consumer;
		}
	}

	private static final class WeakConsumerListener<T> implements Listener<Consumer<T>> {

		private final WeakReference<Consumer<T>> weakReference;

		private WeakConsumerListener(Consumer<T> consumer) {
			this.weakReference = new WeakReference<>(consumer);
		}

		@Override
		public @Nullable Consumer<T> get() {
			return weakReference.get();
		}
	}

	private interface Lock {}
}
