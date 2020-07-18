/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.table;

import is.codion.common.model.table.ColumnSummaryModel;
import is.codion.common.model.table.FilteredTableModel;
import is.codion.swing.common.model.table.AbstractFilteredTableModel;
import is.codion.swing.common.ui.Components;

import javax.swing.JPanel;
import javax.swing.table.TableColumn;

/**
 * A UI component for showing column summary panels for numerical columns in a FilteredTableModel.
 * @param <C> the column identifier
 */
public final class FilteredTableSummaryPanel<C> extends AbstractTableColumnSyncPanel {

  private final FilteredTableModel<?, C, ?> tableModel;

  /**
   * Instantiates a new FilteredTableSummaryPanel
   * @param tableModel the table model
   */
  public FilteredTableSummaryPanel(final AbstractFilteredTableModel<?, C> tableModel) {
    super(tableModel.getColumnModel());
    this.tableModel = tableModel;
    setVerticalFillerWidth(Components.getPreferredScrollBarWidth());
  }

  @Override
  protected JPanel initializeColumnPanel(final TableColumn column) {
    return initializeColumnSummaryPanel(tableModel.getColumnSummaryModel((C) column.getIdentifier()));
  }

  /**
   * Initializes a ColumnSummaryPanel for the given model
   * @param columnSummaryModel the ColumnSummaryModel for which to create a summary panel
   * @param <T> the column type
   * @return a ColumnSummaryPanel based on the given model
   */
  private static <T extends Number> ColumnSummaryPanel initializeColumnSummaryPanel(final ColumnSummaryModel<T> columnSummaryModel) {
    return new ColumnSummaryPanel(columnSummaryModel);
  }
}
