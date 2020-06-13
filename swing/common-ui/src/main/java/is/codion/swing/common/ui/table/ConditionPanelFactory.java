/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.table;

import javax.swing.table.TableColumn;

/**
 * Responsible for creating {@link ColumnConditionPanel}s
 * @param <R> the row type
 * @param <C> the type used as column identifier
 */
public interface ConditionPanelFactory<R, C> {

  /**
   * Creates a ColumnConditionPanel for the given column
   * @param column the column
   * @return a ColumnConditionPanel
   */
  ColumnConditionPanel<R, C> createConditionPanel(TableColumn column);
}
