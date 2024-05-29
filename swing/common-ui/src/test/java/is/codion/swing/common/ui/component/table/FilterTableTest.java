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
 * Copyright (c) 2010 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.common.Separators;
import is.codion.swing.common.model.component.table.FilterTableModel;
import is.codion.swing.common.model.component.table.FilterTableSelectionModel;
import is.codion.swing.common.ui.component.table.DefaultFilterTableSearchModel.DefaultRowColumn;
import is.codion.swing.common.ui.component.table.FilterTableSearchModel.RowColumn;

import org.junit.jupiter.api.Test;

import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SortOrder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static org.junit.jupiter.api.Assertions.*;

public class FilterTableTest {

	private static final TestRow A = new TestRow("a");
	private static final TestRow B = new TestRow("b");
	private static final TestRow C = new TestRow("c");
	private static final TestRow D = new TestRow("d");
	private static final TestRow E = new TestRow("e");
	private static final TestRow F = new TestRow("f");
	private static final TestRow G = new TestRow("g");
	private static final TestRow NULL = new TestRow(null);

	private static final List<TestRow> ITEMS = unmodifiableList(asList(A, B, C, D, E));

	private static final class TestRow {
		private final String value;

		private TestRow(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return value;
		}
	}

	private static FilterTable<TestRow, Integer> createTestTable() {
		return createTestTable(null);
	}

	private static FilterTable<TestRow, Integer> createTestTable(Comparator<String> customComparator) {
		return FilterTable.builder(createTestModel(customComparator), createColumns()).build();
	}

	private static FilterTableModel<TestRow, Integer> createTestModel(Comparator<String> customComparator) {
		return FilterTableModel.<TestRow, Integer>builder(new FilterTableModel.Columns<TestRow, Integer>() {
							@Override
							public List<Integer> identifiers() {
								return singletonList(0);
							}

							@Override
							public Class<?> columnClass(Integer integer) {
								return String.class;
							}

							@Override
							public Object value(TestRow row, Integer integer) {
								if (integer == 0) {
									return row.value;
								}

								throw new IllegalArgumentException();
							}

							@Override
							public Comparator<?> comparator(Integer integer) {
								if (customComparator != null) {
									return customComparator;
								}

								return FilterTableModel.Columns.super.comparator(integer);
							}
						})
						.items(() -> ITEMS)
						.build();
	}

	private static List<FilterTableColumn<Integer>> createColumns() {
		return singletonList(FilterTableColumn.builder(0).build());
	}

	@Test
	void builderNullTableModel() {
		assertThrows(Exception.class, () -> FilterTable.builder(null, emptyList()));
	}

	@Test
	void builderNullColumns() {
		assertThrows(Exception.class, () -> FilterTable.builder(createTestModel(null), null));
	}

	@Test
	void searchField() {
		FilterTableModel.Columns<List<String>, Integer> columns = new FilterTableModel.Columns<List<String>, Integer>() {
			@Override
			public List<Integer> identifiers() {
				return singletonList(0);
			}

			@Override
			public Class<?> columnClass(Integer integer) {
				return String.class;
			}

			@Override
			public Object value(List<String> row, Integer integer) {
				return row.get(integer);
			}
		};

		FilterTableModel<List<String>, Integer> tableModel =
						FilterTableModel.<List<String>, Integer>builder(columns)
										.items(() -> asList(
														singletonList("darri"),
														singletonList("dac"),
														singletonList("dansinn"),
														singletonList("dlabo")))
										.build();

		FilterTable<List<String>, Integer> filterTable = FilterTable.builder(tableModel,
						singletonList(FilterTableColumn.filterTableColumn(0))).build();
		tableModel.refresh();

		new JScrollPane(filterTable);

		JTextField searchField = filterTable.searchField();

		searchField.setText("d");
		assertEquals(0, tableModel.selectionModel().getSelectedIndex());
		searchField.setText("da");
		assertEquals(0, tableModel.selectionModel().getSelectedIndex());
		searchField.setText("dac");
		assertEquals(1, tableModel.selectionModel().getSelectedIndex());
		searchField.setText("dar");
		assertEquals(0, tableModel.selectionModel().getSelectedIndex());
		searchField.setText("dan");
		assertEquals(2, tableModel.selectionModel().getSelectedIndex());
		searchField.setText("dl");
		assertEquals(3, tableModel.selectionModel().getSelectedIndex());
		searchField.setText("darri");
		assertEquals(0, tableModel.selectionModel().getSelectedIndex());
		searchField.setText("dac");
		assertEquals(1, tableModel.selectionModel().getSelectedIndex());
		searchField.setText("dl");
		assertEquals(3, tableModel.selectionModel().getSelectedIndex());
		searchField.setText("dans");
		assertEquals(2, tableModel.selectionModel().getSelectedIndex());
		searchField.setText("dansu");
		assertTrue(tableModel.selectionModel().isSelectionEmpty());

		searchField.setText("");
	}

