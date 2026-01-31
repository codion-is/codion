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
 * Copyright (c) 2024 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.reactive.observer;

import org.jspecify.annotations.Nullable;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * A wrapper for a value, providing a change observer.
 * <p><b>Thread Safety:</b> Listener management is thread-safe,
 * value access thread-safety is implementation dependent.
 * {@snippet :
 *   class Person {
 *       private final Event<String> nameChanged = Event.event();
 *
 *       private String name;
 *
 *       public String getName() {
 *           return name;
 *       }
 *
 *       public void setName(String name) {
 *           this.name = name;
 *           nameChanged.accept(name);
 *      }
 *   }
 *
 *   Person person = new Person();
 *
 *   Observable<String> observableName = new Observable<>() {
 *       @Override
 *       public String get() {
 *           return person.getName();
 *       }
 *
 *       @Override
 *       public Observer<String> observer() {
 *           return person.nameChanged.observer();
 *       }
 *  };
 *
 *  observableName.addConsumer(newName ->
 *          System.out.println("Name changed to " + newName));
 *}
 * @param <T> the value type
 */
public interface Observable<T> extends Observer<T> {

	/**
	 * @return the value
	 */
	@Nullable T get();

	/**
	 * @return the value
	 * @throws NoSuchElementException if no value is present
	 */
	default T getOrThrow() {
		T value = get();
		if (value == null) {
			throw new NoSuchElementException("No value present");
		}

		return value;
	}

	/**
	 * @param message the error message to use when throwing
	 * @return the value
	 * @throws NoSuchElementException if no value is present
	 */
	default T getOrThrow(String message) {
		T value = get();
		if (value == null) {
			throw new NoSuchElementException(requireNonNull(message));
		}

		return value;
	}

	/**
	 * @return an {@link Optional} wrapping this value.
	 */
	default Optional<T> optional() {
		if (isNullable()) {
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
	 * If false then get() is guaranteed to never return null.
	 * @return true if this observable can be null
	 */
	default boolean isNullable() {
		return true;
	}

	/**
	 * Returns true if the underlying value is equal to the given one. Note that null == null.
	 * @param value the value
	 * @return true if the underlying value is equal to the given one
	 */
	default boolean is(@Nullable T value) {
		return Objects.deepEquals(get(), value);
	}

	/**
	 * Returns true if the underlying value is NOT equal to the given one. Note that null == null.
	 * @param value the value
	 * @return true if the underlying value is NOT equal to the given one
	 */
	default boolean isNot(@Nullable T value) {
		return !is(value);
	}
}
