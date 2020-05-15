/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.swing.common.ui.table;

import dev.codion.common.model.table.ColumnSummaryModel;
import dev.codion.common.model.table.FilteredTableModel;
import dev.codion.swing.common.model.table.AbstractFilteredTableModel;
import dev.codion.swing.common.ui.Components;

import javax.swing.JPanel;
import javax.swing.table.TableColumn;

/**
 * A UI component for showing column summary panels for numerical columns in a FilteredTableModel.
 */
public final class FilteredTableSummaryPanel extends AbstractTableColumnSyncPanel {

  private final FilteredTableModel tableModel;

  /**
   * Instantiates a new FilteredTableSummaryPanel
   * @param tableModel the table model
   */
  public FilteredTableSummaryPanel(final AbstractFilteredTableModel tableModel) {
    super(tableModel.getColumnModel());
    this.tableModel = tableModel;
    setVerticalFillerWidth(Components.getPreferredScrollBarWidth());
  }

  @Override
  protected JPanel initializeColumnPanel(final TableColumn column) {
    return initializeColumnSummaryPanel(tableModel.getColumnSummaryModel(column.getIdentifier()));
  }

  /**
   * Initializes a ColumnSummaryPanel for the given model
   * @param columnSummaryModel the ColumnSummaryModel for which to create a summary panel
   * @return a ColumnSummaryPanel based on the given model
   */
  private static ColumnSummaryPanel initializeColumnSummaryPanel(final ColumnSummaryModel columnSummaryModel) {
    return new ColumnSummaryPanel(columnSummaryModel);
  }
}
