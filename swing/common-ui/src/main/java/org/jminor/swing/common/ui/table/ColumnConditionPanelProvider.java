/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.table;

import javax.swing.table.TableColumn;

/**
 * Responsible for creating {@link ColumnConditionPanel}s
 * @param <C> the type used as column identifier
 */
public interface ColumnConditionPanelProvider<C> {

  /**
   * Creates a ColumnConditionPanel for the given column
   * @param column the column
   * @return a ColumnConditionPanel
   */
  ColumnConditionPanel<C> createColumnConditionPanel(TableColumn column);
}
