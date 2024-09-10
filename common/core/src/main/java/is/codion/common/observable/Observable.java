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
package is.codion.common.observable;

import is.codion.common.event.EventObserver;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * A wrapper for a mutable value, providing a change observer.
 * <pre>
 *   class Person {
 *       private final Event&lt;String&gt; nameChanged = Event.event();
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
 *   Observable&lt;String&gt; observableName = new Observable&lt;&gt;() {
 *      {@literal @}Override
 *       public String get() {
 *           return person.getName();
 *       }
 *
 *      {@literal @}Override
 *       public void set(String value) {
 *           person.setName(value);
 *       }
 *
 *      {@literal @}Override
 *       public EventObserver&lt;String&gt; observer() {
 *           return person.nameChanged.observer();
 *       }
 *  };
 *
 *  observableName.addConsumer(newName -&gt;
 *          System.out.println("Name changed to " + newName));
 * </pre>
 * @param <T> the type of the value being observed
 */
public interface Observable<T> extends EventObserver<T> {

	/**
	 * @return the value
	 */
	T get();

	/**
	 * Sets the value
	 * @param value the value to set
	 */
	void set(T value);

	/**
	 * @return an Optional based on the current value
	 */
	default Optional<T> optional() {
		return Optional.ofNullable(get());
	}

	/**
	 * @return an {@link EventObserver} notified each time the value may have changed
	 */
	EventObserver<T> observer();

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
