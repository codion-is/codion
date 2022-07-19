/*
 * Copyright (c) 2010 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.swing.common.model.component.table.FilteredTableColumnModel;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.layout.FlexibleGridLayout;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.UIManager;
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
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * A panel that synchronizes child component sizes and positions to table columns.
 * @param <T> the component type
 */
public final class TableColumnComponentPanel<T extends JComponent> extends JPanel {

  private final TableColumnModel columnModel;
  private final Collection<TableColumn> columns;
  private final Box.Filler scrollBarFiller;
  private final JPanel basePanel;
  private final Map<TableColumn, T> columnComponents;
  private final Map<TableColumn, JPanel> nullComponents = new HashMap<>(0);

  /**
   * Instantiates a new AbstractTableColumnSyncPanel.
   * @param columnModel the column model
   * @param columnComponents the column components mapped to their respective column
   */
  public TableColumnComponentPanel(FilteredTableColumnModel<?> columnModel,
                                   Map<TableColumn, T> columnComponents) {
    this.columnModel = requireNonNull(columnModel);
    this.columns = columnModel.getAllColumns();
    requireNonNull(columnComponents).forEach((column, component) -> {
      if (!columns.contains(column)) {
        throw new IllegalArgumentException("Column with model index " + column.getModelIndex() + " is not part of column model");
      }
    });
    this.columnComponents = Collections.unmodifiableMap(columnComponents);
    this.basePanel = new JPanel(FlexibleGridLayout.builder()
            .rows(1)
            .build());
    Dimension fillerSize = new Dimension(UIManager.getInt("ScrollBar.width"), 0);
    this.scrollBarFiller = new Box.Filler(fillerSize, fillerSize, fillerSize);
    setLayout(new BorderLayout());
    add(basePanel, BorderLayout.WEST);
    bindColumnAndComponentSizes();
    resetPanel();
  }

  @Override
  public void updateUI() {
    super.updateUI();
    Utilities.updateUI(scrollBarFiller, basePanel);
    if (columnComponents != null) {
      Utilities.updateUI(columnComponents.values());
    }
    if (nullComponents != null) {
      Utilities.updateUI(nullComponents.values());
    }
  }

  /**
   * @return the column components mapped their respective columns
   */
  public Map<TableColumn, T> getColumnComponents() {
    return columnComponents;
  }

  private void resetPanel() {
    basePanel.removeAll();
    Enumeration<TableColumn> columnEnumeration = columnModel.getColumns();
    while (columnEnumeration.hasMoreElements()) {
      basePanel.add(getColumnComponent(columnEnumeration.nextElement()));
    }
    basePanel.add(scrollBarFiller);
    syncPanelWidths();
    repaint();
  }

  private void bindColumnAndComponentSizes() {
    columnModel.addColumnModelListener(new SyncColumnModelListener());
    for (TableColumn column : columns) {
      JComponent component = getColumnComponent(column);
      component.setPreferredSize(new Dimension(column.getWidth(), component.getPreferredSize().height));
      column.addPropertyChangeListener(new SyncListener(component, column));
    }
  }

  private void syncPanelWidths() {
    for (TableColumn column : columns) {
      syncPanelWidth(getColumnComponent(column), column);
    }
  }

  private JComponent getColumnComponent(TableColumn column) {
    return columnComponents.getOrDefault(column, (T) nullComponents.computeIfAbsent(column, c -> new JPanel()));
  }

  private static void syncPanelWidth(JComponent component, TableColumn column) {
    component.setPreferredSize(new Dimension(column.getWidth(), component.getPreferredSize().height));
    component.revalidate();
  }

  private static final class SyncListener implements PropertyChangeListener {

    private final JComponent component;
    private final TableColumn column;

    private SyncListener(JComponent component, TableColumn column) {
      this.component = component;
      this.column = column;
    }

    @Override
    public void propertyChange(PropertyChangeEvent changeEvent) {
      if ("width".equals(changeEvent.getPropertyName())) {
        syncPanelWidth(component, column);
      }
    }
  }

  private final class SyncColumnModelListener implements TableColumnModelListener {
    @Override
    public void columnAdded(TableColumnModelEvent e) {
      resetPanel();
    }

    @Override
    public void columnRemoved(TableColumnModelEvent e) {
      resetPanel();
    }

    @Override
    public void columnMoved(TableColumnModelEvent e) {
      resetPanel();
    }

    @Override
    public void columnMarginChanged(ChangeEvent e) {/*Not required*/}

    @Override
    public void columnSelectionChanged(ListSelectionEvent e) {/*Not required*/}
  }
}
