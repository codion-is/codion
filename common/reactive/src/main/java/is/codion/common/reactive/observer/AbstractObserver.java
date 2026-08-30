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
package is.codion.common.reactive.observer;

import org.jspecify.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

/**
 * Thread-safe base implementation of {@link Observer}.
 * <p>
 * This class provides the canonical way to implement {@link Observer}. Subclasses
 * trigger notifications by calling {@link #notifyListeners(Object)}.
 * <p>
 * The {@link #observer()} method returns {@code this}, so the default methods
 * in {@link Observer} resolve directly to the concrete implementations here.
 * <p>
 * All listener management operations are thread-safe.
 * Dead weak references are cleaned up during add/remove operations.
 * @param <T> the type of data propagated to listeners
 * @see Observer
 */
public abstract class AbstractObserver<T> implements Observer<T> {

	private @Nullable ArrayList<Listener<?>> listeners;

	/**
	 * Instantiates a new {@link AbstractObserver}
	 */
	protected AbstractObserver() {}

	/**
	 * Instantiates a new {@link AbstractObserver}
	 * @param builder the builder which listeners to add
	 */
	protected AbstractObserver(AbstractBuilder<T, ?> builder) {
		requireNonNull(builder).listeners.forEach(consumer -> consumer.accept(this));
	}

	@Override
	public final boolean addListener(Runnable listener) {
		return add(new RunnableListener(requireNonNull(listener)));
	}

	@Override
	public final boolean removeListener(Runnable listener) {
		return remove(listener);
	}

	@Override
	public final boolean addConsumer(Consumer<? super T> consumer) {
		return add(new ConsumerListener<>(requireNonNull(consumer)));
	}

	@Override
	public final boolean removeConsumer(Consumer<? super T> consumer) {
		return remove(consumer);
	}

	@Override
	public final boolean addWeakListener(Runnable listener) {
		return add(new WeakRunnableListener(requireNonNull(listener)));
	}

	@Override
	public final boolean removeWeakListener(Runnable listener) {
		return remove(listener);
	}

	@Override
	public final boolean addWeakConsumer(Consumer<? super T> consumer) {
		return add(new WeakConsumerListener<>(requireNonNull(consumer)));
	}

	@Override
	public final boolean removeWeakConsumer(Consumer<? super T> consumer) {
		return remove(consumer);
	}

	@Override
	public final Observer<T> when(@Nullable T value) {
		return new Conditional<>(this, value);
	}

	@Override
	public final Observer<T> when(Predicate<? super @Nullable T> predicate) {
		return new Conditional<>(this, requireNonNull(predicate));
	}

	@Override
	public final Observer<T> observer() {
		return this;
	}

	/**
	 * Called when this observer gains a listener while it had none, before the add returns.
	 * <p>The hooks exist for derived observers whose subscription to their source should follow their own
	 * listeners rather than their lifetime, {@link Conditional} being the one that does.
	 * <p>Called while holding this observer's monitor, so that an add and the attach it triggers can not be
	 * interleaved with a remove. The lock a hook goes on to take is always its source's, never a listener's,
	 * so the ordering runs one way, from derived observer to source.
	 * <p>May fire again without an intervening {@link #onLastListener()}: an add that finds nothing but
	 * dead weak references prunes them and counts as the first. An implementation must tolerate the repeat,
	 * as {@link Conditional}'s does, its subscription deduplicated by the source.
	 */
	void onFirstListener() {}

	/**
	 * Called when this observer loses its last listener, before the remove returns.
	 * @see #onFirstListener()
	 */
	void onLastListener() {}

	/**
	 * Notifies all consumers and listeners
	 * @param data the data to propagate to consumers
	 */
	protected final void notifyListeners(@Nullable T data) {
		for (Listener<?> listener : listeners()) {
			notifyListener(listener, data);
		}
	}

	private synchronized List<Listener<?>> listeners() {
		if (listeners == null) {
			return emptyList();
		}

		return new ArrayList<>(listeners);
	}

	private synchronized boolean add(Listener<?> listener) {
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
			onFirstListener();
		}

		return true;
	}

	private synchronized boolean remove(Object listener) {
		requireNonNull(listener);
		if (listeners == null) {
			return false;
		}
		boolean removed = remove(listener, listeners.listIterator());
		//emptied either by the removal or by the dead weak references it pruned along the way
		if (listeners.isEmpty()) {
			listeners = null;
			onLastListener();
		}
		else {
			listeners.trimToSize();
		}

		return removed;
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

	/**
	 * An abstract base class for an {@link Observer} builder
	 * @param <T> the observed type
	 * @param <B> the builder type
	 */
	public abstract static class AbstractBuilder<T, B extends Builder<T, B>> implements Builder<T, B> {

		private final List<Consumer<Observer<T>>> listeners = new ArrayList<>();

		protected AbstractBuilder() {}

		@Override
		public final B listener(Runnable listener) {
			requireNonNull(listener);
			listeners.add(observer -> observer.addListener(listener));
			return self();
		}

		@Override
		public final B consumer(Consumer<? super T> consumer) {
			requireNonNull(consumer);
			listeners.add(observer -> observer.addConsumer(consumer));
			return self();
		}

		@Override
		public final B weakListener(Runnable weakListener) {
			requireNonNull(weakListener);
			listeners.add(observer -> observer.addWeakListener(weakListener));
			return self();
		}

		@Override
		public final B weakConsumer(Consumer<? super T> weakConsumer) {
			requireNonNull(weakConsumer);
			listeners.add(observer -> observer.addWeakConsumer(weakConsumer));
			return self();
		}

		@Override
		public final B when(@Nullable T value, Runnable listener) {
			requireNonNull(listener);
			listeners.add(observer -> observer.when(value).addListener(listener));
			return self();
		}

		@Override
		public final B when(@Nullable T value, Consumer<? super T> consumer) {
			requireNonNull(consumer);
			listeners.add(observer -> observer.when(value).addConsumer(consumer));
			return self();
		}

		@Override
		public final B when(Predicate<? super T> predicate, Runnable listener) {
			requireNonNull(predicate);
			requireNonNull(listener);
			listeners.add(observer -> observer.when(predicate).addListener(listener));
			return self();
		}

		@Override
		public final B when(Predicate<? super T> predicate, Consumer<? super T> consumer) {
			requireNonNull(predicate);
			requireNonNull(consumer);
			listeners.add(observer -> observer.when(predicate).addConsumer(consumer));
			return self();
		}

		protected final B self() {
			return (B) this;
		}
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
}
