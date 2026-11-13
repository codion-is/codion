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
import static java.util.Objects.deepEquals;
import static java.util.Objects.requireNonNull;

/**
 * <p>The mutators are synchronized, each change made and notified while holding this value's monitor, so changes
 * from different threads take turns, notified in the order they were made. The value is an unmodifiable snapshot,
 * replaced on each change and held in a volatile field, so reading it takes no lock and never waits for listeners.
 * <p>The listener list is not guarded by this monitor, see {@link is.codion.common.reactive.internal.Listeners},
 * so a derived observer can be detached on another thread while this value notifies.
 */
abstract class AbstractValueCollection<T, C extends Collection<T>> extends BaseValue<C> implements ValueCollection<T, C> {

	private volatile C value;

	private @Nullable Value<T> singleValue;

	AbstractValueCollection(AbstractValueCollectionBuilder<C, T, ?> builder) {
		super(builder);
	}

	@Override
	public final C get() {
		return super.get();
	}

	@Override
	public final synchronized void clear() {
		super.clear();
	}

	@Override
	public final synchronized void update(UnaryOperator<@Nullable C> updateFunction) {
		set(requireNonNull(updateFunction).apply(get()));
	}

	@Override
	public final Iterator<T> iterator() {
		return getOrThrow().iterator();
	}

	@Override
	public final Optional<C> optional() {
		C values = getOrThrow();
		if (values.isEmpty()) {
			return Optional.empty();
		}

		return Optional.of(values);
	}

	@Override
	public final synchronized void set(@Nullable Collection<? extends T> values) {
		C newValues = create();
		if (values != null) {
			newValues.addAll(values);
		}

		super.set(unmodifiable(newValues));
	}

	@Override
	public final synchronized boolean add(@Nullable T value) {
		C newValues = create();
		newValues.addAll(getOrThrow());
		boolean added = newValues.add(value);
		super.set(unmodifiable(newValues));

		return added;
	}

	@Override
	@SafeVarargs
	public final boolean addAll(T... values) {
		return addAll(asList(values));
	}

	@Override
	public final synchronized boolean addAll(Collection<? extends T> values) {
		requireNonNull(values);
		C newValues = create();
		newValues.addAll(getOrThrow());
		boolean added = newValues.addAll(values);
		super.set(unmodifiable(newValues));

		return added;
	}

	@Override
	public final synchronized boolean remove(@Nullable T value) {
		C newValues = create();
		newValues.addAll(getOrThrow());
		boolean removed = newValues.remove(value);
		super.set(unmodifiable(newValues));

		return removed;
	}

	@Override
	@SafeVarargs
	public final boolean removeAll(T... values) {
		return removeAll(asList(values));
	}

	@Override
	public final synchronized boolean removeAll(Collection<? extends T> values) {
		requireNonNull(values);
		C newValues = create();
		newValues.addAll(getOrThrow());
		boolean removed = newValues.removeAll(values);
		super.set(unmodifiable(newValues));

		return removed;
	}

	@Override
	public final boolean contains(@Nullable T value) {
		return getOrThrow().contains(value);
	}

	@Override
	public final boolean containsAll(Collection<? extends T> values) {
		requireNonNull(values);
		return getOrThrow().containsAll(values);
	}

	@Override
	public final boolean containsOnly(Collection<? extends T> values) {
		requireNonNull(values);
		C collection = getOrThrow();

		//by membership both ways, which sizes can not stand in for, either side may hold duplicates
		return collection.containsAll(values) && values.containsAll(collection);
	}

	@Override
	public final boolean containsNone(Collection<? extends T> values) {
		requireNonNull(values);
		return disjoint(getOrThrow(), values);
	}

	@Override
	public final boolean isEmpty() {
		return getOrThrow().isEmpty();
	}

	@Override
	public final int size() {
		return getOrThrow().size();
	}

	@Override
	public final synchronized Value<T> value() {
		if (singleValue == null) {
			singleValue = new SingleValue();
		}

		return singleValue;
	}

	@Override
	public final boolean isNull() {
		return false;
	}

