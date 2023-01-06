/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.swing.common.model.component.table.FilteredTableColumn;

/**
 * Responsible for creating {@link ColumnConditionPanel}s
 */
public interface ConditionPanelFactory {

  /**
   * Creates a ColumnConditionPanel for the given column, returns null if none is available
   * @param <C> the column identifier type
   * @param <T> the column value type
   * @param column the column
   * @return a ColumnConditionPanel or null if none is available for the given column
   */
   <C, T> ColumnConditionPanel<C, T> createConditionPanel(FilteredTableColumn<C> column);
}
