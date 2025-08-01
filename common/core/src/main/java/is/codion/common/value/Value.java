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
 * Copyright (c) 2012 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.value;

import is.codion.common.observer.Observable;

import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import static java.util.Objects.requireNonNull;

/**
 * <p>An observable wrapper for a value.</p>
 * <p>Nullable integer based Value:</p>
 * {@snippet :
 * Value<Integer> value = Value.nullable();
 * value.set(42);
 * value.addConsumer(this::onValueChange);
 * value.isNullable(); // true
 *}
 * <p>Non-null boolean based Value, using 'false' as a null substitute:</p>
 * {@snippet :
 * Value<Boolean> value = Value.nonNull(false);
 * value.set(true);
 * value.set(null);
 * value.get(); // false
 * value.isNullable(); // false
 *}
 * <p>Non-null String based Value, using "none" as a null substitute:</p>
 * {@snippet :
 * Value<String> value = Value.builder()
 *         .nonNull("none")
 *         .value("hello")                  // the initial value
 *         .notify(Notify.SET)         // notifies listeners when set
 *         .validator(this::validateString) // using a validator
 *         .listener(this::onStringSet)     // and a listener
 *         .build();
 * value.isNullable();// false
 * value.set("hey");
 * value.set(null); // reverts to the null substitute: "none"
 *}
 * <p>A factory for {@link Value} instances.</p>
 * <p><b>Thread Safety:</b> Listener management (add/remove) is thread-safe and supports concurrent access.
 * However, value modifications via {@link #set(Object)} are NOT thread-safe and should be
 * performed from a single thread (such as an application UI thread). Sublasses may provide thread-safety.</p>
 * @param <T> the type of the wrapped value
 * @see #nullable()
 * @see #nullable(Object)
 * @see #nonNull(Object)
 * @see #builder()
 */
public interface Value<T> extends Observable<T> {

	/**
	 * Specifies when a Value instance notifies its listeners.
	 */
	enum Notify {
		/**
		 * Notify listeners when the underlying value is set via {@link Value#set(Object)},
		 * regardless of whether the new value is equal to the previous value.
		 */
		SET,
		/**
		 * Notify listeners when the underlying value is changed via {@link Value#set(Object)},
		 * that is, only when the new value differs from the previous value, determined by {@link Object#equals(Object)}.
		 */
		CHANGED
	}

	/**
	 * Sets the value. Note that change listener notifications depend on the {@link Notify} policy associated with this value.
	 * @param value the value
	 * @throws IllegalArgumentException in case the given value is invalid
	 * @see #addValidator(Validator)
	 */
	void set(@Nullable T value);

	/**
	 * Clears this value, by setting it to null or the null value in case this is a non-null value.
	 */
	void clear();

	/**
	 * Sets a new value mapped from the current value.
	 * {@snippet :
	 * Value<Integer> value = Value.value(0);
	 *
	 * // increment the value by one
	 * value.map(currentValue -> currentValue + 1);
	 *}
	 * @param mapper maps from the current value to a new value
	 * @throws NullPointerException in case {@code mapper} is null
	 */
	default void map(UnaryOperator<@Nullable T> mapper) {
		set(requireNonNull(mapper).apply(get()));
	}

	/**
	 * @return a read-only {@link Observable} instance for this value
	 */
	Observable<T> observable();

	/**
	 * Creates a bidirectional link between this and the given original value,
	 * so that changes in one are reflected in the other.
	 * Note that after a call to this method this value is the same as {@code originalValue}.
	 * @param originalValue the original value to link this value to
	 * @throws IllegalStateException in case the values are already linked or if a cycle is detected
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
	 * Creates a unidirectional link between this value and the given observable,
	 * so that changes in the observable are reflected in this one.
	 * Note that after a call to this method the value of this value is the same as the observable.
	 * @param observable the observable to link this value to
	 * @throws IllegalArgumentException in case the observable is not valid according to this values validators
	 */
	void link(Observable<T> observable);

	/**
	 * Unlinks this value from the given observable
	 * @param observable the observable to unlink
	 */
	void unlink(Observable<T> observable);

