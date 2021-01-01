/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.table;

import javax.swing.table.TableColumn;

/**
 * Responsible for creating {@link ColumnConditionPanel}s
 * @param <R> the row type
 * @param <C> the type used as column identifier
 * @param <T> the column value type
 */
public interface ConditionPanelFactory<R, C, T> {

  /**
   * Creates a ColumnConditionPanel for the given column
   * @param column the column
   * @return a ColumnConditionPanel
   */
  ColumnConditionPanel<R, C, T> createConditionPanel(TableColumn column);
}
