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
 * Manages listeners and consumers for observable values and events.
 * <p>
 * This interface provides default method implementations that delegate to {@link #observer()}.
 * There are two ways to implement this interface:
 * <ul>
 *   <li>Extend {@link AbstractObserver} - provides concrete implementations where {@code observer()} returns {@code this}</li>
 *   <li>Implement {@code observer()} to return a delegate - useful for wrapper types (see {@link Observable} for an example)</li>
 * </ul>
 * <p>All implementations are thread-safe and support concurrent access.</p>
 * @param <T> the type of data propagated to listeners
 * @see AbstractObserver
 * @see #when(Object)
 * @see #when(Predicate)
 */
public interface Observer<T> {

	/**
	 * Adds {@code listener} to this {@link Observer}.
	 * Adding the same listener a second time has no effect.
	 * <p>Note that if the listener is already registered (whether strongly or via {@link #addWeakListener}) this
	 * returns false and retains the existing registration kind; it does not upgrade a weak registration to strong.
	 * @param listener the listener to add
	 * @return true if this observer did not already contain the specified listener
	 * @throws NullPointerException in case listener is null
	 */
	default boolean addListener(Runnable listener) {
		return observer().addListener(listener);
	}

	/**
	 * Removes {@code listener} from this {@link Observer}
	 * <p>Note that this matches by referent regardless of how the listener was registered, so a listener added
	 * as a consumer may be removed via this method if it is the same instance.
	 * @param listener the listener to remove
	 * @return true if this observer contained the specified listener
	 */
	default boolean removeListener(Runnable listener) {
		return observer().removeListener(listener);
	}

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
	 * Removes {@code consumer} from this {@link Observer}
	 * @param consumer the consumer to remove
	 * @return true if this observer contained the specified consumer
	 */
	default boolean removeConsumer(Consumer<? super T> consumer) {
		return observer().removeConsumer(consumer);
	}

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
	default boolean addWeakListener(Runnable listener) {
		return observer().addWeakListener(listener);
	}

	/**
	 * Removes {@code listener} from this {@link Observer}
	 * @param listener the listener to remove
	 * @return true if this observer contained the specified listener
	 */
	default boolean removeWeakListener(Runnable listener) {
		return observer().removeWeakListener(listener);
	}

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
	default boolean addWeakConsumer(Consumer<? super T> consumer) {
		return observer().addWeakConsumer(consumer);
	}

	/**
	 * Removes {@code consumer} from this {@link Observer}.
	 * @param consumer the consumer to remove
	 * @return true if this observer contained the specified consumer
	 */
	default boolean removeWeakConsumer(Consumer<? super T> consumer) {
		return observer().removeWeakConsumer(consumer);
	}

	/**
	 * Returns a new conditional {@link Observer} notified when this observer instance is triggered with the given value
	 * <p>Note that each call registers a new conditional observer on this observer with no removal path, so avoid
	 * calling this repeatedly (for example per row or per component) on a long-lived observer.
	 * @param value the value on which to trigger the observer
	 * @return a new conditional {@link Observer}
	 */
	default Observer<T> when(@Nullable T value) {
		return observer().when(value);
	}

	/**
	 * Returns a new conditional {@link Observer} notified when this observer instance is triggered with a value satisfying the given predicate.
	 * <p>The predicate is tested with each triggering value, including null, and must tolerate null input.
	 * @param predicate the predicate on which to trigger the observer
	 * @return a new conditional {@link Observer}
	 */
	default Observer<T> when(Predicate<? super T> predicate) {
		return observer().when(predicate);
	}

	/**
	 * Returns the underlying {@link Observer} to which listener operations are delegated.
	 * <p>
	 * For {@link AbstractObserver} subclasses this method returns {@code this}.
	 * For wrapper types, this returns the delegate observer.
	 * @return the {@link Observer} handling listener management
	 */
	Observer<T> observer();

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
		B when(Predicate<? super T> predicate, Runnable listener);

		/**
		 * Adds a conditional consumer
		 * <p>The predicate is tested with each triggering value, including null, and must tolerate null input.
		 * @param predicate the predicate on which to consume the value
		 * @param consumer the consumer to use
		 * @return this builder instance
		 */
		B when(Predicate<? super T> predicate, Consumer<? super T> consumer);

		/**
		 * @return an {@link Observer} based on this builder
		 */
		Observer<T> build();
	}
}
