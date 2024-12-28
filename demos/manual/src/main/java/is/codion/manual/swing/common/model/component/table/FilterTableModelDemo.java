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
import is.codion.common.model.condition.TableConditionModel;
import is.codion.swing.common.model.component.table.FilterTableModel;
import is.codion.swing.common.model.component.table.FilterTableModel.TableColumns;
import is.codion.swing.common.model.component.table.FilterTableModel.TableSelection;
import is.codion.swing.common.model.component.table.FilterTableSortModel;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import static is.codion.common.Operator.EQUAL;
import static is.codion.common.Operator.GREATER_THAN_OR_EQUAL;
import static is.codion.manual.swing.common.model.component.table.FilterTableModelDemo.Person.AGE;
import static is.codion.manual.swing.common.model.component.table.FilterTableModelDemo.Person.NAME;
import static javax.swing.SortOrder.ASCENDING;
import static javax.swing.SortOrder.DESCENDING;

public final class FilterTableModelDemo {

	// tag::person[]
	// Define a record representing the table rows
	public record Person(String name, int age) {

		// Constants identifying the table columns
		public static final String NAME = "Name";
		public static final String AGE = "Age";
	}
	// end::person[]

	// tag::personColumns[]
	// Implement TableColumns, which specifies the column identifiers,
	// the column class and how to extract column values from row objects
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
	// end::personColumns[]

	public static FilterTableModel<Person, String> createFilterTableModel() {
		// tag::filterTableModel[]
		// Implement an item supplier responsible for supplying
		// the data when the table items are refreshed.
		// Without one the model can be populated by adding items manually
		Supplier<Collection<Person>> supplier = () -> List.of(
						new Person("John", 42),
						new Person("Mary", 43),
						new Person("Andy", 33),
						new Person("Joan", 37));

		// Build the table model, providing the TableColumns
		// implementation along with the item supplier.
		FilterTableModel<Person, String> tableModel =
						FilterTableModel.builder(new PersonColumns())
										.supplier(supplier)
										.build();

		// Populate the model
		tableModel.items().refresh();
		// end::filterTableModel[]

		return tableModel;
	}

	static void filter(FilterTableModel<Person, String> tableModel) {
		// tag::filters[]
		TableConditionModel<String> filters = tableModel.filters();

		// Filter out people under 40 years old
		ConditionModel<Integer> ageFilter = filters.get(Person.AGE);

		ageFilter.operator().set(GREATER_THAN_OR_EQUAL);
		ageFilter.operands().upper().set(40);
		// Not necessary since filters auto-enable by default
		// when operators and operands are specified
		ageFilter.enabled().set(true);

		// Filter is automatically disabled when it is cleared
		ageFilter.clear();

		// Filter out anyone besides John and Joan
		ConditionModel<String> nameFilter = filters.get(NAME);

		nameFilter.caseSensitive().set(false);
		// Not necessary, since EQUAL is the default
		nameFilter.operator().set(EQUAL);
		nameFilter.operands().equal().set("jo%");

		// Clear all filters
		filters.clear();
		// end::filters[]
	}

	static void select(FilterTableModel<Person, String> tableModel) {
		// tag::selection[]
		TableSelection<Person> selection = tableModel.selection();

		// Print the selected items when they change
		selection.items().addConsumer(System.out::println);

		// Print a message when the minimum selected index changes
		selection.index().addListener(() ->
						System.out.println("Selected index changed"));

		// Select the first row
		selection.index().set(0);

		// Select the first two rows
		selection.indexes().set(List.of(0, 1));

		// Fetch the selected items
		List<Person> items = selection.items().get();

		// Or just the first (minimun index)
		Person item = selection.item().get();

		// Select a specific person
		selection.item().set(new Person("John", 42));

		// Select all persons over 40
		selection.items().set(person -> person.age() > 40);

		// Increment all selected indexes by
		// one, moving the selection down
		selection.indexes().increment();

		// Clear the selection
		selection.clear();
		// end::selection[]
	}

	static void sort(FilterTableModel<Person, String> tableModel) {
		// tag::sort[]
		FilterTableSortModel<Person, String> sort = tableModel.sort();

		// Sort by age and name, ascending
		sort.ascending(AGE, NAME);

		// Sort by age, descending,
		// set() clears the previous sort
		sort.order(AGE).set(DESCENDING);
		// add sorting by name, ascending,
		// add() adds to any previous sort
		sort.order(NAME).add(ASCENDING);

		// Clear the sorting
		sort.clear();
		// end::sort[]
	}
}