	@Test
	void searchModel() {
		final class Row implements Comparable<Row> {

			private final int id;
			private final String value;

			Row(int id, String value) {
				this.id = id;
				this.value = value;
			}

			@Override
			public int compareTo(Row o) {
				return value.compareTo(o.value);
			}
		}

		FilterTableColumn<Integer> columnId = FilterTableColumn.builder(0).build();
		FilterTableColumn<Integer> columnValue = FilterTableColumn.builder(1).build();

		List<Row> items = asList(
						new Row(0, "a"),
						new Row(1, "b"),
						new Row(2, "c"),
						new Row(3, "d"),
						new Row(4, "e")
		);

		FilterTableModel<Row, Integer> testModel =
						FilterTableModel.<Row, Integer>builder(new FilterTableModel.Columns<Row, Integer>() {
							@Override
							public List<Integer> identifiers() {
								return asList(0, 1);
							}

							@Override
							public Class<?> columnClass(Integer identifier) {
								return String.class;
							}

							@Override
							public Object value(Row row, Integer identifier) {
								if (identifier == 0) {
									return row.id;
								}

								return row.value;
							}
						}).items(() -> items).build();

		FilterTable<Row, Integer> table = FilterTable.builder(testModel, asList(columnId, columnValue)).build();
		testModel.refresh();

		FilterTableSearchModel searchModel = table.searchModel();
		searchModel.searchString().set("b");
		RowColumn rowColumn = searchModel.nextResult().orElse(null);
		assertEquals(new DefaultRowColumn(1, 1), rowColumn);
		searchModel.searchString().set("e");
		rowColumn = searchModel.nextResult().orElse(null);
		assertEquals(new DefaultRowColumn(4, 1), rowColumn);
		searchModel.searchString().set("c");
		rowColumn = searchModel.previousResult().orElse(null);
		assertEquals(new DefaultRowColumn(2, 1), rowColumn);
		searchModel.searchString().set("x");
		rowColumn = searchModel.nextResult().orElse(null);
		assertNull(rowColumn);

		table.sortModel().setSortOrder(1, SortOrder.DESCENDING);

		searchModel.searchString().set("b");
		rowColumn = searchModel.nextResult().orElse(null);
		assertEquals(new DefaultRowColumn(3, 1), rowColumn);
		searchModel.searchString().set("e");
		rowColumn = searchModel.previousResult().orElse(null);
		assertEquals(new DefaultRowColumn(0, 1), rowColumn);

		searchModel.regularExpression().set(true);
		searchModel.searchString().set("(?i)B");
		rowColumn = searchModel.nextResult().orElse(null);
		assertEquals(new DefaultRowColumn(3, 1), rowColumn);

		Predicate<String> predicate = item -> item.equals("b") || item.equals("e");

		searchModel.searchPredicate().set(predicate);
		rowColumn = searchModel.selectPreviousResult().orElse(null);
		assertEquals(new DefaultRowColumn(3, 1), rowColumn);
		rowColumn = searchModel.selectPreviousResult().orElse(null);
		assertEquals(new DefaultRowColumn(0, 1), rowColumn);

		assertEquals(asList(
						new DefaultRowColumn(0, 1),
						new DefaultRowColumn(3, 1)
		), searchModel.searchResults());

		table.sortModel().setSortOrder(1, SortOrder.ASCENDING);
		table.columnModel().moveColumn(1, 0);

		testModel.refresh();
		searchModel.searchString().set("b");
		rowColumn = searchModel.nextResult().orElse(null);
		assertEquals(new DefaultRowColumn(1, 0), rowColumn);
		searchModel.searchString().set("e");
		rowColumn = searchModel.nextResult().orElse(null);
		assertEquals(new DefaultRowColumn(4, 0), rowColumn);
		searchModel.searchString().set("c");
		rowColumn = searchModel.previousResult().orElse(null);
		assertEquals(new DefaultRowColumn(2, 0), rowColumn);
		searchModel.searchString().set("x");
		rowColumn = searchModel.nextResult().orElse(null);
		assertNull(rowColumn);

		table.sortModel().setSortOrder(0, SortOrder.DESCENDING);

		searchModel.searchString().set("b");
		rowColumn = searchModel.nextResult().orElse(null);
		assertEquals(new DefaultRowColumn(3, 0), rowColumn);
		searchModel.searchString().set("e");
		rowColumn = searchModel.previousResult().orElse(null);
		assertEquals(new DefaultRowColumn(0, 0), rowColumn);

		searchModel.regularExpression().set(true);
		searchModel.searchString().set("(?i)B");
		rowColumn = searchModel.nextResult().orElse(null);
		assertEquals(new DefaultRowColumn(3, 0), rowColumn);

		predicate = item -> item.equals("b") || item.equals("e");

		searchModel.searchPredicate().set(predicate);
		rowColumn = searchModel.selectPreviousResult().orElse(null);
		assertEquals(new DefaultRowColumn(3, 0), rowColumn);
		rowColumn = searchModel.selectPreviousResult().orElse(null);
		assertEquals(new DefaultRowColumn(0, 0), rowColumn);

		assertEquals(2, testModel.selectionModel().selectionCount());

		searchModel.selectPreviousResult();
		searchModel.selectNextResult();
		searchModel.selectNextResult();
		searchModel.selectNextResult();

		rowColumn = searchModel.selectPreviousResult().orElse(null);
		rowColumn = searchModel.selectNextResult().orElse(null);
		rowColumn = searchModel.selectNextResult().orElse(null);

		assertEquals(asList(
						new DefaultRowColumn(0, 0),
						new DefaultRowColumn(3, 0)
		), searchModel.searchResults());
	}

