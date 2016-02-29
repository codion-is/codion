/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.table;

import org.jminor.common.i18n.Messages;
import org.jminor.common.model.Event;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.Events;
import org.jminor.common.model.Util;
import org.jminor.common.model.table.ColumnCriteriaModel;
import org.jminor.swing.common.model.DocumentAdapter;
import org.jminor.swing.common.model.table.FilteredTableModel;
import org.jminor.swing.common.model.table.SortingDirective;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.control.Control;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.common.ui.images.Images;
import org.jminor.swing.common.ui.textfield.TextFieldHint;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A UI component based on a FilteredTableModel.
 * This panel uses a {@link BorderLayout} and contains a base panel {@link #getBasePanel()}, itself with
 * a {@link BorderLayout}, containing the actual table at location {@link BorderLayout#CENTER}
 * @see FilteredTableModel
 */
public class FilteredTablePanel<T, C> extends JPanel {

  public static final char FILTER_INDICATOR = '*';

  private static final int SORT_ICON_SIZE = 5;
  private static final Point NULL_POINT = new Point(-1, -1);
  private static final int SELECT_COLUMNS_GRID_ROWS = 15;
  private static final int SEARCH_FIELD_COLUMNS = 8;

  /**
   * Notified when the table summary panel is made visible or hidden
   */
  private final Event<Boolean> summaryPanelVisibleChangedEvent = Events.event();

  /**
   * The table model
   */
  private final FilteredTableModel<T, C> tableModel;

  /**
   * Provides filter panels
   */
  private final ColumnCriteriaPanelProvider<C> searchPanelProvider;

  /**
   * the property filter panels
   */
  private final Map<TableColumn, ColumnCriteriaPanel<C>> columnFilterPanels = new HashMap<>();

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
   * the JTable for showing the underlying entities
   */
  private final JTable table;

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
   * The coordinate of the last search result
   */
  private Point lastSearchResultCoordinate = NULL_POINT;

  /**
   * The text field used for entering the search criteria
   */
  private final JTextField searchField;

  /**
   * If true then sorting via the table header is enabled
   */
  private boolean sortingEnabled = true;

  /**
   * If true then the JTable scrolls to the item selected in the table model
   */
  private boolean scrollToSelectedItem = true;

  /**
   * Instantiates a new FilteredTablePanel.
   * @param tableModel the table model
   */
  public FilteredTablePanel(final FilteredTableModel<T, C> tableModel) {
    this(tableModel, new ColumnCriteriaPanelProvider<C>() {
      @Override
      public ColumnCriteriaPanel<C> createColumnCriteriaPanel(final TableColumn column) {
        //noinspection unchecked
        return new ColumnCriteriaPanel<>(tableModel.getColumnModel().getColumnFilterModel((C) column.getIdentifier()), true, true);
      }
    });
  }

  /**
   * Instantiates a new FilteredTablePanel.
   * @param tableModel the table model
   * @param criteriaPanelProvider the column criteria panel provider
   * the column filter models found in the table model
   */
  public FilteredTablePanel(final FilteredTableModel<T, C> tableModel, final ColumnCriteriaPanelProvider<C> criteriaPanelProvider) {
    Util.rejectNullValue(tableModel, "tableModel");
    this.tableModel = tableModel;
    this.searchPanelProvider = criteriaPanelProvider;
    this.table = initializeJTable();
    this.tableScrollPane = new JScrollPane(table);
    this.horizontalTableScrollBar = tableScrollPane.getHorizontalScrollBar();
    this.searchField = initializeSearchField();
    this.basePanel = new JPanel(new BorderLayout());
    this.basePanel.add(tableScrollPane, BorderLayout.CENTER);
    this.summaryPanel = new FilteredTableSummaryPanel(tableModel);
    this.summaryBasePanel = new JPanel(new BorderLayout());
    this.summaryScrollPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    this.tableScrollPane.getViewport().addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(final ChangeEvent e) {
        horizontalTableScrollBar.setVisible(tableScrollPane.getViewport().getViewSize().width > tableScrollPane.getSize().width);
        revalidate();
      }
    });
    this.summaryScrollPane.getHorizontalScrollBar().setModel(horizontalTableScrollBar.getModel());
    basePanel.add(summaryBasePanel, BorderLayout.SOUTH);
    setLayout(new BorderLayout());
    add(basePanel, BorderLayout.CENTER);
    initializeTableHeader();
    bindEvents();
  }

  /**
   * Hides or shows the active filter panels for this table panel
   * @param value true if the active filter panels should be shown, false if they should be hidden
   */
  public final void setFilterPanelsVisible(final boolean value) {
    for (final ColumnCriteriaPanel columnFilterPanel : columnFilterPanels.values()) {
      if (value) {
        columnFilterPanel.showDialog();
      }
      else {
        columnFilterPanel.hideDialog();
      }
    }
  }

  /**
   * @return the TableModel used by this TablePanel
   */
  public final FilteredTableModel<T, C> getTableModel() {
    return tableModel;
  }

  /**
   * @return the JTable instance
   */
  public final JTable getJTable() {
    return table;
  }

  /**
   * @return the text field used to enter a search criteria
   * @see #initializeSearchField()
   */
  public final JTextField getSearchField() {
    return searchField;
  }

  /**
   * @return the JScrollPane containing the table
   */
  public final JScrollPane getTableScrollPane() {
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
  public final void setSummaryPanelVisible(final boolean visible) {
    if (visible && isSummaryPanelVisible()) {
      return;
    }

    if (summaryScrollPane != null) {
      summaryScrollPane.getViewport().setView(visible ? summaryPanel : null);
      if (visible) {
        summaryBasePanel.add(summaryScrollPane, BorderLayout.NORTH);
        summaryBasePanel.add(horizontalTableScrollBar, BorderLayout.SOUTH);
      }
      else {
        summaryBasePanel.remove(horizontalTableScrollBar);
        getTableScrollPane().setHorizontalScrollBar(horizontalTableScrollBar);
      }
      revalidate();
      summaryPanelVisibleChangedEvent.fire(visible);
    }
  }

  /**
   * @return true if the column summary panel is visible, false if it is hidden
   */
  public final boolean isSummaryPanelVisible() {
    return summaryScrollPane != null && summaryScrollPane.getViewport().getView() == summaryPanel;
  }

  /**
   * Scrolls to the given coordinate.
   * @param row the row
   * @param column the column
   */
  public final void scrollToCoordinate(final int row, final int column) {
    table.scrollRectToVisible(table.getCellRect(row, column, true));
  }

  /**
   * Scrolls the given row and column to the center of the view.
   * Calling this method has no effect if the table is not contained in a JScrollPane.
   * @param rowIndex the row
   * @param colIndex the column
   * Based on http://www.exampledepot.com/egs/javax.swing.table/VisCenter.html
   */
  public final void scrollToCenter(final int rowIndex, final int colIndex) {
    if (!(table.getParent() instanceof JViewport)) {
      return;
    }
    final JViewport viewport = (JViewport) table.getParent();
    // This rectangle is relative to the table where the northwest corner of cell (0,0) is always (0,0)
    final Rectangle cellRectangle = table.getCellRect(rowIndex, colIndex, true);
    // The location of the view relative to the table
    final Rectangle viewRectangle = viewport.getViewRect();
    // Translate the cell location so that it is relative to the view, assuming the northwest corner of the view is (0,0)
    cellRectangle.setLocation(cellRectangle.x - viewRectangle.x, cellRectangle.y - viewRectangle.y);
    // Calculate location of viewRectangle if it were at the center of view
    int centerX = (viewRectangle.width- cellRectangle.width) / 2;
    int centerY = (viewRectangle.height- cellRectangle.height) / 2;
    // Fake the location of the cell so that scrollRectToVisible will move the cell to the center
    if (cellRectangle.x < centerX) {
      centerX = -centerX;
    }
    if (cellRectangle.y < centerY) {
      centerY = -centerY;
    }
    cellRectangle.translate(centerX, centerY);
    viewport.scrollRectToVisible(cellRectangle);
  }

  /**
   * Assumes table is contained in a JScrollPane.
   * @param rowIndex the row index
   * @param colIndex the column index
   * @return true if the cell (rowIndex, vColIndex) is visible within the viewport.
   * Based on http://www.exampledepot.com/egs/javax.swing.table/IsVis.html
   */
  public final boolean isCellVisible(final int rowIndex, final int colIndex) {
    if (!(table.getParent() instanceof JViewport)) {
      return false;
    }
    final JViewport viewport = (JViewport) table.getParent();
    // This rectangle is relative to the table where the northwest corner of cell (0,0) is always (0,0)
    final Rectangle cellRectangle = table.getCellRect(rowIndex, colIndex, true);
    // The location of the viewport relative to the table
    final Point viewPosition = viewport.getViewPosition();
    // Translate the cell location so that it is relative to the view, assuming the northwest corner of the view is (0,0)
    cellRectangle.setLocation(cellRectangle.x - viewPosition.x, cellRectangle.y - viewPosition.y);

    return new Rectangle(viewport.getExtentSize()).intersects(cellRectangle);
  }

  /**
   * @return a control for showing the column selection dialog
   */
  public final Control getSelectColumnsControl() {
    return Controls.methodControl(this, "selectTableColumns",
            Messages.get(Messages.SELECT_COLUMNS) + "...", null,
            Messages.get(Messages.SELECT_COLUMNS));
  }

  /**
   * Initializes the button used to toggle the summary panel state (hidden and visible)
   * @return a summary panel toggle button
   */
  public final Control getToggleSummaryPanelControl() {
    final Control toggleControl = Controls.toggleControl(this, "summaryPanelVisible", null,
            summaryPanelVisibleChangedEvent);
    toggleControl.setIcon(Images.loadImage("Sum16.gif"));
    toggleControl.setDescription(Messages.get(Messages.TOGGLE_SUMMARY_TIP));

    return toggleControl;
  }

  /**
   * @return true if sorting via the table header is enabled
   */
  public final boolean isSortingEnabled() {
    return sortingEnabled;
  }

  /**
   * @param sortingEnabled true if sorting via the table header should be enabled
   */
  public final void setSortingEnabled(final boolean sortingEnabled) {
    this.sortingEnabled = sortingEnabled;
  }

  /**
   * @return true if the JTable instance scrolls automatically to the coordinate
   * of the record selected in the underlying table model
   */
  public final boolean isScrollToSelectedItem() {
    return scrollToSelectedItem;
  }

  /**
   * @param scrollToSelectedItem true if the JTable instance should scroll automatically
   * to the coordinate of the record selected in the underlying table model
   */
  public final void setScrollToSelectedItem(final boolean scrollToSelectedItem) {
    this.scrollToSelectedItem = scrollToSelectedItem;
  }

  /**
   * Shows a dialog for selecting which columns to show/hide
   */
  @SuppressWarnings({"unchecked"})
  public final void selectTableColumns() {
    final List<TableColumn> allColumns = new ArrayList<>(tableModel.getColumnModel().getAllColumns());
    Collections.sort(allColumns, new Comparator<TableColumn>() {
      private final Collator collator = Collator.getInstance();
      @Override
      public int compare(final TableColumn o1, final TableColumn o2) {
        return Util.collateSansSpaces(collator, o1.getIdentifier().toString(), o2.getIdentifier().toString());
      }
    });
    final List<JCheckBox> checkBoxes = new ArrayList<>();
    final int result = JOptionPane.showOptionDialog(this, initializeSelectColumnsPanel(allColumns, checkBoxes),
            Messages.get(Messages.SELECT_COLUMNS), JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null,
            new String[] {Messages.get(Messages.SHOW_ALL_COLUMNS), Messages.get(Messages.CANCEL), Messages.get(Messages.OK)}, Messages.get(Messages.OK));
    if (result != 1) {
      if (result == 0) {
        setSelected(checkBoxes, true);
      }
      for (final JCheckBox chkButton : checkBoxes) {
        final TableColumn column = allColumns.get(checkBoxes.indexOf(chkButton));
        tableModel.getColumnModel().setColumnVisible((C) column.getIdentifier(), chkButton.isSelected());
      }
    }
  }

  /**
   * @param listener a listener notified each time the summary panel visibility changes
   */
  public final void addSummaryPanelVisibleListener(final EventListener listener) {
    summaryPanelVisibleChangedEvent.addListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public final void removeSummaryPanelVisibleListener(final EventListener listener) {
    summaryPanelVisibleChangedEvent.removeListener(listener);
  }

  /**
   * Performs a text search in the underlying table model, relative to the last search result coordinate.
   * @param addToSelection if true then the items found are added to the selection
   * @param forward if true then the search direction is forward (down), otherwise it's backward (up)
   * @param searchText the text to search for
   */
  final void findNextValue(final boolean addToSelection, final boolean forward, final String searchText) {
    performSearch(addToSelection, lastSearchResultCoordinate.y + (forward ? 1 : -1), forward, searchText);
  }

  private JTable initializeJTable() {
    return new JTable(tableModel, tableModel.getColumnModel(), tableModel.getSelectionModel());
  }

  private JTextField initializeSearchField() {
    final JTextField txtSearch = new JTextField();
    txtSearch.setBackground((Color) UIManager.getLookAndFeel().getDefaults().get("TextField.inactiveBackground"));
    txtSearch.setColumns(SEARCH_FIELD_COLUMNS);
    TextFieldHint.enable(txtSearch, Messages.get(Messages.SEARCH_FIELD_HINT));
    txtSearch.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      public void contentsChanged(final DocumentEvent e) {
        performSearch(false, lastSearchResultCoordinate.y == -1 ? 0 : lastSearchResultCoordinate.y, true, txtSearch.getText());
      }
    });
    txtSearch.addKeyListener(new KeyAdapter() {
      @Override
      public void keyReleased(final KeyEvent e) {
        if (e.getModifiers() != 0) {
          return;
        }
        if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_DOWN) {
          findNextValue(e.isShiftDown(), true, txtSearch.getText());
        }
        else if (e.getKeyCode() == KeyEvent.VK_UP) {
          findNextValue(e.isShiftDown(), false, txtSearch.getText());
        }
        else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
          getJTable().requestFocusInWindow();
        }
      }
    });
    UiUtil.selectAllOnFocusGained(txtSearch);

    txtSearch.setComponentPopupMenu(initializeSearchFieldPopupMenu());

    return txtSearch;
  }

  private void performSearch(final boolean addToSelection, final int fromIndex, final boolean forward, final String searchText) {
    if (searchText.length() != 0) {
      final Point coordinate = tableModel.findNextItemCoordinate(fromIndex, forward, searchText);
      if (coordinate != null) {
        lastSearchResultCoordinate = coordinate;
        if (addToSelection) {
          tableModel.getSelectionModel().addSelectedIndex(coordinate.y);
        }
        else {
          tableModel.getSelectionModel().setSelectedIndex(coordinate.y);
          table.setColumnSelectionInterval(coordinate.x, coordinate.x);
        }
        scrollToCenter(coordinate.y, coordinate.x);
      }
      else {
        lastSearchResultCoordinate = NULL_POINT;
      }
    }
    else {
      lastSearchResultCoordinate = NULL_POINT;
    }
  }

  private JPopupMenu initializeSearchFieldPopupMenu() {
    final JCheckBox boxRegexp = new JCheckBox(Messages.get(Messages.REGULAR_EXPRESSION_SEARCH), tableModel.isRegularExpressionSearch());
    final JPanel panel = new JPanel(UiUtil.createGridLayout(1, 1));
    panel.add(boxRegexp);

    final AbstractAction action = new AbstractAction(Messages.get(Messages.OK)) {
      @Override
      public void actionPerformed(final ActionEvent e) {
        tableModel.setRegularExpressionSearch(boxRegexp.isSelected());
      }
    };
    action.putValue(Action.MNEMONIC_KEY, Messages.get(Messages.OK_MNEMONIC).charAt(0));

    final JPopupMenu popupMenu = new JPopupMenu();
    popupMenu.add(new AbstractAction(Messages.get(Messages.SETTINGS)) {
      @Override
      public void actionPerformed(final ActionEvent e) {
        UiUtil.displayInDialog(FilteredTablePanel.this, panel, Messages.get(Messages.SETTINGS), action);
      }
    });

    return popupMenu;
  }

  @SuppressWarnings({"unchecked"})
  private JPanel initializeSelectColumnsPanel(final List<TableColumn> allColumns, final List<JCheckBox> checkBoxes) {
    final JPanel togglePanel = new JPanel(new GridLayout(Math.min(SELECT_COLUMNS_GRID_ROWS, allColumns.size()), 0));
    for (final TableColumn column : allColumns) {
      final JCheckBox chkColumn = new JCheckBox(column.getHeaderValue().toString(), tableModel.getColumnModel().isColumnVisible((C) column.getIdentifier()));
      checkBoxes.add(chkColumn);
      togglePanel.add(chkColumn);
    }
    final JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    southPanel.add(new JButton(new AbstractAction(Messages.get(Messages.SELECT_ALL)) {
      @Override
      public void actionPerformed(final ActionEvent e) {
        setSelected(checkBoxes, true);
      }
    }));
    southPanel.add(new JButton(new AbstractAction(Messages.get(Messages.SELECT_NONE)) {
      @Override
      public void actionPerformed(final ActionEvent e) {
        setSelected(checkBoxes, false);
      }
    }));

    final JPanel base = new JPanel(UiUtil.createBorderLayout());
    base.add(new JScrollPane(togglePanel), BorderLayout.CENTER);
    base.add(southPanel, BorderLayout.SOUTH);

    return base;
  }

  private void initializeTableHeader() {
    table.getTableHeader().setReorderingAllowed(true);
    table.getTableHeader().addMouseListener(new MouseSortHandler());
    table.getTableHeader().setDefaultRenderer(new SortableHeaderRenderer(table.getTableHeader().getDefaultRenderer()));
    table.getTableHeader().addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(final MouseEvent e) {
        if (e.isAltDown() && e.isControlDown()) {
          toggleColumnFilterPanel(e);
        }
      }
    });
  }

  @SuppressWarnings({"unchecked"})
  private void bindEvents() {
    tableModel.addSortingListener(new EventListener() {
      @Override
      public void eventOccurred() {
        table.getTableHeader().repaint();
      }
    });
    tableModel.getSelectionModel().addSelectedIndexListener(new EventListener() {
      @Override
      public void eventOccurred() {
        if (scrollToSelectedItem && !tableModel.getSelectionModel().isSelectionEmpty()) {
          scrollToCoordinate(tableModel.getSelectionModel().getSelectedIndex(), table.getSelectedColumn());
        }
      }
    });
    tableModel.addRefreshStartedListener(new EventListener() {
      @Override
      public void eventOccurred() {
        UiUtil.setWaitCursor(true, FilteredTablePanel.this);
      }
    });
    tableModel.addRefreshDoneListener(new EventListener() {
      @Override
      public void eventOccurred() {
        UiUtil.setWaitCursor(false, FilteredTablePanel.this);
      }
    });
    for (final TableColumn column : tableModel.getColumnModel().getAllColumns()) {
      final ColumnCriteriaModel model = tableModel.getColumnModel().getColumnFilterModel((C) column.getIdentifier());
      if (model != null) {
        model.addCriteriaStateListener(new EventListener() {
          @Override
          public void eventOccurred() {
            if (model.isEnabled()) {
              addFilterIndicator(column);
            }
            else {
              removeFilterIndicator(column);
            }

            getJTable().getTableHeader().repaint();
          }
        });
        if (model.isEnabled()) {
          addFilterIndicator(column);
        }
      }
    }
    UiUtil.addKeyEvent(table, KeyEvent.VK_LEFT, KeyEvent.ALT_DOWN_MASK, new ResizeSelectedColumnAction(table, false));
    UiUtil.addKeyEvent(table, KeyEvent.VK_RIGHT, KeyEvent.ALT_DOWN_MASK, new ResizeSelectedColumnAction(table, true));
    UiUtil.addKeyEvent(table, KeyEvent.VK_LEFT, KeyEvent.ALT_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK, new MoveSelectedColumnAction(table, true));
    UiUtil.addKeyEvent(table, KeyEvent.VK_RIGHT, KeyEvent.ALT_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK, new MoveSelectedColumnAction(table, false));
  }

  private void toggleColumnFilterPanel(final MouseEvent event) {
    final int index = tableModel.getColumnModel().getColumnIndexAtX(event.getX());
    final TableColumn column = tableModel.getColumnModel().getColumn(index);
    if (!columnFilterPanels.containsKey(column)) {
      columnFilterPanels.put(column, searchPanelProvider.createColumnCriteriaPanel(column));
    }

    toggleFilterPanel(event.getLocationOnScreen(), columnFilterPanels.get(column), table);
  }

  private static void toggleFilterPanel(final Point position, final ColumnCriteriaPanel columnFilterPanel,
                                        final Container parent) {
    if (columnFilterPanel.isDialogEnabled()) {
      columnFilterPanel.disableDialog();
    }
    else {
      columnFilterPanel.enableDialog(parent, position);
    }
  }

  private static void addFilterIndicator(final TableColumn column) {
    String val = (String) column.getHeaderValue();
    if (val.length() != 0 && val.charAt(0) != FILTER_INDICATOR) {
      val = FILTER_INDICATOR + val;
    }

    column.setHeaderValue(val);
  }

  private static void removeFilterIndicator(final TableColumn column) {
    String val = (String) column.getHeaderValue();
    if (val.length() != 0 && val.charAt(0) == FILTER_INDICATOR) {
      val = val.substring(1);
    }

    column.setHeaderValue(val);
  }

  private static void setSelected(final List<JCheckBox> checkBoxes, final boolean selected) {
    for (final JCheckBox box : checkBoxes) {
      box.setSelected(selected);
    }
  }

  /**
   * Responsible for creating {@link ColumnCriteriaPanel}s
   * @param <C> the type used as column identifier
   */
  public interface ColumnCriteriaPanelProvider<C> {
    /**
     * Creates a ColumnCriteriaPanel for the given column
     * @param column the column
     * @return a ColumnCriteriaPanel
     */
    ColumnCriteriaPanel<C> createColumnCriteriaPanel(final TableColumn column);
  }

  private final class MouseSortHandler extends MouseAdapter {
    @SuppressWarnings({"unchecked"})
    @Override
    public void mouseClicked(final MouseEvent e) {
      if (!sortingEnabled || e.getButton() != MouseEvent.BUTTON1 || e.isAltDown()) {
        return;
      }

      final JTableHeader tableHeader = (JTableHeader) e.getSource();
      final TableColumnModel columnModel = tableHeader.getColumnModel();
      final int index = columnModel.getColumnIndexAtX(e.getX());
      if (index >= 0) {
        final C columnIdentifier = (C) columnModel.getColumn(index).getIdentifier();
        SortingDirective status = tableModel.getSortModel().getSortingDirective(columnIdentifier);
        final boolean shiftDown = e.isShiftDown();
        switch (status) {
          case UNSORTED:
            if (shiftDown) {
              status = SortingDirective.DESCENDING;
            }
            else {
              status = SortingDirective.ASCENDING;
            }
            break;
          case ASCENDING:
            status = SortingDirective.DESCENDING;
            break;
          default://case DESCENDING:
            status = SortingDirective.ASCENDING;
            break;
        }

        tableModel.getSortModel().setSortingDirective(columnIdentifier, status, e.isControlDown());
      }
    }
  }

  private final class SortableHeaderRenderer implements TableCellRenderer {
    private final TableCellRenderer tableCellRenderer;

    private SortableHeaderRenderer(final TableCellRenderer tableCellRenderer) {
      this.tableCellRenderer = tableCellRenderer;
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
                                                   final boolean hasFocus, final int row, final int column) {
      final Component component = tableCellRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      if (component instanceof JLabel) {
        final JLabel label = (JLabel) component;
        final TableColumn tableColumn = table.getColumnModel().getColumn(column);
        label.setHorizontalTextPosition(JLabel.LEFT);
        label.setIcon(getHeaderRendererIcon((C) tableColumn.getIdentifier(), label.getFont().getSize() + SORT_ICON_SIZE));
      }

      return component;
    }

    private Icon getHeaderRendererIcon(final C columnIdentifier, final int iconSizePixels) {
      final SortingDirective directive = tableModel.getSortModel().getSortingDirective(columnIdentifier);
      if (directive == SortingDirective.UNSORTED) {
        return null;
      }

      return new Arrow(directive == SortingDirective.DESCENDING, iconSizePixels, tableModel.getSortModel().getSortingPriority(columnIdentifier));
    }
  }

  private static final class Arrow implements Icon {
    private static final double PRIORITY_SIZE_RATIO = 0.8;
    private static final double PRIORITY_SIZE_CONST = 2.0;
    public static final int ALIGNMENT_CONSTANT = 6;
    private final boolean descending;
    private final int size;
    private final int priority;

    private Arrow(final boolean descending, final int size, final int priority) {
      this.descending = descending;
      this.size = size;
      this.priority = priority;
    }

    @Override
    public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
      final Color color = c == null ? Color.GRAY : c.getBackground();
      // In a compound sort, make each successive triangle 20% smaller than the previous one.
      final int dx = (int)(size/PRIORITY_SIZE_CONST * Math.pow(PRIORITY_SIZE_RATIO, priority));
      final int dy = descending ? dx : -dx;
      // Align icon (roughly) with font baseline.
      final int theY = y + SORT_ICON_SIZE * size / ALIGNMENT_CONSTANT + (descending ? -dy : 0);
      final int shift = descending ? 1 : -1;
      g.translate(x, theY);

      // Right diagonal.
      g.setColor(color.darker());
      g.drawLine(dx / 2, dy, 0, 0);
      g.drawLine(dx / 2, dy + shift, 0, shift);

      // Left diagonal.
      g.setColor(color.brighter());
      g.drawLine(dx / 2, dy, dx, 0);
      g.drawLine(dx / 2, dy + shift, dx, shift);

      // Horizontal line.
      if (descending) {
        g.setColor(color.darker().darker());
      }
      else {
        g.setColor(color.brighter().brighter());
      }
      g.drawLine(dx, 0, 0, 0);

      g.setColor(color);
      g.translate(-x, -theY);
    }

    @Override
    public int getIconWidth() {
      return size;
    }

    @Override
    public int getIconHeight() {
      return size;
    }
  }

  /**
   * Resizes the selected table column by 10 pixels.
   */
  private static final class ResizeSelectedColumnAction extends  AbstractAction {

    private static final int RESIZE_AMOUNT = 10;
    private final JTable table;
    private final boolean enlarge;

    private ResizeSelectedColumnAction(final JTable table, final boolean enlarge) {
      super("FilteredTablePanel.column" + (enlarge ? "Larger" : "Smaller"));
      this.table = table;
      this.enlarge = enlarge;
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
      if (table.getAutoResizeMode() == JTable.AUTO_RESIZE_OFF) {
        final int selectedColumnIndex = table.getSelectedColumn();
        if (selectedColumnIndex != -1) {
          final TableColumn column = table.getColumnModel().getColumn(selectedColumnIndex);
          column.setPreferredWidth(column.getWidth() + (enlarge ? RESIZE_AMOUNT : -RESIZE_AMOUNT));
        }
      }
    }
  }

  /**
   * Moves the selected table column by one either left or right, with wrap around
   */
  private static final class MoveSelectedColumnAction extends  AbstractAction {

    private final JTable table;
    private final boolean left;

    private MoveSelectedColumnAction(final JTable table, final boolean left) {
      super("FilteredTablePanel.column" + (left ? "Left" : "Right"));
      this.table = table;
      this.left = left;
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
      final TableColumnModel columnModel = table.getColumnModel();
      final int selectedColumnIndex = table.getSelectedColumn();
      if (selectedColumnIndex != -1) {
        final int columnCount = columnModel.getColumnCount();
        final int newIndex;
        if (left) {
          if (selectedColumnIndex == 0) {
            newIndex = columnCount - 1;
          }
          else {
            newIndex = selectedColumnIndex - 1;
          }
        }
        else {
          if (selectedColumnIndex == columnCount - 1) {
            newIndex = 0;
          }
          else {
            newIndex = selectedColumnIndex + 1;
          }
        }
        table.moveColumn(selectedColumnIndex, newIndex);
        table.scrollRectToVisible(table.getCellRect(table.getSelectedRow(), newIndex, true));
      }
    }
  }
}
