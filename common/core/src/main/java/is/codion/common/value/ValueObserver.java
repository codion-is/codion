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

import is.codion.common.observer.Observer;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * A read only value observer
 * @param <T> the type of the value
 */
public interface ValueObserver<T> extends Observer<T> {

	/**
	 * @return the value
	 */
	T get();

	/**
	 * @return the value
	 * @throws NoSuchElementException if no value is present
	 */
	default T getOrThrow() {
		return getOrThrow("No value present");
	}

	/**
	 * @param message the error message to use when throwing
	 * @return the value
	 * @throws NoSuchElementException if no value is present
	 */
	default T getOrThrow(String message) {
		requireNonNull(message);
		T value = get();
		if (value == null) {
			throw new NoSuchElementException(message);
		}

		return value;
	}

	/**
	 * @return an {@link Optional} wrapping this value.
	 */
	default Optional<T> optional() {
		if (nullable()) {
			return Optional.ofNullable(get());
		}

		return Optional.of(getOrThrow());
	}

	/**
	 * @return true if the underlying value is null.
	 */
	default boolean isNull() {
		return get() == null;
	}

	/**
	 * @return true if the underlying value is not null.
	 */
	default boolean isNotNull() {
		return !isNull();
	}

	/**
	 * If false then get() is guaranteed to never return null.
	 * @return true if this value can be null
	 */
	boolean nullable();

	/**
	 * Returns true if the underlying value is equal to the given one. Note that null == null.
	 * @param value the value
	 * @return true if the underlying value is equal to the given one
	 */
	default boolean isEqualTo(T value) {
		return Objects.equals(get(), value);
	}

	/**
	 * Returns true if the underlying value is NOT equal to the given one. Note that null == null.
	 * @param value the value
	 * @return true if the underlying value is NOT equal to the given one
	 */
	default boolean isNotEqualTo(T value) {
		return !isEqualTo(value);
	}
}
