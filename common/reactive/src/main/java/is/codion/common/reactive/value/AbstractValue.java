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
package is.codion.common.reactive.value;

import is.codion.common.reactive.observer.AbstractObserver;
import is.codion.common.reactive.observer.Observable;
import is.codion.common.reactive.observer.Observer;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import static is.codion.common.reactive.value.Value.Notify.CHANGED;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Objects.deepEquals;
import static java.util.Objects.requireNonNull;

/**
 * <p>An abstract {@link Value} implementation handling everything except the value itself.
 * <p>The constructor parameter {@code notify} specifies whether this {@link AbstractValue} instance should call
 * {@link #notifyObserver()} each time the value is set ({@link Notify#SET}) or only when it changes
 * ({@link Notify#CHANGED}), which is determined using {@link Object#equals(Object)}.
 * <p>Implementations that want to handle notifications manually should use the
 * {@link AbstractValue#AbstractValue()} or {@link AbstractValue#AbstractValue(Object)} constructors.
 * @param <T> the value type
 */
public abstract class AbstractValue<T> implements Value<T> {

	private final @Nullable T nullValue;
	private final @Nullable Notify notify;

	private @Nullable Locked locked;
	private @Nullable ValueObserver<T> observer;
	private @Nullable Observer<ValueChange<T>> changeObserver;
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

	/**
	 * @param builder the builder
	 */
	protected AbstractValue(AbstractBuilder<T, ?> builder) {
		requireNonNull(builder);
		nullValue = builder.nullValue;
		notify = builder.notify;
		builder.validators.forEach(this::addValidator);
		set(builder.prepareInitialValue());
		builder.linkedValues.forEach(this::link);
		builder.linkedObservables.forEach(this::link);
		builder.listeners.addListeners(this);
		if (builder.locked) {
			locked().set(true);
		}
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
	public final void update(UnaryOperator<@Nullable T> updateFunction) {
		Value.super.update(updateFunction);
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
			observer = new ValueObserver<>();
		}

		return observer;
	}

	@Override
	public final synchronized Observer<ValueChange<T>> changed() {
		if (changeObserver == null) {
			changeObserver = new ValueChangeObserver<>(this);
		}

		return changeObserver;
	}

	@Override
	public final synchronized Locked locked() {
		if (locked == null) {
			locked = new DefaultLocked();
		}

		return locked;
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
	 * Notifies the underlying observer that the underlying value has changed or at least that it may have changed
	 */
	protected final void notifyObserver() {
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
		T previousValue = get();
		boolean changing = !deepEquals(previousValue, newValue);
		if (changing && isLocked()) {
			throw new IllegalStateException("Value is locked and can not be changed");
		}
		setValue(newValue);
		if (notify == Notify.CHANGED && changing) {
			notifyObserver();
		}
		if (notify == Notify.SET) {
			notifyObserver();
		}
	}

	private boolean isLocked() {
		return locked != null && locked.is();
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

	/**
	 * An abstract base class for a value builder
	 * @param <T> the value type
	 * @param <B> the builder type
	 */
	public abstract static class AbstractBuilder<T, B extends Builder<T, B>> implements Builder<T, B> {

		private final @Nullable T nullValue;
		private final List<Validator<? super T>> validators = new ArrayList<>();
		private final List<Value<T>> linkedValues = new ArrayList<>();
		private final List<Observable<T>> linkedObservables = new ArrayList<>();
		private final ValueListeners<T> listeners = new ValueListeners<>();
		private @Nullable T value;
		private Notify notify = CHANGED;
		private boolean locked = false;

		/**
		 * Instantiates a new builder
		 */
		protected AbstractBuilder() {
			this.nullValue = null;
		}

		/**
		 * Instantiates a new builder
		 * @param nullValue the null value, also used as the initial value
		 * @throws NullPointerException in case {@code nullValue} is null
		 */
		protected AbstractBuilder(T nullValue) {
			this.nullValue = requireNonNull(nullValue);
			this.value = nullValue;
		}

		@Override
		public final B value(@Nullable T value) {
			this.value = value;
			return self();
		}

		@Override
		public final B notify(Notify notify) {
			this.notify = requireNonNull(notify);
			return self();
		}

		@Override
		public final B validator(Validator<? super T> validator) {
			this.validators.add(requireNonNull(validator));
			return self();
		}

		@Override
		public final B locked(boolean locked) {
			this.locked = locked;
			return self();
		}

		@Override
		public final B link(Value<T> originalValue) {
			this.linkedValues.add(requireNonNull(originalValue));
			return self();
		}

		@Override
		public final B link(Observable<T> observable) {
			this.linkedObservables.add(requireNonNull(observable));
			return self();
		}

		@Override
		public final B listener(Runnable listener) {
			this.listeners.listener(listener);
			return self();
		}

		@Override
		public final B consumer(Consumer<? super T> consumer) {
			this.listeners.consumer(consumer);
			return self();
		}

		@Override
		public final B weakListener(Runnable weakListener) {
			this.listeners.weakListener(weakListener);
			return self();
		}

		@Override
		public final B weakConsumer(Consumer<? super T> weakConsumer) {
			this.listeners.weakConsumer(weakConsumer);
			return self();
		}

		@Override
		public final B changeListener(Runnable listener) {
			this.listeners.changeListener(listener);
			return self();
		}

		@Override
		public final B changeConsumer(Consumer<ValueChange<? super T>> consumer) {
			this.listeners.changeConsumer(consumer);
			return self();
		}

		@Override
		public final B weakChangeListener(Runnable weakListener) {
			this.listeners.weakChangeListener(weakListener);
			return self();
		}

		@Override
		public final B weakChangeConsumer(Consumer<ValueChange<? super T>> weakConsumer) {
			this.listeners.weakChangeConsumer(weakConsumer);
			return self();
		}

		@Override
		public final B when(T value, Runnable listener) {
			listeners.when(value, listener);
			return self();
		}

		@Override
		public final B when(T value, Consumer<? super T> consumer) {
			listeners.when(value, consumer);
			return self();
		}

		@Override
		public final B when(Predicate<T> predicate, Runnable listener) {
			listeners.when(predicate, listener);
			return self();
		}

		@Override
		public final B when(Predicate<T> predicate, Consumer<? super T> consumer) {
			listeners.when(predicate, consumer);
			return self();
		}

		/**
		 * @return the initial value
		 */
		protected @Nullable T prepareInitialValue() {
			return value == null ? nullValue : value;
		}

		private B self() {
			return (B) this;
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

	private static final class ValueObserver<T> extends AbstractObserver<T> {

		private void accept(@Nullable T data) {
			notifyListeners(data);
		}
	}

	private static final class DefaultLocked implements Locked {

		private boolean locked = false;

		@Override
		public boolean is() {
			return locked;
		}

		@Override
		public void set(boolean locked) {
			this.locked = locked;
		}
	}
}
