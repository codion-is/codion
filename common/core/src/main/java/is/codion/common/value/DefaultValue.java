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
 * Copyright (c) 2019 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.value;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

class DefaultValue<T> extends AbstractValue<T> {

	protected final Object lock = new Object();

	private T value;

	protected DefaultValue(DefaultBuilder<T, ?> builder) {
		super(builder.nullValue, builder.notify);
		value = builder.prepareInitialValue();
		builder.validators.forEach(this::addValidator);
		builder.linkedValues.forEach(this::link);
		builder.linkedObservers.forEach(this::link);
		builder.listeners.forEach(this::addListener);
		builder.weakListeners.forEach(this::addWeakListener);
		builder.consumers.forEach(this::addConsumer);
		builder.weakConsumers.forEach(this::addWeakConsumer);
	}

	@Override
	protected final T getValue() {
		return value;
	}

	@Override
	protected final void setValue(T value) {
		this.value = value;
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
		public <T> Builder<T, ?> nullable(T value) {
			return (Builder<T, ?>) new DefaultBuilder<>()
							.value(value);
		}
	}

	static class DefaultBuilder<T, B extends Builder<T, B>> implements Builder<T, B> {

		private final T nullValue;
		private final List<Validator<? super T>> validators = new ArrayList<>();
		private final List<Value<T>> linkedValues = new ArrayList<>();
		private final List<ValueObserver<T>> linkedObservers = new ArrayList<>();
		private final List<Runnable> listeners = new ArrayList<>();
		private final List<Runnable> weakListeners = new ArrayList<>();
		private final List<Consumer<? super T>> consumers = new ArrayList<>();
		private final List<Consumer<? super T>> weakConsumers = new ArrayList<>();

		private T value;
		private Notify notify = Notify.WHEN_CHANGED;

		DefaultBuilder() {
			this.nullValue = null;
		}

		DefaultBuilder(T nullValue) {
			this.nullValue = requireNonNull(nullValue);
			this.value = nullValue;
		}

		@Override
		public final B value(T value) {
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
		public final B link(ValueObserver<T> originalValue) {
			this.linkedObservers.add(requireNonNull(originalValue));
			return self();
		}

		@Override
		public final B listener(Runnable listener) {
			this.listeners.add(requireNonNull(listener));
			return self();
		}

		@Override
		public final B consumer(Consumer<? super T> consumer) {
			this.consumers.add(requireNonNull(consumer));
			return self();
		}

		@Override
		public final B weakListener(Runnable weakListener) {
			this.weakListeners.add(requireNonNull(weakListener));
			return self();
		}

		@Override
		public final B weakConsumer(Consumer<? super T> weakConsumer) {
			this.weakConsumers.add(requireNonNull(weakConsumer));
			return self();
		}

		@Override
		public Value<T> build() {
			return new DefaultValue<>(this);
		}

		/**
		 * @return the initial value
		 */
		protected T prepareInitialValue() {
			return value == null ? nullValue : value;
		}

		private B self() {
			return (B) this;
		}
	}
}
