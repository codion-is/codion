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

import is.codion.common.model.condition.ConditionModel;
import is.codion.swing.common.model.component.table.FilterTableModel;
import is.codion.swing.common.model.component.table.FilterTableModel.TableColumns;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import static is.codion.common.Operator.GREATER_THAN_OR_EQUAL;
import static is.codion.manual.swing.common.model.component.table.FilterTableModelDemo.Person.AGE;
import static is.codion.manual.swing.common.model.component.table.FilterTableModelDemo.Person.NAME;
import static javax.swing.SortOrder.ASCENDING;

public final class FilterTableModelDemo {
	// tag::filterTableModel[]
	// Define a record representing the table rows
	public record Person(String name, int age) {

		// Constants identifying the table columns
		public static final String NAME = "Name";
		public static final String AGE = "Age";
	}

	// Implement TableColumns, which specifies the column identifiers,
	// the column class and how to extract column values from the row
	public static final class PersonColumns implements TableColumns<Person, String> {

		private static final List<String> COLUMNS = List.of(NAME, AGE);

		@Override
		public List<String> identifiers() {
			return COLUMNS;
		}

		@Override
		public Class<?> columnClass(String column) {
			return switch (column) {
				case NAME -> String.class;
				case AGE -> Integer.class;
				default -> throw new IllegalArgumentException();
			};
		}

		@Override
		public Object value(Person person, String column) {
			return switch (column) {
				case NAME -> person.name();
				case AGE -> person.age();
				default -> throw new IllegalArgumentException();
			};
		}
	}

	public static FilterTableModel<Person, String> createFilterTableModel() {
		// Implement an item supplier responsible for supplying
		// the table items when the table data is refreshed.
		// Without one the table can be populated by adding items manually
		Supplier<Collection<Person>> supplier = () -> List.of(
						new Person("John", 42),
						new Person("Mary", 43),
						new Person("Andy", 33),
						new Person("Joan", 37));

		// Create the table model
		FilterTableModel<Person, String> tableModel =
						FilterTableModel.builder(new PersonColumns())
										.supplier(supplier)
										.build();

		// Populate the model
		tableModel.items().refresh();

		// Filter out people under 40 years old
		ConditionModel<Integer> ageFilter =
						tableModel.filters().get(Person.AGE);
		ageFilter.operator().set(GREATER_THAN_OR_EQUAL);
		ageFilter.operands().upper().set(40);

		// Sort by name
		tableModel.sort()
						.order(Person.NAME).set(ASCENDING);

		// Print the selected item
		tableModel.selection().item()
						.addConsumer(System.out::println);

		// Select the first row
		tableModel.selection().index().set(0);

		return tableModel;
	}
	// end::filterTableModel[]
}
