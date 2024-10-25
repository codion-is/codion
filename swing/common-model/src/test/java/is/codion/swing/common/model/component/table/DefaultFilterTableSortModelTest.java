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
 * Copyright (c) 2014 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.model.component.table;

import org.junit.jupiter.api.Test;

import javax.swing.SortOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DefaultFilterTableSortModelTest {

	@Test
	void test() {
		DefaultFilterTableSortModel<Row, Integer> model = new DefaultFilterTableSortModel<>(new FilterTableModel.Columns<Row, Integer>() {

			@Override
			public List<Integer> identifiers() {
				return asList(0, 1, 2);
			}

			@Override
			public Class<?> columnClass(Integer identifier) {
				return Integer.class;
			}

			@Override
			public Object value(Row row, Integer identifier) {
				switch (identifier) {
					case 0:
						return row.firstValue;
					case 1:
						return row.secondValue.toString();
					case 2:
						return row.thirdValue;
					default:
						return null;
				}
			}
		});

		Row firstRow = new Row(1, 2, null);
		Row secondRow = new Row(1, 2, 5);
		Row thirdRow = new Row(1, 3, 6);
		List<Row> items = asList(firstRow, secondRow, thirdRow);

		Comparator<Row> rowComparator = model.comparator();

		items.sort(rowComparator);
		assertEquals(0, items.indexOf(firstRow));
		assertEquals(1, items.indexOf(secondRow));
		assertEquals(2, items.indexOf(thirdRow));

		model.setSortOrder(0, SortOrder.ASCENDING);
		items.sort(rowComparator);
		assertEquals(0, items.indexOf(firstRow));
		assertEquals(1, items.indexOf(secondRow));
		assertEquals(2, items.indexOf(thirdRow));

		model.setSortOrder(2, SortOrder.ASCENDING);
		items.sort(rowComparator);
		assertEquals(0, items.indexOf(firstRow));
		assertEquals(1, items.indexOf(secondRow));
		assertEquals(2, items.indexOf(thirdRow));

		model.setSortOrder(0, SortOrder.ASCENDING);
		model.addSortOrder(1, SortOrder.DESCENDING);
		items.sort(rowComparator);
		assertEquals(0, items.indexOf(thirdRow));
		assertEquals(1, items.indexOf(firstRow));
		assertEquals(2, items.indexOf(secondRow));

		model.addSortOrder(2, SortOrder.DESCENDING);
		items.sort(rowComparator);
		assertEquals(0, items.indexOf(thirdRow));
		assertEquals(1, items.indexOf(secondRow));
		assertEquals(2, items.indexOf(firstRow));

		model.addSortOrder(2, SortOrder.ASCENDING);
		items.sort(rowComparator);
		assertEquals(0, items.indexOf(thirdRow));
		assertEquals(1, items.indexOf(firstRow));
		assertEquals(2, items.indexOf(secondRow));

		model.setSortOrder(2, SortOrder.ASCENDING);
		items.sort(rowComparator);
		assertEquals(0, items.indexOf(firstRow));
		assertEquals(1, items.indexOf(secondRow));
		assertEquals(2, items.indexOf(thirdRow));

		model.clear();
		model.setSortOrder(2, SortOrder.ASCENDING);
		model.sortingEnabled(2).set(false);
		assertEquals(SortOrder.UNSORTED, model.columnSortOrder(2).sortOrder());
		assertThrows(IllegalStateException.class, () -> model.setSortOrder(2, SortOrder.DESCENDING));
		model.sortingEnabled(2).set(true);
		model.setSortOrder(2, SortOrder.DESCENDING);
		assertEquals(SortOrder.DESCENDING, model.columnSortOrder(2).sortOrder());

		assertThrows(IllegalArgumentException.class, () -> model.setSortOrder(3, SortOrder.ASCENDING));//unknown column
	}

	@Test
	void nonComparableColumnClass() {
		DefaultFilterTableSortModel<ArrayList, Integer> model = new DefaultFilterTableSortModel<>(new FilterTableModel.Columns<ArrayList, Integer>() {
			@Override
			public List<Integer> identifiers() {
				return Collections.singletonList(0);
			}

			@Override
			public Class<?> columnClass(Integer identifier) {
				return ArrayList.class;
			}

			@Override
			public Object value(ArrayList row, Integer identifier) {
				return row.toString();
			}
		});
		List<ArrayList> collections = asList(new ArrayList(), new ArrayList());
		model.setSortOrder(0, SortOrder.DESCENDING);
		collections.sort(model.comparator());
	}

	private static final class Row {
		private final Integer firstValue;
		private final Column secondValue;
		private final Integer thirdValue;

		private Row(Integer firstValue, Integer secondValue, Integer thirdValue) {
			this.firstValue = firstValue;
			this.secondValue = new Column(secondValue);
			this.thirdValue = thirdValue;
		}
	}

	private static final class Column {
		private final Integer value;

		private Column(Integer value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return value.toString();
		}
	}
}
