/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.table;

import org.jminor.swing.common.model.table.ColumnSummaryModel;
import org.jminor.swing.common.model.table.FilteredTableModel;

import javax.swing.JPanel;
import javax.swing.table.TableColumn;

/**
 * A UI component for showing column summary panels for numerical columns in a FilteredTableModel.
 */
public final class FilteredTableSummaryPanel extends AbstractTableColumnSyncPanel {

  private final FilteredTableModel tableModel;

  /**
   * Instantiates a new EntityTableSummaryPanel
   * @param tableModel the table model
   */
  public FilteredTableSummaryPanel(final FilteredTableModel tableModel) {
    super(tableModel.getColumnModel());
    this.tableModel = tableModel;
    resetPanel();
  }

  /** {@inheritDoc} */
  @Override
  protected JPanel initializeColumnPanel(final TableColumn column) {
    final ColumnSummaryModel summaryModel = tableModel.getColumnSummaryModel(column.getIdentifier());

    return initializeColumnSummaryPanel(summaryModel);
  }

  /**
   * Initializes a ColumnSummaryPanel for the given model
   * @param columnSummaryModel the ColumnSummaryModel for which to create a summary panel
   * @return a ColumnSummaryPanel based on the given model
   */
  private ColumnSummaryPanel initializeColumnSummaryPanel(final ColumnSummaryModel columnSummaryModel) {
    return new ColumnSummaryPanel(columnSummaryModel);
  }
}
