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

import is.codion.common.reactive.internal.Listeners;
import is.codion.common.reactive.sensor.Sensor;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

/**
 * Thread-safe base implementation of {@link Observer}.
 * <p>
 * This class provides the canonical way to implement {@link Observer}. Subclasses
 * trigger notifications by calling {@link #notifyListeners(Object)}, which notifies
 * the listeners and consumers in the order they were added.
 * <p>
 * The {@link #observer()} method returns {@code this}, so the default methods
 * in {@link Observer} resolve directly to the concrete implementations here.
 * <p>
 * All listener management operations are thread-safe.
 * Dead weak references are cleaned up during add/remove operations.
 * @param <T> the type of data propagated to consumers
 * @see Observer
 * @see is.codion.common.reactive.sensor.AbstractSensor
 */
public abstract class AbstractObserver<T> implements Observer<T> {

	private final Listeners listeners = new Listeners(this);

	/**
	 * Instantiates a new {@link AbstractObserver}
	 */
	protected AbstractObserver() {}

	/**
	 * Instantiates a new {@link AbstractObserver}
	 * @param builder the builder whose listeners and consumers to add
	 */
	protected AbstractObserver(AbstractBuilder<T, ?> builder) {
		requireNonNull(builder).listeners.forEach(consumer -> consumer.accept(this));
	}

	@Override
	public final boolean addListener(Runnable listener) {
		return listeners.addListener(listener);
	}

	@Override
	public final boolean removeListener(Runnable listener) {
		return listeners.remove(listener);
	}

	@Override
	public final boolean addConsumer(Consumer<? super T> consumer) {
		return listeners.addConsumer(consumer);
	}

	@Override
	public final boolean removeConsumer(Consumer<? super T> consumer) {
		return listeners.remove(consumer);
	}

	@Override
	public final boolean addWeakListener(Runnable listener) {
		return listeners.addWeakListener(listener);
	}

	@Override
	public final boolean removeWeakListener(Runnable listener) {
		return listeners.remove(listener);
	}

	@Override
	public final boolean addWeakConsumer(Consumer<? super T> consumer) {
		return listeners.addWeakConsumer(consumer);
	}

	@Override
	public final boolean removeWeakConsumer(Consumer<? super T> consumer) {
		return listeners.remove(consumer);
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

	@Override
	public final Sensor sensor() {
		return this;
	}

	/**
	 * Notifies the listeners and consumers, in the order they were added
	 * @param data the data to propagate to consumers
	 */
	protected final void notifyListeners(@Nullable T data) {
		listeners.notifyListeners(data);
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
		public final B when(Predicate<? super @Nullable T> predicate, Runnable listener) {
			requireNonNull(predicate);
			requireNonNull(listener);
			listeners.add(observer -> observer.when(predicate).addListener(listener));
			return self();
		}

		@Override
		public final B when(Predicate<? super @Nullable T> predicate, Consumer<? super T> consumer) {
			requireNonNull(predicate);
			requireNonNull(consumer);
			listeners.add(observer -> observer.when(predicate).addConsumer(consumer));
			return self();
		}

		protected final B self() {
			return (B) this;
		}
	}
}
