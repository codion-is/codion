/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.common.model.table;

import dev.codion.common.event.EventDataListener;

import java.util.Collection;
import java.util.List;

/**
 * A TableColumnModel handling hidden columns
 * @param <R> the type of rows
 * @param <C> the type of column identifier
 * @param <T> the type representing table columns
 */
public interface FilteredTableColumnModel<R, C, T> {

  /**
   * @return all columns in this model, both hidden and visible
   */
  List<T> getAllColumns();

  /**
   * @return an unmodifiable view of hidden table columns
   */
  Collection<T> getHiddenColumns();

  /**
   * Shows the column with the given columnIdentifier.
   * @param columnIdentifier the column identifier
   */
  void showColumn(C columnIdentifier);

  /**
   * Hides the column with the given columnIdentifier.
   * @param columnIdentifier the column identifier
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
   * Returns the {@link ColumnConditionModel} for the column with the given identifier.
   * @param columnIdentifier the column identifier
   * @return the ColumnConditionModel for the column with the given identifier, null if none exists.
   */
  ColumnConditionModel<R, C> getColumnFilterModel(C columnIdentifier);

  /**
   * @return the ColumnFilterModel instances
   */
  Collection<ColumnConditionModel<R, C>> getColumnFilterModels();

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
