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
package is.codion.common.model.component.table;

import is.codion.common.model.component.table.FilterTableModel.TableColumns;
import is.codion.common.model.condition.ConditionModel;
import is.codion.common.utilities.Operator;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class DefaultFilterTableModelTest {

	private static final int NAME = 0;
	private static final int AGE = 1;
	private static final int OTHER = 2;

	@Test
	void defaultFilters() {
		FilterTableModel<Row, Integer> model = FilterTableModel.builder()
						.columns(new RowColumns())
						.build();
		// none for the column which is not Comparable
		assertEquals(new HashSet<>(asList(NAME, AGE)), model.filters().get().keySet());
		assertEquals(String.class, model.filters().get(NAME).type());
		assertEquals(Integer.class, model.filters().get(AGE).type());
		assertEquals("Age", model.filters().get(AGE).caption().orElse(null));
	}

	@Test
	void excludeAndConfigure() {
		FilterTableModel<Row, Integer> model = FilterTableModel.builder()
						.columns(new RowColumns())
						.items(() -> asList(new Row("John", 42), new Row("Mary", 33)))
						.filters(filters -> filters
										.exclude(NAME)
										.condition(AGE, age -> age
														.operators(asList(Operator.EQUAL, Operator.GREATER_THAN))
														.operator(Operator.GREATER_THAN)))
						.build();
		assertEquals(singleton(AGE), model.filters().get().keySet());
		ConditionModel<Integer> age = model.filters().get(AGE);
		assertEquals(asList(Operator.EQUAL, Operator.GREATER_THAN), age.operators());
		assertEquals(Operator.GREATER_THAN, age.operator().get());
		// the defaults kept
		assertEquals("Age", age.caption().orElse(null));
		model.items().refresh();
		age.operands().lower().set(40);
		assertEquals(1, model.items().included().size());
	}

	@Test
	void invalidFilters() {
		// not a column
		assertThrows(IllegalArgumentException.class, () -> FilterTableModel.builder()
						.columns(new RowColumns())
						.filters(filters -> filters.exclude(3))
						.build());
		// not a filterable column
		assertThrows(IllegalArgumentException.class, () -> FilterTableModel.builder()
						.columns(new RowColumns())
						.filters(filters -> filters.condition(OTHER, other -> {}))
						.build());
		// both excluded and configured
		assertThrows(IllegalArgumentException.class, () -> FilterTableModel.builder()
						.columns(new RowColumns())
						.filters(filters -> filters
										.exclude(AGE)
										.condition(AGE, age -> {}))
						.build());
	}

	@Test
	void filterType() {
		// a String filter is applied to the formatted value, whatever the column type
		FilterTableModel<Row, Integer> model = FilterTableModel.builder()
						.columns(new RowColumns() {
							@Override
							public Optional<ConditionModel.Builder<?>> filter(Integer identifier) {
								return identifier == AGE ? Optional.of(ConditionModel.builder().type(String.class)) : super.filter(identifier);
							}
						})
						.items(() -> asList(new Row("John", 42), new Row("Mary", 33)))
						.build();
		model.items().refresh();
		model.filters().<String>get(AGE).set().equalTo("42");
		assertEquals(1, model.items().included().size());
		// neither String nor the column type
		assertThrows(IllegalArgumentException.class, () -> FilterTableModel.builder()
						.columns(new RowColumns() {
							@Override
							public Optional<ConditionModel.Builder<?>> filter(Integer identifier) {
								return identifier == AGE ? Optional.of(ConditionModel.builder().type(Long.class)) : super.filter(identifier);
							}
						})
						.build());
	}

	@Test
	void primitiveColumnType() {
		assertThrows(IllegalArgumentException.class, () -> FilterTableModel.builder()
						.columns(new RowColumns() {
							@Override
							public Class<?> type(Integer identifier) {
								return identifier == AGE ? int.class : super.type(identifier);
							}
						}));
	}

	private static final class Row {

		private final String name;
		private final Integer age;
		private final Object other = new Object();

		private Row(String name, Integer age) {
			this.name = name;
			this.age = age;
		}
	}

	private static class RowColumns implements TableColumns<Row, Integer> {

		@Override
		public List<Integer> identifiers() {
			return asList(NAME, AGE, OTHER);
		}

		@Override
		public String caption(Integer identifier) {
			switch (identifier) {
				case NAME:
					return "Name";
				case AGE:
					return "Age";
				case OTHER:
					return "Other";
				default:
					throw new IllegalArgumentException();
			}
		}

		@Override
		public Class<?> type(Integer identifier) {
			switch (identifier) {
				case NAME:
					return String.class;
				case AGE:
					return Integer.class;
				case OTHER:
					return Object.class;
				default:
					throw new IllegalArgumentException();
			}
		}

		@Override
		public Object value(Row row, Integer identifier) {
			switch (identifier) {
				case NAME:
					return row.name;
				case AGE:
					return row.age;
				case OTHER:
					return row.other;
				default:
					throw new IllegalArgumentException();
			}
		}
	}
}
