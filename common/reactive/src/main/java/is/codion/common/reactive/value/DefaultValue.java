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
 * Copyright (c) 2019 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.reactive.value;

import is.codion.common.reactive.observer.Observable;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static is.codion.common.reactive.value.Value.Notify.CHANGED;
import static java.util.Objects.requireNonNull;

/**
 * Thread-safe implementation providing a custom Lock interface for synchronization.
 * Subclasses should synchronize on the {@link #lock} field for thread safety.
 */
class DefaultValue<T> extends AbstractValue<T> {

	static final BuilderFactory BUILDER_FACTORY = new DefaultBuilderFactory();

	protected final Object lock = new Lock() {};

	private @Nullable T value;

	protected DefaultValue(DefaultBuilder<T, ?> builder) {
		super(builder.nullValue, builder.notify);
		value = builder.prepareInitialValue();
		builder.validators.forEach(this::addValidator);
		builder.linkedValues.forEach(this::link);
		builder.linkedObservables.forEach(this::link);
		builder.listeners.addListeners(this);
		if (builder.locked) {
			locked().set(true);
		}
	}

	@Override
	protected final @Nullable T getValue() {
		return value;
	}

	@Override
	protected final void setValue(@Nullable T value) {
		this.value = value;
	}

	private static final class DefaultBuilderFactory implements BuilderFactory {

		@Override
		public <T> Builder<T, ?> nonNull(T nullValue) {
			return new DefaultBuilder<>(nullValue);
		}

		@Override
		public <T> Builder<T, ?> nullable() {
			return new DefaultBuilder<>();
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
		private final ValueListeners<T> listeners = new ValueListeners<>();
		private @Nullable T value;
		private Notify notify = CHANGED;
		private boolean locked = false;

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
		public final B locked(boolean locked) {
			this.locked = locked;
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
			this.listeners.listener(listener);
			return self();
		}

		@Override
		public final B consumer(Consumer<? super T> consumer) {
			this.listeners.consumer(consumer);
			return self();
		}

		@Override
		public final B weakListener(Runnable weakListener) {
			this.listeners.weakListener(weakListener);
			return self();
		}

		@Override
		public final B weakConsumer(Consumer<? super T> weakConsumer) {
			this.listeners.weakConsumer(weakConsumer);
			return self();
		}

		@Override
		public final B changeListener(Runnable listener) {
			this.listeners.changeListener(listener);
			return self();
		}

		@Override
		public final B changeConsumer(Consumer<ValueChange<? super T>> consumer) {
			this.listeners.changeConsumer(consumer);
			return self();
		}

		@Override
		public final B weakChangeListener(Runnable weakListener) {
			this.listeners.weakChangeListener(weakListener);
			return self();
		}

		@Override
		public final B weakChangeConsumer(Consumer<ValueChange<? super T>> weakConsumer) {
			this.listeners.weakChangeConsumer(weakConsumer);
			return self();
		}

		@Override
		public final B when(T value, Runnable listener) {
			listeners.when(value, listener);
			return self();
		}

		@Override
		public final B when(T value, Consumer<? super T> consumer) {
			listeners.when(value, consumer);
			return self();
		}

		@Override
		public final B when(Predicate<T> predicate, Runnable listener) {
			listeners.when(predicate, listener);
			return self();
		}

		@Override
		public final B when(Predicate<T> predicate, Consumer<? super T> consumer) {
			listeners.when(predicate, consumer);
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

	private interface Lock {}
}
