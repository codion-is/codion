package org.jminor.framework.client.ui;

import org.jminor.common.ui.UiUtil;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.domain.Property;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.TableColumn;
import java.awt.FlowLayout;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class EntityTableSummaryPanel extends JPanel {

  private final EntityTableModel tableModel;

  private final Map<String, JPanel> summaryPanels;

  public EntityTableSummaryPanel(final EntityTableModel tableModel) {
    if (tableModel == null)
      throw new IllegalArgumentException("EntityTableSummaryPanel requires a EntityTableModel instance");
    this.tableModel = tableModel;
    this.summaryPanels = initializeSummaryPanels();
    initializeUI();
  }

  public void bindToColumnSizes(final JTable table) {
    UiUtil.bindColumnAndPanelSizes(table.getColumnModel(), summaryPanels);
  }

  protected void initializeUI() {
    setLayout(new FlowLayout(FlowLayout.LEFT,0,0));
    resetPanel();
    tableModel.getTableColumnModel().addColumnModelListener(new TableColumnModelListener() {
      public void columnAdded(final TableColumnModelEvent e) {
        resetPanel();
      }

      public void columnRemoved(final TableColumnModelEvent e) {
        resetPanel();
      }

      public void columnMoved(final TableColumnModelEvent e) {
        resetPanel();
      }

      public void columnMarginChanged(final ChangeEvent e) {}

      public void columnSelectionChanged(final ListSelectionEvent e) {}
    });
  }

  /**
   * Initializes the panel containing the table column summary panels
   * @return the summary panel
   */
  protected Map<String, JPanel> initializeSummaryPanels() {
    final Map<String, JPanel> panels = new HashMap<String, JPanel>();
    final Enumeration<TableColumn> columns = tableModel.getTableColumnModel().getColumns();
    while (columns.hasMoreElements()) {
      final Property property = (Property) columns.nextElement().getIdentifier();
      final PropertySummaryPanel summaryPanel = initializeSummaryPanel(property);
      panels.put(property.getPropertyID(), summaryPanel);
    }

    return panels;
  }

  /**
   * Initializes a PropertySummaryPanel for the given property
   * @param property the property for which to create a summary panel
   * @return a PropertySummaryPanel for the given property
   */
  protected PropertySummaryPanel initializeSummaryPanel(final Property property) {
    return new PropertySummaryPanel(tableModel.getPropertySummaryModel(property));
  }

  private void resetPanel() {
    removeAll();
    final Enumeration<TableColumn> columnEnumeration = tableModel.getTableColumnModel().getColumns();
    while (columnEnumeration.hasMoreElements())
      add(summaryPanels.get(((Property) columnEnumeration.nextElement().getIdentifier()).getPropertyID()));

    repaint();
  }
}
