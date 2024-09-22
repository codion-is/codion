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
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.manual.swing.common.model.component.table;

import is.codion.swing.common.model.component.table.FilterTableModel;
import is.codion.swing.common.model.component.table.FilterTableModel.Columns;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

public final class FilterTableModelDemo {
	// tag::filterTableModel[]
	// Define a record representing the table rows
	public record Person(String name, int age) {}

	// Define an enum identifying the table columns
	public enum PersonColumn {
		NAME,
		AGE
	}

	// Implement Columns, specifying the table columns
	public static final class PersonColumns implements Columns<Person, PersonColumn> {

		private static final List<PersonColumn> COLUMNS = List.of(PersonColumn.values());

		@Override
		public List<PersonColumn> identifiers() {
			return COLUMNS;
		}

		@Override
		public Class<?> columnClass(PersonColumn column) {
			return switch (column) {
				case NAME -> String.class;
				case AGE -> Integer.class;
			};
		}

		@Override
		public Object value(Person person, PersonColumn column) {
			return switch (column) {
				case NAME -> person.name();
				case AGE -> person.age();
			};
		}
	}

	public static FilterTableModel<Person, PersonColumn> createFilterTableModel() {
		// Implement an item supplier responsible for supplying
		// the table items when the table data is refreshed.
		// Without one the table can be populated by adding items manually
		Supplier<Collection<Person>> items = () -> List.of(
						new Person("John", 42),
						new Person("Mary", 43));

		// Create the table model
		FilterTableModel<Person, PersonColumn> tableModel =
						FilterTableModel.builder(new PersonColumns())
										.items(items)
										.build();

		// Populate the model
		tableModel.refresh();

		// Select the first row
		tableModel.selection().index().set(0);

		return tableModel;
	}
	// end::filterTableModel[]
}
