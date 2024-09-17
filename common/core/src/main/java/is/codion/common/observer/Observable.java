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
 * Copyright (c) 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.observer;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * A wrapper for a value, providing a change observer.
 * <pre>
 * {@code
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
 * }
 * </pre>
 * @param <T> the value type
 */
public interface Observable<T> extends Observer<T> {

	/**
	 * @return the value
	 */
	T get();

	/**
	 * @return an Optional based on the current value
	 */
	default Optional<T> optional() {
		return Optional.ofNullable(get());
	}

	/**
	 * @return an {@link Observer} notified each time the value may have changed
	 */
	Observer<T> observer();

	@Override
	default boolean addListener(Runnable listener) {
		return observer().addListener(listener);
	}

	@Override
	default boolean removeListener(Runnable listener) {
		return observer().removeListener(listener);
	}

	@Override
	default boolean addConsumer(Consumer<? super T> consumer) {
		return observer().addConsumer(consumer);
	}

	@Override
	default boolean removeConsumer(Consumer<? super T> consumer) {
		return observer().removeConsumer(consumer);
	}

	@Override
	default boolean addWeakListener(Runnable listener) {
		return observer().addWeakListener(listener);
	}

	@Override
	default boolean removeWeakListener(Runnable listener) {
		return observer().removeWeakListener(listener);
	}

	@Override
	default boolean addWeakConsumer(Consumer<? super T> consumer) {
		return observer().addWeakConsumer(consumer);
	}

	@Override
	default boolean removeWeakConsumer(Consumer<? super T> consumer) {
		return observer().removeWeakConsumer(consumer);
	}
}
