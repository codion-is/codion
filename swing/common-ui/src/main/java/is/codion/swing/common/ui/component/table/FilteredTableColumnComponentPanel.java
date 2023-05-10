/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.swing.common.model.component.table.FilteredTableColumn;
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
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
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
 * For instances use the {@link #filteredTableColumnComponentPanel(FilteredTableColumnModel, Map)} factory method.
 * @param <T> the component type
 * @see #filteredTableColumnComponentPanel(FilteredTableColumnModel, Map)
 */
public final class FilteredTableColumnComponentPanel<C, T extends JComponent> extends JPanel {

  private final FilteredTableColumnModel<C> columnModel;
  private final Collection<FilteredTableColumn<C>> columns;
  private final Box.Filler scrollBarFiller;
  private final JPanel basePanel;
  private final Map<C, T> columnComponents;
  private final Map<C, JPanel> nullComponents = new HashMap<>(0);

  private FilteredTableColumnComponentPanel(FilteredTableColumnModel<C> columnModel, Map<C, T> columnComponents) {
    this.columnModel = requireNonNull(columnModel);
    this.columns = columnModel.columns();
    requireNonNull(columnComponents).forEach((columnIdentifier, component) -> {
      if (!columnModel.containsColumn(columnIdentifier)) {
        throw new IllegalArgumentException("Column " + columnIdentifier + " is not part of column model");
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
   * @return the column components mapped their respective column identifiers
   */
  public Map<C, T> columnComponents() {
    return columnComponents;
  }

  /**
   * Instantiates a new {@link FilteredTableColumnComponentPanel}.
   * @param columnModel the column model
   * @param columnComponents the column components mapped to their respective column
   * @param <C> the column identifier type
   * @param <T> the component type
   * @return a new {@link FilteredTableColumnComponentPanel}
   */
  public static <C, T extends JComponent> FilteredTableColumnComponentPanel<C, T> filteredTableColumnComponentPanel(FilteredTableColumnModel<C> columnModel,
                                                                                                                    Map<C, T> columnComponents) {
    return new FilteredTableColumnComponentPanel<>(columnModel, columnComponents);
  }

  private void resetPanel() {
    Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    if (Utilities.getParentOfType(FilteredTableColumnComponentPanel.class, focusOwner) != null) {
      basePanel.requestFocusInWindow();
    }
    basePanel.removeAll();
    Enumeration<TableColumn> columnEnumeration = columnModel.getColumns();
    while (columnEnumeration.hasMoreElements()) {
      basePanel.add(columnComponent((FilteredTableColumn<C>) columnEnumeration.nextElement()));
    }
    basePanel.add(scrollBarFiller);
    syncPanelWidths();
    repaint();
    if (focusOwner != null) {
      focusOwner.requestFocusInWindow();
    }
  }

  private void bindColumnAndComponentSizes() {
    columnModel.addColumnModelListener(new SyncColumnModelListener());
    for (FilteredTableColumn<C> column : columns) {
      JComponent component = columnComponent(column);
      component.setPreferredSize(new Dimension(column.getWidth(), component.getPreferredSize().height));
      column.addPropertyChangeListener(new SyncListener(component, column));
    }
  }

  private void syncPanelWidths() {
    for (FilteredTableColumn<C> column : columns) {
      syncPanelWidth(columnComponent(column), column);
    }
  }

  private JComponent columnComponent(FilteredTableColumn<C> column) {
    return columnComponents.getOrDefault(column.getIdentifier(),
            (T) nullComponents.computeIfAbsent(column.getIdentifier(), c -> new JPanel()));
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
      if (e.getFromIndex() != e.getToIndex()) {
        resetPanel();
      }
    }

    @Override
    public void columnMarginChanged(ChangeEvent e) {/*Not required*/}

    @Override
    public void columnSelectionChanged(ListSelectionEvent e) {/*Not required*/}
  }
}
