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
 * Copyright (c) 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.observable;

import is.codion.common.event.EventObserver;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A wrapper for a value, providing a change observer.
 * @param <T> the type being observed
 */
public interface Observable<T> extends EventObserver<T>, Supplier<T> {

	/**
	 * Sets the value
	 * @param value the value to set
	 */
	void set(T value);

	/**
	 * @return an Optional based on the current value
	 */
	default Optional<T> optional() {
		return Optional.ofNullable(get());
	}

	/**
	 * @return an {@link EventObserver} notified each time the value may have changed
	 */
	EventObserver<T> observer();

	@Override
	default boolean removeWeakConsumer(Consumer<? super T> consumer) {
		return observer().removeWeakConsumer(consumer);
	}

	@Override
	default boolean addWeakConsumer(Consumer<? super T> consumer) {
		return observer().addWeakConsumer(consumer);
	}

	@Override
	default boolean removeWeakListener(Runnable listener) {
		return observer().removeWeakListener(listener);
	}

	@Override
	default boolean addWeakListener(Runnable listener) {
		return observer().addWeakListener(listener);
	}

	@Override
	default boolean removeConsumer(Consumer<? super T> consumer) {
		return observer().removeConsumer(consumer);
	}

	@Override
	default boolean addConsumer(Consumer<? super T> consumer) {
		return observer().addConsumer(consumer);
	}

	@Override
	default boolean removeListener(Runnable listener) {
		return observer().removeListener(listener);
	}

	@Override
	default boolean addListener(Runnable listener) {
		return observer().addListener(listener);
	}
}
