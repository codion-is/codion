/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.table;

import is.codion.swing.common.model.table.SwingFilteredTableColumnModel;
import is.codion.swing.common.ui.layout.FlexibleGridLayout;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A panel that synchronizes child component sizes and positions to table columns.
 */
public abstract class AbstractTableColumnComponentPanel<T extends JComponent> extends JPanel {

  private final TableColumnModel columnModel;
  private final List<TableColumn> columns;
  private final Box.Filler verticalFiller;
  private final JPanel basePanel;
  private Map<TableColumn, T> columnComponents;

  /**
   * Instantiates a new AbstractTableColumnSyncPanel.
   * @param columnModel the column model
   */
  public AbstractTableColumnComponentPanel(final SwingFilteredTableColumnModel<?, ?> columnModel) {
    setLayout(new BorderLayout());
    this.basePanel = new JPanel(new FlexibleGridLayout(1, 0, 0, 0));
    this.columnModel = columnModel;
    this.columns = columnModel.getAllColumns();
    this.columnModel.addColumnModelListener(new SyncColumnModelListener());
    final Dimension fillerSize = new Dimension();
    this.verticalFiller = new Box.Filler(fillerSize, fillerSize, fillerSize);
    add(basePanel, BorderLayout.WEST);
  }

  /**
   * @return the column panels mapped their respective columns
   */
  public final Map<TableColumn, T> getColumnComponents() {
    if (columnComponents == null) {
      columnComponents = initializeColumnComponents();
      bindColumnAndPanelSizes();
    }

    return columnComponents;
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
    basePanel.removeAll();
    final Enumeration<TableColumn> columnEnumeration = columnModel.getColumns();
    while (columnEnumeration.hasMoreElements()) {
      basePanel.add(getColumnComponents().get(columnEnumeration.nextElement()));
    }
    basePanel.add(verticalFiller);
    syncPanelWidths();
    repaint();
  }

  /**
   * Initializes the column component for the given column
   * @param column the column
   * @return the column component for the given column
   */
  protected abstract T initializeComponent(TableColumn column);

  private void bindColumnAndPanelSizes() {
    for (final TableColumn column : columns) {
      final JComponent component = columnComponents.get(column);
      component.setPreferredSize(new Dimension(column.getWidth(), component.getPreferredSize().height));
      column.addPropertyChangeListener(new SyncListener(component, column));
    }
  }

  private Map<TableColumn, T> initializeColumnComponents() {
    final Map<TableColumn, T> components = new HashMap<>();
    for (final TableColumn column : columns) {
      components.put(column, initializeComponent(column));
    }

    return components;
  }

  private void syncPanelWidths() {
    for (final TableColumn column : columns) {
      syncPanelWidth(columnComponents.get(column), column);
    }
  }

  private static void syncPanelWidth(final JComponent component, final TableColumn column) {
    component.setPreferredSize(new Dimension(column.getWidth(), component.getPreferredSize().height));
    component.revalidate();
  }

  private static final class SyncListener implements PropertyChangeListener {
    private final JComponent component;
    private final TableColumn column;

    private SyncListener(final JComponent component, final TableColumn column) {
      this.component = component;
      this.column = column;
    }

    @Override
    public void propertyChange(final PropertyChangeEvent changeEvent) {
      if ("width".equals(changeEvent.getPropertyName())) {
        syncPanelWidth(component, column);
      }
    }
  }

  private final class SyncColumnModelListener implements TableColumnModelListener {
    @Override
    public void columnAdded(final TableColumnModelEvent e) {
      final TableColumn column = columnModel.getColumn(e.getToIndex());
      if (!columnComponents.containsKey(column)) {
        columnComponents.put(column, initializeComponent(column));
      }
      resetPanel();
    }

    @Override
    public void columnRemoved(final TableColumnModelEvent e) {
      resetPanel();
    }

    @Override
    public void columnMoved(final TableColumnModelEvent e) {
      resetPanel();
    }

    @Override
    public void columnMarginChanged(final ChangeEvent e) {/*Not required*/}

    @Override
    public void columnSelectionChanged(final ListSelectionEvent e) {/*Not required*/}
  }
}
