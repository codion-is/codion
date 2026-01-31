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

import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Manages event listeners.
 * <p>All implementations are thread-safe and support concurrent access.</p>
 * @param <T> the type of data propagated to listeners.
 * @see AbstractObserver
 * @see #when(Object)
 * @see #when(Predicate)
 */
public interface Observer<T> {

	/**
	 * Adds {@code listener} to this {@link Observer}.
	 * Adding the same listener a second time has no effect.
	 * @param listener the listener to add
	 * @return true if this observer did not already contain the specified listener
	 * @throws NullPointerException in case listener is null
	 */
	boolean addListener(Runnable listener);

	/**
	 * Removes {@code listener} from this {@link Observer}
	 * @param listener the listener to remove
	 * @return true if this observer contained the specified listener
	 */
	boolean removeListener(Runnable listener);

	/**
	 * Adds {@code consumer} to this {@link Observer}.
	 * Adding the same consumer a second time has no effect.
	 * @param consumer the consumer to add
	 * @return true if this observer did not already contain the specified consumer
	 * @throws NullPointerException in case consumer is null
	 */
	boolean addConsumer(Consumer<? super T> consumer);

	/**
	 * Removes {@code consumer} from this {@link Observer}
	 * @param consumer the consumer to remove
	 * @return true if this observer contained the specified consumer
	 */
	boolean removeConsumer(Consumer<? super T> consumer);

	/**
	 * Uses a {@link java.lang.ref.WeakReference}, adding {@code listener} does not prevent it from being garbage collected.
	 * Adding the same listener a second time has no effect.
	 * <p>
	 * Note: Dead weak references accumulate until cleaned up, which happens automatically
	 * when listeners are added or removed. To trigger cleanup manually without modifying
	 * the listener set, call {@link #removeWeakListener(Runnable)} with any non-existing listener:
	 * {@snippet :
	 * // Clean up dead weak references
	 * observer.removeWeakListener(() -> {});
	 *}
	 * @param listener the listener
	 * @return true if this observer did not already contain the specified listener
	 */
	boolean addWeakListener(Runnable listener);

	/**
	 * Removes {@code listener} from this {@link Observer}
	 * @param listener the listener to remove
	 * @return true if this observer contained the specified listener
	 */
	boolean removeWeakListener(Runnable listener);

	/**
	 * Uses a {@link java.lang.ref.WeakReference}, adding {@code consumer} does not prevent it from being garbage collected.
	 * Adding the same consumer a second time has no effect.
	 * <p>
	 * Note: Dead weak references accumulate until cleaned up, which happens automatically
	 * when listeners are added or removed. To trigger cleanup manually without modifying
	 * the listener set, call {@link #removeWeakConsumer(Consumer)} with any non-existing consumer:
	 * {@snippet :
	 * // Clean up dead weak references
	 * observer.removeWeakConsumer(data -> {});
	 *}
	 * @param consumer the consumer
	 * @return true if this observer did not already contain the specified consumer
	 */
	boolean addWeakConsumer(Consumer<? super T> consumer);

	/**
	 * Removes {@code consumer} from this {@link Observer}.
	 * @param consumer the consumer to remove
	 * @return true if this observer contained the specified consumer
	 */
	boolean removeWeakConsumer(Consumer<? super T> consumer);

	/**
	 * Returns a new conditional {@link Observer} notified when this observer instance is triggered with the given value
	 * @param value the value on which to trigger the observer
	 * @return a new conditional {@link Observer}
	 */
	Observer<T> when(@Nullable T value);

	/**
	 * Returns a new conditional {@link Observer} notified when this observer instance is triggered with a value satisfying the given predicate
	 * @param predicate the predicate on which to trigger the observer
	 * @return a new conditional {@link Observer}
	 */
	Observer<T> when(Predicate<? super T> predicate);

	/**
	 * Builds an {@link Observer}
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
		B when(T value, Runnable listener);

		/**
		 * Adds a conditional consumer
		 * @param value the value to consume
		 * @param consumer the consumer
		 * @return this builder instance
		 */
		B when(T value, Consumer<? super T> consumer);

		/**
		 * Adds a conditional listener
		 * @param predicate the predicate on which to run
		 * @param listener the runnable
		 * @return this builder instance
		 */
		B when(Predicate<T> predicate, Runnable listener);

		/**
		 * Adds a conditional consumer
		 * @param predicate the predicate on which to consume the value
		 * @param consumer the consumer to use
		 * @return this builder instance
		 */
		B when(Predicate<T> predicate, Consumer<? super T> consumer);

		/**
		 * @return an {@link Observer} based on this builder
		 */
		Observer<T> build();
	}
}
