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

import static java.util.Objects.requireNonNull;

final class DefaultConditional<T> implements Conditional<T> {

	private final Observer<T> observer;
	private final Predicate<? super T> predicate;

	DefaultConditional(Observer<T> observer, @Nullable T value) {
		this(observer, new Equals<>(value));
	}

	DefaultConditional(Observer<T> observer, Predicate<? super T> predicate) {
		this.observer = observer;
		this.predicate = predicate;
	}

	@Override
	public Observer<T> run(Runnable runnable) {
		new Runner<>(observer, predicate, requireNonNull(runnable));

		return observer;
	}

	@Override
	public Observer<T> accept(Consumer<? super T> consumer) {
		new Acceptor<>(observer, predicate, requireNonNull(consumer));

		return observer;
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

	private static final class Runner<T> implements Consumer<T> {

		private final Predicate<? super T> predicate;
		private final Runnable listener;

		private Runner(Observer<T> observer, Predicate<? super T> predicate, Runnable listener) {
			this.predicate = predicate;
			this.listener = listener;
			observer.addConsumer(this);
		}

		@Override
		public void accept(T value) {
			if (predicate.test(value)) {
				listener.run();
			}
		}
	}

	private static final class Acceptor<T> implements Consumer<T> {

		private final Predicate<? super T> predicate;
		private final Consumer<? super T> consumer;

		private Acceptor(Observer<T> observer, Predicate<? super T> predicate, Consumer<? super T> consumer) {
			this.predicate = predicate;
			this.consumer = consumer;
			observer.addConsumer(this);
		}

		@Override
		public void accept(T value) {
			if (predicate.test(value)) {
				consumer.accept(value);
			}
		}
	}
}
