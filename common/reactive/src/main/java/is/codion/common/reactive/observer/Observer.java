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
 * Copyright (c) 2008 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.reactive.observer;

import is.codion.common.reactive.observer.Conditional.OnCondition;

import java.util.function.Consumer;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

/**
 * Manages event listeners. Implemented by {@link is.codion.common.reactive.event.Event}.
 * <p>All implementations are thread-safe and support concurrent access.</p>
 * @param <T> the type of data propagated to listeners.
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
	 * Returns an {@link OnCondition} instance for adding conditional listeners to this observer.
	 * This provides a fluent API for reacting to specific values or conditions.
	 * {@snippet :
	 * // React to a specific value
	 * viewState
	 *     .when(View.VISIBLE)
	 *     .run(this::onVisible);
	 *
	 * // Chain multiple conditions
	 * value
	 *     .when(1).run(() -> System.out.println("one"))
	 *     .when(2).run(() -> System.out.println("two"))
	 *     .when(v -> v > 10).accept(this::handleLarge);
	 *}
	 * @return a new {@link OnCondition}
	 */
	default OnCondition<T> when(T value) {
		return new DefaultConditional<>(this).when(value);
	}

	/**
	 * Returns an {@link OnCondition} for adding conditional listeners to the given observer.
	 * This provides a fluent API for reacting to specific conditions.
	 * {@snippet :
	 * selection.item()
	 *     .when(Objects::nonNull)
	 *     .accept(this::handleSelectedItem)
	 *     .when(Objects::isNull)
	 *     .run(this::onEmptySelection);
	 *}
	 * @return a new {@link Conditional}
	 */
	default OnCondition<T> when(Predicate<? super T> predicate) {
		return new DefaultConditional<>(this).when(requireNonNull(predicate));
	}
}
