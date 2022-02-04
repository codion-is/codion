/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.table;

import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.state.State;

import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import java.util.Collection;
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

/**
 * A TableColumnModel handling hidden columns
 * @param <C> the type of column identifier
 */
public final class SwingFilteredTableColumnModel<C> extends DefaultTableColumnModel implements FilteredTableColumnModel<C> {

  private static final String COLUMN_IDENTIFIER = "columnIdentifier";

  private final Event<C> columnHiddenEvent = Event.event();
  private final Event<C> columnShownEvent = Event.event();

  /**
   * All columns in this column model, visible and hidden
   */
  private final Map<C, TableColumn> columns = new LinkedHashMap<>();
  /**
   * All column identifiers mapped to their respective column model index
   */
  private final Map<Integer, C> columnIdentifiers = new HashMap<>();
  /**
   * Contains columns that have been hidden
   */
  private final Map<C, HiddenColumn> hiddenColumns = new LinkedHashMap<>();
  /**
   * A lock which prevents adding or removing columns from this column model
   */
  private final State lockedState = State.state();

  /**
   * Instantiates a new SwingFilteredTableColumnModel.
   * @param tableColumns the columns to base this model on
   */
  public SwingFilteredTableColumnModel(final List<TableColumn> tableColumns) {
    if (requireNonNull(tableColumns, "columns").isEmpty()) {
      throw new IllegalArgumentException("One or more columns must be specified");
    }
    tableColumns.forEach(column -> {
      final C identifier = (C) column.getIdentifier();
      columns.put(identifier, column);
      columnIdentifiers.put(column.getModelIndex(), identifier);
      addColumn(column);
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
  public boolean setColumnVisible(final C columnIdentifier, final boolean visible) {
    checkIfLocked();
    if (visible) {
      return showColumn(columnIdentifier);
    }

    return hideColumn(columnIdentifier);
  }

  @Override
  public boolean isColumnVisible(final C columnIdentifier) {
    return !hiddenColumns.containsKey(requireNonNull(columnIdentifier, COLUMN_IDENTIFIER));
  }

  @Override
  public void setColumns(final C... columnIdentifiers) {
    setColumns(asList(columnIdentifiers));
  }

  @Override
  public void setColumns(final List<C> columnIdentifiers) {
    requireNonNull(columnIdentifiers);
    checkIfLocked();
    int columnIndex = 0;
    for (final C identifier : columnIdentifiers) {
      showColumn(identifier);
      moveColumn(getColumnIndex(identifier), columnIndex++);
    }
    for (final TableColumn column : getAllColumns()) {
      if (!columnIdentifiers.contains(column.getIdentifier())) {
        hideColumn((C) column.getIdentifier());
      }
    }
  }

  @Override
  public List<C> getVisibleColumns() {
    return unmodifiableList(tableColumns.stream()
            .map(column -> (C) column.getIdentifier())
            .collect(Collectors.toList()));
  }

  @Override
  public Collection<C> getHiddenColumns() {
    return unmodifiableCollection(hiddenColumns.keySet());
  }

  @Override
  public TableColumn getTableColumn(final C columnIdentifier) {
    final TableColumn column = columns.get(requireNonNull(columnIdentifier, COLUMN_IDENTIFIER));
    if (column != null) {
      return column;
    }

    throw new IllegalArgumentException("Column not found: " + columnIdentifier);
  }

  @Override
  public boolean containsColumn(final C columnIdentifier) {
    return columns.containsKey(requireNonNull(columnIdentifier, COLUMN_IDENTIFIER));
  }

  @Override
  public C getColumnIdentifier(final int columnModelIndex) {
    final C identifier = columnIdentifiers.get(columnModelIndex);
    if (identifier != null) {
      return identifier;
    }

    throw new IllegalArgumentException("Column at model index not found: " + columnModelIndex);
  }

  @Override
  public void addColumnHiddenListener(final EventDataListener<C> listener) {
    columnHiddenEvent.addDataListener(listener);
  }

  @Override
  public void removeColumnHiddenListener(final EventDataListener<C> listener) {
    columnHiddenEvent.removeDataListener(listener);
  }

  @Override
  public void addColumnShownListener(final EventDataListener<C> listener) {
    columnShownEvent.addDataListener(listener);
  }

  @Override
  public void removeColumnShownListener(final EventDataListener<C> listener) {
    columnShownEvent.removeDataListener(listener);
  }

  private boolean showColumn(final C columnIdentifier) {
    checkIfLocked();
    final HiddenColumn column = hiddenColumns.get(requireNonNull(columnIdentifier, COLUMN_IDENTIFIER));
    if (column != null) {
      hiddenColumns.remove(columnIdentifier);
      addColumn(column.column);
      moveColumn(getColumnCount() - 1, column.getIndexWhenShown());
      columnShownEvent.onEvent(columnIdentifier);

      return true;
    }

    return false;
  }

  private boolean hideColumn(final C columnIdentifier) {
    checkIfLocked();
    if (!hiddenColumns.containsKey(requireNonNull(columnIdentifier, COLUMN_IDENTIFIER))) {
      final HiddenColumn hiddenColumn = new HiddenColumn(getTableColumn(columnIdentifier));
      hiddenColumns.put(columnIdentifier, hiddenColumn);
      removeColumn(hiddenColumn.column);
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

    private HiddenColumn(final TableColumn column) {
      this.column = column;
      this.columnsToTheRight = getColumnsToTheRightOf(column);
    }

    private Set<TableColumn> getColumnsToTheRightOf(final TableColumn column) {
      final Set<TableColumn> set = new HashSet<>();
      for (int i = tableColumns.indexOf(column) + 1; i < tableColumns.size(); i++) {
        set.add(tableColumns.get(i));
      }

      return set;
    }

    private int getIndexWhenShown() {
      for (int i = 0; i < tableColumns.size(); i++) {
        if (columnsToTheRight.contains(tableColumns.get(i))) {
          return i;
        }
      }

      return tableColumns.size() - 1;
    }
  }
}
