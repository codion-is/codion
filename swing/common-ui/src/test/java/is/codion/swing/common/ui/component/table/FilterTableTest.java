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

import is.codion.swing.common.model.component.table.FilterTableModel;
import is.codion.swing.common.model.component.table.FilterTableModel.TableSelection;
import is.codion.swing.common.model.component.table.FilterTableSortModel;
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
import java.util.function.Predicate;

import static is.codion.swing.common.ui.component.table.FilterTableColumn.filterTableColumn;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static org.junit.jupiter.api.Assertions.*;

public class FilterTableTest {

	private static final TestRow A = new TestRow("a");
	private static final TestRow B = new TestRow("b");
	private static final TestRow C = new TestRow("c");
	private static final TestRow D = new TestRow("d");
	private static final TestRow E = new TestRow("e");
	private static final TestRow NULL = new TestRow(null);
	private static final String LINE_SEPARATOR = System.lineSeparator();

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
		return FilterTableModel.<TestRow, Integer>builder(new FilterTableModel.TableColumns<TestRow, Integer>() {
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

								return FilterTableModel.TableColumns.super.comparator(integer);
							}
						})
						.supplier(() -> ITEMS)
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
		FilterTableModel.TableColumns<List<String>, Integer> columns = new FilterTableModel.TableColumns<List<String>, Integer>() {
			@Override
			public List<Integer> identifiers() {
				return asList(0, 1);
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
										.supplier(() -> asList(
														asList("darri", "hidden"),
														asList("dac", "hidden"),
														asList("dansinn", "hidden"),
														asList("dlabo", "hidden")))
										.build();

		FilterTable<List<String>, Integer> filterTable = FilterTable.builder(tableModel,
						asList(filterTableColumn(0), filterTableColumn(1))).build();
		filterTable.columnModel().visible(1).set(false);
		tableModel.items().refresh();

		new JScrollPane(filterTable);

		JTextField searchField = filterTable.searchField();

		searchField.setText("d");
		assertEquals(0, tableModel.selection().index().get());
		searchField.setText("da");
		assertEquals(0, tableModel.selection().index().get());
		searchField.setText("dac");
		assertEquals(1, tableModel.selection().index().get());
		searchField.setText("dar");
		assertEquals(0, tableModel.selection().index().get());
		searchField.setText("dan");
		assertEquals(2, tableModel.selection().index().get());
		searchField.setText("dl");
		assertEquals(3, tableModel.selection().index().get());
		searchField.setText("darri");
		assertEquals(0, tableModel.selection().index().get());
		searchField.setText("dac");
		assertEquals(1, tableModel.selection().index().get());
		searchField.setText("dl");
		assertEquals(3, tableModel.selection().index().get());
		searchField.setText("dans");
		assertEquals(2, tableModel.selection().index().get());
		searchField.setText("dansu");
		assertTrue(tableModel.selection().isSelectionEmpty());

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
						FilterTableModel.<Row, Integer>builder(new FilterTableModel.TableColumns<Row, Integer>() {
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
						}).supplier(() -> items).build();

		FilterTable<Row, Integer> table = FilterTable.builder(testModel, asList(columnId, columnValue)).build();
		testModel.items().refresh();

		FilterTableSearchModel searchModel = table.search();
		searchModel.searchString().set("b");
		RowColumn rowColumn = searchModel.results().next().orElse(null);
		assertEquals(new DefaultRowColumn(1, 1), rowColumn);
		searchModel.searchString().set("e");
		rowColumn = searchModel.results().next().orElse(null);
		assertEquals(new DefaultRowColumn(4, 1), rowColumn);
		searchModel.searchString().set("c");
		rowColumn = searchModel.results().previous().orElse(null);
		assertEquals(new DefaultRowColumn(2, 1), rowColumn);
		searchModel.searchString().set("x");
		rowColumn = searchModel.results().next().orElse(null);
		assertNull(rowColumn);

		table.model().sort().descending(1);

		searchModel.searchString().set("b");
		rowColumn = searchModel.results().next().orElse(null);
		assertEquals(new DefaultRowColumn(3, 1), rowColumn);
		searchModel.searchString().set("e");
		rowColumn = searchModel.results().previous().orElse(null);
		assertEquals(new DefaultRowColumn(0, 1), rowColumn);

		searchModel.regularExpression().set(true);
		searchModel.searchString().set("(?i)B");
		rowColumn = searchModel.results().next().orElse(null);
		assertEquals(new DefaultRowColumn(3, 1), rowColumn);

		Predicate<String> predicate = item -> item.equals("b") || item.equals("e");

		searchModel.predicate().set(predicate);
		rowColumn = searchModel.results().selectPrevious().orElse(null);
		assertEquals(new DefaultRowColumn(3, 1), rowColumn);
		rowColumn = searchModel.results().selectPrevious().orElse(null);
		assertEquals(new DefaultRowColumn(0, 1), rowColumn);

		assertEquals(asList(
						new DefaultRowColumn(0, 1),
						new DefaultRowColumn(3, 1)
		), searchModel.results().get());

		table.model().sort().ascending(1);
		table.columnModel().moveColumn(1, 0);

		testModel.items().refresh();
		searchModel.searchString().set("b");
		rowColumn = searchModel.results().next().orElse(null);
		assertEquals(new DefaultRowColumn(1, 0), rowColumn);
		searchModel.searchString().set("e");
		rowColumn = searchModel.results().next().orElse(null);
		assertEquals(new DefaultRowColumn(4, 0), rowColumn);
		searchModel.searchString().set("c");
		rowColumn = searchModel.results().previous().orElse(null);
		assertEquals(new DefaultRowColumn(2, 0), rowColumn);
		searchModel.searchString().set("x");
		rowColumn = searchModel.results().next().orElse(null);
		assertNull(rowColumn);

		table.model().sort().descending(0);

		searchModel.searchString().set("b");
		rowColumn = searchModel.results().next().orElse(null);
		assertEquals(new DefaultRowColumn(3, 0), rowColumn);
		searchModel.searchString().set("e");
		rowColumn = searchModel.results().previous().orElse(null);
		assertEquals(new DefaultRowColumn(0, 0), rowColumn);

		searchModel.regularExpression().set(true);
		searchModel.searchString().set("(?i)B");
		rowColumn = searchModel.results().next().orElse(null);
		assertEquals(new DefaultRowColumn(3, 0), rowColumn);

		predicate = item -> item.equals("b") || item.equals("e");

		searchModel.predicate().set(predicate);
		rowColumn = searchModel.results().selectPrevious().orElse(null);
		assertEquals(new DefaultRowColumn(3, 0), rowColumn);
		rowColumn = searchModel.results().selectPrevious().orElse(null);
		assertEquals(new DefaultRowColumn(0, 0), rowColumn);

		assertEquals(2, testModel.selection().count());

		searchModel.results().selectPrevious();
		searchModel.results().selectNext();
		searchModel.results().selectNext();
		searchModel.results().selectNext();

		searchModel.results().selectPrevious().orElse(null);
		searchModel.results().selectNext().orElse(null);
		searchModel.results().selectNext().orElse(null);

		assertEquals(asList(
						new DefaultRowColumn(0, 0),
						new DefaultRowColumn(3, 0)
		), searchModel.results().get());
	}

	@Test
	void sorting() {
		FilterTable<TestRow, Integer> table = createTestTable();
		FilterTableModel<TestRow, Integer> tableModel = table.model();
		AtomicInteger actionsPerformed = new AtomicInteger();
		Runnable consumer = actionsPerformed::incrementAndGet;
		table.model().sort().observer().addListener(consumer);

		tableModel.items().refresh();
		FilterTableSortModel<TestRow, Integer> sortModel = table.model().sort();
		sortModel.order(0).set(SortOrder.DESCENDING);
		assertEquals(SortOrder.DESCENDING, sortModel.columns().get(0).sortOrder());
		assertEquals(E, tableModel.items().visible().itemAt(0));
		assertEquals(1, actionsPerformed.get());
		sortModel.order(0).set(SortOrder.ASCENDING);
		assertEquals(SortOrder.ASCENDING, sortModel.columns().get(0).sortOrder());
		assertEquals(A, tableModel.items().visible().itemAt(0));
		assertEquals(0, sortModel.columns().get().get(0).identifier());
		assertEquals(2, actionsPerformed.get());

		sortModel.order(0).set(SortOrder.DESCENDING);
		tableModel.items().refresh();
		assertEquals(A, tableModel.items().visible().itemAt(4));
		assertEquals(E, tableModel.items().visible().itemAt(0));
		sortModel.order(0).set(SortOrder.ASCENDING);

		List<TestRow> items = new ArrayList<>();
		items.add(NULL);
		tableModel.items().visible().addItemsAt(0, items);
		sortModel.order(0).set(SortOrder.ASCENDING);
		assertEquals(0, tableModel.items().visible().indexOf(NULL));
		sortModel.order(0).set(SortOrder.DESCENDING);
		assertEquals(tableModel.items().visible().count() - 1, tableModel.items().visible().indexOf(NULL));

		tableModel.items().refresh();
		items.add(NULL);
		tableModel.items().visible().addItemsAt(0, items);
		sortModel.order(0).set(SortOrder.ASCENDING);
		assertEquals(0, tableModel.items().visible().indexOf(NULL));
		sortModel.order(0).set(SortOrder.DESCENDING);
		assertEquals(tableModel.items().visible().count() - 2, tableModel.items().visible().indexOf(NULL));
		sortModel.order(0).set(SortOrder.UNSORTED);
		table.model().sort().observer().removeListener(consumer);
	}

	@Test
	void customSorting() {
		FilterTable<TestRow, Integer> table = createTestTable(Comparator.reverseOrder());
		FilterTableModel<TestRow, Integer> tableModel = table.model();
		tableModel.items().refresh();
		FilterTableSortModel<TestRow, Integer> sortModel = table.model().sort();
		sortModel.order(0).set(SortOrder.ASCENDING);
		assertEquals(E, tableModel.items().visible().itemAt(0));
		sortModel.order(0).set(SortOrder.DESCENDING);
		assertEquals(A, tableModel.items().visible().itemAt(0));
	}

	@Test
	void selectionAndSorting() {
		FilterTable<TestRow, Integer> table = createTestTable();
		FilterTableModel<TestRow, Integer> tableModel = table.model();
		tableModel.items().refresh();
		assertTrue(tableModelContainsAll(ITEMS, false, tableModel));

		//test selection and filtering together
		TableSelection<TestRow> selectionModel = tableModel.selection();
		tableModel.selection().index().set(3);
		assertEquals(3, selectionModel.index().get());

		tableModel.filters().get(0).operands().equal().set("d");
		tableModel.filters().get(0).enabled().set(false);

		selectionModel.indexes().set(singletonList(3));
		assertEquals(3, selectionModel.getMinSelectionIndex());
		assertEquals(ITEMS.get(2), selectionModel.item().get());

		table.model().sort().ascending(0);
		assertEquals(ITEMS.get(2), selectionModel.item().get());
		assertEquals(2, selectionModel.getMinSelectionIndex());

		tableModel.selection().indexes().set(singletonList(0));
		assertEquals(ITEMS.get(0), selectionModel.item().get());
		table.model().sort().descending(0);
		assertEquals(4, selectionModel.getMinSelectionIndex());

		assertEquals(singletonList(4), selectionModel.indexes().get());
		assertEquals(ITEMS.get(0), selectionModel.item().get());
		assertEquals(4, selectionModel.getMinSelectionIndex());
		assertEquals(ITEMS.get(0), selectionModel.item().get());
	}

	@Test
	void columnModel() {
		FilterTableColumn<Integer> column = createTestTable().columnModel().getColumn(0);
		assertEquals(0, column.identifier());
	}

	@Test
	void export() {
		FilterTable<TestRow, Integer> table = createTestTable();
		table.model().items().refresh();

		String expected = "0" + LINE_SEPARATOR +
						"a" + LINE_SEPARATOR +
						"b" + LINE_SEPARATOR +
						"c" + LINE_SEPARATOR +
						"d" + LINE_SEPARATOR +
						"e";
		assertEquals(expected, table.export()
						.delimiter('\t')
						.get());

		table.model().selection().indexes().set(asList(0, 1, 3));

		String selected = "a" + LINE_SEPARATOR +
						"b" + LINE_SEPARATOR +
						"d";
		assertEquals(selected, table.export()
						.delimiter('\t')
						.header(false)
						.selected(true)
						.get());
	}

	@Test
	void nonUniqueColumnIdentifiers() {
		FilterTableModel<Object, Integer> model = FilterTableModel.builder(new FilterTableModel.TableColumns<Object, Integer>() {
							@Override
							public List<Integer> identifiers() {
								return List.of(0, 1);
							}

							@Override
							public Class<?> columnClass(Integer identifier) {
								return Object.class;
							}

							@Override
							public Object value(Object row, Integer identifier) {
								return null;
							}
						})
						.build();
		assertThrows(IllegalArgumentException.class, () -> FilterTable.builder(model, asList(
						filterTableColumn(0, 0),
						filterTableColumn(0, 1)
		)));
	}

	@Test
	void invalidColumnModelIndexes() {
		FilterTableModel<Object, Integer> model = FilterTableModel.builder(new FilterTableModel.TableColumns<Object, Integer>() {
							@Override
							public List<Integer> identifiers() {
								return List.of(0, 1, 2, 3);
							}

							@Override
							public Class<?> columnClass(Integer identifier) {
								return Object.class;
							}

							@Override
							public Object value(Object row, Integer identifier) {
								return null;
							}
						})
						.build();
		assertThrows(IllegalArgumentException.class, () -> FilterTable.builder(model, asList(
						filterTableColumn(0, 0),
						filterTableColumn(1, 1),
						filterTableColumn(2, 4),
						filterTableColumn(3, 3)
		)));
		assertThrows(IllegalArgumentException.class, () -> FilterTable.builder(model, asList(
						filterTableColumn(0,-1),
						filterTableColumn(3, 0),
						filterTableColumn(2, 1),
						filterTableColumn(1, 2)
		)));
		assertThrows(IllegalArgumentException.class, () -> FilterTable.builder(model, asList(
						filterTableColumn(0,42),
						filterTableColumn(1, 0),
						filterTableColumn(2, 1),
						filterTableColumn(3, 2)
		)));
	}

	@Test
	void cellRenderers() {
		FilterTableModel.TableColumns<Object, Integer> columns = new FilterTableModel.TableColumns<>() {
			@Override
			public List<Integer> identifiers() {
				return List.of(0, 1);
			}

			@Override
			public Class<?> columnClass(Integer identifier) {
				return Object.class;
			}

			@Override
			public Object value(Object row, Integer identifier) {
				return 1;
			}
		};
		List<FilterTableColumn<Integer>> tableColumns = asList(
						FilterTableColumn.builder(0).build(),
						FilterTableColumn.builder(1).build());

		FilterTableModel<Object, Integer> model = FilterTableModel.builder(columns).build();
		model.items().addItem(1);

		FilterTableCellRenderer<Object> zeroRenderer = FilterTableCellRenderer.builder(Object.class).build();
		FilterTableCellRenderer<Object> oneRenderer = FilterTableCellRenderer.builder(Object.class).build();

		FilterTable<Object, Integer> table = FilterTable.builder(model, tableColumns)
						.cellRenderer(0, zeroRenderer)
						.cellRendererFactory((identifier, tableModel) -> oneRenderer)
						.build();

		assertSame(zeroRenderer, table.columnModel().column(0).getCellRenderer());
		assertSame(zeroRenderer, table.getCellRenderer(0, 0));

		assertSame(oneRenderer, table.columnModel().column(1).getCellRenderer());
		assertSame(oneRenderer, table.getCellRenderer(0, 1));
	}

	private static boolean tableModelContainsAll(List<TestRow> rows, boolean includeFiltered,
																							 FilterTableModel<TestRow, Integer> model) {
		for (TestRow row : rows) {
			if (includeFiltered) {
				if (!model.items().contains(row)) {
					return false;
				}
			}
			else if (!model.items().visible().contains(row)) {
				return false;
			}
		}

		return true;
	}
}
