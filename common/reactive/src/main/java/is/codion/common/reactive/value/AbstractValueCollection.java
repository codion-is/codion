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
 * Copyright (c) 2024 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.reactive.value;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;

abstract class AbstractValueCollection<T, C extends Collection<T>> extends BaseValue<C> implements ValueCollection<T, C> {

	private final Supplier<? extends C> create;
	private final UnaryOperator<C> unmodifiable;

	private C value;

	private @Nullable Value<T> singleValue;

	AbstractValueCollection(AbstractValueCollectionBuilder<C, T, ?> builder) {
		super(builder);
		this.create = builder.create;
		this.unmodifiable = builder.unmodifiable;
	}

	@Override
	public final C get() {
		synchronized (lock) {
			return super.get();
		}
	}

	@Override
	public final void clear() {
		synchronized (lock) {
			super.clear();
		}
	}

	@Override
	public final Iterator<T> iterator() {
		return getOrThrow().iterator();
	}

	@Override
	public final Optional<C> optional() {
		synchronized (lock) {
			C values = getOrThrow();
			if (values.isEmpty()) {
				return Optional.empty();
			}

			return Optional.of(values);
		}
	}

	@Override
	public final void set(@Nullable Collection<T> values) {
		synchronized (lock) {
			C newValues = create.get();
			if (values != null) {
				newValues.addAll(values);
			}

			super.set(unmodifiable.apply(newValues));
		}
	}

	@Override
	public final boolean add(@Nullable T value) {
		synchronized (lock) {
			C newValues = create.get();
			newValues.addAll(getOrThrow());
			boolean added = newValues.add(value);
			super.set(unmodifiable.apply(newValues));

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
			newValues.addAll(getOrThrow());
			boolean added = newValues.addAll(values);
			super.set(unmodifiable.apply(newValues));

			return added;
		}
	}

	@Override
	public final boolean remove(@Nullable T value) {
		synchronized (lock) {
			C newValues = create.get();
			newValues.addAll(getOrThrow());
			boolean removed = newValues.remove(value);
			super.set(unmodifiable.apply(newValues));

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
			newValues.addAll(getOrThrow());
			boolean removed = newValues.removeAll(values);
			super.set(unmodifiable.apply(newValues));

			return removed;
		}
	}

	@Override
	public final boolean contains(@Nullable T value) {
		synchronized (lock) {
			return getOrThrow().contains(value);
		}
	}

	@Override
	public final boolean containsAll(Collection<T> values) {
		requireNonNull(values);
		synchronized (lock) {
			return getOrThrow().containsAll(values);
		}
	}

	@Override
	public final boolean containsOnly(Collection<T> values) {
		requireNonNull(values);
		synchronized (lock) {
			C collection = getOrThrow();

			return collection.size() == values.size() && collection.containsAll(values);
		}
	}

	@Override
	public final boolean containsNone(Collection<T> values) {
		requireNonNull(values);
		synchronized (lock) {
			return disjoint(getOrThrow(), values);
		}
	}

	@Override
	public final boolean isEmpty() {
		synchronized (lock) {
			return getOrThrow().isEmpty();
		}
	}

	@Override
	public final int size() {
		synchronized (lock) {
			return getOrThrow().size();
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
	public final void sort(Comparator<? super T> comparator) {
		requireNonNull(comparator);
		synchronized (lock) {
			List<T> list = new ArrayList<>(getOrThrow());
			list.sort(comparator);
			set(list);
		}
	}

	@Override
	protected final C getValue() {
		return value;
	}

	@Override
	protected final void setValue(C value) {
		this.value = value;
	}

	@Override
	public ObservableValueCollection<T, C> observable() {
		return (ObservableValueCollection<T, C>) super.observable();
	}

	private final class SingleValue extends AbstractValue<T> {

		private SingleValue() {
			AbstractValueCollection.this.addListener(this::notifyObserver);
		}

		@Override
		protected synchronized @Nullable T getValue() {
			C collection = AbstractValueCollection.this.getOrThrow();

			return collection.isEmpty() ? null : collection.iterator().next();
		}

		@Override
		protected synchronized void setValue(@Nullable T value) {
			AbstractValueCollection.this.set(value == null ? emptyList() : singleton(value));
		}
	}

	abstract static class AbstractValueCollectionBuilder<C extends Collection<T>, T, B extends ValueCollection.Builder<T, C, B>>
					extends BaseValue.BaseBuilder<C, B> implements ValueCollection.Builder<T, C, B> {

		private final Supplier<C> create;
		private final UnaryOperator<C> unmodifiable;

		AbstractValueCollectionBuilder(Supplier<C> create, UnaryOperator<C> unmodifiable) {
			super(requireNonNull(unmodifiable).apply(requireNonNull(create).get()));
			this.create = create;
			this.unmodifiable = unmodifiable;
		}

		@Override
		protected final C prepareInitialValue() {
			return unmodifiable.apply(super.prepareInitialValue());
		}
	}

	protected abstract static class AbstractObservableValueCollection<T, C extends Collection<T>>
					extends ObservableValue<C, ValueCollection<T, C>>
					implements ObservableValueCollection<T, C> {

		protected AbstractObservableValueCollection(ValueCollection<T, C> values) {
			super(values);
		}

		@Override
		public final Iterator<T> iterator() {
			return super.value().iterator();
		}

		@Override
		public final boolean contains(@Nullable T value) {
			return super.value().contains(value);
		}

		@Override
		public final boolean containsAll(Collection<T> values) {
			return super.value().containsAll(values);
		}

		@Override
		public final boolean containsOnly(Collection<T> values) {
			return super.value().containsOnly(values);
		}

		@Override
		public final boolean containsNone(Collection<T> values) {
			return super.value().containsNone(values);
		}

		@Override
		public final boolean isEmpty() {
			return super.value().isEmpty();
		}

		@Override
		public final int size() {
			return super.value().size();
		}

		@Override
		public final Optional<C> optional() {
			return super.value().optional();
		}
	}
}
