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
 * Copyright (c) 2019 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.value;

import is.codion.common.observable.Observable;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * Thread-safe implementation providing a custom Lock interface for synchronization.
 * Subclasses should synchronize on the {@link #lock} field for thread safety.
 */
class DefaultValue<T> extends AbstractValue<T> {

	protected final Lock lock = new Lock() {};

	private @Nullable T value;

	protected DefaultValue(DefaultBuilder<T, ?> builder) {
		super(builder.nullValue, builder.notify);
		value = builder.prepareInitialValue();
		builder.validators.forEach(this::addValidator);
		builder.linkedValues.forEach(this::link);
		builder.linkedObservables.forEach(this::link);
		builder.listeners.forEach(this::addListener);
	}

	@Override
	protected final @Nullable T getValue() {
		return value;
	}

	@Override
	protected final void setValue(@Nullable T value) {
		this.value = value;
	}

	private void addListener(Listener listener) {
		if (listener instanceof RunnableListener) {
			addListener(((RunnableListener) listener).runnable);
		}
		else if (listener instanceof ConsumerListener) {
			addConsumer(((ConsumerListener<T>) listener).consumer);
		}
		else if (listener instanceof WeakRunnableListener) {
			addWeakListener(((WeakRunnableListener) listener).runnable);
		}
		else if (listener instanceof WeakConsumerListener) {
			addWeakConsumer(((WeakConsumerListener<T>) listener).consumer);
		}
	}

	static final class DefaultBuilderFactory implements BuilderFactory {

		@Override
		public <T> Builder<T, ?> nonNull(T nullValue) {
			return new DefaultValue.DefaultBuilder<>(nullValue);
		}

		@Override
		public <T> Builder<T, ?> nullable() {
			return new DefaultValue.DefaultBuilder<>();
		}

		@Override
		public <T> Builder<T, ?> nullable(@Nullable T value) {
			return (Builder<T, ?>) new DefaultBuilder<>()
							.value(value);
		}
	}

	static class DefaultBuilder<T, B extends Builder<T, B>> implements Builder<T, B> {

		private final @Nullable T nullValue;
		private final List<Validator<? super T>> validators = new ArrayList<>();
		private final List<Value<T>> linkedValues = new ArrayList<>();
		private final List<Observable<T>> linkedObservables = new ArrayList<>();
		private final List<Listener> listeners = new ArrayList<>();

		private @Nullable T value;
		private Notify notify = Notify.CHANGED;

		DefaultBuilder() {
			this.nullValue = null;
		}

		DefaultBuilder(T nullValue) {
			this.nullValue = requireNonNull(nullValue);
			this.value = nullValue;
		}

		@Override
		public final B value(@Nullable T value) {
			this.value = value;
			return self();
		}

		@Override
		public final B notify(Notify notify) {
			this.notify = requireNonNull(notify);
			return self();
		}

		@Override
		public final B validator(Validator<? super T> validator) {
			this.validators.add(requireNonNull(validator));
			return self();
		}

		@Override
		public final B link(Value<T> originalValue) {
			this.linkedValues.add(requireNonNull(originalValue));
			return self();
		}

		@Override
		public final B link(Observable<T> observable) {
			this.linkedObservables.add(requireNonNull(observable));
			return self();
		}

		@Override
		public final B listener(Runnable listener) {
			this.listeners.add(new RunnableListener(requireNonNull(listener)));
			return self();
		}

		@Override
		public final B consumer(Consumer<? super T> consumer) {
			this.listeners.add(new ConsumerListener<>(requireNonNull(consumer)));
			return self();
		}

		@Override
		public final B weakListener(Runnable weakListener) {
			this.listeners.add(new WeakRunnableListener(requireNonNull(weakListener)));
			return self();
		}

		@Override
		public final B weakConsumer(Consumer<? super T> weakConsumer) {
			this.listeners.add(new WeakConsumerListener<>(requireNonNull(weakConsumer)));
			return self();
		}

		@Override
		public Value<T> build() {
			return new DefaultValue<>(this);
		}

		/**
		 * @return the initial value
		 */
		protected @Nullable T prepareInitialValue() {
			return value == null ? nullValue : value;
		}

		private B self() {
			return (B) this;
		}
	}

	private interface Listener {}

	private static final class RunnableListener implements Listener {

		private final Runnable runnable;

		private RunnableListener(Runnable runnable) {
			this.runnable = runnable;
		}
	}

	private static final class WeakRunnableListener implements Listener {

		private final Runnable runnable;

		private WeakRunnableListener(Runnable runnable) {
			this.runnable = runnable;
		}
	}

	private static final class ConsumerListener<T> implements Listener {

		private final Consumer<T> consumer;

		private ConsumerListener(Consumer<T> consumer) {
			this.consumer = consumer;
		}
	}

	private static final class WeakConsumerListener<T> implements Listener {

		private final Consumer<T> consumer;

		private WeakConsumerListener(Consumer<T> consumer) {
			this.consumer = consumer;
		}
	}
	
	private interface Lock {}
}
