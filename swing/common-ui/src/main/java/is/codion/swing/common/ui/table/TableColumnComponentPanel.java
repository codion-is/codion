/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.table;

import is.codion.swing.common.model.table.SwingFilteredTableColumnModel;
import is.codion.swing.common.ui.Components;
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
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * A panel that synchronizes child component sizes and positions to table columns.
 */
public final class TableColumnComponentPanel<T extends JComponent> extends JPanel {

  private final TableColumnModel columnModel;
  private final List<TableColumn> columns;
  private final Box.Filler scrollBarFiller;
  private final JPanel basePanel;
  private final Map<TableColumn, T> columnComponents;
  private final Map<TableColumn, JComponent> nullComponents = new HashMap<>(0);

  /**
   * Instantiates a new AbstractTableColumnSyncPanel.
   * @param columnModel the column model
   * @param columnComponents the column components mapped to their respective column
   * @param verticalFillerWidth the width of a vertical fill component
   */
  public TableColumnComponentPanel(final SwingFilteredTableColumnModel<?, ?> columnModel,
                                   final Map<TableColumn, T> columnComponents) {
    requireNonNull(columnModel);
    requireNonNull(columnComponents);
    setLayout(new BorderLayout());
    this.basePanel = new JPanel(new FlexibleGridLayout(1, 0, 0, 0));
    this.columnModel = columnModel;
    this.columns = columnModel.getAllColumns();
    this.columnModel.addColumnModelListener(new SyncColumnModelListener());
    final Dimension fillerSize = new Dimension(Components.getPreferredScrollBarWidth(), 0);
    this.scrollBarFiller = new Box.Filler(fillerSize, fillerSize, fillerSize);
    this.columnComponents = Collections.unmodifiableMap(columnComponents);
    columnModel.getAllColumns().forEach(column -> {
      if (!columnComponents.containsKey(column)) {
        nullComponents.put(column, new JPanel());
      }
    });
    add(basePanel, BorderLayout.WEST);
    bindColumnAndPanelSizes();
    resetPanel();
  }

  /**
   * @return the column components mapped their respective columns
   */
  public Map<TableColumn, T> getColumnComponents() {
    return columnComponents;
  }

  private void resetPanel() {
    basePanel.removeAll();
    final Enumeration<TableColumn> columnEnumeration = columnModel.getColumns();
    while (columnEnumeration.hasMoreElements()) {
      basePanel.add(getColumnComponent(columnEnumeration.nextElement()));
    }
    basePanel.add(scrollBarFiller);
    syncPanelWidths();
    repaint();
  }

  private void bindColumnAndPanelSizes() {
    for (final TableColumn column : columns) {
      final JComponent component = getColumnComponent(column);
      component.setPreferredSize(new Dimension(column.getWidth(), component.getPreferredSize().height));
      column.addPropertyChangeListener(new SyncListener(component, column));
    }
  }

  private void syncPanelWidths() {
    for (final TableColumn column : columns) {
      syncPanelWidth(getColumnComponent(column), column);
    }
  }

  private JComponent getColumnComponent(final TableColumn column) {
    JComponent component = columnComponents.get(column);
    if (component == null) {
      component = nullComponents.get(column);
    }

    return component;
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
