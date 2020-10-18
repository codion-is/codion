/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.table;

import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.Events;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.FilteredTableColumnModel;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Arrays.fill;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

/**
 * A TableColumnModel handling hidden columns
 * @param <R> the table model row type
 * @param <C> the type of column identifier
 */
public final class SwingFilteredTableColumnModel<R, C> extends DefaultTableColumnModel implements FilteredTableColumnModel<R, C, TableColumn> {

  private static final String COLUMN_IDENTIFIER = "columnIdentifier";

  private final Event<C> columnHiddenEvent = Events.event();
  private final Event<C> columnShownEvent = Events.event();

  /**
   * All columns in this column model, visible and hidden
   */
  private final List<TableColumn> columns;

  /**
   * Contains columns that have been hidden
   */
  private final Map<C, TableColumn> hiddenColumns = new HashMap<>();

  /**
   * The ColumnConditionModels used for filtering
   */
  private final Map<C, ColumnConditionModel<R, C, ?>> columnFilterModels = new HashMap<>();

  /**
   * Caches the column indexes in the model
   */
  private final int[] columnIndexCache;

  /**
   * Instantiates a new SwingFilteredTableColumnModel.
   * @param columns the columns to base this model on
   * @param columnFilterModels the filter models if any
   */
  public SwingFilteredTableColumnModel(final List<TableColumn> columns,
                                       final Collection<? extends ColumnConditionModel<R, C, ?>> columnFilterModels) {
    if (columns == null || columns.isEmpty()) {
      throw new IllegalArgumentException("One or more columns must be specified");
    }
    this.columns = unmodifiableList(columns);
    this.columnIndexCache = new int[columns.size()];
    fill(this.columnIndexCache, -1);
    for (final TableColumn column : columns) {
      addColumn(column);
    }
    if (columnFilterModels != null) {
      for (final ColumnConditionModel<R, C, ?> columnFilterModel : columnFilterModels) {
        this.columnFilterModels.put(columnFilterModel.getColumnIdentifier(), columnFilterModel);
      }
    }
    bindEvents();
  }

  @Override
  public List<TableColumn> getAllColumns() {
    return columns;
  }

  @Override
  public void showColumn(final C columnIdentifier) {
    final TableColumn column = hiddenColumns.get(columnIdentifier);
    if (column != null) {
      hiddenColumns.remove(columnIdentifier);
      addColumn(column);
      moveColumn(getColumnCount() - 1, 0);
      columnShownEvent.onEvent((C) column.getIdentifier());
    }
  }

  @Override
  public void hideColumn(final C columnIdentifier) {
    if (!hiddenColumns.containsKey(columnIdentifier)) {
      final TableColumn column = getTableColumn(columnIdentifier);
      removeColumn(column);
      hiddenColumns.put((C) column.getIdentifier(), column);
      columnHiddenEvent.onEvent((C) column.getIdentifier());
    }
  }

  @Override
  public boolean isColumnVisible(final C columnIdentifier) {
    return !hiddenColumns.containsKey(columnIdentifier);
  }

  @Override
  public void setColumns(final C... columnIdentifiers) {
    if (columnIdentifiers != null) {
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
  }

  @Override
  public List<TableColumn> getHiddenColumns() {
    return new ArrayList<>(hiddenColumns.values());
  }

  @Override
  public TableColumn getTableColumn(final C columnIdentifier) {
    requireNonNull(columnIdentifier, COLUMN_IDENTIFIER);
    for (final TableColumn column : columns) {
      if (columnIdentifier.equals(column.getIdentifier())) {
        return column;
      }
    }

    throw new IllegalArgumentException("Column not found: " + columnIdentifier);
  }

  @Override
  public boolean containsColumn(final C columnIdentifier) {
    requireNonNull(columnIdentifier, COLUMN_IDENTIFIER);

    return columns.stream().anyMatch(column -> columnIdentifier.equals(column.getIdentifier()));
  }

  @Override
  public <T> ColumnConditionModel<R, C, T> getColumnFilterModel(final C columnIdentifier) {
    requireNonNull(columnIdentifier, COLUMN_IDENTIFIER);

    return (ColumnConditionModel<R, C, T>) columnFilterModels.get(columnIdentifier);
  }

  @Override
  public Collection<ColumnConditionModel<R, C, ?>> getColumnFilterModels() {
    return columnFilterModels.values();
  }

  @Override
  public C getColumnIdentifier(final int columnModelIndex) {
    return (C) getColumn(convertColumnIndexToView(columnModelIndex)).getIdentifier();
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

  /**
   * Converts the index of the column in the table model at
   * {@code modelColumnIndex} to the index of the column
   * in the view. Returns the index of the
   * corresponding column in the view; returns -1 if this column is not
   * being displayed. If {@code modelColumnIndex} is less than zero,
   * this returns {@code modelColumnIndex}.
   * @param modelColumnIndex the index of the column in the model
   * @return the index of the corresponding column in the view
   */
  private int convertColumnIndexToView(final int modelColumnIndex) {
    if (modelColumnIndex < 0) {
      return modelColumnIndex;
    }

    final int cachedIndex = columnIndexCache[modelColumnIndex];
    if (cachedIndex >= 0) {
      return cachedIndex;
    }

    for (int index = 0; index < getColumnCount(); index++) {
      if (getColumn(index).getModelIndex() == modelColumnIndex) {
        columnIndexCache[modelColumnIndex] = index;
        return index;
      }
    }

    return -1;
  }

  private void bindEvents() {
    addColumnModelListener(new TableColumnModelListener() {
      @Override
      public void columnAdded(final TableColumnModelEvent e) {
        fill(columnIndexCache, -1);
      }
      @Override
      public void columnRemoved(final TableColumnModelEvent e) {
        fill(columnIndexCache, -1);
      }
      @Override
      public void columnMoved(final TableColumnModelEvent e) {
        fill(columnIndexCache, -1);
      }
      @Override
      public void columnMarginChanged(final ChangeEvent e) {/*Not required*/}
      @Override
      public void columnSelectionChanged(final ListSelectionEvent e) {/*Not required*/}
    });
  }
}
