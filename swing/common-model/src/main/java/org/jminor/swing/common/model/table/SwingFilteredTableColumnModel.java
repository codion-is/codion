/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.model.table;

import org.jminor.common.event.Event;
import org.jminor.common.event.EventDataListener;
import org.jminor.common.event.Events;
import org.jminor.common.model.table.ColumnConditionModel;
import org.jminor.common.model.table.FilteredTableColumnModel;

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
 * @param <C> the type of column identifier
 */
public class SwingFilteredTableColumnModel<C> extends DefaultTableColumnModel implements FilteredTableColumnModel<C, TableColumn> {

  private final Event<C> columnHiddenEvent = Events.event();
  private final Event<C> columnShownEvent = Events.event();

  /**
   * The columns available to this table model
   */
  private final List<TableColumn> columns;

  /**
   * Contains columns that have been hidden
   */
  private final Map<C, TableColumn> hiddenColumns = new HashMap<>();

  /**
   * The ColumnConditionModels used for filtering
   */
  private final Map<C, ColumnConditionModel<C>> columnFilterModels = new HashMap<>();

  /**
   * Caches the column indexes in the model
   */
  private final int[] columnIndexCache;

  /**
   * Instantiates a new SwingFilteredTableColumnModel, note that the TableColumnModel
   * this model is to be based on must contain all the columns when this constructor is called
   * @param columns the columns to base this model on
   * @param columnFilterModels the filter models if any
   */
  public SwingFilteredTableColumnModel(final List<TableColumn> columns,
                                       final Collection<? extends ColumnConditionModel<C>> columnFilterModels) {
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
      for (final ColumnConditionModel<C> columnFilterModel : columnFilterModels) {
        this.columnFilterModels.put(columnFilterModel.getColumnIdentifier(), columnFilterModel);
      }
    }
    bindEvents();
  }

  /** {@inheritDoc} */
  @Override
  public final List<TableColumn> getAllColumns() {
    return columns;
  }

  /** {@inheritDoc} */
  @Override
  public final void showColumn(final C columnIdentifier) {
    final TableColumn column = hiddenColumns.get(columnIdentifier);
    if (column != null) {
      hiddenColumns.remove(columnIdentifier);
      addColumn(column);
      moveColumn(getColumnCount() - 1, 0);
      columnShownEvent.onEvent((C) column.getIdentifier());
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void hideColumn(final C columnIdentifier) {
    if (!hiddenColumns.containsKey(columnIdentifier)) {
      final TableColumn column = getTableColumn(columnIdentifier);
      removeColumn(column);
      hiddenColumns.put((C) column.getIdentifier(), column);
      columnHiddenEvent.onEvent((C) column.getIdentifier());
    }
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isColumnVisible(final C columnIdentifier) {
    return !hiddenColumns.containsKey(columnIdentifier);
  }

  /** {@inheritDoc} */
  @Override
  public final void setColumns(final C... columnIdentifiers) {
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

  /** {@inheritDoc} */
  @Override
  public final List<TableColumn> getHiddenColumns() {
    return new ArrayList<>(hiddenColumns.values());
  }

  /** {@inheritDoc} */
  @Override
  public final TableColumn getTableColumn(final C identifier) {
    requireNonNull(identifier, "identifier");
    for (final TableColumn column : columns) {
      if (identifier.equals(column.getIdentifier())) {
        return column;
      }
    }

    throw new IllegalArgumentException("Column not found: " + identifier);
  }

  /** {@inheritDoc} */
  @Override
  public final boolean containsColumn(final C identifier) {
    requireNonNull(identifier, "identifier");

    return columns.stream().anyMatch(column -> identifier.equals(column.getIdentifier()));
  }

  /** {@inheritDoc} */
  @Override
  public final ColumnConditionModel<C> getColumnFilterModel(final C columnIdentifier) {
    return columnFilterModels.get(columnIdentifier);
  }

  /** {@inheritDoc} */
  @Override
  public final Collection<ColumnConditionModel<C>> getColumnFilterModels() {
    return columnFilterModels.values();
  }

  /** {@inheritDoc} */
  @Override
  public final C getColumnIdentifier(final int columnModelIndex) {
    return (C) getColumn(convertColumnIndexToView(columnModelIndex)).getIdentifier();
  }

  /** {@inheritDoc} */
  @Override
  public final void addColumnHiddenListener(final EventDataListener<C> listener) {
    columnHiddenEvent.addDataListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeColumnHiddenListener(final EventDataListener<C> listener) {
    columnHiddenEvent.removeDataListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addColumnShownListener(final EventDataListener<C> listener) {
    columnShownEvent.addDataListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeColumnShownListener(final EventDataListener<C> listener) {
    columnShownEvent.removeDataListener(listener);
  }

  /**
   * Converts the index of the column in the table model at
   * {@code modelColumnIndex} to the index of the column
   * in the view. Returns the index of the
   * corresponding column in the view; returns -1 if this column is not
   * being displayed.  If {@code modelColumnIndex} is less than zero,
   * this returns {@code modelColumnIndex}.
   * @param modelColumnIndex the index of the column in the model
   * @return the index of the corresponding column in the view
   */
  protected final int convertColumnIndexToView(final int modelColumnIndex) {
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
