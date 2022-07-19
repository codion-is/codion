/*
 * Copyright (c) 2019 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.table;

import javax.swing.table.TableColumn;

/**
 * Responsible for creating {@link ColumnConditionPanel}s
 */
public interface ConditionPanelFactory {

  /**
   * Creates a ColumnConditionPanel for the given column, returns null if none is available
   * @param <T> the column value type
   * @param column the column
   * @return a ColumnConditionPanel or null if none is available for the given column
   */
   <T> ColumnConditionPanel<?, T> createConditionPanel(TableColumn column);
}
