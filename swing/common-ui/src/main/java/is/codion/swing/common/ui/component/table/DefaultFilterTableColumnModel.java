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

import is.codion.common.event.Event;
import is.codion.common.observer.Observer;
import is.codion.common.state.State;

import javax.swing.ListSelectionModel;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

final class DefaultFilterTableColumnModel<C> implements FilterTableColumnModel<C> {

	private final DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
	private final Event<C> columnHidden = Event.event();
	private final Event<C> columnShown = Event.event();
	private final Map<C, FilterTableColumn<C>> columns = new LinkedHashMap<>();
	private final Map<Integer, C> columnIdentifiers = new HashMap<>();
	private final Map<C, HiddenColumn> hiddenColumns = new LinkedHashMap<>();
	private final Map<C, State> visibleStates = new HashMap<>();
	private final State locked = State.state();

	DefaultFilterTableColumnModel(List<FilterTableColumn<C>> tableColumns) {
		if (requireNonNull(tableColumns, "columns").isEmpty()) {
			throw new IllegalArgumentException("One or more columns must be specified");
		}
		tableColumns.forEach(this::initializeColumn);
	}

	@Override
	public Collection<FilterTableColumn<C>> columns() {
		return unmodifiableCollection(columns.values());
	}

	@Override
	public State locked() {
		return locked;
	}

	@Override
	public void setVisibleColumns(C... identifiers) {
		setVisibleColumns(asList(identifiers));
	}

	@Override
	public void setVisibleColumns(List<C> identifiers) {
		requireNonNull(identifiers);
		identifiers.forEach(this::validateColumn);
		int columnIndex = 0;
		for (C identifier : identifiers) {
			visibleStates.get(identifier).set(true);
			moveColumn(getColumnIndex(identifier), columnIndex++);
		}
		for (FilterTableColumn<C> column : columns()) {
			if (!identifiers.contains(column.identifier())) {
				visibleStates.get(column.identifier()).set(false);
			}
		}
		if (!identifiers.isEmpty()) {
			tableColumnModel.getSelectionModel().setSelectionInterval(0, 0);
		}
	}

	@Override
	public List<FilterTableColumn<C>> visible() {
		List<FilterTableColumn<C>> tableColumns = new ArrayList<>(tableColumnModel.getColumnCount());
		Enumeration<TableColumn> columnEnumeration = tableColumnModel.getColumns();
		while (columnEnumeration.hasMoreElements()) {
			tableColumns.add((FilterTableColumn<C>) columnEnumeration.nextElement());
		}

		return unmodifiableList(tableColumns);
	}

	@Override
	public Collection<FilterTableColumn<C>> hidden() {
		return unmodifiableCollection(hiddenColumns.values().stream()
						.map(hiddenColumn -> hiddenColumn.column)
						.collect(Collectors.toList()));
	}

	@Override
	public FilterTableColumn<C> column(C identifier) {
		FilterTableColumn<C> column = columns.get(requireNonNull(identifier));
		if (column != null) {
			return column;
		}

		throw new IllegalArgumentException("Column not found: " + identifier);
	}

	@Override
	public boolean containsColumn(C identifier) {
		return columns.containsKey(requireNonNull(identifier));
	}

	@Override
	public State visible(C identifier) {
		return validateColumn(requireNonNull(identifier));
	}

	@Override
	public C identifier(int columnModelIndex) {
		C identifier = columnIdentifiers.get(columnModelIndex);
		if (identifier != null) {
			return identifier;
		}

		throw new IllegalArgumentException("Column at model index not found: " + columnModelIndex);
	}

	@Override
	public void resetColumns() {
		setVisibleColumns(new ArrayList<>(columns.keySet()));
	}

	/* TableColumnModel implementation begins */

	@Override
	public void addColumn(TableColumn column) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeColumn(TableColumn column) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void moveColumn(int fromIndex, int toIndex) {
		tableColumnModel.moveColumn(fromIndex, toIndex);
	}

	@Override
	public void setColumnMargin(int columnMargin) {
		tableColumnModel.setColumnMargin(columnMargin);
	}

	@Override
	public int getColumnCount() {
		return tableColumnModel.getColumnCount();
	}

	@Override
	public Enumeration<TableColumn> getColumns() {
		return tableColumnModel.getColumns();
	}

	@Override
	public int getColumnIndex(Object columnIdentifier) {
		return tableColumnModel.getColumnIndex(columnIdentifier);
	}

	@Override
	public FilterTableColumn<C> getColumn(int columnIndex) {
		return (FilterTableColumn<C>) tableColumnModel.getColumn(columnIndex);
	}

