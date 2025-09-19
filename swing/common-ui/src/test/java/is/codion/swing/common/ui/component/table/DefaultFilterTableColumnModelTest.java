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
 * Copyright (c) 2013 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.swing.common.ui.component.table.FilterTableColumn.DefaultFilterTableColumnBuilder;
import is.codion.swing.common.ui.component.table.FilterTableColumnModel.ColumnSelection;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
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
		assertEquals(1, testModel.hidden().getOrThrow().size());
		testModel.visible(0).set(true);
		assertEquals(1, shown.size());

		testModel.columnHidden().removeConsumer(hideConsumer);
		testModel.columnShown().removeConsumer(showConsumer);

		assertTrue(testModel.contains(0));
		assertFalse(testModel.contains(1));
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
		FilterTableColumn<Integer> column0 = new DefaultFilterTableColumnBuilder<>(0, 0).build();
		FilterTableColumn<Integer> column1 = new DefaultFilterTableColumnBuilder<>(1, 1).build();
		FilterTableColumn<Integer> column2 = new DefaultFilterTableColumnBuilder<>(2, 2).build();
		FilterTableColumn<Integer> column3 = new DefaultFilterTableColumnBuilder<>(3, 3).build();

		DefaultFilterTableColumnModel<Integer> columnModel =
						new DefaultFilterTableColumnModel<>(asList(column0, column1, column2, column3));

		columnModel.visible().set(1, 3);
		assertEquals(0, columnModel.getSelectionModel().getLeadSelectionIndex());
		assertTrue(columnModel.visible(1).is());
		assertTrue(columnModel.visible(3).is());
		assertFalse(columnModel.visible(0).is());
		assertFalse(columnModel.visible(2).is());
		assertEquals(0, columnModel.getColumnIndex(1));
		assertEquals(1, columnModel.getColumnIndex(3));
		columnModel.visible().set(0, 1);
		assertEquals(0, columnModel.getSelectionModel().getLeadSelectionIndex());
		assertTrue(columnModel.visible(0).is());
		assertTrue(columnModel.visible(0).is());
		assertTrue(columnModel.visible(1).is());
		assertFalse(columnModel.visible(2).is());
		assertFalse(columnModel.visible(2).is());
		assertFalse(columnModel.visible(3).is());
		assertEquals(0, columnModel.getColumnIndex(0));
		assertEquals(1, columnModel.getColumnIndex(1));
		columnModel.visible().set(3);
		assertEquals(0, columnModel.getSelectionModel().getLeadSelectionIndex());
		assertTrue(columnModel.visible(3).is());
		assertFalse(columnModel.visible(2).is());
		assertFalse(columnModel.visible(1).is());
		assertFalse(columnModel.visible(0).is());
		assertEquals(0, columnModel.getColumnIndex(3));
		columnModel.visible().set();
		assertFalse(columnModel.visible(3).is());
		assertFalse(columnModel.visible(2).is());
		assertFalse(columnModel.visible(1).is());
		assertFalse(columnModel.visible(0).is());
		columnModel.visible().set(3, 2, 1, 0);
		assertEquals(0, columnModel.getSelectionModel().getLeadSelectionIndex());
		assertTrue(columnModel.visible(3).is());
		assertTrue(columnModel.visible(2).is());
		assertTrue(columnModel.visible(1).is());
		assertTrue(columnModel.visible(0).is());
		assertEquals(0, columnModel.getColumnIndex(3));
		assertEquals(1, columnModel.getColumnIndex(2));
		assertEquals(2, columnModel.getColumnIndex(1));
		assertEquals(3, columnModel.getColumnIndex(0));
	}

	@Test
	void lock() {
		FilterTableColumn<Integer> column0 = new DefaultFilterTableColumnBuilder<>(0, 0).build();
		FilterTableColumn<Integer> column1 = new DefaultFilterTableColumnBuilder<>(1, 1).build();
		FilterTableColumn<Integer> column2 = new DefaultFilterTableColumnBuilder<>(2, 2).build();
		FilterTableColumn<Integer> column3 = new DefaultFilterTableColumnBuilder<>(3, 3).build();

		FilterTableColumnModel<Integer> columnModel =
						new DefaultFilterTableColumnModel<>(asList(column0, column1, column2, column3));

		columnModel.locked().set(true);
		assertThrows(IllegalStateException.class, () -> columnModel.visible(0).set(false));
		columnModel.locked().set(false);
		columnModel.visible(0).set(false);
		columnModel.locked().set(true);
		assertThrows(IllegalStateException.class, () -> columnModel.visible(0).set(true));
		assertThrows(IllegalStateException.class, () -> columnModel.visible().set(0));

		columnModel.locked().set(false);
		columnModel.visible().set(3, 2, 1);
		columnModel.locked().set(true);
		assertThrows(IllegalStateException.class, () -> columnModel.visible().set(1, 0, 2));
	}

	@Test
	void visibleColumns() {
		FilterTableColumnModel<Integer> testModel = createTestModel();
		assertThrows(IllegalArgumentException.class, () -> testModel.visible().set(0, 1));
		assertThrows(IllegalArgumentException.class, () -> testModel.visible(1));
	}

	@Test
	void nonUniqueColumns() {
		assertThrows(IllegalArgumentException.class, () -> new DefaultFilterTableColumnModel<>(asList(
						new DefaultFilterTableColumnBuilder<>(0, 0).build(),
						new DefaultFilterTableColumnBuilder<>(1, 0).build())));
		assertThrows(IllegalArgumentException.class, () -> new DefaultFilterTableColumnModel<>(asList(
						new DefaultFilterTableColumnBuilder<>(0, 0).build(),
						new DefaultFilterTableColumnBuilder<>(0, 1).build())));
	}

	@Test
	void events() {
		FilterTableColumn<Integer> column0 = new DefaultFilterTableColumnBuilder<>(0, 0).build();
		FilterTableColumn<Integer> column1 = new DefaultFilterTableColumnBuilder<>(1, 1).build();
		FilterTableColumn<Integer> column2 = new DefaultFilterTableColumnBuilder<>(2, 2).build();
		FilterTableColumn<Integer> column3 = new DefaultFilterTableColumnBuilder<>(3, 3).build();

		DefaultFilterTableColumnModel<Integer> columnModel =
						new DefaultFilterTableColumnModel<>(asList(column0, column1, column2, column3));

		Set<Integer> hidden = new HashSet<>();
		columnModel.hidden().addConsumer(identifiers -> {
			hidden.clear();
			hidden.addAll(identifiers);
		});
		columnModel.visible().set(1, 2);
		assertEquals(hidden, new HashSet<>(asList(0, 3)));

		columnModel.visible(1).set(false);
		assertEquals(hidden, new HashSet<>(asList(0, 1, 3)));

		columnModel.visible(2).set(false);
		assertEquals(hidden, new HashSet<>(asList(0, 1, 2, 3)));

		columnModel.visible(0).set(true);
		assertEquals(hidden, new HashSet<>(asList(1, 2, 3)));

		columnModel.visible().set(0, 1, 2, 3);
		assertTrue(hidden.isEmpty());
	}

	@Test
	void selection() {
		FilterTableColumnModel<String> columnModel = new DefaultFilterTableColumnModel<>(asList(
						new DefaultFilterTableColumnBuilder<>("0", 0).build(),
						new DefaultFilterTableColumnBuilder<>("1", 1).build(),
						new DefaultFilterTableColumnBuilder<>("2", 2).build()));
		ColumnSelection<String> selection = columnModel.selection();
		assertTrue(selection.empty().is());
		assertFalse(selection.anchor().present().is());
		assertFalse(selection.lead().present().is());

		selection.setSelectionInterval(1, 2);
		assertFalse(selection.empty().is());
		assertTrue(selection.anchor().present().is());
		assertTrue(selection.lead().present().is());
		assertEquals(1, selection.anchor().get());
		assertEquals(2, selection.lead().get());
		selection.setSelectionInterval(2, 1);
		assertEquals(1, selection.lead().get());
		selection.setSelectionInterval(0, 2);
		assertEquals(asList(0, 1, 2), selection.indexes().get());
		assertEquals(asList("0", "1", "2"), selection.identifiers().get());
		selection.removeSelectionInterval(1, 2);
		assertEquals(asList(0), selection.indexes().get());
		assertEquals(asList("0"), selection.identifiers().get());
		assertEquals(1, selection.anchor().get());
		assertEquals(2, selection.lead().get());
		selection.addSelectionInterval(2, 2);
		assertEquals(asList(0, 2), selection.indexes().get());
		assertEquals(asList("0", "2"), selection.identifiers().get());
		selection.clearSelection();
		assertTrue(selection.empty().is());
	}

	private static FilterTableColumnModel<Integer> createTestModel() {
		return new DefaultFilterTableColumnModel<>(singletonList(new DefaultFilterTableColumnBuilder<>(0, 0).build()));
	}
}
