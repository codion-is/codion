/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.table.ColumnSummaryModel;
import org.jminor.common.ui.table.AbstractTableColumnSyncPanel;
import org.jminor.common.ui.table.ColumnSummaryPanel;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.domain.Property;

import javax.swing.JPanel;
import javax.swing.table.TableColumn;

/**
 * A UI component for showing summary panels for numerical properties in a EntityTableModel.
 */
public final class EntityTableSummaryPanel extends AbstractTableColumnSyncPanel {

  private final EntityTableModel tableModel;

  /**
   * Instantiates a new EntityTableSummaryPanel
   * @param tableModel the table model
   */
  public EntityTableSummaryPanel(final EntityTableModel tableModel) {
    super(tableModel.getColumnModel());
    this.tableModel = tableModel;
    resetPanel();
  }

  /** {@inheritDoc} */
  @Override
  protected JPanel initializeColumnPanel(final TableColumn column) {
    final Property property = (Property) column.getIdentifier();
    final ColumnSummaryModel summaryModel = tableModel.getPropertySummaryModel(property);

    return initializePropertySummaryPanel(summaryModel);
  }

  /**
   * Initializes a PropertySummaryPanel for the given model
   * @param propertySummaryModel the ColumnSummaryModel for which to create a summary panel
   * @return a ColumnSummaryPanel based on the given model
   */
  private ColumnSummaryPanel initializePropertySummaryPanel(final ColumnSummaryModel propertySummaryModel) {
    return new ColumnSummaryPanel(propertySummaryModel);
  }
}
