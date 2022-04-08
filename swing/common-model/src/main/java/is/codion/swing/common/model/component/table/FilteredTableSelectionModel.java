/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.component.table;

import is.codion.common.model.table.SelectionModel;

import javax.swing.ListSelectionModel;

/**
 * A table selection model
 * @param <R> the type of rows
 */
public interface FilteredTableSelectionModel<R> extends ListSelectionModel, SelectionModel<R> {

  /**
   * Instantiates a new {@link FilteredTableSelectionModel}
   * @param tableModel the FilteredTableModel to base this selection model on
   * @param <R> the row type
   * @return a new {@link FilteredTableSelectionModel}
   */
  static <R> FilteredTableSelectionModel<R> create(FilteredTableModel<R, ?> tableModel) {
    return new DefaultFilteredTableSelectionModel<>(tableModel);
  }
}
