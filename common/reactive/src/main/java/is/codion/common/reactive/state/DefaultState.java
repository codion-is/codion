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
package is.codion.common.reactive.state;

import is.codion.common.reactive.observer.Observer;
import is.codion.common.reactive.value.Value;
import is.codion.common.reactive.value.Value.Validator;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

/**
 * Implementation of State. Listener management is thread-safe; state modification and notification
 * are single-thread by design (typically an application UI thread). The internal synchronization guards
 * lazy field initialization and listener bookkeeping, not concurrent modification.
 */
final class DefaultState implements State {

	private final Value<Boolean> value;

	//volatile, read by the notifier without the value's lock, which it can not take while the value is being built
	private volatile @Nullable DefaultObservableState observableState;

	private DefaultState(DefaultBuilder builder) {
		this.value = builder.value(new Notifier());
		builder.groups.forEach(group -> group.add(this));
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
		//a pure delegation, and must stay one: value() is public, so a step taken here and not on the
		//value is a step a caller writing through the value silently skips
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

		/**
		 * Called while the value is built as well, by a link of the builder setting it, before this state has a
		 * value to synchronize on, or observers to notify.
		 */
		@Override
		public void accept(Boolean value) {
			DefaultObservableState observable = observableState;
			if (observable != null) {
				//the value notifies on change only, and a boolean changes to its opposite
				observable.notifyObservers(value, !value);
			}
		}
	}

	static final class DefaultBuilder implements Builder {

		//applied to a value builder of each state's own, each state's notifier added to its own value alone,
		//after the listeners and consumers set here, and so before the links, which a value adds after them
		private final List<Consumer<Value.Builder<Boolean, ?>>> settings = new ArrayList<>();
		private final Collection<Group> groups = new ArrayList<>(1);

		DefaultBuilder() {}

		@Override
		public Builder value(boolean value) {
			settings.add(builder -> builder.value(value));
			return this;
		}

		@Override
		public Builder validator(Validator<? super Boolean> validator) {
			requireNonNull(validator);
			settings.add(builder -> builder.validator(validator));
			return this;
		}

		@Override
		public Builder link(State originalState) {
			Value<Boolean> originalValue = requireNonNull(originalState).value();
			settings.add(builder -> builder.link(originalValue));
			return this;
		}

		@Override
		public Builder group(Group group) {
			this.groups.add(requireNonNull(group));
			return this;
		}

		@Override
		public Builder locked(boolean locked) {
			settings.add(builder -> builder.locked(locked));
			return this;
		}

		@Override
		public Builder listener(Runnable listener) {
			requireNonNull(listener);
			settings.add(builder -> builder.listener(listener));
			return this;
		}

		@Override
		public Builder consumer(Consumer<? super Boolean> consumer) {
			requireNonNull(consumer);
			settings.add(builder -> builder.consumer(consumer));
			return this;
		}

		@Override
		public Builder weakListener(Runnable weakListener) {
			requireNonNull(weakListener);
			settings.add(builder -> builder.weakListener(weakListener));
			return this;
		}

		@Override
		public Builder weakConsumer(Consumer<? super Boolean> weakConsumer) {
			requireNonNull(weakConsumer);
			settings.add(builder -> builder.weakConsumer(weakConsumer));
			return this;
		}

		@Override
		public Builder when(boolean value, Runnable listener) {
			requireNonNull(listener);
			settings.add(builder -> builder.when(value, listener));
			return this;
		}

		@Override
		public Builder when(boolean value, Consumer<? super Boolean> consumer) {
			requireNonNull(consumer);
			settings.add(builder -> builder.when(value, consumer));
			return this;
		}

		@Override
		public Builder when(Predicate<Boolean> predicate, Runnable listener) {
			requireNonNull(predicate);
			requireNonNull(listener);
			settings.add(builder -> builder.when(predicate, listener));
			return this;
		}

		@Override
		public Builder when(Predicate<Boolean> predicate, Consumer<? super Boolean> consumer) {
			requireNonNull(predicate);
			requireNonNull(consumer);
			settings.add(builder -> builder.when(predicate, consumer));
			return this;
		}

		@Override
		public State build() {
			return new DefaultState(this);
		}

		private Value<Boolean> value(Consumer<Boolean> notifier) {
			Value.Builder<Boolean, ?> builder = Value.builder().nonNull(false);
			settings.forEach(setting -> setting.accept(builder));

			return builder.consumer(notifier).build();
		}
	}
}
