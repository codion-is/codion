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
 * Copyright (c) 2010 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.reactive.state;

import is.codion.common.reactive.observer.Observer;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Specifies an observable for a {@link State} instance.
 */
public interface ObservableState extends Observer<Boolean> {

	/**
	 * @return the value of this state
	 */
	boolean is();

	/**
	 * @return A {@link ObservableState} instance that is always the reverse of this {@link ObservableState} instance
	 */
	ObservableState not();

	/**
	 * @return an {@link Observer} notified each time the observed value may have changed
	 */
	Observer<Boolean> observer();

	@Override
	default boolean addListener(Runnable listener) {
		return observer().addListener(listener);
	}

	@Override
	default boolean removeListener(Runnable listener) {
		return observer().removeListener(listener);
	}

	@Override
	default boolean addConsumer(Consumer<? super Boolean> consumer) {
		return observer().addConsumer(consumer);
	}

	@Override
	default boolean removeConsumer(Consumer<? super Boolean> consumer) {
		return observer().removeConsumer(consumer);
	}

	@Override
	default boolean addWeakListener(Runnable listener) {
		return observer().addWeakListener(listener);
	}

	@Override
	default boolean removeWeakListener(Runnable listener) {
		return observer().removeWeakListener(listener);
	}

	@Override
	default boolean addWeakConsumer(Consumer<? super Boolean> consumer) {
		return observer().addWeakConsumer(consumer);
	}

	@Override
	default boolean removeWeakConsumer(Consumer<? super Boolean> consumer) {
		return observer().removeWeakConsumer(consumer);
	}

	@Override
	default Observer<Boolean> when(Boolean value) {
		return observer().when(value);
	}

	@Override
	default Observer<Boolean> when(Predicate<? super Boolean> predicate) {
		return observer().when(predicate);
	}
}
