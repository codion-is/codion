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

import is.codion.common.reactive.event.Event;
import is.codion.common.reactive.observer.Observer;
import is.codion.common.reactive.state.ObservableState;
import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.ObservableValueList;
import is.codion.common.reactive.value.Value;
import is.codion.common.reactive.value.ValueList;
import is.codion.swing.common.ui.component.table.FilterTableColumnModel.ColumnSelection.ColumnIndex;

import org.jspecify.annotations.Nullable;

import javax.swing.DefaultListSelectionModel;
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

import static java.text.MessageFormat.format;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

final class DefaultFilterTableColumnModel<C> implements FilterTableColumnModel<C> {

	private final DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
	private final Event<C> columnHidden = Event.event();
	private final Event<C> columnShown = Event.event();
	private final Map<C, FilterTableColumn<C>> columns;
	private final Map<Integer, C> columnIdentifiers = new HashMap<>();
	private final Map<C, HiddenColumn> hiddenColumnMap = new LinkedHashMap<>();
	private final Map<C, State> visibleStates = new HashMap<>();
	private final State locked = State.state();
	private final DefaultVisibleColumns visibleColumns = new DefaultVisibleColumns();
	private final DefaultHiddenColumns hiddenColumns = new DefaultHiddenColumns();
	private final DefaultColumnSelection columnSelection = new DefaultColumnSelection();

	DefaultFilterTableColumnModel(List<FilterTableColumn<C>> tableColumns) {
		if (requireNonNull(tableColumns).isEmpty()) {
			throw new IllegalArgumentException("One or more columns must be specified");
		}
		this.columns = initializeColumns(tableColumns);
		this.tableColumnModel.setSelectionModel(columnSelection);
	}

	@Override
	public Collection<FilterTableColumn<C>> columns() {
		return columns.values();
	}

	@Override
	public Collection<C> identifiers() {
		return columns.keySet();
	}

	@Override
	public State locked() {
		return locked;
	}

	@Override
	public VisibleColumns<C> visible() {
		return visibleColumns;
	}

	@Override
	public HiddenColumns<C> hidden() {
		return hiddenColumns;
	}

