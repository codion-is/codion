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
 * Copyright (c) 2014 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.model.component.table;

import org.junit.jupiter.api.Test;

import javax.swing.SortOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DefaultFilterTableSortTest {

	DefaultFilterTableSort<Row, Integer> model = new DefaultFilterTableSort<>(new FilterTableModel.TableColumns<Row, Integer>() {

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

	@Test
	void setSortOrder() {
		Row firstRow = new Row(1, 2, null);
		Row secondRow = new Row(1, 2, 5);
		Row thirdRow = new Row(1, 3, 6);
		List<Row> items = asList(firstRow, secondRow, thirdRow);

		items.sort(model);
		assertEquals(0, items.indexOf(firstRow));
		assertEquals(1, items.indexOf(secondRow));
		assertEquals(2, items.indexOf(thirdRow));

		model.order(0).set(SortOrder.ASCENDING);
		items.sort(model);
		assertEquals(0, items.indexOf(firstRow));
		assertEquals(1, items.indexOf(secondRow));
		assertEquals(2, items.indexOf(thirdRow));

		model.order(2).set(SortOrder.ASCENDING);
		items.sort(model);
		assertEquals(0, items.indexOf(firstRow));
		assertEquals(1, items.indexOf(secondRow));
		assertEquals(2, items.indexOf(thirdRow));

		model.order(0).set(SortOrder.ASCENDING);
		model.order(1).add(SortOrder.DESCENDING);
		items.sort(model);
		assertEquals(0, items.indexOf(thirdRow));
		assertEquals(1, items.indexOf(firstRow));
		assertEquals(2, items.indexOf(secondRow));

		model.order(2).add(SortOrder.DESCENDING);
		items.sort(model);
		assertEquals(0, items.indexOf(thirdRow));
		assertEquals(1, items.indexOf(secondRow));
		assertEquals(2, items.indexOf(firstRow));

		model.order(2).add(SortOrder.ASCENDING);
		items.sort(model);
		assertEquals(0, items.indexOf(thirdRow));
		assertEquals(1, items.indexOf(firstRow));
		assertEquals(2, items.indexOf(secondRow));

		model.order(2).set(SortOrder.ASCENDING);
		items.sort(model);
		assertEquals(0, items.indexOf(firstRow));
		assertEquals(1, items.indexOf(secondRow));
		assertEquals(2, items.indexOf(thirdRow));

		model.clear();
		model.order(2).set(SortOrder.ASCENDING);
		model.order(2).locked().set(true);
		assertThrows(IllegalStateException.class, () -> model.order(2).set(SortOrder.DESCENDING));
		model.order(2).locked().set(false);
		model.order(2).set(SortOrder.DESCENDING);
		assertEquals(SortOrder.DESCENDING, model.columns().get(2).sortOrder());

		assertThrows(IllegalArgumentException.class, () -> model.order(3).set(SortOrder.ASCENDING));//unknown column
	}

	@Test
	void ascendingDescending() {
		Row firstRow = new Row(1, 3, null);
		Row secondRow = new Row(1, 2, 5);
		Row thirdRow = new Row(1, 2, 6);
		List<Row> items = asList(firstRow, secondRow, thirdRow);

		model.ascending(1, 2);
		items.sort(model);
		assertEquals(0, items.indexOf(secondRow));
		assertEquals(1, items.indexOf(thirdRow));
		assertEquals(2, items.indexOf(firstRow));

		model.descending(1, 2);
		items.sort(model);
		assertEquals(0, items.indexOf(firstRow));
		assertEquals(1, items.indexOf(thirdRow));
		assertEquals(2, items.indexOf(secondRow));

		model.order(1).locked().set(true);
		assertThrows(IllegalStateException.class, () -> model.ascending(2, 1));
		assertThrows(IllegalStateException.class, () -> model.descending(2, 1));
	}

	@Test
	void nonComparableColumnClass() {
		DefaultFilterTableSort<ArrayList<Object>, Integer> sortModel = new DefaultFilterTableSort<>(new FilterTableModel.TableColumns<ArrayList<Object>, Integer>() {
			@Override
			public List<Integer> identifiers() {
				return Collections.singletonList(0);
			}

			@Override
			public Class<?> columnClass(Integer identifier) {
				return ArrayList.class;
			}

			@Override
			public Object value(ArrayList<Object> row, Integer identifier) {
				return row.toString();
			}
		});
		List<ArrayList<Object>> collections = asList(new ArrayList<Object>(), new ArrayList<Object>());
		sortModel.descending(0);
		collections.sort(sortModel);
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
