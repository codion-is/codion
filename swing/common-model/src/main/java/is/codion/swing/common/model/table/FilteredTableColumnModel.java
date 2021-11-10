/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.table;

import is.codion.common.event.EventDataListener;
import is.codion.common.state.State;

import java.util.Collection;

/**
 * A TableColumnModel handling hidden columns
 * @param <C> the type of column identifier
 * @param <T> the type representing table columns
 */
public interface FilteredTableColumnModel<C, T> {

  /**
   * @return all columns in this model, both hidden and visible, in no particular order
   */
  Collection<T> getAllColumns();

  /**
   * @return an unmodifiable view of hidden table columns
   */
  Collection<T> getHiddenColumns();

  /**
   * Returns a {@link State} instance controlling whether this model is locked or not.
   * A locked column model does not allow adding or removing of columns, but columns can be reordered.
   * @return a {@link State} controlling whether this model is locked or not
   */
  State getLockedState();

  /**
   * Shows the column with the given columnIdentifier.
   * @param columnIdentifier the column identifier
   * @throws IllegalStateException in case this model is locked
   * @see #getLockedState()
   */
  void showColumn(C columnIdentifier);

  /**
   * Hides the column with the given columnIdentifier.
   * @param columnIdentifier the column identifier
   * @throws IllegalStateException in case this model is locked
   * @see #getLockedState()
   */
  void hideColumn(C columnIdentifier);

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
   * Returns the TableColumn with the given identifier
   * @param identifier the column identifier
   * @return the TableColumn with the given identifier
   * @throws IllegalArgumentException in case this table model does not contain a column with the given identifier
   */
  T getTableColumn(C identifier);

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
