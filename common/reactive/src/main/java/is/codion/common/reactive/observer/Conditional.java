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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.reactive.observer;

import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

final class Conditional<T> extends DefaultObserver<T> implements Consumer<T> {

	private final Predicate<? super T> predicate;

	Conditional(Observer<T> observer, @Nullable T value) {
		this(observer, new Equals<>(value));
	}

	Conditional(Observer<T> observer, Predicate<? super T> predicate) {
		this.predicate = predicate;
		observer.addConsumer(this);
	}

	@Override
	public void accept(@Nullable T value) {
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
			return Objects.equals(this.value, value);
		}
	}
}
