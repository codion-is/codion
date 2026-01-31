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

import org.jspecify.annotations.Nullable;

import java.util.function.UnaryOperator;

/**
 * <p>An abstract {@link Value} implementation handling everything except the value itself.
 * <p>The constructor parameter {@code notify} specifies whether this {@link AbstractValue} instance should call
 * {@link #notifyObserver()} each time the value is set ({@link Notify#SET}) or only when it changes
 * ({@link Notify#CHANGED}), which is determined using {@link Object#equals(Object)}.
 * <p>Implementations that want to handle notifications manually should use the
 * {@link AbstractValue#AbstractValue()} or {@link AbstractValue#AbstractValue(Object)} constructors.
 * @param <T> the value type
 */
public abstract class AbstractValue<T> extends BaseValue<T> {

	/**
	 * <p>Creates a nullable {@link AbstractValue} instance, which does not notify listeners.
	 */
	protected AbstractValue() {}

	/**
	 * Creates a nullable {@link AbstractValue} instance.
	 * @param notify specifies when to notify listeners
	 */
	protected AbstractValue(Notify notify) {
		super(notify);
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
		super(nullValue);
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
		super(nullValue, notify);
	}

	/**
	 * @param builder the builder
	 */
	protected AbstractValue(AbstractBuilder<T, ?> builder) {
		super(builder);
	}

	@Override
	public final @Nullable T get() {
		return super.get();
	}

	@Override
	public final void set(@Nullable T value) {
		super.set(value);
	}

	@Override
	public final void clear() {
		set(null);
	}

	@Override
	public final void update(UnaryOperator<@Nullable T> updateFunction) {
		super.update(updateFunction);
	}

	/**
	 * An abstract base class for a value builder
	 * @param <T> the value type
	 * @param <B> the builder type
	 */
	public abstract static class AbstractBuilder<T, B extends Value.Builder<T, B>> extends BaseBuilder<T, B> {

		/**
		 * Instantiates a new builder
		 */
		protected AbstractBuilder() {}

		/**
		 * Instantiates a new builder
		 * @param nullValue the null value, also used as the initial value
		 * @throws NullPointerException in case {@code nullValue} is null
		 */
		protected AbstractBuilder(T nullValue) {
			super(nullValue);
		}
	}
}
