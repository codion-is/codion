/*
 * Copyright (c) 2004 - 2013, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.table;

import org.jminor.common.model.EventInfoListener;

import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.util.Collection;
import java.util.List;

/**
 * A TableColumnModel handling hidden columns
 * @param <C> the type of column identifier
 */
public interface FilteredTableColumnModel<C> extends TableColumnModel {

  /**
   * @return all columns in this model, both hidden and visible
   */
  List<TableColumn> getAllColumns();

  /**
   * @return an unmodifiable view of hidden table columns
   */
  Collection<TableColumn> getHiddenColumns();

  /**
   * @param columnIdentifier the key for which to query if its column is visible
   * @return true if the column is visible, false if it is hidden
   */
  boolean isColumnVisible(final C columnIdentifier);

  /**
   * Toggles the visibility of the column representing the given columnIdentifier.<br>
   * @param columnIdentifier the column identifier
   * @param visible if true the column is shown, otherwise it is hidden
   */
  void setColumnVisible(final C columnIdentifier, final boolean visible);

  /**
   * Arranges the columns so that only the given columns are visible and in the given order
   * @param columnIdentifiers the column identifiers
   */
  void setColumns(final C... columnIdentifiers);

  /**
   * Returns the TableColumn with the given identifier
   * @param identifier the column identifier
   * @return the TableColumn with the given identifier
   * @throws IllegalArgumentException in case this table model does not contain a column with the given identifier
   */
  TableColumn getTableColumn(final C identifier);

  /**
   * @param modelColumnIndex the column model index
   * @return the column identifier
   */
  C getColumnIdentifier(final int modelColumnIndex);

  /**
   * @param columnIdentifier the column identifier
   * @return the SearchModel at the given column index
   */
  ColumnSearchModel<C> getFilterModel(final C columnIdentifier);

  /**
   * @return the ColumnFilterModel instances
   */
  Collection<ColumnSearchModel<C>> getColumnFilterModels();

  /**
   * @param listener a listener to be notified each time a column is hidden
   */
  void addColumnHiddenListener(final EventInfoListener<C> listener);

  /**
   * @param listener the listener to remove
   */
  void removeColumnHiddenListener(final EventInfoListener<C> listener);

  /**
   * @param listener a listener to be notified each time a column is shown
   */
  void addColumnShownListener(final EventInfoListener<C> listener);

  /**
   * @param listener the listener to remove
   */
  void removeColumnShownListener(final EventInfoListener<C> listener);
}
