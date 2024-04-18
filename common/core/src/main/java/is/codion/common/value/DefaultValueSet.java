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
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.value;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;

final class DefaultValueSet<T> extends AbstractValue<Set<T>> implements ValueSet<T> {

	private final Set<T> values = new LinkedHashSet<>();

	private Value<T> value;

	private DefaultValueSet(DefaultBuilder<T> builder) {
		super(emptySet(), builder.notify);
		set(builder.initialValue);
		builder.validators.forEach(this::addValidator);
		builder.linkedValueSets.forEach(this::link);
		builder.linkedValueSetObservers.forEach(this::link);
		builder.listeners.forEach(this::addListener);
		builder.weakListeners.forEach(this::addWeakListener);
		builder.consumers.forEach(this::addConsumer);
		builder.weakConsumers.forEach(this::addWeakConsumer);
	}

	DefaultValueSet(Set<T> initialValues, Notify notify) {
		super(emptySet(), notify);
		set(requireNonNull(initialValues, "initialValues"));
	}

	@Override
	public Iterator<T> iterator() {
		return get().iterator();
	}

	@Override
	public void set(Collection<T> values) {
		synchronized (this.values) {
			set(values == null ? null : new LinkedHashSet<>(values));
		}
	}

	@Override
	public Set<T> get() {
		synchronized (this.values) {
			return unmodifiableSet(new LinkedHashSet<>(values));
		}
	}

	@Override
	public boolean add(T value) {
		synchronized (this.values) {
			Set<T> newValues = new LinkedHashSet<>(values);
			boolean added = newValues.add(value);
			set(newValues);

			return added;
		}
	}

	@Override
	public boolean addAll(T... values) {
		return addAll(asList(values));
	}

	@Override
	public boolean addAll(Collection<T> values) {
		requireNonNull(values);
		synchronized (this.values) {
			Set<T> newValues = new LinkedHashSet<>(this.values);
			boolean added = false;
			for (T val : values) {
				added = newValues.add(val) || added;
			}
			set(newValues);

			return added;
		}
	}

	@Override
	public boolean remove(T value) {
		synchronized (this.values) {
			Set<T> newValues = new LinkedHashSet<>(values);
			boolean removed = newValues.remove(value);
			set(newValues);

			return removed;
		}
	}

	@Override
	public boolean removeAll(T... values) {
		return removeAll(asList(values));
	}

	@Override
	public boolean removeAll(Collection<T> values) {
		requireNonNull(values);
		synchronized (this.values) {
			Set<T> newValues = new LinkedHashSet<>(this.values);
			boolean removed = false;
			for (T val : values) {
				removed = newValues.remove(val) || removed;
			}
			set(newValues);

			return removed;
		}
	}

	@Override
	public boolean contains(T value) {
		synchronized (this.values) {
			return this.values.contains(value);
		}
	}

	@Override
	public boolean containsAll(Collection<T> values) {
		requireNonNull(values);
		synchronized (this.values) {
			return this.values.containsAll(values);
		}
	}

	@Override
	public boolean empty() {
		synchronized (this.values) {
			return values.isEmpty();
		}
	}

	@Override
	public boolean notEmpty() {
		return !empty();
	}

	@Override
	public int size() {
		synchronized (this.values) {
			return values.size();
		}
	}

	@Override
	public Value<T> value() {
		synchronized (this.values) {
			if (value == null) {
				value = new SingleValue();
			}

			return value;
		}
	}

	@Override
	public boolean isNull() {
		return false;
	}

	@Override
	public synchronized ValueSetObserver<T> observer() {
		return (ValueSetObserver<T>) super.observer();
	}

	@Override
	protected ValueObserver<Set<T>> createObserver() {
		return new DefaultValueSetObserver<>(this);
	}

	@Override
	protected void setValue(Set<T> values) {
		synchronized (this.values) {
			this.values.clear();
			this.values.addAll(values);
		}
	}

	@Override
	protected void clearValue() {
		synchronized (this.values) {
			set(emptySet());
		}
	}

	private class SingleValue extends AbstractValue<T> {

		private SingleValue() {
			super(null);
			DefaultValueSet.this.addListener(this::notifyListeners);
		}

		@Override
		public T get() {
			Set<T> set = DefaultValueSet.this.get();

			return set.isEmpty() ? null : set.iterator().next();
		}

		@Override
		protected void setValue(T value) {
			synchronized (DefaultValueSet.this.values) {
				DefaultValueSet.this.set(value == null ? emptySet() : singleton(value));
			}
		}
	}

	static final class DefaultBuilder<T> implements ValueSet.Builder<T> {

		private final List<Validator<Set<T>>> validators = new ArrayList<>();
		private final List<ValueSet<T>> linkedValueSets = new ArrayList<>();
		private final List<ValueSetObserver<T>> linkedValueSetObservers = new ArrayList<>();
		private final List<Runnable> listeners = new ArrayList<>();
		private final List<Runnable> weakListeners = new ArrayList<>();
		private final List<Consumer<Set<T>>> consumers = new ArrayList<>();
		private final List<Consumer<Set<T>>> weakConsumers = new ArrayList<>();

		private Set<T> initialValue = emptySet();
		private Notify notify = Notify.WHEN_CHANGED;

		@Override
		public ValueSet.Builder<T> initialValue(Set<T> initialValue) {
			this.initialValue = requireNonNull(initialValue);
			return this;
		}

		@Override
		public ValueSet.Builder<T> notify(Notify notify) {
			this.notify = requireNonNull(notify);
			return this;
		}

		@Override
		public ValueSet.Builder<T> validator(Validator<Set<T>> validator) {
			this.validators.add(requireNonNull(validator));
			return this;
		}

		@Override
		public ValueSet.Builder<T> link(ValueSet<T> originalValueSet) {
			this.linkedValueSets.add(requireNonNull(originalValueSet));
			return this;
		}

		@Override
		public ValueSet.Builder<T> link(ValueSetObserver<T> originalValueSet) {
			this.linkedValueSetObservers.add(requireNonNull(originalValueSet));
			return this;
		}

		@Override
		public ValueSet.Builder<T> listener(Runnable listener) {
			this.listeners.add(requireNonNull(listener));
			return this;
		}

		@Override
		public ValueSet.Builder<T> consumer(Consumer<Set<T>> consumer) {
			this.consumers.add(requireNonNull(consumer));
			return this;
		}

		@Override
		public ValueSet.Builder<T> weakListener(Runnable weakListener) {
			this.weakListeners.add(requireNonNull(weakListener));
			return this;
		}

		@Override
		public ValueSet.Builder<T> weakConsumer(Consumer<Set<T>> weakConsumer) {
			this.weakConsumers.add(requireNonNull(weakConsumer));
			return this;
		}

		@Override
		public ValueSet<T> build() {
			return new DefaultValueSet<>(this);
		}
	}
}
