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
package is.codion.common.reactive.state;

import is.codion.common.reactive.observer.Observer;
import is.codion.common.reactive.value.Value;
import is.codion.common.reactive.value.Value.Notify;
import is.codion.common.reactive.value.Value.Validator;

import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

/**
 * Thread-safe implementation of State.
 * All state operations are synchronized on the internal value.
 */
final class DefaultState implements State {

	private final Value<Boolean> value;

	private @Nullable DefaultObservableState observableState;

	private DefaultState(DefaultBuilder builder) {
		this.value = builder.valueBuilder.consumer(new Notifier()).build();
		if (builder.group != null) {
			builder.group.add(this);
		}
	}

	@Override
	public String toString() {
		return Boolean.toString(is());
	}

	@Override
	public boolean is() {
		synchronized (this.value) {
			return this.value.getOrThrow();
		}
	}

	@Override
	public void set(boolean value) {
		synchronized (this.value) {
			this.value.set(value);
		}
	}

	@Override
	public void toggle() {
		synchronized (this.value) {
			this.value.set(!this.value.getOrThrow());
		}
	}

	@Override
	public ObservableState observable() {
		synchronized (this.value) {
			if (observableState == null) {
				observableState = new DefaultObservableState(this, false);
			}

			return observableState;
		}
	}

	@Override
	public ObservableState not() {
		return observable().not();
	}

	@Override
	public Observer<Boolean> observer() {
		return observable().observer();
	}

	@Override
	public Value<Boolean> value() {
		return value;
	}

	@Override
	public void link(State originalState) {
		value.link(requireNonNull(originalState).value());
	}

	@Override
	public void unlink(State originalState) {
		value.unlink(requireNonNull(originalState).value());
	}

	@Override
	public boolean addValidator(Validator<? super Boolean> validator) {
		return this.value.addValidator(validator);
	}

	@Override
	public boolean removeValidator(Validator<? super Boolean> validator) {
		return this.value.removeValidator(validator);
	}

	private final class Notifier implements Consumer<Boolean> {

		@Override
		public void accept(Boolean value) {
			synchronized (DefaultState.this.value) {
				if (observableState != null) {
					observableState.notifyObservers(value, !value);
				}
			}
		}
	}

	static final class DefaultBuilder implements Builder {

		private final Value.Builder<Boolean, ?> valueBuilder = Value.builder().nonNull(false);

		private @Nullable Group group;

		DefaultBuilder() {}

		@Override
		public Builder value(boolean value) {
			this.valueBuilder.value(value);
			return this;
		}

		@Override
		public Builder notify(Notify notify) {
			valueBuilder.notify(notify);
			return this;
		}

		@Override
		public Builder validator(Validator<? super Boolean> validator) {
			valueBuilder.validator(validator);
			return this;
		}

		@Override
		public Builder link(State originalState) {
			valueBuilder.link(requireNonNull(originalState).value());
			return this;
		}

		@Override
		public Builder group(Group group) {
			this.group = requireNonNull(group);
			return this;
		}

		@Override
		public Builder listener(Runnable listener) {
			valueBuilder.listener(listener);
			return this;
		}

		@Override
		public Builder consumer(Consumer<? super Boolean> consumer) {
			valueBuilder.consumer(consumer);
			return this;
		}

		@Override
		public Builder weakListener(Runnable weakListener) {
			valueBuilder.weakListener(weakListener);
			return this;
		}

		@Override
		public Builder weakConsumer(Consumer<? super Boolean> weakConsumer) {
			valueBuilder.weakConsumer(weakConsumer);
			return this;
		}

		@Override
		public Builder when(boolean value, Runnable runnable) {
			valueBuilder.when(value, runnable);
			return this;
		}

		@Override
		public Builder when(boolean value, Consumer<? super Boolean> consumer) {
			valueBuilder.when(value, consumer);
			return this;
		}

		@Override
		public Builder when(Predicate<Boolean> predicate, Runnable runnable) {
			valueBuilder.when(predicate, runnable);
			return this;
		}

		@Override
		public Builder when(Predicate<Boolean> predicate, Consumer<? super Boolean> consumer) {
			valueBuilder.when(predicate, consumer);
			return this;
		}

		@Override
		public State build() {
			return new DefaultState(this);
		}
	}
}
