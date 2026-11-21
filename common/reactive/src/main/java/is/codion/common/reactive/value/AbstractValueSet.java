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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.reactive.value;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;

/**
 * <p>An abstract {@link ValueSet} implementation, the extension point for a set value with operations of its own:
 * the set is held here, so a subclass supplies nothing and adds only its own methods.
 * <p>Every method is final, so a subclass can not put a step of its own in front of the ordering every value
 * follows, see {@link AbstractValue}. The mutators are synchronized, each change made and notified while holding this
 * value's monitor, and the value is an unmodifiable snapshot, replaced on each change, so reading it takes no lock.
 * The set keeps the order the items were added in.
 * <p>Validators, links and initial items are added in the subclass constructor, via {@link #addValidator(Validator)},
 * {@link #link(Value)} and {@link #addAll(Collection)}. A subclass changes the set through the mutators, never
 * via {@link #setValue(Object)}, the storage the mutators write once they have validated, which itself neither
 * validates nor notifies.
 * @param <T> the value type
 * @see ValueSet#valueSet()
 */
public abstract class AbstractValueSet<T> extends AbstractValueCollection<T, Set<T>> implements ValueSet<T> {

	/**
	 * Creates an empty {@link AbstractValueSet}, notifying listeners when it changes.
	 */
	protected AbstractValueSet() {
		this(Notify.CHANGED);
	}

	/**
	 * Creates an empty {@link AbstractValueSet}.
	 * @param notify specifies when to notify listeners
	 */
	protected AbstractValueSet(Notify notify) {
		super(emptySet(), notify);
	}

	AbstractValueSet(AbstractValueCollectionBuilder<Set<T>, T, ?> builder) {
		super(builder);
	}

	/**
	 * <p>{@link Value#set(Object)}, typed to the set itself, which overload resolution prefers whenever a caller
	 * passes the declared type - {@code valueSet.set(aSet)} lands here rather than on
	 * {@link ValueCollection#set(Collection)}.
	 * <p>It must therefore take the same snapshot. Storing the caller's collection would leave the value's
	 * contents mutable from the outside and able to change behind its back, with no validation and no
	 * notification, and hand observers a snapshot that is not one.
	 * <p>Declared here rather than on the base class shared with {@link AbstractValueList}, where it would
	 * erase to the same signature as {@link ValueCollection#set(Collection)}.
	 */
	@Override
	public final synchronized void set(@Nullable Set<T> values) {
		set((Collection<T>) values);
	}

	@Override
	public final ObservableValueSet<T> observable() {
		return (ObservableValueSet<T>) super.observable();
	}

	@Override
	protected final ObservableValueSet<T> createObservable() {
		return new DefaultObservableValueSet<>(this);
	}

	@Override
	final Set<T> create() {
		return new LinkedHashSet<>();
	}

	@Override
	final Set<T> unmodifiable(Set<T> values) {
		return unmodifiableSet(values);
	}

	private static final class DefaultObservableValueSet<T>
					extends AbstractObservableValueCollection<T, Set<T>>
					implements ObservableValueSet<T> {

		private DefaultObservableValueSet(ValueSet<T> valueSet) {
			super(valueSet);
		}
	}
}
