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

import is.codion.common.value.AbstractValue;
import is.codion.common.value.ObservableValueList;
import is.codion.common.value.ValueList;
import is.codion.swing.common.model.component.table.FilterTableModel;
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

final class ScenarioRandomizerPanel<T> extends JPanel {

	private final ItemRandomizer<Scenario<T>> itemRandomizer;
	private final Collection<ScenarioRow> scenarioRows;
	private final FilterTableModel<ScenarioRow, String> tableModel;
	private final ValueList<Scenario<T>> selected = ValueList.<Scenario<T>>builder().build();

	ScenarioRandomizerPanel(ItemRandomizer<Scenario<T>> itemRandomizer) {
		this.itemRandomizer = requireNonNull(itemRandomizer);
		this.scenarioRows = createRows();
		this.tableModel = FilterTableModel.builder()
						.columns(new ScenarioColumns())
						.editor(t -> new ScenarioEditor())
						.items(() -> scenarioRows)
						.build();
		FilterTable<ScenarioRow, String> table = FilterTable.builder()
						.model(tableModel)
						.columns(this::configureColumn)
						.surrendersFocusOnKeystroke(true)
						.cellEditor(ScenarioRow.ENABLED, FilterTableCellEditor.builder()
										.component(checkBox()::buildValue)
										.clickCountToStart(1)
										.build())
						.cellEditor(ScenarioRow.WEIGHT, FilterTableCellEditor.builder()
										.component(integerSpinner().minimum(0)::buildValue)
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

	/**
	 * @return the selected items
	 */
	public ObservableValueList<Scenario<T>> selectedScenarios() {
		return selected.observable();
	}

	private void configureColumn(FilterTableColumn.Builder<String> column) {
		switch (column.identifier()) {
			case ScenarioRow.ENABLED:
			case ScenarioRow.WEIGHT:
				column.fixedWidth(80);
				break;
			default:
				break;
		}
	}

	private Collection<ScenarioRow> createRows() {
		return itemRandomizer.items().stream()
						.sorted(comparing(item -> item.item().name()))
						.map(this::createRow)
						.collect(toList());
	}

	private ScenarioRow createRow(RandomItem<Scenario<T>> item) {
		return new ScenarioRow(item.item(), new EnabledValue(item.item()), new WeightValue(item.item()));
	}

	private void onSelectionChanged(List<ScenarioRow> scenarioRows) {
		selected.set(scenarioRows.stream()
						.map(scenarioRow -> scenarioRow.scenario)
						.collect(toList()));
	}

	private final class ScenarioRow {

		private static final String SCENARIO = "Scenario";
		private static final String ENABLED = "Enabled";
		private static final String WEIGHT = "Weight";

		private final Scenario<T> scenario;
		private final EnabledValue enabled;
		private final WeightValue weight;

		private ScenarioRow(Scenario<T> scenario, EnabledValue enabled, WeightValue weight) {
			this.scenario = scenario;
			this.enabled = enabled;
			this.weight = weight;
		}
	}

	private final class ScenarioColumns implements FilterTableModel.TableColumns<ScenarioRow, String> {

		private final List<String> identifiers = unmodifiableList(asList(ScenarioRow.SCENARIO, ScenarioRow.ENABLED, ScenarioRow.WEIGHT));

		@Override
		public List<String> identifiers() {
			return identifiers;
		}

		@Override
		public Class<?> columnClass(String identifier) {
			switch (identifier) {
				case ScenarioRow.SCENARIO:
					return String.class;
				case ScenarioRow.ENABLED:
					return Boolean.class;
				case ScenarioRow.WEIGHT:
					return Integer.class;
				default:
					throw new IllegalArgumentException();
			}
		}

		@Override
		public Object value(ScenarioRow row, String identifier) {
			switch (identifier) {
				case ScenarioRow.SCENARIO:
					return row.scenario.name();
				case ScenarioRow.ENABLED:
					return row.enabled.get();
				case ScenarioRow.WEIGHT:
					return row.weight.get();
				default:
					throw new IllegalArgumentException();
			}
		}
	}

	private final class ScenarioEditor implements FilterTableModel.Editor<ScenarioRow, String> {

		@Override
		public boolean editable(ScenarioRow row, String identifier) {
			return ScenarioRow.ENABLED.equals(identifier) || ScenarioRow.WEIGHT.equals(identifier);
		}

		@Override
		public void set(Object value, int rowIndex, ScenarioRow row, String identifier) {
			if (ScenarioRow.ENABLED.equals(identifier)) {
				row.enabled.set((Boolean) value);
			}
			else if (ScenarioRow.WEIGHT.equals(identifier)) {
				row.weight.set((Integer) value);
			}
		}
	}

	private final class EnabledValue extends AbstractValue<Boolean> {

		private final Scenario<T> item;

		private EnabledValue(Scenario<T> item) {
			super(false);
			this.item = item;
		}

		@Override
		protected Boolean getValue() {
			return itemRandomizer.isItemEnabled(item);
		}

		@Override
		protected void setValue(Boolean value) {
			itemRandomizer.setItemEnabled(item, value);
		}
	}

	private final class WeightValue extends AbstractValue<Integer> {

		private final Scenario<T> item;

		private WeightValue(Scenario<T> item) {
			super(0);
			this.item = item;
		}

		@Override
		protected Integer getValue() {
			return itemRandomizer.weight(item);
		}

		@Override
		protected void setValue(Integer value) {
			itemRandomizer.setWeight(item, value);
		}
	}
}