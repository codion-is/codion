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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.reactive.observer;

import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A filtered view of another {@link Observer}, notifying its own listeners with the values its predicate accepts.
 * <p>It subscribes to the observer it filters only while it has listeners of its own, attaching on the first and
 * detaching on the last. Since the subscription is what keeps it reachable - the observer holds {@link #consumer},
 * which holds this - one that is never listened to, or whose listeners are all removed, is left to be collected
 * rather than accumulating on an observer that may outlive it by a lot.
 * @param <T> the observed type
 */
final class Conditional<T> extends AbstractObserver<T> {

	private final Observer<T> observer;
	private final Predicate<? super @Nullable T> predicate;
	private final Consumer<@Nullable T> consumer = this::notify;

	Conditional(Observer<T> observer, @Nullable T value) {
		this(observer, new Equals<>(value));
	}

	Conditional(Observer<T> observer, Predicate<? super T> predicate) {
		this.observer = observer;
		this.predicate = predicate;
	}

	@Override
	void onFirstListener() {
		observer.addConsumer(consumer);
	}

	@Override
	void onLastListener() {
		observer.removeConsumer(consumer);
	}

	private void notify(@Nullable T value) {
		if (predicate.test(value)) {
			notifyListeners(value);
		}
	}

	private static final class Equals<T> implements Predicate<T> {

		private final @Nullable T value;

		private Equals(@Nullable T value) {
			this.value = value;
		}

		@Override
		public boolean test(T value) {
			return Objects.deepEquals(this.value, value);
		}
	}
}
