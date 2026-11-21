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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;

/**
 * <p>An abstract {@link ValueList} implementation, the extension point for a list value with operations of its own:
 * the list is held here, so a subclass supplies nothing and adds only its own methods.
 * <p>Every method is final, so a subclass can not put a step of its own in front of the ordering every value
 * follows, see {@link AbstractValue}. The mutators are synchronized, each change made and notified while holding this
 * value's monitor, and the value is an unmodifiable snapshot, replaced on each change, so reading it takes no lock.
 * The list keeps the order the items were added in, duplicates included.
 * <p>Validators, links and initial items are added in the subclass constructor, via {@link #addValidator(Validator)},
 * {@link #link(Value)} and {@link #addAll(Collection)}. A subclass changes the list through the mutators, never
 * via {@link #setValue(Object)}, the storage the mutators write once they have validated, which itself neither
 * validates nor notifies.
 * @param <T> the value type
 * @see ValueList#valueList()
 */
public abstract class AbstractValueList<T> extends AbstractValueCollection<T, List<T>> implements ValueList<T> {

	/**
	 * Creates an empty {@link AbstractValueList}, notifying listeners when it changes.
	 */
	protected AbstractValueList() {
		this(Notify.CHANGED);
	}

	/**
	 * Creates an empty {@link AbstractValueList}.
	 * @param notify specifies when to notify listeners
	 */
	protected AbstractValueList(Notify notify) {
		super(emptyList(), notify);
	}

	AbstractValueList(AbstractValueCollectionBuilder<List<T>, T, ?> builder) {
		super(builder);
	}

	/**
	 * <p>{@link Value#set(Object)}, typed to the list itself, which overload resolution prefers whenever a caller
	 * passes the declared type - {@code valueList.set(aList)} lands here rather than on
	 * {@link ValueCollection#set(Collection)}.
	 * <p>It must therefore take the same snapshot. Storing the caller's collection would leave the value's
	 * contents mutable from the outside and able to change behind its back, with no validation and no
	 * notification, and hand observers a snapshot that is not one.
	 * <p>Declared here rather than on the base class shared with {@link AbstractValueSet}, where it would
	 * erase to the same signature as {@link ValueCollection#set(Collection)}.
	 */
	@Override
	public final synchronized void set(@Nullable List<T> values) {
		set((Collection<T>) values);
	}

	@Override
	public final ObservableValueList<T> observable() {
		return (ObservableValueList<T>) super.observable();
	}

	@Override
	protected final ObservableValueList<T> createObservable() {
		return new DefaultObservableValueList<>(this);
	}

	@Override
	final List<T> create() {
		return new ArrayList<>();
	}

	@Override
	final List<T> unmodifiable(List<T> values) {
		return unmodifiableList(values);
	}

	private static final class DefaultObservableValueList<T>
					extends AbstractObservableValueCollection<T, List<T>>
					implements ObservableValueList<T> {

		private DefaultObservableValueList(ValueList<T> valueList) {
			super(valueList);
		}
	}
}
