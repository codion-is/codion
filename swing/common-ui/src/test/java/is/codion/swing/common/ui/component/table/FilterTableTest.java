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
 * Copyright (c) 2010 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.swing.common.model.component.list.FilterListSelection;
import is.codion.swing.common.model.component.table.FilterTableModel;
import is.codion.swing.common.model.component.table.FilterTableSort;
import is.codion.swing.common.ui.ancestor.Ancestor;
import is.codion.swing.common.ui.component.table.ConditionPanel.ConditionView;
import is.codion.swing.common.ui.component.table.DefaultFilterTableSearchModel.DefaultRowColumn;
import is.codion.swing.common.ui.component.table.FilterTableSearchModel.RowColumn;
import is.codion.swing.common.ui.component.text.NumberField;

import org.junit.jupiter.api.Test;

import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import java.awt.Dimension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.*;

public class FilterTableTest {

	private static final TestRow A = new TestRow("a");
	private static final TestRow B = new TestRow("b");
	private static final TestRow C = new TestRow("c");
	private static final TestRow D = new TestRow("d");
	private static final TestRow E = new TestRow("e");
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
		return FilterTable.builder().model(createTestModel(customComparator)).build();
	}

	private static FilterTableModel<TestRow, Integer> createTestModel(Comparator<String> customComparator) {
		return FilterTableModel.builder()
						.columns(new FilterTableModel.TableColumns<TestRow, Integer>() {
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
						.items(() -> ITEMS)
						.build();
	}

	@Test
	void builderNullTableModel() {
		assertThrows(Exception.class, () -> FilterTable.builder().model(null));
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
						FilterTableModel.builder()
										.columns(columns)
										.items(() -> asList(
														asList("darri", "hidden"),
														asList("dac", "hidden"),
														asList("dansinn", "hidden"),
														asList("dlabo", "hidden")))
										.refresh(true)
										.build();

		FilterTable<List<String>, Integer> filterTable = FilterTable.builder()
						.model(tableModel)
						.build();
		filterTable.columnModel().visible(1).set(false);

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

		List<Row> items = asList(
						new Row(0, "a"),
						new Row(1, "b"),
						new Row(2, "c"),
						new Row(3, "d"),
						new Row(4, "e")
		);

		FilterTableModel<Row, Integer> testModel =
						FilterTableModel.builder()
										.columns(new FilterTableModel.TableColumns<Row, Integer>() {
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
										})
										.items(() -> items)
										.refresh(true)
										.build();

		FilterTable<Row, Integer> table = FilterTable.builder()
						.model(testModel)
						.build();

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
		FilterTableSort<TestRow, Integer> sortModel = table.model().sort();
		sortModel.order(0).set(SortOrder.DESCENDING);
		assertEquals(SortOrder.DESCENDING, sortModel.columns().get(0).sortOrder());
		assertEquals(E, tableModel.items().included().get(0));
		assertEquals(1, actionsPerformed.get());
		sortModel.order(0).set(SortOrder.ASCENDING);
		assertEquals(SortOrder.ASCENDING, sortModel.columns().get(0).sortOrder());
		assertEquals(A, tableModel.items().included().get(0));
		assertEquals(0, sortModel.columns().get().get(0).identifier());
		assertEquals(2, actionsPerformed.get());

		sortModel.order(0).set(SortOrder.DESCENDING);
		tableModel.items().refresh();
		assertEquals(A, tableModel.items().included().get(4));
		assertEquals(E, tableModel.items().included().get(0));
		sortModel.order(0).set(SortOrder.ASCENDING);

		List<TestRow> items = new ArrayList<>();
		items.add(NULL);
		tableModel.items().included().add(0, items);
		sortModel.order(0).set(SortOrder.ASCENDING);
		assertEquals(0, tableModel.items().included().indexOf(NULL));
		sortModel.order(0).set(SortOrder.DESCENDING);
		assertEquals(tableModel.items().included().size() - 1, tableModel.items().included().indexOf(NULL));

		tableModel.items().refresh();
		items.add(NULL);
		tableModel.items().included().add(0, items);
		sortModel.order(0).set(SortOrder.ASCENDING);
		assertEquals(0, tableModel.items().included().indexOf(NULL));
		sortModel.order(0).set(SortOrder.DESCENDING);
		assertEquals(tableModel.items().included().size() - 2, tableModel.items().included().indexOf(NULL));
		sortModel.order(0).set(SortOrder.UNSORTED);
		table.model().sort().observer().removeListener(consumer);
	}

	@Test
	void customSorting() {
		FilterTable<TestRow, Integer> table = createTestTable(Comparator.reverseOrder());
		FilterTableModel<TestRow, Integer> tableModel = table.model();
		tableModel.items().refresh();
		FilterTableSort<TestRow, Integer> sortModel = table.model().sort();
		sortModel.order(0).set(SortOrder.ASCENDING);
		assertEquals(E, tableModel.items().included().get(0));
		sortModel.order(0).set(SortOrder.DESCENDING);
		assertEquals(A, tableModel.items().included().get(0));
	}

	@Test
	void selectionAndSorting() {
		FilterTable<TestRow, Integer> table = createTestTable();
		FilterTableModel<TestRow, Integer> tableModel = table.model();
		tableModel.items().refresh();
		assertTrue(tableModelContainsAll(ITEMS, false, tableModel));

		//test selection and filtering together
		FilterListSelection<TestRow> selectionModel = tableModel.selection();
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
	void cellRenderers() {
		final class Row {}

		FilterTableModel.TableColumns<Row, Integer> columns = new FilterTableModel.TableColumns<>() {
			@Override
			public List<Integer> identifiers() {
				return List.of(0, 1);
			}

			@Override
			public Class<?> columnClass(Integer identifier) {
				return Integer.class;
			}

			@Override
			public Object value(Row row, Integer identifier) {
				return 1;
			}
		};

		FilterTableModel<Row, Integer> model = FilterTableModel.builder()
						.columns(columns)
						.build();
		model.items().add(new Row());

		FilterTableCellRenderer<Row, Integer, Integer> oneRenderer = FilterTableCellRenderer.<Row, Integer>builder()
						.columnClass(Integer.class)
						.horizontalAlignment(SwingConstants.LEFT)
						.build();

		FilterTable<Row, Integer> table = FilterTable.builder()
						.model(model)
						.cellRenderer(0, Integer.class, renderer ->
										renderer.horizontalAlignment(SwingConstants.CENTER))
						.cellRenderers((identifier, tableModel) -> oneRenderer)
						// Trigger the condition panel to be built right away
						.filterView(ConditionView.SIMPLE)
						.build();

		assertSame(oneRenderer, table.columnModel().column(1).getCellRenderer());
		assertSame(oneRenderer, table.getCellRenderer(0, 1));

		// Should follow cell renderer alignment
		NumberField<Integer> equalComponent = (NumberField<Integer>)
						((ColumnConditionPanel<Integer>) table.filters().panel(0)).operands().equal().get();
		assertEquals(SwingConstants.CENTER, equalComponent.getHorizontalAlignment());
		equalComponent = (NumberField<Integer>) ((ColumnConditionPanel<Integer>) table.filters().panel(1)).operands().equal().get();
		assertEquals(SwingConstants.LEFT, equalComponent.getHorizontalAlignment());
	}

	@Test
	void scrollToAdded() {
		FilterTable<TestRow, Integer> table = FilterTable.builder()
						.model(createTestModel(null))
						.scrollToAddedItem(true)
						.build();
		JScrollPane scrollPane = new JScrollPane(table);
		scrollPane.setPreferredSize(new Dimension(200, 200));
		FilterTableModel<TestRow, Integer> model = table.model();
		List<TestRow> rows = IntStream.range(0, 100)
						.mapToObj(i -> new TestRow("" + i))
						.collect(toList());

		model.items().add(rows);
		model.sort().ascending(0);
		model.items().add(new TestRow("200"));

		JViewport viewport = Ancestor.ofType(JViewport.class).of(table).get();
		int row = table.rowAtPoint(viewport.getViewPosition());
		TestRow testRow = model.items().included().get(row);
		assertEquals("200", testRow.value);

		rows = IntStream.range(301, 350)
						.mapToObj(i -> new TestRow("" + i))
						.collect(toList());

		model.items().add(rows);
		row = table.rowAtPoint(viewport.getViewPosition());
		testRow = model.items().included().get(row);
		assertEquals("301", testRow.value);

		model.sort().clear();

		model.items().included().add(0, new TestRow("400"));
		row = table.rowAtPoint(viewport.getViewPosition());
		testRow = model.items().included().get(row);
		assertEquals(0, row);
		assertEquals("400", testRow.value);

		model.items().included().add(20, new TestRow("401"));
		row = table.rowAtPoint(viewport.getViewPosition());
		testRow = model.items().included().get(row);
		assertEquals(20, row);
		assertEquals("401", testRow.value);
	}

	/**
	 * Note that this method be made static and called from a main() method, in order to play with table editing
	 */
	@Test
	void tableEditTest() {

		enum TestEnum {
			ONE, TWO, THREE;
		}

		class Row {
			private static final List<Class<?>> COLUMN_CLASSES = asList(
							LocalTime.class, LocalDate.class, LocalDateTime.class, OffsetDateTime.class,
							String.class, Short.class, Integer.class, Long.class, Double.class, BigInteger.class,
							BigDecimal.class, Boolean.class, TestEnum.class, Character.class);

			private LocalTime localTime;
			private LocalDate localDate;
			private LocalDateTime localDateTime;
			private OffsetDateTime offsetDateTime;
			private String string;
			private Short shortValue;
			private Integer integer;
			private Long longValue;
			private Double doubleValue;
			private BigInteger bigInteger;
			private BigDecimal bigDecimal;
			private Boolean booleanValue;
			private TestEnum enumValue;
			private Character character;

			private Row(LocalTime localTime, LocalDate localDate, LocalDateTime localDateTime, OffsetDateTime offsetDateTime,
									String string, Short shortValue, Integer integer, Long longValue, Double doubleValue, BigInteger bigInteger,
									BigDecimal bigDecimal, Boolean booleanValue, TestEnum enumValue, Character character) {
				this.localTime = localTime;
				this.localDate = localDate;
				this.localDateTime = localDateTime;
				this.offsetDateTime = offsetDateTime;
				this.string = string;
				this.shortValue = shortValue;
				this.integer = integer;
				this.longValue = longValue;
				this.doubleValue = doubleValue;
				this.bigInteger = bigInteger;
				this.bigDecimal = bigDecimal;
				this.booleanValue = booleanValue;
				this.enumValue = enumValue;
				this.character = character;
			}
		}

		FilterTableModel.TableColumns<Row, Integer> columns = new FilterTableModel.TableColumns<Row, Integer>() {

			private static final List<Integer> IDENTIFIERS = IntStream.range(0, 14).boxed().collect(toList());

			@Override
			public List<Integer> identifiers() {
				return IDENTIFIERS;
			}

			@Override
			public Class<?> columnClass(Integer identifier) {
				return Row.COLUMN_CLASSES.get(identifier);
			}

			@Override
			public String caption(Integer identifier) {
				return Row.COLUMN_CLASSES.get(identifier).getSimpleName();
			}

			@Override
			public Object value(Row row, Integer identifier) {
				switch (identifier) {
					case 0:
						return row.localTime;
					case 1:
						return row.localDate;
					case 2:
						return row.localDateTime;
					case 3:
						return row.offsetDateTime;
					case 4:
						return row.string;
					case 5:
						return row.shortValue;
					case 6:
						return row.integer;
					case 7:
						return row.longValue;
					case 8:
						return row.doubleValue;
					case 9:
						return row.bigInteger;
					case 10:
						return row.bigDecimal;
					case 11:
						return row.booleanValue;
					case 12:
						return row.enumValue;
					case 13:
						return row.character;
					default:
						throw new IllegalArgumentException();
				}
			}
		};
		FilterTableModel.Editor<Row, Integer> editor = new FilterTableModel.Editor<Row, Integer>() {

			@Override
			public boolean editable(Row row, Integer identifier) {
				return true;
			}

			@Override
			public void set(Object value, int rowIndex, Row row, Integer identifier) {
				switch (identifier) {
					case 0:
						row.localTime = (LocalTime) value;
						break;
					case 1:
						row.localDate = (LocalDate) value;
						break;
					case 2:
						row.localDateTime = (LocalDateTime) value;
						break;
					case 3:
						row.offsetDateTime = (OffsetDateTime) value;
						break;
					case 4:
						row.string = (String) value;
						break;
					case 5:
						row.shortValue = (Short) value;
						break;
					case 6:
						row.integer = (Integer) value;
						break;
					case 7:
						row.longValue = (Long) value;
						break;
					case 8:
						row.doubleValue = (Double) value;
						break;
					case 9:
						row.bigInteger = (BigInteger) value;
						break;
					case 10:
						row.bigDecimal = (BigDecimal) value;
						break;
					case 11:
						row.booleanValue = (Boolean) value;
						break;
					case 12:
						row.enumValue = (TestEnum) value;
						break;
					case 13:
						row.character = (Character) value;
						break;
					default:
						throw new IllegalArgumentException();
				}
			}
		};

		FilterTableModel<Row, Integer> model = FilterTableModel.builder()
						.columns(columns)
						.editor(m -> editor)
						.items(() -> asList(
										new Row(LocalTime.now(), LocalDate.now(), LocalDateTime.now(), OffsetDateTime.now(), "Test",
														(short) 10, 42, 24345L, 12.456, BigInteger.valueOf(1200),
														BigDecimal.valueOf(499.999), false, TestEnum.ONE, 'a'),
										new Row(LocalTime.now(), LocalDate.now(), LocalDateTime.now(), OffsetDateTime.now(), "Best",
														(short) 11, 43, 2345L, 2124.456, BigInteger.valueOf(1202000),
														BigDecimal.valueOf(49900.999), true, TestEnum.TWO, 'B')
						))
						.build();

		FilterTable<Row, Integer> table = FilterTable.builder()
						.model(model)
						.surrendersFocusOnKeystroke(true)
						.build();

		model.items().refresh();

		for (int i = 0; i < columns.identifiers().size(); i++) {
			table.editCellAt(0, i);
			table.getCellEditor().stopCellEditing();
		}

//		Dialogs.builder()
//						.component(new JScrollPane(table))
//						.show();
	}

	private static boolean tableModelContainsAll(List<TestRow> rows, boolean includeFiltered,
																							 FilterTableModel<TestRow, Integer> model) {
		for (TestRow row : rows) {
			if (includeFiltered) {
				if (!model.items().contains(row)) {
					return false;
				}
			}
			else if (!model.items().included().contains(row)) {
				return false;
			}
		}

		return true;
	}
}
