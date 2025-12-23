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
package is.codion.swing.common.ui.component.builder;

import is.codion.common.reactive.value.Value;
import is.codion.common.reactive.value.ValueChange;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

/**
 * A utility for adding listeners and consumers to a Value, in the order they were defined.
 * @param <T> the value type
 */
final class ValueListeners<T> {

	private final List<Adder<T>> listeners = new ArrayList<>();

	void addListeners(Value<T> value) {
		listeners.forEach(adder -> adder.add(value));
	}

	void listener(Runnable listener) {
		listeners.add(new AddListener<>(requireNonNull(listener)));
	}

	void consumer(Consumer<? super T> consumer) {
		listeners.add(new AddConsumer<>(requireNonNull(consumer)));
	}

	void weakListener(Runnable weakListener) {
		listeners.add(new AddWeakListener<>(requireNonNull(weakListener)));
	}

	void weakConsumer(Consumer<? super T> weakConsumer) {
		listeners.add(new AddWeakConsumer<>(requireNonNull(weakConsumer)));
	}

	void changeListener(Runnable listener) {
		listeners.add(new AddChangeListener<>(requireNonNull(listener)));
	}

	void changeConsumer(Consumer<ValueChange<? super T>> consumer) {
		listeners.add(new AddChangeConsumer<>(requireNonNull(consumer)));
	}

	void weakChangeListener(Runnable weakListener) {
		listeners.add(new AddWeakChangeListener<>(requireNonNull(weakListener)));
	}

	void weakChangeConsumer(Consumer<ValueChange<? super T>> weakConsumer) {
		listeners.add(new AddWeakChangeConsumer<>(requireNonNull(weakConsumer)));
	}

	void when(T value, Runnable listener) {
		listeners.add(new AddValueListener<>(value, requireNonNull(listener)));
	}

	void when(T value, Consumer<? super T> consumer) {
		listeners.add(new AddValueConsumer<>(value, requireNonNull(consumer)));
	}

	void when(Predicate<T> predicate, Runnable listener) {
		listeners.add(new AddPredicateListener<>(requireNonNull(predicate), requireNonNull(listener)));
	}

	void when(Predicate<T> predicate, Consumer<? super T> consumer) {
		listeners.add(new AddPredicateConsumer<>(requireNonNull(predicate), requireNonNull(consumer)));
	}

	private interface Adder<T> {

		void add(Value<T> value);
	}

	private static final class AddListener<T> implements Adder<T> {

		private final Runnable listener;

		private AddListener(Runnable listener) {
			this.listener = listener;
		}

		@Override
		public void add(Value<T> value) {
			value.addListener(listener);
		}
	}

	private static final class AddWeakListener<T> implements Adder<T> {

		private final Runnable listener;

		private AddWeakListener(Runnable listener) {
			this.listener = listener;
		}

		@Override
		public void add(Value<T> value) {
			value.addWeakListener(listener);
		}
	}

	private static final class AddConsumer<T> implements Adder<T> {

		private final Consumer<? super T> consumer;

		private AddConsumer(Consumer<? super T> consumer) {
			this.consumer = consumer;
		}

		@Override
		public void add(Value<T> value) {
			value.addConsumer(consumer);
		}
	}

	private static final class AddWeakConsumer<T> implements Adder<T> {

		private final Consumer<? super T> consumer;

		private AddWeakConsumer(Consumer<? super T> consumer) {
			this.consumer = consumer;
		}

		@Override
		public void add(Value<T> value) {
			value.addWeakConsumer(consumer);
		}
	}

	private static final class AddChangeListener<T> implements Adder<T> {

		private final Runnable listener;

		private AddChangeListener(Runnable listener) {
			this.listener = listener;
		}

		@Override
		public void add(Value<T> value) {
			value.changed().addListener(listener);
		}
	}

	private static final class AddWeakChangeListener<T> implements Adder<T> {

		private final Runnable listener;

		private AddWeakChangeListener(Runnable listener) {
			this.listener = listener;
		}

		@Override
		public void add(Value<T> value) {
			value.changed().addWeakListener(listener);
		}
	}

	private static final class AddChangeConsumer<T> implements Adder<T> {

		private final Consumer<ValueChange<? super T>> consumer;

		private AddChangeConsumer(Consumer<ValueChange<? super T>> consumer) {
			this.consumer = consumer;
		}

		@Override
		public void add(Value<T> value) {
			value.changed().addConsumer(consumer);
		}
	}

	private static final class AddWeakChangeConsumer<T> implements Adder<T> {

		private final Consumer<ValueChange<? super T>> consumer;

		private AddWeakChangeConsumer(Consumer<ValueChange<? super T>> consumer) {
			this.consumer = consumer;
		}

		@Override
		public void add(Value<T> value) {
			value.changed().addWeakConsumer(consumer);
		}
	}

	private static final class AddValueListener<T> implements Adder<T> {

		private final @Nullable T value;
		private final Runnable listener;

		private AddValueListener(T value, Runnable listener) {
			this.value = value;
			this.listener = listener;
		}

		@Override
		public void add(Value<T> value) {
			value.when(this.value).addListener(listener);
		}
	}

	private static final class AddValueConsumer<T> implements Adder<T> {

		private final @Nullable T value;
		private final Consumer<? super T> consumer;

		private AddValueConsumer(T value, Consumer<? super T> consumer) {
			this.value = value;
			this.consumer = consumer;
		}

		@Override
		public void add(Value<T> value) {
			value.when(this.value).addConsumer(consumer);
		}
	}

	private static final class AddPredicateListener<T> implements Adder<T> {

		private final Predicate<T> predicate;
		private final Runnable listener;

		private AddPredicateListener(Predicate<T> predicate, Runnable listener) {
			this.predicate = predicate;
			this.listener = listener;
		}

		@Override
		public void add(Value<T> value) {
			value.when(predicate).addListener(listener);
		}
	}

	private static final class AddPredicateConsumer<T> implements Adder<T> {

		private final Predicate<T> predicate;
		private final Consumer<? super T> consumer;

		private AddPredicateConsumer(Predicate<T> predicate, Consumer<? super T> consumer) {
			this.predicate = predicate;
			this.consumer = consumer;
		}

		@Override
		public void add(Value<T> value) {
			value.when(predicate).addConsumer(consumer);
		}
	}
}
