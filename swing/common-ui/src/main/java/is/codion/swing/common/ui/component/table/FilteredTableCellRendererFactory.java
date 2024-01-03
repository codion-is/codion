/*
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.swing.common.model.component.table.FilteredTableColumn;

import javax.swing.table.TableCellRenderer;

/**
 * A factory for {@link TableCellRenderer} instances.
 */
public interface FilteredTableCellRendererFactory<C> {

  /**
   * @param column the column
   * @return a {@link TableCellRenderer} instance for the given column
   */
  TableCellRenderer tableCellRenderer(FilteredTableColumn<C> column);
}