	@Test
	void sorting() {
		FilterTable<TestRow, Integer> table = createTestTable();
		FilterTableModel<TestRow, Integer> tableModel = table.model();
		AtomicInteger actionsPerformed = new AtomicInteger();
		Consumer<Integer> consumer = columnIdentifier -> actionsPerformed.incrementAndGet();
		table.sortModel().sortingChangedEvent().addConsumer(consumer);

		tableModel.refresh();
		FilterTableSortModel<TestRow, Integer> sortModel = table.sortModel();
		sortModel.setSortOrder(0, SortOrder.DESCENDING);
		assertEquals(SortOrder.DESCENDING, sortModel.sortOrder(0));
		assertEquals(E, tableModel.itemAt(0));
		assertEquals(1, actionsPerformed.get());
		sortModel.setSortOrder(0, SortOrder.ASCENDING);
		assertEquals(SortOrder.ASCENDING, sortModel.sortOrder(0));
		assertEquals(A, tableModel.itemAt(0));
		assertEquals(0, sortModel.columnSortOrder().get(0).columnIdentifier());
		assertEquals(2, actionsPerformed.get());

		sortModel.setSortOrder(0, SortOrder.DESCENDING);
		tableModel.refresh();
		assertEquals(A, tableModel.itemAt(4));
		assertEquals(E, tableModel.itemAt(0));
		sortModel.setSortOrder(0, SortOrder.ASCENDING);

		List<TestRow> items = new ArrayList<>();
		items.add(NULL);
		tableModel.addItemsAt(0, items);
		sortModel.setSortOrder(0, SortOrder.ASCENDING);
		assertEquals(0, tableModel.indexOf(NULL));
		sortModel.setSortOrder(0, SortOrder.DESCENDING);
		assertEquals(tableModel.getRowCount() - 1, tableModel.indexOf(NULL));

		tableModel.refresh();
		items.add(NULL);
		tableModel.addItemsAt(0, items);
		sortModel.setSortOrder(0, SortOrder.ASCENDING);
		assertEquals(0, tableModel.indexOf(NULL));
		sortModel.setSortOrder(0, SortOrder.DESCENDING);
		assertEquals(tableModel.getRowCount() - 2, tableModel.indexOf(NULL));
		sortModel.setSortOrder(0, SortOrder.UNSORTED);
		table.sortModel().sortingChangedEvent().removeConsumer(consumer);
	}

