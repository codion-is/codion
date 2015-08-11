/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.ui.table.AbstractTableColumnSyncPanel;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.client.model.PropertySummaryModel;
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
    final PropertySummaryModel summaryModel = tableModel.getPropertySummaryModel(property);

    return initializePropertySummaryPanel(summaryModel);
  }

  /**
   * Initializes a PropertySummaryPanel for the given model
   * @param propertySummaryModel the PropertySummaryModel for which to create a summary panel
   * @return a PropertySummaryPanel based on the given model
   */
  private PropertySummaryPanel initializePropertySummaryPanel(final PropertySummaryModel propertySummaryModel) {
    return new PropertySummaryPanel(propertySummaryModel);
  }
}
