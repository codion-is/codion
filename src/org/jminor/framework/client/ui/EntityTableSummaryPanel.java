/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.ui.AbstractTableColumnSyncPanel;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.client.model.PropertySummaryModel;
import org.jminor.framework.domain.Property;

import javax.swing.JPanel;
import javax.swing.table.TableColumn;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * A UI component for showing summary panels for numerical properties in a EntityTableModel.
 */
public class EntityTableSummaryPanel extends AbstractTableColumnSyncPanel {

  private final EntityTableModel tableModel;

  public EntityTableSummaryPanel(final EntityTableModel tableModel) {
    super(tableModel.getColumnModel());
    this.tableModel = tableModel;
    resetPanel();
  }

  /**
   * Initializes the panel containing the table column summary panels
   * @return the summary panel
   */
  @Override
  protected Map<TableColumn, JPanel> initializeColumnPanels() {
    final Map<TableColumn, JPanel> panels = new HashMap<TableColumn, JPanel>();
    final Enumeration<TableColumn> columns = tableModel.getColumnModel().getColumns();
    while (columns.hasMoreElements()) {
      final TableColumn column = columns.nextElement();
      final Property property = (Property) column.getIdentifier();
      final PropertySummaryModel summaryModel = tableModel.getPropertySummaryModel(property);
      final PropertySummaryPanel summaryPanel = initializePropertySummaryPanel(summaryModel);
      panels.put(column, summaryPanel);
    }

    return panels;
  }

  /**
   * Initializes a PropertySummaryPanel for the given model
   * @param propertySummaryModel the PropertySummaryModel for which to create a summary panel
   * @return a PropertySummaryPanel based on the given model
   */
  protected PropertySummaryPanel initializePropertySummaryPanel(final PropertySummaryModel propertySummaryModel) {
    return new PropertySummaryPanel(propertySummaryModel);
  }
}
