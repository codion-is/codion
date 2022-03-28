/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.component.table;

import is.codion.common.event.EventDataListener;
import is.codion.common.state.State;

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
   * @return an unmodifiable view of all columns in this model, both hidden and visible, in no particular order
   */
  Collection<TableColumn> getAllColumns();

  /**
   * @return an unmodifiable view of the currently visible column identifiers
   */
  List<C> getVisibleColumns();

  /**
   * @return an unmodifiable view of hidden table column identifiers
   */
  Collection<C> getHiddenColumns();

  /**
   * Returns a {@link State} instance controlling whether this model is locked or not.
   * A locked column model does not allow adding or removing of columns, but columns can be reordered.
   * @return a {@link State} controlling whether this model is locked or not
   */
  State getLockedState();

  /**
   * @param columnIdentifier the columnd identifier
   * @param visible true if the column should be visible, false if it should be hidden
   * @return true if the column visibility changed
   * @throws IllegalStateException in case this model is locked
   * @see #getLockedState()
   */
  boolean setColumnVisible(C columnIdentifier, boolean visible);

  /**
   * @param columnIdentifier the key for which to query if its column is visible
   * @return true if the column is visible, false if it is hidden
   */
  boolean isColumnVisible(C columnIdentifier);

  /**
   * Arranges the columns so that only the given columns are visible and in the given order
   * @param columnIdentifiers the column identifiers
   */
  void setColumns(C... columnIdentifiers);

  /**
   * Arranges the columns so that only the given columns are visible and in the given order
   * @param columnIdentifiers the column identifiers
   */
  void setColumns(List<C> columnIdentifiers);

  /**
   * Returns the TableColumn with the given identifier
   * @param identifier the column identifier
   * @return the TableColumn with the given identifier
   * @throws IllegalArgumentException in case this table model does not contain a column with the given identifier
   */
  TableColumn getTableColumn(C identifier);

  /**
   * @param identifier the column identifier
   * @return true if this column model contains a column with the given identifier
   */
  boolean containsColumn(C identifier);

  /**
   * @param modelColumnIndex the column model index
   * @return the column identifier
   */
  C getColumnIdentifier(int modelColumnIndex);

  /**
   * Resets the columns to their original location and visibility
   */
  void resetColumns();

  /**
   * @param listener a listener to be notified each time a column is hidden
   */
  void addColumnHiddenListener(EventDataListener<C> listener);

  /**
   * @param listener the listener to remove
   */
  void removeColumnHiddenListener(EventDataListener<C> listener);

  /**
   * @param listener a listener to be notified each time a column is shown
   */
  void addColumnShownListener(EventDataListener<C> listener);

  /**
   * @param listener the listener to remove
   */
  void removeColumnShownListener(EventDataListener<C> listener);
}
