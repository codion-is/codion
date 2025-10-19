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
import java.util.function.UnaryOperator;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;

/**
 * <p>An abstract {@link Value} implementation handling everything except the value itself.
 * <p>The constructor parameter {@code notify} specifies whether this {@link AbstractValue} instance should call
 * {@link #notifyListeners()} each time the value is set ({@link Notify#SET}) or only when it changes
 * ({@link Notify#CHANGED}), which is determined using {@link Object#equals(Object)}.
 * <p>Implementations that want to handle notifications manually should use the
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
	 * <p>Creates a nullable {@link AbstractValue} instance, which does not notify listeners.
	 */
	protected AbstractValue() {
		this.nullValue = null;
		this.notify = null;
	}

	/**
	 * Creates a nullable {@link AbstractValue} instance.
	 * @param notify specifies when to notify listeners
	 */
	protected AbstractValue(Notify notify) {
		this.nullValue = null;
		this.notify = requireNonNull(notify);
	}

	/**
	 * <p>Creates an {@link AbstractValue} instance, which does not notify listeners.
	 * <p>If {@code nullValue} is non-null, this {@link AbstractValue} instance
	 * will be non-nullable, meaning {@link #isNullable()} returns false, {@link #get()}
	 * is guaranteed to never return null and when {@link #set(Object)} receives null
	 * it is automatically translated to {@code nullValue}.
	 * @param nullValue the value to use instead of null
	 */
	protected AbstractValue(@Nullable T nullValue) {
		this.nullValue = nullValue;
		this.notify = null;
	}

	/**
	 * <p>Creates an {@link AbstractValue} instance.
	 * <p>If {@code nullValue} is non-null, this {@link AbstractValue} instance
	 * will be non-nullable, meaning {@link #isNullable()} returns false, {@link #get()}
	 * is guaranteed to never return null and when {@link #set(Object)} receives null
	 * it is automatically translated to {@code nullValue}.
	 * @param nullValue the value to use instead of null
	 * @param notify specifies when to notify listeners
	 */
	protected AbstractValue(@Nullable T nullValue, Notify notify) {
		this.nullValue = nullValue;
		this.notify = requireNonNull(notify);
	}

	@Override
	public final @Nullable T get() {
		T value = getValue();

		return value == null ? nullValue : value;
	}

	@Override
	public final void set(@Nullable T value) {
		T newValue = value == null ? nullValue : value;
		for (Validator<? super T> validator : validators()) {
			validator.validate(newValue);
		}
		setAndNotify(newValue);
	}

	@Override
	public final void clear() {
		set(null);
	}

	@Override
	public final void map(UnaryOperator<@Nullable T> mapper) {
		Value.super.map(mapper);
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
	public final boolean isNullable() {
		return nullValue == null;
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

	private void setAndNotify(@Nullable T newValue) {
		if (notify == Notify.CHANGED) {
			T previousValue = getValue();
			setValue(newValue);
			if (!Objects.equals(previousValue, newValue)) {
				notifyListeners();
			}
		}
		else if (notify == Notify.SET) {
			setValue(newValue);
			notifyListeners();
		}
		else {
			setValue(newValue);
		}
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

	static class ObservableValue<T, V extends Value<T>> implements Observable<T> {

		private final V value;

		ObservableValue(V value) {
			this.value = requireNonNull(value);
		}

		@Override
		public final @Nullable T get() {
			return value.get();
		}

		@Override
		public final boolean isNullable() {
			return value.isNullable();
		}

		@Override
		public final Observer<T> observer() {
			return value.observer();
		}

		protected final V value() {
			return value;
		}
	}
}
