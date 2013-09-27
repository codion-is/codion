/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.table;

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
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A panel that synchronizes child panel sizes and positions to table columns.
 */
public abstract class AbstractTableColumnSyncPanel extends JPanel {

  private final TableColumnModel columnModel;
  private final List<TableColumn> columns;
  private final Box.Filler verticalFiller;
  private Map<TableColumn, JPanel> columnPanels;

  /**
   * Instantiates a new AbstractTableColumnSyncPanel.
   * @param columnModel the column model
   */
  public AbstractTableColumnSyncPanel(final TableColumnModel columnModel, final List<TableColumn> columns) {
    setLayout(new FlowLayout(FlowLayout.LEFT,0,0));
    this.columnModel = columnModel;
    this.columns = Collections.unmodifiableList(columns);
    columnModel.addColumnModelListener(new SyncColumnModelListener());
    final Dimension fillerSize = new Dimension();
    this.verticalFiller = new Box.Filler(fillerSize, fillerSize, fillerSize);
  }

  /**
   * @return the column panels mapped their respective columns
   */
  public final Map<TableColumn, JPanel> getColumnPanels() {
    if (columnPanels == null) {
      columnPanels = initializeColumnPanels();
      bindColumnAndPanelSizes();
    }

    return columnPanels;
  }

  /**
   * Sets the width of the rightmost vertical filler
   * @param width the required width
   */
  public final void setVerticalFillerWidth(final int width) {
    final Dimension dimension = new Dimension(width, verticalFiller.getHeight());
    verticalFiller.changeShape(dimension, dimension, dimension);
    resetPanel();
  }

  /**
   * Resets the panel and lays out all sub-panels.
   */
  public final void resetPanel() {
    removeAll();
    final Enumeration<TableColumn> columnEnumeration = columnModel.getColumns();
    while (columnEnumeration.hasMoreElements()) {
      add(getColumnPanels().get(columnEnumeration.nextElement()));
    }
    add(verticalFiller);

    syncPanelWidths();
    repaint();
  }

  /**
   * Initializes the column panel for the given column
   * @param column the column
   * @return the column panel for the given column
   */
  protected abstract JPanel initializeColumnPanel(final TableColumn column);

  private void bindColumnAndPanelSizes() {
    for (final TableColumn column : columns) {
      final JPanel panel = columnPanels.get(column);
      panel.setPreferredSize(new Dimension(column.getWidth(), panel.getPreferredSize().height));
      column.addPropertyChangeListener(new SyncListener(panel, column));
    }
  }

  private Map<TableColumn, JPanel> initializeColumnPanels() {
    final Map<TableColumn, JPanel> panels = new HashMap<>();
    for (final TableColumn column : columns) {
      panels.put(column, initializeColumnPanel(column));
    }

    return panels;
  }

  private void syncPanelWidths() {
    for (final TableColumn column : columns) {
      final JPanel panel = columnPanels.get(column);
      syncPanelWidth(panel, column);
    }
  }

  private static void syncPanelWidth(final JPanel panel, final TableColumn column) {
    panel.setPreferredSize(new Dimension(column.getWidth(), panel.getPreferredSize().height));
    panel.revalidate();
  }

  private static final class SyncListener implements PropertyChangeListener {
    private final JPanel panel;
    private final TableColumn column;

    private SyncListener(final JPanel panel, final TableColumn column) {
      this.panel = panel;
      this.column = column;
    }

    /** {@inheritDoc} */
    @Override
    public void propertyChange(final PropertyChangeEvent evt) {
      if (evt.getPropertyName().equals("width")) {
        syncPanelWidth(panel, column);
      }
    }
  }

  private final class SyncColumnModelListener implements TableColumnModelListener {
    /** {@inheritDoc} */
    @Override
    public void columnAdded(final TableColumnModelEvent e) {
      final TableColumn column = columnModel.getColumn(e.getToIndex());
      if (!columnPanels.containsKey(column)) {
        columnPanels.put(column, initializeColumnPanel(column));
      }
      resetPanel();
    }

    /** {@inheritDoc} */
    @Override
    public void columnRemoved(final TableColumnModelEvent e) {
      resetPanel();
    }

    /** {@inheritDoc} */
    @Override
    public void columnMoved(final TableColumnModelEvent e) {
      resetPanel();
    }

    /** {@inheritDoc} */
    @Override
    public void columnMarginChanged(final ChangeEvent e) {}

    /** {@inheritDoc} */
    @Override
    public void columnSelectionChanged(final ListSelectionEvent e) {}
  }
}
