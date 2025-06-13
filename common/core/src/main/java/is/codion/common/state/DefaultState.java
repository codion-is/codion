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
package is.codion.common.state;

import is.codion.common.observable.Observable;
import is.codion.common.observable.Observer;
import is.codion.common.value.Value;

import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * Thread-safe implementation of State.
 * All state operations are synchronized on the internal value.
 */
final class DefaultState implements State {

	private final Value<Boolean> value;

	private @Nullable DefaultObservableState observableState;

	private DefaultState(Value.Builder<Boolean, ?> valueBuilder) {
		this.value = valueBuilder.consumer(new Notifier()).build();
	}

	@Override
	public String toString() {
		return Boolean.toString(value.getOrThrow());
	}

	@Override
	public Boolean get() {
		synchronized (this.value) {
			return this.value.getOrThrow();
		}
	}

	@Override
	public void set(@Nullable Boolean value) {
		synchronized (this.value) {
			this.value.set(value);
		}
	}

	@Override
	public void clear() {
		set(null);
	}

	@Override
	public void map(UnaryOperator<Boolean> mapper) {
		synchronized (this.value) {
			this.value.map(mapper);
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
	public void link(Value<Boolean> originalValue) {
		this.value.link(originalValue);
	}

	@Override
	public void unlink(Value<Boolean> originalValue) {
		this.value.unlink(originalValue);
	}

	@Override
	public void link(Observable<Boolean> observable) {
		this.value.link(observable);
	}

	@Override
	public void unlink(Observable<Boolean> observable) {
		this.value.unlink(observable);
	}

	@Override
	public boolean addValidator(Validator<? super Boolean> validator) {
		return this.value.addValidator(validator);
	}

	@Override
	public boolean removeValidator(Validator<? super Boolean> validator) {
		return this.value.removeValidator(validator);
	}

	@Override
	public void validate(Boolean value) {
		this.value.validate(value);
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

		DefaultBuilder(boolean value) {
			valueBuilder.value(value);
		}

		@Override
		public Builder value(@Nullable Boolean value) {
			valueBuilder.value(value);
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
		public Builder link(Value<Boolean> originalState) {
			valueBuilder.link(originalState);
			return this;
		}

		@Override
		public Builder link(Observable<Boolean> observable) {
			valueBuilder.link(observable);
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
		public State build() {
			return new DefaultState(valueBuilder);
		}
	}
}
