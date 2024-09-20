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

/**
 * A wrapper for a mutable value, providing a change observer.
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
 *   Mutable<String> mutableName = new Mutable<>() {
 *       @Override
 *       public String get() {
 *           return person.getName();
 *       }
 *
 *       @Override
 *       public void set(String value) {
 *           person.setName(value);
 *       }
 *
 *       @Override
 *       public Observer<String> observer() {
 *           return person.nameChanged.observer();
 *       }
 *  };
 *
 *  mutableName.addConsumer(newName ->
 *          System.out.println("Name changed to " + newName));
 * }
 * </pre>
 * @param <T> the value type
 */
public interface Mutable<T> extends Observable<T> {

	/**
	 * Sets the value
	 * @param value the value to set
	 */
	void set(T value);

	/**
	 * Clears this {@link Mutable} instance by setting the value to null
	 */
	default void clear() {
		set(null);
	}
}