	/**
	 * Adds a validator to this {@link Value}.
	 * Adding the same validator again has no effect.
	 * @param validator the validator
	 * @return true if this value did not already contain the specified validator
	 * @throws IllegalArgumentException in case the current value is invalid according to the validator
	 */
	boolean addValidator(Validator<? super T> validator);

	/**
	 * Removes the given validator from this value
	 * @param validator the validator
	 * @return true if this value contained the specified validator
	 */
	boolean removeValidator(Validator<? super T> validator);

	/**
	 * Validate the given value using all validators
	 * @param value the value to validate
	 * @throws IllegalArgumentException in case the given value is invalid according to a validator
	 */
	void validate(T value);

	/**
	 * Creates a new nullable {@link Value} instance, wrapping a null initial value, using {@link Notify#CHANGED}.
	 * @param <T> the value type
	 * @return a nullable Value
	 */
	static <T> Value<T> nullable() {
		return nullable(null);
	}

	/**
	 * Creates a new nullable {@link Value} instance, wrapping the given initial value, using {@link Notify#CHANGED}.
	 * @param <T> the value type
	 * @param value the initial value
	 * @return a nullable Value
	 */
	static <T> Value<T> nullable(@Nullable T value) {
		return builder()
						.nullable(value)
						.build();
	}

	/**
	 * Creates a new non-null {@link Value} instance, using the given value as a null-substitute, using {@link Notify#CHANGED}.
	 * @param <T> the value type
	 * @param nullValue the null value substitute
	 * @return a non-null Value
	 */
	static <T> Value<T> nonNull(T nullValue) {
		return builder()
						.nonNull(nullValue)
						.build();
	}

	/**
	 * @return a new {@link Value.BuilderFactory} instance
	 */
	static BuilderFactory builder() {
		return DefaultValue.BUILDER_FACTORY;
	}

	/**
	 * Provides {@link Value.Builder} instances for nullable or non-nullable values.
	 */
	interface BuilderFactory {

		/**
		 * @param nullValue the actual value to use when the value is set to null, also serves as the initial value
		 * @param <T> the value type
		 * @return a builder for a non-null {@link Value}
		 */
		<T> Builder<T, ?> nonNull(T nullValue);

		/**
		 * @param <T> the value type
		 * @return a builder for a nullable {@link Value}
		 */
		<T> Builder<T, ?> nullable();

		/**
		 * @param value the initial value
		 * @param <T> the value type
		 * @return a builder for a nullable {@link Value}
		 */
		<T> Builder<T, ?> nullable(@Nullable T value);
	}

	/**
	 * Builds a {@link Value} instance.
	 * @param <T> the value type
	 * @param <B> the builder type
	 */
	interface Builder<T, B extends Builder<T, B>> {

		/**
		 * @param value the initial value
		 * @return this builder instance
		 */
		B value(@Nullable T value);

		/**
		 * @param notify the notify policy for this value, default {@link Notify#CHANGED}
		 * @return this builder instance
		 */
		B notify(Notify notify);

		/**
		 * Adds a validator to the resulting value
		 * @param validator the validator to add
		 * @return this builder instance
		 */
		B validator(Validator<? super T> validator);

		/**
		 * Links the given value to the resulting value
		 * @param originalValue the original value to link
		 * @return this builder instance
		 * @see Value#link(Value)
		 */
		B link(Value<T> originalValue);

		/**
		 * Links the given observable to the resulting value
		 * @param observable the value to link
		 * @return this builder instance
		 * @see Value#link(Observable)
		 */
		B link(Observable<T> observable);

		/**
		 * @param listener a listener to add
		 * @return this builder instance
		 */
		B listener(Runnable listener);

		/**
		 * @param consumer a consumer to add
		 * @return this builder instance
		 */
		B consumer(Consumer<? super T> consumer);

		/**
		 * @param weakListener a weak listener to add
		 * @return this builder instance
		 */
		B weakListener(Runnable weakListener);

		/**
		 * @param weakConsumer a weak consumer to add
		 * @return this builder instance
		 */
		B weakConsumer(Consumer<? super T> weakConsumer);

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
		void validate(@Nullable T value);
	}
}
