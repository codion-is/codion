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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.reactive.state;

import is.codion.common.reactive.observer.Observable;

import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Predicate;

final class ObservableMatches<T> implements Runnable {

	private final Observable<T> observable;
	private final State state = State.state();
	private final Predicate<? super T> predicate;

	ObservableMatches(Observable<T> observable, @Nullable T value) {
		this(observable, new Equals<>(value));
	}

	ObservableMatches(Observable<T> observable, Predicate<? super T> predicate) {
		this.observable = observable;
		this.predicate = predicate;
		this.state.set(predicate.test(observable.get()));
		observable.addListener(this);
	}

	@Override
	public void run() {
		state.set(predicate.test(observable.get()));
	}

	ObservableState observable() {
		return state.observable();
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
