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
 * Copyright (c) 2023 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.manual.swing.common.ui.component.table;

import is.codion.common.reactive.state.State;
import is.codion.manual.swing.common.model.component.table.FilterTableModelDemo.Person;
import is.codion.swing.common.model.component.table.SwingFilterTableModel;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.table.ConditionPanel;
import is.codion.swing.common.ui.component.table.ConditionPanel.ConditionView;
import is.codion.swing.common.ui.component.table.FilterTable;
import is.codion.swing.common.ui.component.table.FilterTable.Filters;
import is.codion.swing.common.ui.component.table.FilterTableColumnModel;
import is.codion.swing.common.ui.component.table.FilterTableSearchModel;
import is.codion.swing.common.ui.component.table.FilterTableSearchModel.RowColumn;
import is.codion.swing.common.ui.component.table.TableConditionPanel;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;

import static is.codion.manual.swing.common.model.component.table.FilterTableModelDemo.createFilterTableModel;
import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER;

final class FilterTableDemo {

	static void demo() {
		// tag::filterTable[]
		// See FilterTableModel example
		SwingFilterTableModel<Person, String> tableModel = createFilterTableModel();

		FilterTable<Person, String> table =
						FilterTable.builder()
										.model(tableModel)
										.cellRenderer(Person.AGE, Integer.class, renderer -> renderer
														.horizontalAlignment(SwingConstants.CENTER))
										.doubleClick(Control.command(() ->
														tableModel.selection().item().optional()
																		.ifPresent(System.out::println)))
										.autoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS)
										.build();
		// end::filterTable[]
	}

	static void search(FilterTable<Person, String> table) {
		// tag::search[]
		FilterTableSearchModel search = table.search();

		// Search for the value "43" in the table
		search.predicate().set(value -> value.equals("43"));

		RowColumn searchResult = search.results().current().get();

		System.out.println(searchResult); // row: 1, column: 1

		// Print the next available result
		search.results().next().ifPresent(System.out::println);
		// end::search[]
	}

	static void filters() {
		// tag::filters[]
		SwingFilterTableModel<Person, String> tableModel = createFilterTableModel();

		FilterTable<Person, String> table =
						FilterTable.builder()
										.model(tableModel)
										// The table displays its filter panel, above the table header
										.filters(Filters.ABOVE_HEADER)
										// A single field per column, initially
										.filterView(ConditionView.SIMPLE)
										.build();

		// The filter panel is displayed in the column header of the
		// enclosing scroll pane, so the table must be the view of one
		JScrollPane scrollPane = new JScrollPane(table);
		// end::filters[]
	}

	static void filterView(FilterTable<Person, String> table) {
		// tag::filterView[]
		TableConditionPanel<String> filters = table.filters();

		// Hidden, one field per column or the
		// operator and both bounds per column
		filters.view().set(ConditionView.HIDDEN);
		filters.view().set(ConditionView.SIMPLE);
		filters.view().set(ConditionView.ADVANCED);

		// The view is an observable Value, base your own controls on it
		State advanced = State.state();
		advanced.addConsumer(isAdvanced -> filters.view().set(isAdvanced ?
						ConditionView.ADVANCED : ConditionView.SIMPLE));

		JCheckBox advancedCheckBox = Components.checkBox()
						.link(advanced)
						.text("Advanced filters")
						.build();

		// The filter panel of a single column
		ConditionPanel<?> ageFilterPanel = filters.panel(Person.AGE);
		// end::filterView[]
	}

	static JPopupMenu filterControls(FilterTable<Person, String> table) {
		// tag::filterControls[]
		// Controls for selecting the filter view and clearing the filters.
		// These have no caption, copy them in order to add one.
		Controls filterControls = table.filters().controls().copy()
						.caption("Filters")
						.build();

		// As a submenu in the table popup menu, see FilterTable.Builder.popupMenu()
		JPopupMenu popupMenu = Components.menu()
						.controls(Controls.builder()
										.control(filterControls)
										.separator()
										.control(Controls.builder()
														.caption("Columns")
														.control(table.createToggleColumnsControls())
														.control(table.createResetColumnsControl())))
						.buildPopupMenu();
		// end::filterControls[]

		return popupMenu;
	}

	static void filterPanelLayout(SwingFilterTableModel<Person, String> tableModel) {
		// tag::filterPanelLayout[]
		FilterTable<Person, String> table =
						FilterTable.builder()
										.model(tableModel)
										// The default, the table does not display its filter panel
										.filters(Filters.NONE)
										.filterView(ConditionView.SIMPLE)
										.build();

		JScrollPane tableScrollPane = new JScrollPane(table);

		// A scroll pane without scroll bars, following
		// the horizontal scrolling of the table scroll pane
		JScrollPane filterScrollPane = Components.scrollPane()
						.view(table.filters())
						.horizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER)
						.verticalScrollBarPolicy(VERTICAL_SCROLLBAR_NEVER)
						.followHorizontal(tableScrollPane)
						.build();

		// The filter panel below the table
		JPanel tablePanel = Components.borderLayoutPanel()
						.center(tableScrollPane)
						.south(filterScrollPane)
						.build();
		// end::filterPanelLayout[]
	}

	static void columns(FilterTable<Person, String> table) {
		// tag::columns[]
		FilterTableColumnModel<String> columns = table.columns();

		// Reorder the columns
		columns.visible().set(Person.AGE, Person.NAME);

		// Print hidden columns when they change
		columns.hidden().addConsumer(System.out::println);

		// Hide the age column
		columns.visible(Person.AGE).set(false);

		// Only show the age column
		columns.visible().set(Person.AGE);

		// Reset columns to their default location and visibility
		columns.reset();
		// end::columns[]
	}
}
