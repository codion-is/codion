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
 * Copyright (c) 2012 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.value;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * An observable wrapper for a value.
 * A factory for {@link Value} instances.
 * @param <T> the type of the wrapped value
 * @see #value()
 */
public interface Value<T> extends ValueObserver<T>, Consumer<T> {

	/**
	 * Specifies when a Value instance notifies its listeners.
	 */
	enum Notify {
		/**
		 * Notify listeners when the underlying value is set via {@link Value#set(Object)},
		 * regardless of whether the new value is equal to the previous value.
		 */
		WHEN_SET,
		/**
		 * Notify listeners when the underlying value is changed via {@link Value#set(Object)},
		 * that is, only when the new value differs from the previous value.
		 */
		WHEN_CHANGED
	}

	/**
	 * Sets the value. Note that change listener notifications depend on the {@link Notify} policy associated with this value.
	 * @param value the value
	 * @throws IllegalArgumentException in case the given value is invalid
	 * @see #addValidator(Validator)
	 */
	@Override
	default void accept(T value) {
		set(value);
	}

	/**
	 * Sets the value. Note that change listener notifications depend on the {@link Notify} policy associated with this value.
	 * @param value the value
	 * @return true if the underlying value changed
	 * @throws IllegalArgumentException in case the given value is invalid
	 * @see #addValidator(Validator)
	 */
	boolean set(T value);

	/**
	 * Sets a new value mapped from the current value.
	 * <pre>
	 * {@code
	 * Value<Integer> value = Value.value(0);
	 *
	 * // increment the value by one
	 * value.map(currentValue -> currentValue + 1);
	 * }
	 * </pre>
	 * @param mapper maps from the current value to a new value
	 * @return true if the underlying value changed
	 * @throws NullPointerException in case {@code mapper} is null
	 */
	default boolean map(Function<T, T> mapper) {
		return set(requireNonNull(mapper).apply(get()));
	}

	/**
	 * Sets a new value in case the current value is null.
	 * <pre>
	 * {@code
	 * Value<Integer> value = Value.value(null);
	 *
	 * // replace null with 1
	 * value.mapNull(() -> 1);
	 *
	 * // has no effect since the value is non-null
	 * value.mapNull(() -> 2);
	 * }
	 * </pre>
	 * @param supplier supplies the value to use as replacement for null
	 * @return true if the underlying value changed
	 * @throws NullPointerException in case {@code supplier} is null
	 */
	default boolean mapNull(Supplier<T> supplier) {
		requireNonNull(supplier);
		if (isNull()) {
			return set(supplier.get());
		}

		return false;
	}

	/**
	 * Returns a {@link ValueObserver} notified each time this value changes.
	 * @return a {@link ValueObserver} for this value
	 */
	ValueObserver<T> observer();

	/**
	 * Creates a bidirectional link between this and the given original value,
	 * so that changes in one are reflected in the other.
	 * Note that after a call to this method this value is the same as {@code originalValue}.
	 * @param originalValue the original value to link this value to
	 * @throws IllegalStateException in case the values are already linked
	 * @throws IllegalArgumentException in case the original value is not valid according to this values validators
	 */
	void link(Value<T> originalValue);

	/**
	 * Unlinks this value from the given original value
	 * @param originalValue the original value to unlink from this one
	 * @throws IllegalStateException in case the values are not linked
	 */
	void unlink(Value<T> originalValue);

	/**
	 * Creates a unidirectional link between this value and the given original value observer,
	 * so that changes in the original value are reflected in this one.
	 * Note that after a call to this method the value of this value is the same as the original value.
	 * @param originalValue the original value to link this value to
	 * @throws IllegalArgumentException in case the original value is not valid according to this values validators
	 */
	void link(ValueObserver<T> originalValue);

	/**
	 * Unlinks this value from the given original value observer
	 * @param originalValue the original value to unlink
	 */
	void unlink(ValueObserver<T> originalValue);

	/**
	 * Adds a validator to this {@link Value}.
	 * Adding the same validator again has no effect.
	 * @param validator the validator
	 * @return true if this value did not already contain the specified validator
	 * @throws IllegalArgumentException in case the current value is invalid according to the validator
	 */
	boolean addValidator(Validator<T> validator);

	/**
	 * Removes the given validator from this value
	 * @param validator the validator
	 * @return true if this value contained the specified validator
	 */
	boolean removeValidator(Validator<T> validator);

	/**
	 * Validate the given value using all validators
	 * @param value the value to validate
	 * @throws IllegalArgumentException in case the given value is invalid according to a validator
	 */
	void validate(T value);

	/**
	 * @param nullValue the actual value to use when the value is set to null
	 * @return a builder for a non-null Value
	 * @param <T> the value type
	 */
	static <T> Builder<T> nonNull(T nullValue) {
		return new DefaultValue.DefaultBuilder<>(nullValue);
	}

	/**
	 * @param initialValue the initial value
	 * @return a builder for a nullable Value
	 * @param <T> the value type
	 */
	static <T> Builder<T> nullable(T initialValue) {
		return (Builder<T>) new DefaultValue.DefaultBuilder<>()
						.initialValue(initialValue);
	}

	/**
	 * Builds a {@link Value} instance.
	 * @param <T> the value type
	 */
	interface Builder<T> {

		/**
		 * @param initialValue the initial value
		 * @return this builder instance
		 */
		Builder<T> initialValue(T initialValue);

		/**
		 * @param notify the notify policy for this value, default {@link Notify#WHEN_CHANGED}
		 * @return this builder instance
		 */
		Builder<T> notify(Notify notify);

		/**
		 * Adds a validator to the resulting value
		 * @param validator the validator to add
		 * @return this builder instance
		 */
		Builder<T> validator(Validator<T> validator);

		/**
		 * Links the given value to the resulting value
		 * @param originalValue the original value to link
		 * @return this builder instance
		 * @see Value#link(Value)
		 */
		Builder<T> link(Value<T> originalValue);

		/**
		 * Links the given value observer to the resulting value
		 * @param originalValue the value to link
		 * @return this builder instance
		 * @see Value#link(ValueObserver)
		 */
		Builder<T> link(ValueObserver<T> originalValue);

		/**
		 * @return a new {@link Value} instance based on this builder
		 */
		Value<T> build();
	}

	/**
	 * A {@link Validator} for {@link Value}s.
	 * @param <T> the value type
	 */
	interface Validator<T> {

		/**
		 * Validates the given value.
		 * @param value the value to validate
		 * @throws IllegalArgumentException in case of an invalid value
		 */
		void validate(T value);
	}

	/**
	 * Creates a new {@link Value} instance, wrapping a null initial value, using {@link Notify#WHEN_CHANGED}.
	 * @param <T> the value type
	 * @return a Value for the given type
	 */
	static <T> Value<T> value() {
		return nullable((T) null).build();
	}
}
