/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.client.model.PropertySummaryModel;
import org.jminor.framework.domain.Property;

import javax.swing.JPanel;
import javax.swing.table.TableColumn;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class EntityTableSummaryPanel extends EntityTableColumnPanel {

  private final EntityTableModel tableModel;

  public EntityTableSummaryPanel(final EntityTableModel tableModel) {
    super(tableModel.getTableColumnModel());
    this.tableModel = tableModel;
    resetPanel();
  }

  /**
   * Initializes the panel containing the table column summary panels
   * @return the summary panel
   */
  @Override
  protected Map<String, JPanel> initializeColumnPanels() {
    final Map<String, JPanel> panels = new HashMap<String, JPanel>();
    final Enumeration<TableColumn> columns = tableModel.getTableColumnModel().getColumns();
    while (columns.hasMoreElements()) {
      final Property property = (Property) columns.nextElement().getIdentifier();
      final PropertySummaryModel summaryModel = tableModel.getPropertySummaryModel(property);
      final PropertySummaryPanel summaryPanel = initializePropertySummaryPanel(summaryModel);
      panels.put(property.getPropertyID(), summaryPanel);
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