	@Override
	public ColumnSelection<C> selection() {
		return columnSelection;
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
	public boolean contains(C identifier) {
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
	public void reset() {
		visibleColumns.set(new ArrayList<>(columns.keySet()));
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
		throw new UnsupportedOperationException("FilterTableColumnModel selection model can not be changed");
	}

	@Override
	public ColumnSelection<C> getSelectionModel() {
		return (ColumnSelection<C>) tableColumnModel.getSelectionModel();
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

	private Map<C, FilterTableColumn<C>> initializeColumns(List<FilterTableColumn<C>> tableColumns) {
		Map<C, FilterTableColumn<C>> columnMap = new LinkedHashMap<>();
		for (FilterTableColumn<C> column : tableColumns) {
			C identifier = column.identifier();
			if (columnMap.containsKey(identifier)) {
				throw new IllegalArgumentException(format("Column with identifier {0} already exists", identifier));
			}
			int modelIndex = column.getModelIndex();
			if (columnIdentifiers.containsKey(modelIndex)) {
				throw new IllegalArgumentException(format("Column with model index {0} already exists as {1}",
								modelIndex, columnIdentifiers.get(modelIndex)));
			}
			columnMap.put(identifier, column);
			columnIdentifiers.put(column.getModelIndex(), identifier);
			tableColumnModel.addColumn(column);
			visibleStates.put(identifier, createVisibleState(identifier));
		}

		return unmodifiableMap(columnMap);
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
		HiddenColumn hiddenColumn = hiddenColumnMap.remove(identifier);
		if (hiddenColumn != null) {
			tableColumnModel.addColumn(hiddenColumn.column);
			tableColumnModel.moveColumn(getColumnCount() - 1, hiddenColumn.indexWhenShown());
			columnShown.accept(identifier);
		}
	}

	private void hideColumn(C identifier) {
		if (!hiddenColumnMap.containsKey(identifier)) {
			FilterTableColumn<C> column = column(identifier);
			hiddenColumnMap.put(identifier, new HiddenColumn(column));
			tableColumnModel.removeColumn(column);
			columnHidden.accept(identifier);
		}
	}

	private void checkIfLocked() {
		if (locked.is()) {
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

	private final class DefaultVisibleColumns implements VisibleColumns<C> {

		private final Event<List<C>> event = Event.event();

		private DefaultVisibleColumns() {
			columnHidden.addListener(this::changed);
			columnShown.addListener(this::changed);
		}

		@Override
		public void set(C... identifiers) {
			set(asList(identifiers));
		}

		@Override
		public void set(List<C> identifiers) {
			requireNonNull(identifiers);
			identifiers.forEach(DefaultFilterTableColumnModel.this::validateColumn);
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
		public List<C> get() {
			return columns().stream()
							.map(FilterTableColumn::identifier)
							.collect(toList());
		}

		@Override
		public List<FilterTableColumn<C>> columns() {
			List<FilterTableColumn<C>> tableColumns = new ArrayList<>(tableColumnModel.getColumnCount());
			Enumeration<TableColumn> columnEnumeration = tableColumnModel.getColumns();
			while (columnEnumeration.hasMoreElements()) {
				tableColumns.add((FilterTableColumn<C>) columnEnumeration.nextElement());
			}

			return unmodifiableList(tableColumns);
		}

		@Override
		public Observer<List<C>> observer() {
			return event.observer();
		}

		private void changed() {
			event.accept(get());
		}
	}

	private final class DefaultHiddenColumns implements HiddenColumns<C> {

		private final Event<Collection<C>> event = Event.event();

		private DefaultHiddenColumns() {
			columnHidden.addListener(this::changed);
			columnShown.addListener(this::changed);
		}

		@Override
		public Collection<C> get() {
			return unmodifiableCollection(hiddenColumnMap.keySet());
		}

		@Override
		public Observer<Collection<C>> observer() {
			return event.observer();
		}

		@Override
		public Collection<FilterTableColumn<C>> columns() {
			return unmodifiableCollection(hiddenColumnMap.values().stream()
							.map(hiddenColumn -> hiddenColumn.column)
							.collect(toList()));
		}

		private void changed() {
			event.accept(get());
		}
	}

	private final class DefaultColumnSelection extends DefaultListSelectionModel implements ColumnSelection<C> {

		private final ValueList<Integer> indexes = ValueList.valueList();
		private final ValueList<C> identifiers = ValueList.valueList();
		private final DefaultColumnIndex anchor = new DefaultColumnIndex();
		private final DefaultColumnIndex lead = new DefaultColumnIndex();
		private final ObservableState empty = State.present(indexes).not();

		@Override
		public ObservableValueList<Integer> indexes() {
			return indexes.observable();
		}

		@Override
		public ObservableValueList<C> identifiers() {
			return identifiers.observable();
		}

		@Override
		public ObservableState empty() {
			return empty;
		}

		@Override
		public ColumnIndex anchor() {
			return anchor;
		}

		@Override
		public ColumnIndex lead() {
			return lead;
		}

		@Override
		protected void fireValueChanged(int firstIndex, int lastIndex, boolean isAdjusting) {
			super.fireValueChanged(firstIndex, lastIndex, isAdjusting);
			if (!isAdjusting) {
				List<Integer> selectedIndexes = IntStream.of(getSelectedIndices())
								.boxed()
								.collect(toList());
				indexes.set(selectedIndexes);
				identifiers.set(selectedIndexes.stream()
								.map(DefaultFilterTableColumnModel.this::identifier)
								.collect(toList()));
				anchor.set(getAnchorSelectionIndex());
				lead.set(getLeadSelectionIndex());
			}
		}
	}

	private static final class DefaultColumnIndex implements ColumnIndex {

		private final Value<Integer> index = Value.nullable();
		private final ObservableState present = State.present(index);

		@Override
		public ObservableState present() {
			return present;
		}

		@Override
		public @Nullable Integer get() {
			return index.get();
		}

		@Override
		public Observer<Integer> observer() {
			return index.observer();
		}

		private void set(int value) {
			index.set(value == -1 ? null : value);
		}
	}
}
