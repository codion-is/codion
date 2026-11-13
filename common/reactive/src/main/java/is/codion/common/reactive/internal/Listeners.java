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

	private final Object owner;
	private final @Nullable Hooks hooks;
	private final boolean superseding;

	private @Nullable ArrayList<Listener<?>> listeners;
	//the notifications of a superseding owner started, a nested one included, on the one thread notifying at a time
	private int notifications = 0;

	/**
	 * @param owner the sensor or observer owning these listeners, synchronized on, its {@link Hooks} called if it
	 * implements them, its notifications {@link Superseding} if it is
	 */
	public Listeners(Object owner) {
		this.owner = requireNonNull(owner);
		this.hooks = owner instanceof Hooks ? (Hooks) owner : null;
		this.superseding = owner instanceof Superseding;
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
				if (hooks != null) {
					hooks.onLastListener();
				}
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
		int notification = superseding ? ++notifications : 0;
		for (Listener<?> listener : listeners()) {
			if (superseding && notification != notifications) {
				//a listener notified the owner again, which the listeners after it have heard
				return;
			}
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
			if (first && hooks != null) {
				hooks.onFirstListener();
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

	/**
	 * Implemented by a sensor or observer whose subscription to its source follows its own listeners and consumers
	 * rather than its lifetime, a derived observer, such as the conditional {@code when()} returns.
	 * <p>The hooks are called while holding the owner's monitor, so that an add and the subscription it triggers
	 * can not be interleaved with a remove. The locks a hook goes on to take are its source's, and its source's
	 * source's along a chain of derived observers, never a listener's, so the ordering runs one way, from derived
	 * observer to source, as long as a source notifies outside the monitor its listeners are kept under. A value
	 * collection notifies inside it, so a hook on another thread can deadlock against its notification.
	 */
	public interface Hooks {

		/**
		 * Called when the owner gains a listener or consumer while it had neither, before the add returns.
		 * <p>May be called again without an intervening {@link #onLastListener()}: an add that finds nothing but
		 * dead weak references prunes them and counts as the first. An implementation must tolerate the repeat.
		 */
		void onFirstListener();

		/**
		 * Called when the owner loses its last listener or consumer, by removal or by the pruning of dead weak
		 * references a removal does, before the remove returns.
		 */
		void onLastListener();
	}

	/**
	 * Marks an owner whose data is a value rather than an occurrence, the latest value being the one that counts: a
	 * listener notifying it again, by setting the value it holds, supersedes the notification in progress, which
	 * ends once the nested one has run, the listeners after it hearing the new value alone, rather than the new one
	 * followed by the one it replaced. An occurrence, an event or a signal, counts each time, so its owner is not
	 * superseding.
	 */
	public interface Superseding {}

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
