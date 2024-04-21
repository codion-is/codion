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
 * Copyright (c) 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.value;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Objects.requireNonNull;

class DefaultValues<C extends Collection<T>, T> extends DefaultValue<C>
				implements Values<T, C> {

	private final Supplier<? extends C> create;
	private final Function<C, C> unmodifiable;

	private Value<T> singleValue;

	DefaultValues(DefaultBuilder<C, T, ?> builder) {
		super(builder);
		this.create = builder.create;
		this.unmodifiable = builder.unmodifiable;
	}

	@Override
	public final Iterator<T> iterator() {
		return get().iterator();
	}

	@Override
	public final void set(Collection<T> values) {
		synchronized (lock) {
			C newValues = create.get();
			if (values != null) {
				newValues.addAll(values);
			}
			set(unmodifiable.apply(newValues));
		}
	}

	@Override
	public final C get() {
		synchronized (lock) {
			return super.get();
		}
	}

	@Override
	public final boolean add(T value) {
		synchronized (lock) {
			C newValues = create.get();
			newValues.addAll(get());
			boolean added = newValues.add(value);
			set(unmodifiable.apply(newValues));

			return added;
		}
	}

	@Override
	public final boolean addAll(T... values) {
		return addAll(asList(values));
	}

	@Override
	public final boolean addAll(Collection<T> values) {
		requireNonNull(values);
		synchronized (lock) {
			C newValues = create.get();
			newValues.addAll(get());
			boolean added = newValues.addAll(values);
			set(unmodifiable.apply(newValues));

			return added;
		}
	}

	@Override
	public final boolean remove(T value) {
		synchronized (lock) {
			C newValues = create.get();
			newValues.addAll(get());
			boolean removed = newValues.remove(value);
			set(unmodifiable.apply(newValues));

			return removed;
		}
	}

	@Override
	public final boolean removeAll(T... values) {
		return removeAll(asList(values));
	}

	@Override
	public final boolean removeAll(Collection<T> values) {
		requireNonNull(values);
		synchronized (lock) {
			C newValues = create.get();
			newValues.addAll(get());
			boolean removed = newValues.removeAll(values);
			set(unmodifiable.apply(newValues));

			return removed;
		}
	}

	@Override
	public final boolean contains(T value) {
		synchronized (lock) {
			return get().contains(value);
		}
	}

	@Override
	public final boolean containsAll(Collection<T> values) {
		requireNonNull(values);
		synchronized (lock) {
			return get().containsAll(values);
		}
	}

	@Override
	public final boolean empty() {
		synchronized (lock) {
			return get().isEmpty();
		}
	}

	@Override
	public final boolean notEmpty() {
		return !empty();
	}

	@Override
	public final int size() {
		synchronized (lock) {
			return get().size();
		}
	}

	@Override
	public final Value<T> value() {
		synchronized (lock) {
			if (singleValue == null) {
				singleValue = new SingleValue();
			}

			return singleValue;
		}
	}

	@Override
	public final boolean isNull() {
		return false;
	}

	@Override
	public synchronized ValuesObserver<T, C> observer() {
		return (ValuesObserver<T, C>) super.observer();
	}

	@Override
	protected ValuesObserver<T, C> createObserver() {
		return new DefaultValuesObserver<>(this);
	}

	private final class SingleValue extends AbstractValue<T> {

		private SingleValue() {
			super(null);
			DefaultValues.this.addListener(this::notifyListeners);
		}

		@Override
		public T get() {
			C collection = DefaultValues.this.get();

			return collection.isEmpty() ? null : collection.iterator().next();
		}

		@Override
		protected void setValue(T value) {
			synchronized (DefaultValues.this) {
				DefaultValues.this.set(value == null ? emptyList() : singleton(value));
			}
		}
	}

	static class DefaultBuilder<C extends Collection<T>, T, B extends Values.Builder<T, C, B>>
					extends DefaultValue.DefaultBuilder<C, B> implements Values.Builder<T, C, B> {

		private final Supplier<C> create;
		private final Function<C, C> unmodifiable;

		DefaultBuilder(Supplier<C> create, Function<C, C> unmodifiable) {
			super(requireNonNull(unmodifiable).apply(requireNonNull(create).get()));
			this.create = create;
			this.unmodifiable = unmodifiable;
		}

		@Override
		public Values<T, C> build() {
			return new DefaultValues<>(this);
		}

		@Override
		protected C prepareInitialValue() {
			return unmodifiable.apply(super.prepareInitialValue());
		}
	}
}
