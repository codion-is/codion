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

import is.codion.common.event.Event;
import is.codion.common.observer.Observable;
import is.codion.common.observer.Observer;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import static is.codion.common.value.Value.Notify.WHEN_CHANGED;
import static is.codion.common.value.Value.Notify.WHEN_SET;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;

/**
 * An abstract {@link Value} implementation handling everything except the value itself.
 * <p>
 * The constructor parameter {@code notify} specifies whether this {@link Value} instance should call
 * {@link #notifyListeners()} when the value is set or changed via {@link AbstractValue#set(Object)}.
 * Implementations that want to handle notifications manually should use the
 * {@link AbstractValue#AbstractValue()} or {@link AbstractValue#AbstractValue(Object)} constructors.
 * @param <T> the value type
 */
public abstract class AbstractValue<T> implements Value<T> {

	private final @Nullable T nullValue;
	private final @Nullable Notify notify;

	private @Nullable Event<T> observer;
	private @Nullable Set<Validator<? super T>> validators;
	private @Nullable Map<Value<T>, ValueLink<T>> linkedValues;
	private @Nullable Map<Observable<T>, ObservableLink> linkedObservables;
	private @Nullable Observable<T> observable;

	/**
	 * Creates a {@link AbstractValue} instance, which does not notify listeners.
	 */
	protected AbstractValue() {
		this(null);
	}

	/**
	 * Creates a {@link AbstractValue} instance, which does not notify listeners.
	 * @param nullValue the value to use instead of null
	 */
	protected AbstractValue(@Nullable T nullValue) {
		this.nullValue = nullValue;
		this.notify = null;
	}

	/**
	 * Creates an {@link AbstractValue} instance.
	 * @param nullValue the value to use instead of null
	 * @param notify specifies when to notify listeners
	 */
	protected AbstractValue(@Nullable T nullValue, Notify notify) {
		this.nullValue = nullValue;
		this.notify = requireNonNull(notify);
	}

	@Override
	public final @Nullable T get() {
		return getValue();
	}

	@Override
	public final boolean set(@Nullable T value) {
		T newValue = value == null ? nullValue : value;
		for (Validator<? super T> validator : validators()) {
			validator.validate(newValue);
		}
		T previousValue = get();
		setValue(newValue);

		return notifyListeners(!Objects.equals(previousValue, newValue));
	}

	@Override
	public final void clear() {
		set(null);
	}

	@Override
	public final boolean map(Function<T, T> mapper) {
		return Value.super.map(mapper);
	}

	@Override
	public synchronized Observable<T> observable() {
		if (observable == null) {
			observable = createObservable();
		}

		return observable;
	}

	@Override
	public final synchronized Observer<T> observer() {
		if (observer == null) {
			observer = Event.event();
		}

		return observer;
	}

	@Override
	public final boolean nullable() {
		return nullValue == null;
	}

	@Override
	public final void accept(@Nullable T data) {
		set(data);
	}

	@Override
	public final void link(Value<T> originalValue) {
		requireNonNull(originalValue);
		if (linkedValues == null) {
			linkedValues = new LinkedHashMap<>(1);
		}
		if (linkedValues.containsKey(originalValue)) {
			throw new IllegalStateException("Values are already linked");
		}
		linkedValues.put(originalValue, new ValueLink<>(this, originalValue));
	}

	@Override
	public final void unlink(Value<T> originalValue) {
		requireNonNull(originalValue);
		if (linkedValues == null || !linkedValues.containsKey(originalValue)) {
			throw new IllegalStateException("Values are not linked");
		}
		linkedValues.remove(originalValue).unlink();
		if (linkedValues.isEmpty()) {
			linkedValues = null;
		}
	}

	@Override
	public final void link(Observable<T> observable) {
		requireNonNull(observable);
		if (linkedObservables == null) {
			linkedObservables = new LinkedHashMap<>(1);
		}
		set(observable.get());
		linkedObservables.put(observable, new ObservableLink(observable));
	}

	@Override
	public final void unlink(Observable<T> observable) {
		requireNonNull(observable);
		if (linkedObservables == null || !linkedObservables.containsKey(observable)) {
			throw new IllegalStateException("Values are not linked");
		}
		observable.removeConsumer(linkedObservables.remove(observable));
		if (linkedObservables.isEmpty()) {
			linkedObservables = null;
		}
	}

	@Override
	public final boolean addValidator(Validator<? super T> validator) {
		requireNonNull(validator).validate(get());
		if (validators == null) {
			validators = new LinkedHashSet<>(1);
		}

		return validators.add(validator);
	}

	@Override
	public final boolean removeValidator(Validator<? super T> validator) {
		requireNonNull(validator);
		if (validators != null) {
			return validators.remove(validator);
		}

		return false;
	}

	@Override
	public final void validate(@Nullable T value) {
		validators().forEach(validator -> validator.validate(value));
	}

	/**
	 * Returns the actual internal value.
	 * @return the value
	 */
	protected abstract @Nullable T getValue();

	/**
	 * Sets the actual internal value.
	 * @param value the value
	 */
	protected abstract void setValue(@Nullable T value);

	/**
	 * Notifies listeners that the underlying value has changed or at least that it may have changed
	 */
	protected final void notifyListeners() {
		if (observer != null) {
			observer.accept(get());
		}
	}

	/**
	 * @return a new {@link Observable} instance representing this value
	 */
	protected Observable<T> createObservable() {
		return new ObservableValue<>(this);
	}

	final Set<Value<T>> linkedValues() {
		return linkedValues == null ? emptySet() : linkedValues.keySet();
	}

	final Collection<Validator<? super T>> validators() {
		return validators == null ? emptyList() : validators;
	}

	private boolean notifyListeners(boolean changed) {
		if (notify == WHEN_SET || (notify == WHEN_CHANGED && changed)) {
			notifyListeners();
		}

		return changed;
	}

	private final class ObservableLink implements Consumer<T> {

		private ObservableLink(Observable<T> originalValue) {
			originalValue.addConsumer(this);
		}

		@Override
		public void accept(T value) {
			set(value);
		}
	}
}