	@Test
	void customSorting() {
		FilterTable<TestRow, Integer> table = createTestTable(Comparator.reverseOrder());
		FilterTableModel<TestRow, Integer> tableModel = table.model();
		tableModel.refresh();
		FilterTableSortModel<TestRow, Integer> sortModel = table.sortModel();
		sortModel.setSortOrder(0, SortOrder.ASCENDING);
		assertEquals(E, tableModel.itemAt(0));
		sortModel.setSortOrder(0, SortOrder.DESCENDING);
		assertEquals(A, tableModel.itemAt(0));
	}

	@Test
	void selectionAndSorting() {
		FilterTable<TestRow, Integer> table = createTestTable();
		FilterTableModel<TestRow, Integer> tableModel = table.model();
		tableModel.refresh();
		assertTrue(tableModelContainsAll(ITEMS, false, tableModel));

		//test selection and filtering together
		FilterTableSelectionModel<TestRow> selectionModel = tableModel.selectionModel();
		tableModel.selectionModel().setSelectedIndex(3);
		assertEquals(3, selectionModel.getSelectedIndex());

		tableModel.filterModel().conditionModel(0).setEqualValue("d");
		tableModel.filterModel().conditionModel(0).enabled().set(false);

		selectionModel.setSelectedIndexes(singletonList(3));
		assertEquals(3, selectionModel.getMinSelectionIndex());
		assertEquals(ITEMS.get(2), selectionModel.getSelectedItem());

		table.sortModel().setSortOrder(0, SortOrder.ASCENDING);
		assertEquals(ITEMS.get(2), selectionModel.getSelectedItem());
		assertEquals(2, selectionModel.getMinSelectionIndex());

		tableModel.selectionModel().setSelectedIndexes(singletonList(0));
		assertEquals(ITEMS.get(0), selectionModel.getSelectedItem());
		table.sortModel().setSortOrder(0, SortOrder.DESCENDING);
		assertEquals(4, selectionModel.getMinSelectionIndex());

		assertEquals(singletonList(4), selectionModel.getSelectedIndexes());
		assertEquals(ITEMS.get(0), selectionModel.getSelectedItem());
		assertEquals(4, selectionModel.getMinSelectionIndex());
		assertEquals(ITEMS.get(0), selectionModel.getSelectedItem());
	}

	@Test
	void columnModel() {
		FilterTableColumn<Integer> column = createTestTable().columnModel().getColumn(0);
		assertEquals(0, column.identifier());
	}

	@Test
	void export() {
		FilterTable<TestRow, Integer> table = createTestTable();
		table.model().refresh();

		String expected = "0" + Separators.LINE_SEPARATOR +
						"a" + Separators.LINE_SEPARATOR +
						"b" + Separators.LINE_SEPARATOR +
						"c" + Separators.LINE_SEPARATOR +
						"d" + Separators.LINE_SEPARATOR +
						"e";
		assertEquals(expected, table.export()
						.delimiter('\t')
						.get());

		table.model().selectionModel().setSelectedIndexes(asList(0, 1, 3));

		String selected = "a" + Separators.LINE_SEPARATOR +
						"b" + Separators.LINE_SEPARATOR +
						"d";
		assertEquals(selected, table.export()
						.delimiter('\t')
						.header(false)
						.selected(true)
						.get());
	}

	private static boolean tableModelContainsAll(List<TestRow> rows, boolean includeFiltered,
																							 FilterTableModel<TestRow, Integer> model) {
		for (TestRow row : rows) {
			if (includeFiltered) {
				if (!model.containsItem(row)) {
					return false;
				}
			}
			else if (!model.visible(row)) {
				return false;
			}
		}

		return true;
	}
}
