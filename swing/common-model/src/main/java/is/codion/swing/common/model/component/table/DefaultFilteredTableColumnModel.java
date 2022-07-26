/*
 * Copyright (c) 2013 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.component.table;

import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.state.State;

import javax.swing.ListSelectionModel;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

final class DefaultFilteredTableColumnModel<C> implements FilteredTableColumnModel<C> {

  private static final String COLUMN_IDENTIFIER = "columnIdentifier";

  private final DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
  private final Event<C> columnHiddenEvent = Event.event();
  private final Event<C> columnShownEvent = Event.event();
  private final Map<C, TableColumn> columns = new LinkedHashMap<>();
  private final Map<Integer, C> columnIdentifiers = new HashMap<>();
  private final Map<C, HiddenColumn> hiddenColumns = new LinkedHashMap<>();
  private final State lockedState = State.state();

  /**
   * Instantiates a new DefaultFilteredTableColumnModel.
   * @param tableColumns the columns to base this model on
   */
  DefaultFilteredTableColumnModel(List<TableColumn> tableColumns) {
    if (requireNonNull(tableColumns, "columns").isEmpty()) {
      throw new IllegalArgumentException("One or more columns must be specified");
    }
    tableColumns.forEach(column -> {
      C identifier = (C) column.getIdentifier();
      columns.put(identifier, column);
      columnIdentifiers.put(column.getModelIndex(), identifier);
      tableColumnModel.addColumn(column);
    });
  }

  @Override
  public Collection<TableColumn> getAllColumns() {
    return unmodifiableCollection(columns.values());
  }

  @Override
  public State getLockedState() {
    return lockedState;
  }

  @Override
  public boolean setColumnVisible(C columnIdentifier, boolean visible) {
    checkIfLocked();
    if (visible) {
      return showColumn(columnIdentifier);
    }

    return hideColumn(columnIdentifier);
  }

  @Override
  public boolean isColumnVisible(C columnIdentifier) {
    return !hiddenColumns.containsKey(requireNonNull(columnIdentifier, COLUMN_IDENTIFIER));
  }

  @Override
  public void setColumns(C... columnIdentifiers) {
    setColumns(asList(columnIdentifiers));
  }

  @Override
  public void setColumns(List<C> columnIdentifiers) {
    requireNonNull(columnIdentifiers);
    checkIfLocked();
    int columnIndex = 0;
    for (C identifier : columnIdentifiers) {
      showColumn(identifier);
      moveColumn(getColumnIndex(identifier), columnIndex++);
    }
    for (TableColumn column : getAllColumns()) {
      if (!columnIdentifiers.contains(column.getIdentifier())) {
        hideColumn((C) column.getIdentifier());
      }
    }
  }

  @Override
  public List<TableColumn> getVisibleColumns() {
    return unmodifiableList(Collections.list(tableColumnModel.getColumns()));
  }

  @Override
  public Collection<TableColumn> getHiddenColumns() {
    return unmodifiableCollection(hiddenColumns.values().stream()
            .map(hiddenColumn -> hiddenColumn.column)
            .collect(Collectors.toList()));
  }

  @Override
  public TableColumn getTableColumn(C columnIdentifier) {
    TableColumn column = columns.get(requireNonNull(columnIdentifier, COLUMN_IDENTIFIER));
    if (column != null) {
      return column;
    }

    throw new IllegalArgumentException("Column not found: " + columnIdentifier);
  }

  @Override
  public boolean containsColumn(C columnIdentifier) {
    return columns.containsKey(requireNonNull(columnIdentifier, COLUMN_IDENTIFIER));
  }

  @Override
  public C getColumnIdentifier(int columnModelIndex) {
    C identifier = columnIdentifiers.get(columnModelIndex);
    if (identifier != null) {
      return identifier;
    }

    throw new IllegalArgumentException("Column at model index not found: " + columnModelIndex);
  }

  @Override
  public void resetColumns() {
    setColumns(new ArrayList<>(columns.keySet()));
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
  public TableColumn getColumn(int columnIndex) {
    return tableColumnModel.getColumn(columnIndex);
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
  public void addColumnHiddenListener(EventDataListener<C> listener) {
    columnHiddenEvent.addDataListener(listener);
  }

  @Override
  public void removeColumnHiddenListener(EventDataListener<C> listener) {
    columnHiddenEvent.removeDataListener(listener);
  }

  @Override
  public void addColumnShownListener(EventDataListener<C> listener) {
    columnShownEvent.addDataListener(listener);
  }

  @Override
  public void removeColumnShownListener(EventDataListener<C> listener) {
    columnShownEvent.removeDataListener(listener);
  }

  private boolean showColumn(C columnIdentifier) {
    checkIfLocked();
    HiddenColumn column = hiddenColumns.get(requireNonNull(columnIdentifier, COLUMN_IDENTIFIER));
    if (column != null) {
      hiddenColumns.remove(columnIdentifier);
      tableColumnModel.addColumn(column.column);
      tableColumnModel.moveColumn(getColumnCount() - 1, column.getIndexWhenShown());
      columnShownEvent.onEvent(columnIdentifier);

      return true;
    }

    return false;
  }

  private boolean hideColumn(C columnIdentifier) {
    checkIfLocked();
    if (!hiddenColumns.containsKey(requireNonNull(columnIdentifier, COLUMN_IDENTIFIER))) {
      HiddenColumn hiddenColumn = new HiddenColumn(getTableColumn(columnIdentifier));
      hiddenColumns.put(columnIdentifier, hiddenColumn);
      tableColumnModel.removeColumn(hiddenColumn.column);
      columnHiddenEvent.onEvent(columnIdentifier);

      return true;
    }

    return false;
  }

  private void checkIfLocked() {
    if (lockedState.get()) {
      throw new IllegalStateException("Column model is locked");
    }
  }

  private final class HiddenColumn {

    private final TableColumn column;
    private final Set<TableColumn> columnsToTheRight;

    private HiddenColumn(TableColumn column) {
      this.column = column;
      this.columnsToTheRight = getColumnsToTheRightOf(column);
    }

    private Set<TableColumn> getColumnsToTheRightOf(TableColumn column) {
      Set<TableColumn> set = new HashSet<>();
      for (int i = tableColumnModel.getColumnIndex(column.getIdentifier()) + 1; i < tableColumnModel.getColumnCount(); i++) {
        set.add(tableColumnModel.getColumn(i));
      }

      return set;
    }

    private int getIndexWhenShown() {
      for (int i = 0; i < tableColumnModel.getColumnCount(); i++) {
        if (columnsToTheRight.contains(tableColumnModel.getColumn(i))) {
          return i;
        }
      }

      return tableColumnModel.getColumnCount() - 1;
    }
  }
}
