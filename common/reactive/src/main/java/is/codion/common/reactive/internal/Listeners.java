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
 * Copyright (c) 2019 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.reactive.internal;

import org.jspecify.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Consumer;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

/**
 * The listeners and consumers of an {@link is.codion.common.reactive.sensor.AbstractSensor}
 * or an {@link is.codion.common.reactive.observer.AbstractObserver}, in the order they were
 * added, strong and weak, synchronized on the sensor or observer owning them, as its monitor.
 * <p><b>This is an internal class, NOT public API.</b> It is public only so that the reactive packages can share it,
 * its package is not exported from the module, and it may change or be removed at any time, without notice.
 */
public final class Listeners {

	private static final Runnable NONE = () -> {};

	private final Object owner;
	private final Runnable onFirstListener;
	private final Runnable onLastListener;

	private @Nullable ArrayList<Listener<?>> listeners;

	public Listeners(Object owner) {
		this(owner, NONE, NONE);
	}

	/**
	 * @param owner the sensor or observer owning these listeners, synchronized on
	 * @param onFirstListener called when a listener or consumer is added while there were none, before the add returns
	 * @param onLastListener called when the last listener or consumer is removed, before the remove returns
	 */
	public Listeners(Object owner, Runnable onFirstListener, Runnable onLastListener) {
		this.owner = requireNonNull(owner);
		this.onFirstListener = requireNonNull(onFirstListener);
		this.onLastListener = requireNonNull(onLastListener);
	}

	public boolean addListener(Runnable listener) {
		return add(new RunnableListener(requireNonNull(listener)));
	}

	public boolean addWeakListener(Runnable listener) {
		return add(new WeakRunnableListener(requireNonNull(listener)));
	}

	public boolean addConsumer(Consumer<?> consumer) {
		return add(new ConsumerListener<>(requireNonNull(consumer)));
	}

	public boolean addWeakConsumer(Consumer<?> consumer) {
		return add(new WeakConsumerListener<>(requireNonNull(consumer)));
	}

	public boolean remove(Object listener) {
		requireNonNull(listener);
		synchronized (owner) {
			if (listeners == null) {
				return false;
			}
			boolean removed = remove(listener, listeners.listIterator());
			//emptied either by the removal or by the dead weak references it pruned along the way
			if (listeners.isEmpty()) {
				listeners = null;
				onLastListener.run();
			}
			else {
				listeners.trimToSize();
			}

			return removed;
		}
	}

	/**
	 * Notifies the listeners, the consumers left out
	 */
	public void notifyListeners() {
		for (Listener<?> listener : listeners()) {
			if (listener instanceof RunnableListener) {
				((RunnableListener) listener).get().run();
			}
			else if (listener instanceof WeakRunnableListener) {
				Runnable runnable = ((WeakRunnableListener) listener).get();
				if (runnable != null) {
					runnable.run();
				}
			}
		}
	}

	/**
	 * Notifies the listeners and consumers, in the order they were added
	 * @param data the data to propagate to the consumers
	 * @param <T> the data type
	 */
	public <T> void notifyListeners(@Nullable T data) {
		for (Listener<?> listener : listeners()) {
			notifyListener(listener, data);
		}
	}

	private List<Listener<?>> listeners() {
		synchronized (owner) {
			if (listeners == null) {
				return emptyList();
			}

			return new ArrayList<>(listeners);
		}
	}

	private boolean add(Listener<?> listener) {
		synchronized (owner) {
			if (contains(listener)) {
				return false;
			}
			//contains() prunes dead weak references, so the list can be non-null and empty
			boolean first = listeners == null || listeners.isEmpty();
			if (listeners == null) {
				listeners = new ArrayList<>(1);
			}

			listeners.add(listener);
			listeners.trimToSize();
			if (first) {
				onFirstListener.run();
			}

			return true;
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

	private static <T> void notifyListener(Listener<?> listener, @Nullable T data) {
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

		@Nullable T get();
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
}
