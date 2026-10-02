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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.manual.javadoc;

import is.codion.common.reactive.event.Event;
import is.codion.common.reactive.observer.Observable;
import is.codion.common.reactive.observer.Observer;

/**
 * The {@link Observable} javadoc snippets, each the region of the same name.
 */
final class ObservableSnippets {

	void usage() {
		class Person { // @start region=usage
			private final Event<String> nameChanged = Event.event();

			private String name;

			public String getName() {
				return name;
			}

			public void setName(String name) {
				this.name = name;
				nameChanged.accept(name);
			}
		}

		Person person = new Person();

		Observable<String> observableName = new Observable<>() {
			@Override
			public String get() {
				return person.getName();
			}

			@Override
			public Observer<String> observer() {
				return person.nameChanged.observer();
			}
		};

		observableName.addConsumer(newName ->
						System.out.println("Name changed to " + newName)); // @end
	}
}