	@Override
	public int getColumnMargin() {
		return tableColumnModel.getColumnMargin();
	}

	@Override
	public int getColumnIndexAtX(int xPosition) {
		return tableColumnModel.getColumnIndexAtX(xPosition);
	}

	@Override
	public int getTotalColumnWidth() {
		return tableColumnModel.getTotalColumnWidth();
	}

	@Override
	public void setColumnSelectionAllowed(boolean columnSelectionAllowed) {
		tableColumnModel.setColumnSelectionAllowed(columnSelectionAllowed);
	}

	@Override
	public boolean getColumnSelectionAllowed() {
		return tableColumnModel.getColumnSelectionAllowed();
	}

	@Override
	public int[] getSelectedColumns() {
		return tableColumnModel.getSelectedColumns();
	}

	@Override
	public int getSelectedColumnCount() {
		return tableColumnModel.getSelectedColumnCount();
	}

	@Override
	public void setSelectionModel(ListSelectionModel listSelectionModel) {
		tableColumnModel.setSelectionModel(listSelectionModel);
	}

	@Override
	public ListSelectionModel getSelectionModel() {
		return tableColumnModel.getSelectionModel();
	}

	@Override
	public void addColumnModelListener(TableColumnModelListener listener) {
		tableColumnModel.addColumnModelListener(listener);
	}

	@Override
	public void removeColumnModelListener(TableColumnModelListener listener) {
		tableColumnModel.removeColumnModelListener(listener);
	}

	/* TableColumnModel implementation ends */

	@Override
	public Observer<C> columnHidden() {
		return columnHidden.observer();
	}

	@Override
	public Observer<C> columnShown() {
		return columnShown.observer();
	}

	private void initializeColumn(FilterTableColumn<C> column) {
		C identifier = column.identifier();
		columns.put(identifier, column);
		columnIdentifiers.put(column.getModelIndex(), identifier);
		tableColumnModel.addColumn(column);
		visibleStates.put(identifier, createVisibleState(identifier));
	}

	private State createVisibleState(C identifier) {
		State visibleState = State.state(true);
		visibleState.addValidator(value -> checkIfLocked());
		visibleState.addConsumer(visible -> setColumnVisible(identifier, visible));

		return visibleState;
	}

	private State validateColumn(C identifier) {
		State visibleState = visibleStates.get(identifier);
		if (visibleState == null) {
			throw new IllegalArgumentException("Column not found: " + identifier);
		}

		return visibleState;
	}

	private void setColumnVisible(C identifier, boolean visible) {
		if (visible) {
			showColumn(identifier);
		}
		else {
			hideColumn(identifier);
		}
	}

	private void showColumn(C identifier) {
		HiddenColumn column = hiddenColumns.get(identifier);
		if (column != null) {
			hiddenColumns.remove(identifier);
			tableColumnModel.addColumn(column.column);
			tableColumnModel.moveColumn(getColumnCount() - 1, column.indexWhenShown());
			columnShown.accept(identifier);
		}
	}

	private void hideColumn(C identifier) {
		if (!hiddenColumns.containsKey(identifier)) {
			HiddenColumn hiddenColumn = new HiddenColumn(column(identifier));
			hiddenColumns.put(identifier, hiddenColumn);
			tableColumnModel.removeColumn(hiddenColumn.column);
			columnHidden.accept(identifier);
		}
	}

	private void checkIfLocked() {
		if (locked.get()) {
			throw new IllegalStateException("Column model is locked");
		}
	}

	private final class HiddenColumn {

		private final FilterTableColumn<C> column;
		private final Set<FilterTableColumn<C>> columnsToTheRight;

		private HiddenColumn(FilterTableColumn<C> column) {
			this.column = column;
			this.columnsToTheRight = columnsToTheRightOf(column);
		}

		private Set<FilterTableColumn<C>> columnsToTheRightOf(FilterTableColumn<C> column) {
			return IntStream.range(tableColumnModel.getColumnIndex(column.identifier()) + 1, tableColumnModel.getColumnCount())
							.mapToObj(columnIndex -> (FilterTableColumn<C>) tableColumnModel.getColumn(columnIndex))
							.collect(Collectors.toSet());
		}

		private int indexWhenShown() {
			for (int i = 0; i < tableColumnModel.getColumnCount(); i++) {
				if (columnsToTheRight.contains(tableColumnModel.getColumn(i))) {
					return i;
				}
			}

			return tableColumnModel.getColumnCount() - 1;
		}
	}
}
