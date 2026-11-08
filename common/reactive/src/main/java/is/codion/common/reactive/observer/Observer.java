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
 * Copyright (c) 2008 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.reactive.observer;

import is.codion.common.reactive.observable.Observable;
import is.codion.common.reactive.sensor.Sensor;

import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Manages consumers, notified with data each time something happens, along with the listeners of the {@link Sensor}
 * it extends, all of them in the order they were added.
 * <p>
 * This interface provides default method implementations that delegate to {@link #observer()}.
 * There are two ways to implement this interface:
 * <ul>
 *   <li>Extend {@link AbstractObserver} - provides concrete implementations where {@code observer()} returns {@code this}</li>
 *   <li>Implement {@code observer()} to return a delegate - useful for wrapper types (see {@link Observable} for an example)</li>
 * </ul>
 * <p>All implementations are thread-safe and support concurrent access.</p>
 * @param <T> the type of data propagated to consumers
 * @see AbstractObserver
 * @see is.codion.common.reactive.signal.Signal
 * @see #when(Object)
 * @see #when(Predicate)
 */
public interface Observer<T> extends Sensor {

	/**
	 * Adds {@code consumer} to this {@link Observer}.
	 * Adding the same consumer a second time has no effect.
	 * @param consumer the consumer to add
	 * @return true if this observer did not already contain the specified consumer
	 * @throws NullPointerException in case consumer is null
	 */
	default boolean addConsumer(Consumer<? super T> consumer) {
		return observer().addConsumer(consumer);
	}

	/**
	 * Removes {@code consumer} from this {@link Observer}, whether it was added via {@link #addConsumer(Consumer)}
	 * or {@link #addWeakConsumer(Consumer)}.
	 * @param consumer the consumer to remove
	 * @return true if this observer contained the specified consumer
	 */
	default boolean removeConsumer(Consumer<? super T> consumer) {
		return observer().removeConsumer(consumer);
	}

	/**
	 * Uses a {@link java.lang.ref.WeakReference}, adding {@code consumer} does not prevent it from being garbage collected.
	 * Adding the same consumer a second time has no effect.
	 * <p>
	 * Note: Dead weak references accumulate until cleaned up, which happens automatically
	 * when listeners or consumers are added or removed. To trigger cleanup manually without modifying
	 * them, call {@link #removeWeakConsumer(Consumer)} with any non-existing consumer:
	 * {@snippet class = "is.codion.manual.javadoc.ObserverSnippets" region = "removeWeakConsumer":
	 * // Clean up dead weak references // @start region=removeWeakConsumer
	 * observer.removeWeakConsumer(data -> {}); // @end}
	 * @param consumer the consumer
	 * @return true if this observer did not already contain the specified consumer
	 */
	default boolean addWeakConsumer(Consumer<? super T> consumer) {
		return observer().addWeakConsumer(consumer);
	}

	/**
	 * Removes {@code consumer} from this {@link Observer}, whether it was added via {@link #addConsumer(Consumer)}
	 * or {@link #addWeakConsumer(Consumer)}.
	 * @param consumer the consumer to remove
	 * @return true if this observer contained the specified consumer
	 */
	default boolean removeWeakConsumer(Consumer<? super T> consumer) {
		return observer().removeWeakConsumer(consumer);
	}

	/**
	 * Returns a new conditional {@link Observer} notified when this observer instance is triggered with the given value
	 * <p>The conditional observer subscribes to this one only while it has listeners or consumers of its own, attaching
	 * on the first and detaching on the last, so one that is never listened to - or whose listeners and consumers are all
	 * removed again - leaves nothing behind on this observer. Removing the last of them is therefore how a conditional
	 * observer is disposed of; there is nothing else to release.
	 * @param value the value on which to trigger the observer
	 * @return a new conditional {@link Observer}
	 */
	default Observer<T> when(@Nullable T value) {
		return observer().when(value);
	}

	/**
	 * Returns a new conditional {@link Observer} notified when this observer instance is triggered with a value satisfying the given predicate.
	 * <p>The predicate is tested with each triggering value, including null, and must tolerate null input.
	 * <p>Subscribes to this observer only while it has listeners or consumers of its own, see {@link #when(Object)}.
	 * @param predicate the predicate on which to trigger the observer
	 * @return a new conditional {@link Observer}
	 */
	default Observer<T> when(Predicate<? super @Nullable T> predicate) {
		return observer().when(predicate);
	}

	/**
	 * Returns the underlying {@link Observer} to which listener and consumer operations are delegated.
	 * <p>
	 * For {@link AbstractObserver} subclasses this method returns {@code this}.
	 * For wrapper types, this returns the delegate observer.
	 * @return the {@link Observer} handling listener and consumer management
	 */
	Observer<T> observer();

	/**
	 * An observer is its own sensor, via {@link #observer()}.
	 * @return the {@link Sensor} handling listener management, this observer's {@link #observer()}
	 */
	@Override
	default Sensor sensor() {
		return observer();
	}

	/**
	 * A base builder for adding listeners and consumers, extended by the builders of observable
	 * types; there is no factory for a standalone {@link Observer} builder.
	 * @param <T> the observed type
	 * @param <B> the builder type
	 */
	interface Builder<T, B extends Builder<T, B>> {

		/**
		 * @param listener a listener to add
		 * @return this builder instance
		 */
		B listener(Runnable listener);

		/**
		 * @param consumer a consumer to add
		 * @return this builder instance
		 */
		B consumer(Consumer<? super T> consumer);

		/**
		 * @param weakListener a weak listener to add
		 * @return this builder instance
		 */
		B weakListener(Runnable weakListener);

		/**
		 * @param weakConsumer a weak consumer to add
		 * @return this builder instance
		 */
		B weakConsumer(Consumer<? super T> weakConsumer);

		/**
		 * Adds a conditional listener
		 * @param value the value on which to run
		 * @param listener the listener
		 * @return this builder instance
		 */
		B when(@Nullable T value, Runnable listener);

		/**
		 * Adds a conditional consumer
		 * @param value the value to consume
		 * @param consumer the consumer
		 * @return this builder instance
		 */
		B when(@Nullable T value, Consumer<? super T> consumer);

		/**
		 * Adds a conditional listener
		 * <p>The predicate is tested with each triggering value, including null, and must tolerate null input.
		 * @param predicate the predicate on which to run
		 * @param listener the listener
		 * @return this builder instance
		 */
		B when(Predicate<? super @Nullable T> predicate, Runnable listener);

		/**
		 * Adds a conditional consumer
		 * <p>The predicate is tested with each triggering value, including null, and must tolerate null input.
		 * @param predicate the predicate on which to consume the value
		 * @param consumer the consumer to use
		 * @return this builder instance
		 */
		B when(Predicate<? super @Nullable T> predicate, Consumer<? super T> consumer);

		/**
		 * @return an {@link Observer} based on this builder
		 */
		Observer<T> build();
	}
}
