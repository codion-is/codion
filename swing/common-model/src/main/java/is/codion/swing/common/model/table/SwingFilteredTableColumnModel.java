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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
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
  private final Map<C, TableColumn> columns = new HashMap<>();
  /**
   * All column identifiers mapped to their respective column model index
   */
  private final Map<Integer, C> columnIdentifiers = new HashMap<>();
  /**
   * Contains columns that have been hidden
   */
  private final Map<C, TableColumn> hiddenColumns = new HashMap<>();

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
    return Collections.unmodifiableCollection(columns.values());
  }

  @Override
  public State getLockedState() {
    return lockedState;
  }

  @Override
  public void showColumn(final C columnIdentifier) {
    final TableColumn column = hiddenColumns.get(requireNonNull(columnIdentifier, COLUMN_IDENTIFIER));
    if (column != null) {
      checkIfLocked();
      hiddenColumns.remove(columnIdentifier);
      addColumn(column);
      moveColumn(getColumnCount() - 1, 0);
      columnShownEvent.onEvent(columnIdentifier);
    }
  }

  @Override
  public void hideColumn(final C columnIdentifier) {
    if (!hiddenColumns.containsKey(requireNonNull(columnIdentifier, COLUMN_IDENTIFIER))) {
      checkIfLocked();
      final TableColumn column = getTableColumn(columnIdentifier);
      removeColumn(column);
      hiddenColumns.put(columnIdentifier, column);
      columnHiddenEvent.onEvent(columnIdentifier);
    }
  }

  @Override
  public boolean isColumnVisible(final C columnIdentifier) {
    return !hiddenColumns.containsKey(requireNonNull(columnIdentifier, COLUMN_IDENTIFIER));
  }

  @Override
  public void setColumns(final C... columnIdentifiers) {
    requireNonNull(columnIdentifiers);
    checkIfLocked();
    final List<C> identifiers = asList(columnIdentifiers);
    int columnIndex = 0;
    for (final C identifier : identifiers) {
      showColumn(identifier);
      moveColumn(getColumnIndex(identifier), columnIndex++);
    }
    for (final TableColumn column : getAllColumns()) {
      if (!identifiers.contains(column.getIdentifier())) {
        hideColumn((C) column.getIdentifier());
      }
    }
  }

  @Override
  public Collection<TableColumn> getHiddenColumns() {
    return Collections.unmodifiableCollection(hiddenColumns.values());
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

  private void checkIfLocked() {
    if (lockedState.get()) {
      throw new IllegalStateException("Column model is locked");
    }
  }
}
