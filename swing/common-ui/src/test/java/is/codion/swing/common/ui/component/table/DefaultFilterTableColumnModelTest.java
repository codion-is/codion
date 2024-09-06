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
 * Copyright (c) 2013 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.table;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultFilterTableColumnModelTest {

	@Test
	void testModel() {
		FilterTableColumnModel<Integer> testModel = createTestModel();
		Collection<Object> hidden = new ArrayList<>();
		Collection<Object> shown = new ArrayList<>();
		Consumer<Integer> hideConsumer = hidden::add;
		Consumer<Integer> showConsumer = shown::add;
		testModel.columnHidden().addConsumer(hideConsumer);
		testModel.columnShown().addConsumer(showConsumer);

		assertEquals(1, testModel.getColumnCount());
		assertNotNull(testModel.column(0));

		testModel.visible(0).set(false);
		assertEquals(1, hidden.size());
		assertEquals(1, testModel.hidden().size());
		testModel.visible(0).set(true);
		assertEquals(1, shown.size());

		testModel.columnHidden().removeConsumer(hideConsumer);
		testModel.columnShown().removeConsumer(showConsumer);

		assertTrue(testModel.containsColumn(0));
		assertFalse(testModel.containsColumn(1));
	}

	@Test
	void tableColumnNotFound() {
		FilterTableColumnModel<Integer> testModel = createTestModel();
		assertThrows(IllegalArgumentException.class, () -> testModel.column(42));
	}

	@Test
	void constructorNullColumns() {
		assertThrows(NullPointerException.class, () -> new DefaultFilterTableColumnModel<>(null));
	}

	@Test
	void constructorNoColumns() {
		assertThrows(IllegalArgumentException.class, () -> new DefaultFilterTableColumnModel<>(new ArrayList<>()));
	}

	@Test
	void setColumns() {
		FilterTableColumn<Integer> column0 = FilterTableColumn.filterTableColumn(0);
		FilterTableColumn<Integer> column1 = FilterTableColumn.filterTableColumn(1);
		FilterTableColumn<Integer> column2 = FilterTableColumn.filterTableColumn(2);
		FilterTableColumn<Integer> column3 = FilterTableColumn.filterTableColumn(3);

		DefaultFilterTableColumnModel<Integer> columnModel =
						new DefaultFilterTableColumnModel<>(asList(column0, column1, column2, column3));

		columnModel.setVisibleColumns(1, 3);
		assertEquals(0, columnModel.getSelectionModel().getLeadSelectionIndex());
		assertTrue(columnModel.visible(1).get());
		assertTrue(columnModel.visible(3).get());
		assertFalse(columnModel.visible(0).get());
		assertFalse(columnModel.visible(2).get());
		assertEquals(0, columnModel.getColumnIndex(1));
		assertEquals(1, columnModel.getColumnIndex(3));
		columnModel.setVisibleColumns(0, 1);
		assertEquals(0, columnModel.getSelectionModel().getLeadSelectionIndex());
		assertTrue(columnModel.visible(0).get());
		assertTrue(columnModel.visible(0).get());
		assertTrue(columnModel.visible(1).get());
		assertFalse(columnModel.visible(2).get());
		assertFalse(columnModel.visible(2).get());
		assertFalse(columnModel.visible(3).get());
		assertEquals(0, columnModel.getColumnIndex(0));
		assertEquals(1, columnModel.getColumnIndex(1));
		columnModel.setVisibleColumns(3);
		assertEquals(0, columnModel.getSelectionModel().getLeadSelectionIndex());
		assertTrue(columnModel.visible(3).get());
		assertFalse(columnModel.visible(2).get());
		assertFalse(columnModel.visible(1).get());
		assertFalse(columnModel.visible(0).get());
		assertEquals(0, columnModel.getColumnIndex(3));
		columnModel.setVisibleColumns();
		assertFalse(columnModel.visible(3).get());
		assertFalse(columnModel.visible(2).get());
		assertFalse(columnModel.visible(1).get());
		assertFalse(columnModel.visible(0).get());
		columnModel.setVisibleColumns(3, 2, 1, 0);
		assertEquals(0, columnModel.getSelectionModel().getLeadSelectionIndex());
		assertTrue(columnModel.visible(3).get());
		assertTrue(columnModel.visible(2).get());
		assertTrue(columnModel.visible(1).get());
		assertTrue(columnModel.visible(0).get());
		assertEquals(0, columnModel.getColumnIndex(3));
		assertEquals(1, columnModel.getColumnIndex(2));
		assertEquals(2, columnModel.getColumnIndex(1));
		assertEquals(3, columnModel.getColumnIndex(0));
	}

	@Test
	void lock() {
		FilterTableColumn<Integer> column0 = FilterTableColumn.filterTableColumn(0);
		FilterTableColumn<Integer> column1 = FilterTableColumn.filterTableColumn(1);
		FilterTableColumn<Integer> column2 = FilterTableColumn.filterTableColumn(2);
		FilterTableColumn<Integer> column3 = FilterTableColumn.filterTableColumn(3);

		FilterTableColumnModel<Integer> columnModel =
						new DefaultFilterTableColumnModel<>(asList(column0, column1, column2, column3));

		columnModel.locked().set(true);
		assertThrows(IllegalStateException.class, () -> columnModel.visible(0).set(false));
		columnModel.locked().set(false);
		columnModel.visible(0).set(false);
		columnModel.locked().set(true);
		assertThrows(IllegalStateException.class, () -> columnModel.visible(0).set(true));
		assertThrows(IllegalStateException.class, () -> columnModel.setVisibleColumns(0));

		columnModel.locked().set(false);
		columnModel.setVisibleColumns(3, 2, 1);
		columnModel.locked().set(true);
		assertThrows(IllegalStateException.class, () -> columnModel.setVisibleColumns(1, 0, 2));
	}

	@Test
	void visibleColumns() {
		FilterTableColumnModel<Integer> testModel = createTestModel();
		assertThrows(IllegalArgumentException.class, () -> testModel.setVisibleColumns(0, 1));
		assertThrows(IllegalArgumentException.class, () -> testModel.visible(1));
	}

	private static FilterTableColumnModel<Integer> createTestModel() {
		return new DefaultFilterTableColumnModel<>(singletonList(FilterTableColumn.filterTableColumn(0)));
	}
}
