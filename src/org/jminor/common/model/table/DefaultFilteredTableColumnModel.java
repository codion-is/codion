/*
 * Copyright (c) 2004 - 2013, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.table;

import org.jminor.common.model.Event;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.Events;
import org.jminor.common.model.Util;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A TableColumnModel handling hidden columns
 * @param <C> the type of column identifier
 */
public class DefaultFilteredTableColumnModel<C> extends DefaultTableColumnModel implements FilteredTableColumnModel<C> {

  private final Event columnHiddenEvent = Events.event();
  private final Event columnShownEvent = Events.event();

  /**
   * The columns available to this table model
   */
  private final List<TableColumn> columns;

  /**
   * Contains columns that have been hidden
   */
  private final Map<C, TableColumn> hiddenColumns = new HashMap<C, TableColumn>();

  /**
   * The ColumnSearchModels used for filtering
   */
  private final Map<C, ColumnSearchModel<C>> columnFilterModels = new HashMap<C, ColumnSearchModel<C>>();

  /**
   * Caches the column indexes in the model
   */
  private final int[] columnIndexCache;

  /**
   * Instantiates a new DefaultFilteredTableColumnModel, note that the TableColumnModel
   * this model is to be based on must contain all the columns when this constructor is called
   * @param columns the columns to base this model on
   * @param columnFilterModels the filter models if any
   */
  public DefaultFilteredTableColumnModel(final List<TableColumn> columns, final Collection<? extends ColumnSearchModel<C>> columnFilterModels) {
    if (columns == null || columns.isEmpty()) {
      throw new IllegalArgumentException("One or more columns must be specified");
    }
    this.columns = Collections.unmodifiableList(columns);
    this.columnIndexCache = new int[columns.size()];
    Arrays.fill(this.columnIndexCache, -1);
    for (final TableColumn column : columns) {
      addColumn(column);
    }
    if (columnFilterModels != null) {
      for (final ColumnSearchModel<C> columnFilterModel : columnFilterModels) {
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
  @SuppressWarnings({"unchecked"})
  public final void setColumnVisible(final C columnIdentifier, final boolean visible) {
    if (visible) {
      final TableColumn column = hiddenColumns.get(columnIdentifier);
      if (column != null) {
        hiddenColumns.remove(columnIdentifier);
        addColumn(column);
        moveColumn(getColumnCount() - 1, 0);
        columnShownEvent.fire(column.getIdentifier());
      }
    }
    else {
      if (!hiddenColumns.containsKey(columnIdentifier)) {
        final TableColumn column = getTableColumn(columnIdentifier);
        removeColumn(column);
        hiddenColumns.put((C) column.getIdentifier(), column);
        columnHiddenEvent.fire(column.getIdentifier());
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public final boolean isColumnVisible(final C columnIdentifier) {
    return !hiddenColumns.containsKey(columnIdentifier);
  }

  /** {@inheritDoc} */
  @Override
  public final List<TableColumn> getHiddenColumns() {
    return new ArrayList<TableColumn>(hiddenColumns.values());
  }

  /** {@inheritDoc} */
  @Override
  public final TableColumn getTableColumn(final C identifier) {
    Util.rejectNullValue(identifier, "identifier");
    final Enumeration<TableColumn> visibleColumns = getColumns();
    while (visibleColumns.hasMoreElements()) {
      final TableColumn column = visibleColumns.nextElement();
      if (identifier.equals(column.getIdentifier())) {
        return column;
      }
    }

    throw new IllegalArgumentException("Column not found: " + identifier);
  }

  /** {@inheritDoc} */
  @Override
  public final ColumnSearchModel<C> getFilterModel(final C columnIdentifier) {
    return columnFilterModels.get(columnIdentifier);
  }

  /** {@inheritDoc} */
  @Override
  public Collection<ColumnSearchModel<C>> getColumnFilterModels() {
    return columnFilterModels.values();
  }

  /** {@inheritDoc} */
  @Override
  @SuppressWarnings({"unchecked"})
  public final C getColumnIdentifier(final int modelColumnIndex) {
    return (C) getColumn(convertColumnIndexToView(modelColumnIndex)).getIdentifier();
  }

  /** {@inheritDoc} */
  @Override
  public final void addColumnHiddenListener(final EventListener<C> listener) {
    columnHiddenEvent.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeColumnHiddenListener(final EventListener<C> listener) {
    columnHiddenEvent.removeListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void addColumnShownListener(final EventListener<C> listener) {
    columnShownEvent.addListener(listener);
  }

  /** {@inheritDoc} */
  @Override
  public final void removeColumnShownListener(final EventListener<C> listener) {
    columnShownEvent.removeListener(listener);
  }

  /**
   * Converts the index of the column in the table model at
   * <code>modelColumnIndex</code> to the index of the column
   * in the view. Returns the index of the
   * corresponding column in the view; returns -1 if this column is not
   * being displayed.  If <code>modelColumnIndex</code> is less than zero,
   * this returns <code>modelColumnIndex</code>.
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
      /** {@inheritDoc} */
      @Override
      public void columnAdded(final TableColumnModelEvent e) {
        Arrays.fill(columnIndexCache, -1);
      }
      /** {@inheritDoc} */
      @Override
      public void columnRemoved(final TableColumnModelEvent e) {
        Arrays.fill(columnIndexCache, -1);
      }
      /** {@inheritDoc} */
      @Override
      public void columnMoved(final TableColumnModelEvent e) {
        Arrays.fill(columnIndexCache, -1);
      }
      /** {@inheritDoc} */
      @Override
      public void columnMarginChanged(final ChangeEvent e) {}
      /** {@inheritDoc} */
      @Override
      public void columnSelectionChanged(final ListSelectionEvent e) {}
    });
  }
}