	@Override
	public final synchronized void sort(Comparator<? super T> comparator) {
		requireNonNull(comparator);
		C previous = getOrThrow();
		List<T> sorted = new ArrayList<>(previous);
		sorted.sort(comparator);
		boolean reordered = !sameOrder(previous, sorted);
		//equality can miss a reorder entirely - Set equality ignores item order - so neither the lock nor the
		//notification policy of set() sees it. Sorting is the one mutation whose whole purpose is a change
		//equality may not see, so the sort checks the lock itself, and announces itself where set() did not
		//notify, it notifying on a change only
		if (reordered && locked().is()) {
			throw new IllegalStateException("Value is locked and can not be changed");
		}
		set(sorted);
		if (reordered && notifiesOnChange() && deepEquals(previous, getOrThrow())) {
			notifyObserver();
		}
	}

	/**
	 * Methods rather than fields assigned from the builder, since the base constructor applies the builder's
	 * links, which set this value, before a field of this class is assigned.
	 * @return a new empty modifiable collection
	 */
	abstract C create();

	/**
	 * @param values the values
	 * @return an unmodifiable view of the given values
	 */
	abstract C unmodifiable(C values);

	private static boolean sameOrder(Collection<?> previous, Collection<?> current) {
		Iterator<?> previousItems = previous.iterator();
		Iterator<?> currentItems = current.iterator();
		while (previousItems.hasNext() && currentItems.hasNext()) {
			if (!deepEquals(previousItems.next(), currentItems.next())) {
				return false;
			}
		}

		return !previousItems.hasNext() && !currentItems.hasNext();
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
	public synchronized ObservableValueCollection<T, C> observable() {
		return (ObservableValueCollection<T, C>) super.observable();
	}

	/**
	 * The {@link ValueCollection#value()} projection: the first item, or null when empty.
	 * <p>A no-notify {@link AbstractValue}, so the collection listener below is its only notification
	 * source, and the last notified item is remembered so that the projection notifies when it moves
	 * rather than whenever the collection changes underneath it.
	 */
	private final class SingleValue extends AbstractValue<T> {

		private @Nullable T notified;

		private SingleValue() {
			notified = getValue();
			AbstractValueCollection.this.addListener(this::onCollectionChanged);
		}

		private void onCollectionChanged() {
			synchronized (AbstractValueCollection.this) {
				T current = getValue();
				if (deepEquals(notified, current)) {
					return;
				}
				notified = current;
			}
			notifyObserver();
		}

		@Override
		protected @Nullable T getValue() {
			C collection = AbstractValueCollection.this.getOrThrow();

			return collection.isEmpty() ? null : collection.iterator().next();
		}

		@Override
		protected void setValue(@Nullable T value) {
			synchronized (AbstractValueCollection.this) {
				AbstractValueCollection.this.set(value == null ? emptyList() : singleton(value));
			}
		}
	}

	abstract static class AbstractValueCollectionBuilder<C extends Collection<T>, T, B extends ValueCollection.Builder<T, C, B>>
					extends BaseValue.BaseBuilder<C, B> implements ValueCollection.Builder<T, C, B> {

		private final Supplier<C> create;
		private final UnaryOperator<C> unmodifiable;

		AbstractValueCollectionBuilder(C nullValue, Supplier<C> create, UnaryOperator<C> unmodifiable) {
			super(nullValue);
			this.create = requireNonNull(create);
			this.unmodifiable = requireNonNull(unmodifiable);
		}

		@Override
		protected final C prepareInitialValue() {
			//copied, as set() copies, so that the caller's collection can not change the value behind its back
			C values = create.get();
			values.addAll(super.prepareInitialValue());

			return unmodifiable.apply(values);
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
		public final boolean containsAll(Collection<? extends T> values) {
			return super.value().containsAll(values);
		}

		@Override
		public final boolean containsOnly(Collection<? extends T> values) {
			return super.value().containsOnly(values);
		}

		@Override
		public final boolean containsNone(Collection<? extends T> values) {
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
