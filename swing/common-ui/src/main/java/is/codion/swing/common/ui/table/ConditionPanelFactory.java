/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.table;

import javax.swing.table.TableColumn;

/**
 * Responsible for creating {@link ColumnConditionPanel}s
 */
public interface ConditionPanelFactory {

  /**
   * Creates a ColumnConditionPanel for the given column, returns null if none is available
   * @param column the column
   * @return a ColumnConditionPanel or null if none is available for the given column
   */
   ColumnConditionPanel<?, ?> createConditionPanel(TableColumn column);
}
