/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.framework.domain.Property;

import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Enumeration;
import java.util.Map;

/**
 * A panel that synchronizes child panel sizes and positions to table columns
 */
public abstract class EntityTableColumnPanel extends JPanel {

  private final TableColumnModel tableColumnModel;
  private final Box.Filler verticalFiller;
  private Map<String, JPanel> columnPanels;

  public EntityTableColumnPanel(final TableColumnModel tableColumnModel) {
    setLayout(new FlowLayout(FlowLayout.LEFT,0,0));
    this.tableColumnModel = tableColumnModel;
    final Dimension fillerSize = new Dimension();
    this.verticalFiller = new Box.Filler(fillerSize, fillerSize, fillerSize);
  }

  public Map<String, JPanel> getColumnPanels() {
    if (columnPanels == null) {
      columnPanels = initializeColumnPanels();
      bindColumnAndPanelSizes();
    }

    return columnPanels;
  }

  /** {@inheritDoc} */
  @Override
  public Dimension getPreferredSize() {
    for (final JPanel searchPanel : getColumnPanels().values())
      if (searchPanel instanceof PropertySearchPanel)
        return new Dimension(super.getPreferredSize().width, searchPanel.getPreferredSize().height);

    return new Dimension(super.getPreferredSize().width, getColumnPanels().values().iterator().next().getPreferredSize().height);
  }

  /**
   * Sets the width of the rightmost vertical filler
   * @param width the required width
   */
  public void setVerticalFillerWidth(final int width) {
    final Dimension dimension = new Dimension(width, verticalFiller.getHeight());
    verticalFiller.changeShape(dimension, dimension, dimension);
    resetPanel();
  }

  protected abstract Map<String, JPanel> initializeColumnPanels();

  protected void resetPanel() {
    removeAll();
    final Enumeration<TableColumn> columnEnumeration = tableColumnModel.getColumns();
    while (columnEnumeration.hasMoreElements())
      add(getColumnPanels().get(((Property) columnEnumeration.nextElement().getIdentifier()).getPropertyID()));
    add(verticalFiller);

    syncPanelWidths(tableColumnModel, columnPanels);
    repaint();
  }

  private void bindColumnAndPanelSizes() {
    if (tableColumnModel.getColumnCount() != columnPanels.size())
      throw new IllegalArgumentException("An equal number of columns and panels is required when binding sizes");

    tableColumnModel.addColumnModelListener(new TableColumnModelListener() {
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
    final Enumeration<TableColumn> columnEnumeration = tableColumnModel.getColumns();
    while (columnEnumeration.hasMoreElements()) {
      final TableColumn column = columnEnumeration.nextElement();
      final JPanel panel = columnPanels.get(((Property) column.getIdentifier()).getPropertyID());
      panel.setPreferredSize(new Dimension(column.getWidth(), panel.getPreferredSize().height));
      column.addPropertyChangeListener(new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent e) {
          if (e.getPropertyName().equals("width")) {
            syncPanelWidth(panel, column);
          }
        }
      });
    }
  }

  private static void syncPanelWidths(final TableColumnModel columnModel, final Map<String, JPanel> panelMap) {
    final Enumeration<TableColumn> columnEnumeration = columnModel.getColumns();
    while (columnEnumeration.hasMoreElements()) {
      final TableColumn column = columnEnumeration.nextElement();
      final JPanel panel = panelMap.get(((Property) column.getIdentifier()).getPropertyID());
      syncPanelWidth(panel, column);
    }
  }

  private static void syncPanelWidth(final JPanel panel, final TableColumn column) {
    panel.setPreferredSize(new Dimension(column.getWidth(), panel.getPreferredSize().height));
    panel.revalidate();
  }
}
