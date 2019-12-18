/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.table;

import org.jminor.common.event.Event;
import org.jminor.common.event.EventListener;
import org.jminor.common.event.Events;
import org.jminor.common.model.table.FilteredTableModel;
import org.jminor.swing.common.model.table.AbstractFilteredTableModel;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.control.Control;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.common.ui.images.Images;

import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JViewport;
import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Locale;
import java.util.ResourceBundle;

import static java.util.Objects.requireNonNull;

/**
 * A UI component based on a FilteredTableModel.
 * This panel uses a {@link BorderLayout} and contains a base panel {@link #getBasePanel()}, itself with
 * a {@link BorderLayout}, containing the actual table at location {@link BorderLayout#CENTER}
 * @param <R> the type representing the rows in the table model
 * @param <C> type type used to identify columns in the table model
 * @param <M> the table model type
 * @see FilteredTableModel
 */
public final class FilteredTablePanel<R, C, M extends AbstractFilteredTableModel<R, C>> extends JPanel {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(FilteredTablePanel.class.getName(), Locale.getDefault());

  /**
   * Notified when the table summary panel is made visible or hidden
   */
  private final Event<Boolean> summaryPanelVisibleChangedEvent = Events.event();

  /**
   * the column summary panel
   */
  private final FilteredTableSummaryPanel summaryPanel;

  /**
   * the panel used as a base panel for the summary panels, used for showing/hiding the summary panels
   */
  private final JPanel summaryBasePanel;

  /**
   * the scroll pane used for the summary panel
   */
  private final JScrollPane summaryScrollPane;

  /**
   * the FilteredTable for displaying the underlying rows
   */
  private final FilteredTable<R, C, M> table;

  /**
   * the scroll pane used by the JTable instance
   */
  private final JScrollPane tableScrollPane;

  /**
   * the horizontal table scroll bar
   */
  private final JScrollBar horizontalTableScrollBar;

  /**
   * The base panel containing the table scrollpane
   */
  private final JPanel basePanel;

  /**
   * Instantiates a new FilteredTablePanel.
   * @param table the table to use
   * @param conditionPanelProvider the column condition panel provider the column filter models found in the table model
   * @see FilteredTableModel#getColumnModel()
   * @see FilteredTableModel#getSelectionModel()
   */
  public FilteredTablePanel(final FilteredTable<R, C, M> table) {
    this.table = requireNonNull(table, "table");
    this.tableScrollPane = new JScrollPane(table);
    this.horizontalTableScrollBar = tableScrollPane.getHorizontalScrollBar();
    this.basePanel = new JPanel(new BorderLayout());
    this.basePanel.add(tableScrollPane, BorderLayout.CENTER);
    this.summaryPanel = new FilteredTableSummaryPanel(table.getModel());
    this.summaryScrollPane = new JScrollPane(summaryPanel, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    this.summaryBasePanel = new JPanel(new BorderLayout());
    this.summaryBasePanel.add(summaryScrollPane, BorderLayout.NORTH);
    this.summaryBasePanel.add(horizontalTableScrollBar, BorderLayout.SOUTH);
    this.tableScrollPane.getViewport().addChangeListener(e -> {
      horizontalTableScrollBar.setVisible(tableScrollPane.getViewport().getViewSize().width > tableScrollPane.getSize().width);
      revalidate();
    });
    UiUtil.linkBoundedRangeModels(horizontalTableScrollBar.getModel(), summaryScrollPane.getHorizontalScrollBar().getModel());
    setSummaryPanelVisible(false);
    basePanel.add(summaryBasePanel, BorderLayout.SOUTH);
    setLayout(new BorderLayout());
    add(basePanel, BorderLayout.CENTER);
    bindEvents();
  }

  /**
   * @return the JTable instance
   */
  public FilteredTable<R, C, M> getTable() {
    return table;
  }

  /**
   * @return the text field used to enter a search condition
   */
  public JTextField getSearchField() {
    return table.getSearchField();
  }

  /**
   * @return the JScrollPane containing the table
   */
  public JScrollPane getTableScrollPane() {
    return tableScrollPane;
  }

  /**
   * Returns the base panel containing the table scroll pane (BorderLayout.CENTER).
   * @return the panel containing the table scroll pane
   */
  public JPanel getBasePanel() {
    return basePanel;
  }

  /**
   * Hides or shows the column summary panel for this EntityTablePanel
   * @param visible if true then the summary panel is shown, if false it is hidden
   */
  public void setSummaryPanelVisible(final boolean visible) {
    if (visible && isSummaryPanelVisible()) {
      return;
    }

    summaryScrollPane.setVisible(visible);
    revalidate();
    summaryPanelVisibleChangedEvent.fire(visible);
  }

  /**
   * @return true if the column summary panel is visible, false if it is hidden
   */
  public boolean isSummaryPanelVisible() {
    return summaryScrollPane.isVisible();
  }

  /**
   * Returns true if the given cell is visible.
   * @param row the row
   * @param column the column
   * @return true if the cell with the given coordinates is visible
   */
  public boolean isCellVisible(final int row, final int column) {
    final JViewport viewport = (JViewport) getTable().getParent();
    final Rectangle cellRect = getTable().getCellRect(row, column, true);
    final Point viewPosition = viewport.getViewPosition();
    cellRect.setLocation(cellRect.x - viewPosition.x, cellRect.y - viewPosition.y);

    return new Rectangle(viewport.getExtentSize()).contains(cellRect);
  }

  /**
   * Scrolls horizontally so that the column identified by columnIdentifier becomes visible, centered if possible
   * @param columnIdentifier the column identifier
   */
  public void scrollToColumn(final Object columnIdentifier) {
    table.scrollToCoordinate(table.rowAtPoint(getTableScrollPane().getViewport().getViewPosition()),
            getTable().getModel().getColumnModel().getColumnIndex(columnIdentifier), false, false);
  }

  /**
   * Initializes the button used to toggle the summary panel state (hidden and visible)
   * @return a summary panel toggle button
   */
  public Control getToggleSummaryPanelControl() {
    final Control toggleControl = Controls.toggleControl(this, "summaryPanelVisible", null,
            summaryPanelVisibleChangedEvent);
    toggleControl.setIcon(Images.loadImage("Sum16.gif"));
    toggleControl.setDescription(MESSAGES.getString("toggle_summary_tip"));

    return toggleControl;
  }

  /**
   * @param listener a listener notified each time the summary panel visibility changes
   */
  public void addSummaryPanelVisibleListener(final EventListener listener) {
    summaryPanelVisibleChangedEvent.addListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public void removeSummaryPanelVisibleListener(final EventListener listener) {
    summaryPanelVisibleChangedEvent.removeListener(listener);
  }

  private void bindEvents() {
    final AbstractFilteredTableModel<R, C> tableModel = getTable().getModel();
    tableModel.addSortListener(table.getTableHeader()::repaint);
    tableModel.addRefreshStartedListener(() -> UiUtil.setWaitCursor(true, FilteredTablePanel.this));
    tableModel.addRefreshDoneListener(() -> UiUtil.setWaitCursor(false, FilteredTablePanel.this));
  }
}
