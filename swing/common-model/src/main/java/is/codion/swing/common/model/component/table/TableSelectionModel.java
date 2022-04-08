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
public interface TableSelectionModel<R> extends ListSelectionModel, SelectionModel<R> {

  /**
   * Instantiates a new {@link TableSelectionModel}
   * @param tableModel the FilteredTableModel to base this selection model on
   * @return a new {@link TableSelectionModel}
   */
  static <R> TableSelectionModel<R> create(FilteredTableModel<R, ?> tableModel) {
    return new SwingTableSelectionModel<>(tableModel);
  }
}
