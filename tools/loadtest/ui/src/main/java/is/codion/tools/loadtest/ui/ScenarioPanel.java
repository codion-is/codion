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
 * Copyright (c) 2010 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.tools.loadtest.ui;

import is.codion.common.value.ObservableValueList;
import is.codion.common.value.ValueList;
import is.codion.swing.common.model.component.table.FilterTableModel;
import is.codion.swing.common.model.component.table.FilterTableModel.Editor;
import is.codion.swing.common.model.component.table.FilterTableModel.TableColumns;
import is.codion.swing.common.ui.component.table.FilterTable;
import is.codion.swing.common.ui.component.table.FilterTableCellEditor;
import is.codion.swing.common.ui.component.table.FilterTableColumn;
import is.codion.tools.loadtest.Scenario;
import is.codion.tools.loadtest.randomizer.ItemRandomizer;
import is.codion.tools.loadtest.randomizer.ItemRandomizer.RandomItem;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.util.Collection;
import java.util.List;

import static is.codion.swing.common.ui.component.Components.checkBox;
import static is.codion.swing.common.ui.component.Components.integerSpinner;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

final class ScenarioPanel<T> extends JPanel {

	private final ItemRandomizer<Scenario<T>> itemRandomizer;
	private final Collection<RandomItem<Scenario<T>>> scenarioRows;
	private final FilterTableModel<RandomItem<Scenario<T>>, String> tableModel;
	private final ValueList<Scenario<T>> selected = ValueList.<Scenario<T>>builder().build();

	ScenarioPanel(ItemRandomizer<Scenario<T>> randomizer) {
		this.itemRandomizer = requireNonNull(randomizer);
		this.scenarioRows = createRows();
		this.tableModel = FilterTableModel.builder()
						.columns(new ScenarioColumns())
						.editor(t -> new ScenarioEditor())
						.items(() -> scenarioRows)
						.build();
		FilterTable<RandomItem<Scenario<T>>, String> table = FilterTable.builder()
						.model(tableModel)
						.columns(this::configureColumn)
						.surrendersFocusOnKeystroke(true)
						.cellEditor(ScenarioColumns.ENABLED, FilterTableCellEditor.builder()
										.component(checkBox()::buildValue)
										.clickCountToStart(1)
										.build())
						.cellEditor(ScenarioColumns.WEIGHT, FilterTableCellEditor.builder()
										.component(integerSpinner().minimum(0)::buildValue)
										.clickCountToStart(1)
										.build())
						.columnReordering(false)
						.columnResizing(false)
						.autoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS)
						.build();
		this.tableModel.selection().items().addConsumer(this::onSelectionChanged);
		this.tableModel.items().refresh();
		setLayout(borderLayout());
		add(new JScrollPane(table), BorderLayout.CENTER);
	}

	ObservableValueList<Scenario<T>> selectedScenarios() {
		return selected.observable();
	}

	private void configureColumn(FilterTableColumn.Builder<String> column) {
		switch (column.identifier()) {
			case ScenarioColumns.ENABLED:
			case ScenarioColumns.WEIGHT:
				column.fixedWidth(80);
				break;
			default:
				break;
		}
	}

	private Collection<RandomItem<Scenario<T>>> createRows() {
		return itemRandomizer.items().stream()
						.sorted(comparing(item -> item.item().name()))
						.collect(toList());
	}

	private void onSelectionChanged(List<RandomItem<Scenario<T>>> scenarioRows) {
		selected.set(scenarioRows.stream()
						.map(RandomItem::item)
						.collect(toList()));
	}

	private final class ScenarioColumns implements TableColumns<RandomItem<Scenario<T>>, String> {

		private static final String SCENARIO = "Scenario";
		private static final String WEIGHT = "Weight";
		private static final String ENABLED = "Enabled";

		private final List<String> identifiers = unmodifiableList(asList(SCENARIO, ENABLED, WEIGHT));

		@Override
		public List<String> identifiers() {
			return identifiers;
		}

		@Override
		public Class<?> columnClass(String identifier) {
			switch (identifier) {
				case SCENARIO:
					return String.class;
				case ENABLED:
					return Boolean.class;
				case WEIGHT:
					return Integer.class;
				default:
					throw new IllegalArgumentException();
			}
		}

		@Override
		public Object value(RandomItem<Scenario<T>> row, String identifier) {
			switch (identifier) {
				case SCENARIO:
					return row.item().name();
				case ENABLED:
					return row.enabled().is();
				case WEIGHT:
					return row.weight().get();
				default:
					throw new IllegalArgumentException();
			}
		}
	}

	private final class ScenarioEditor implements Editor<RandomItem<Scenario<T>>, String> {

		@Override
		public boolean editable(RandomItem<Scenario<T>> row, String identifier) {
			return ScenarioColumns.ENABLED.equals(identifier) || ScenarioColumns.WEIGHT.equals(identifier);
		}

		@Override
		public void set(Object value, int rowIndex, RandomItem<Scenario<T>> row, String identifier) {
			if (ScenarioColumns.ENABLED.equals(identifier)) {
				row.enabled().set((Boolean) value);
			}
			else if (ScenarioColumns.WEIGHT.equals(identifier)) {
				row.weight().set((Integer) value);
			}
		}
	}
}