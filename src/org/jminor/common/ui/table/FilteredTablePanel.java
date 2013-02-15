/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.table;

import org.jminor.common.i18n.Messages;
import org.jminor.common.model.DocumentAdapter;
import org.jminor.common.model.EventAdapter;
import org.jminor.common.model.Util;
import org.jminor.common.model.table.ColumnSearchModel;
import org.jminor.common.model.table.FilteredTableModel;
import org.jminor.common.model.table.SortingDirective;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.Control;
import org.jminor.common.ui.control.Controls;
import org.jminor.common.ui.textfield.TextFieldHint;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
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
import java.util.Enumeration;
import java.util.List;

/**
 * A UI component based on a FilteredTableModel.
 * @see FilteredTableModel
 */
public class FilteredTablePanel<T, C> extends JPanel {

  public static final char FILTER_INDICATOR = '*';

  private static final Point NULL_POINT = new Point(-1, -1);
  private static final int SELECT_COLUMNS_GRID_ROWS = 15;

  /**
   * The table model
   */
  private final FilteredTableModel<T, C> tableModel;

  /**
   * the property filter panels
   */
  private final List<ColumnSearchPanel<C>> columnFilterPanels;

  /**
   * the JTable for showing the underlying entities
   */
  private final JTable table;

  /**
   * the scroll pane used by the JTable instance
   */
  private final JScrollPane tableScrollPane;

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
    this(tableModel, initializeFilterPanels(tableModel));
  }

  /**
   * Instantiates a new FilteredTablePanel.
   * @param tableModel the table model
   * @param columnFilterPanels the column filter panels to use, these must be based on
   * the column filter models found in the table model
   */
  public FilteredTablePanel(final FilteredTableModel<T, C> tableModel, final List<ColumnSearchPanel<C>> columnFilterPanels) {
    Util.rejectNullValue(tableModel, "tableModel");
    this.tableModel = tableModel;
    this.table = initializeJTable();
    this.tableScrollPane = new JScrollPane(table);
    this.searchField = initializeSearchField();
    this.columnFilterPanels = columnFilterPanels;
    initializeTableHeader();
    bindEvents();
  }

  /**
   * @return the column search panels serving as column filter panels
   */
  public final List<ColumnSearchPanel<C>> getColumnFilterPanels() {
    return Collections.unmodifiableList(columnFilterPanels);
  }

  /**
   * Hides or shows the active filter panels for this table panel
   * @param value true if the active filter panels should be shown, false if they should be hidden
   */
  public final void setFilterPanelsVisible(final boolean value) {
    for (final ColumnSearchPanel columnFilterPanel : getColumnFilterPanels()) {
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
   * @return the scrollpane the containing the table
   */
  public final JScrollPane getTableScrollPane() {
    return tableScrollPane;
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
    final List<TableColumn> allColumns = Collections.list(tableModel.getColumnModel().getColumns());
    allColumns.addAll(tableModel.getHiddenColumns());
    Collections.sort(allColumns, new Comparator<TableColumn>() {
      private final Collator collator = Collator.getInstance();
      /** {@inheritDoc} */
      @Override
      public int compare(final TableColumn o1, final TableColumn o2) {
        return Util.collateSansSpaces(collator, o1.getIdentifier().toString(), o2.getIdentifier().toString());
      }
    });
    final JPanel togglePanel = new JPanel(new GridLayout(Math.min(SELECT_COLUMNS_GRID_ROWS, allColumns.size()), 0));
    final List<JCheckBox> buttonList = new ArrayList<JCheckBox>();
    for (final TableColumn column : allColumns) {
      final JCheckBox chkColumn = new JCheckBox(column.getHeaderValue().toString(), tableModel.isColumnVisible((C) column.getIdentifier()));
      buttonList.add(chkColumn);
      togglePanel.add(chkColumn);
    }
    final JScrollPane scroller = new JScrollPane(togglePanel);
    final int result = JOptionPane.showOptionDialog(this, scroller,
            Messages.get(Messages.SELECT_COLUMNS), JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
            new String[] {Messages.get(Messages.SHOW_ALL_COLUMNS), Messages.get(Messages.CANCEL), Messages.get(Messages.OK)}, Messages.get(Messages.OK));
    if (result != 1) {
      if (result == 0) {
        for (final JCheckBox box : buttonList) {
          box.setSelected(true);
        }
      }
      for (final JCheckBox chkButton : buttonList) {
        final TableColumn column = allColumns.get(buttonList.indexOf(chkButton));
        tableModel.setColumnVisible((C) column.getIdentifier(), chkButton.isSelected());
      }
    }
  }

  private JTable initializeJTable() {
    return new JTable(tableModel, tableModel.getColumnModel(), tableModel.getSelectionModel());
  }

  private JTextField initializeSearchField() {
    final JTextField txtSearch = new JTextField();
    txtSearch.setBackground((Color) UIManager.getLookAndFeel().getDefaults().get("TextField.inactiveBackground"));
    txtSearch.setColumns(8);
    TextFieldHint.enable(txtSearch, Messages.get(Messages.SEARCH_FIELD_HINT));
    txtSearch.getDocument().addDocumentListener(new DocumentAdapter() {
      /** {@inheritDoc} */
      @Override
      public void contentsChanged(final DocumentEvent e) {
        doSearch(false, lastSearchResultCoordinate.y == -1 ? 0 : lastSearchResultCoordinate.y, true, txtSearch.getText());
      }
    });
    txtSearch.addKeyListener(new KeyAdapter() {
      /** {@inheritDoc} */
      @Override
      public void keyReleased(final KeyEvent e) {
        if (e.getModifiers() != 0) {
          return;
        }
        if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_DOWN) {
          doSearch(e.isShiftDown(), lastSearchResultCoordinate.y + 1, true, txtSearch.getText());
        }
        else if (e.getKeyCode() == KeyEvent.VK_UP) {
          doSearch(e.isShiftDown(), lastSearchResultCoordinate.y - 1, false, txtSearch.getText());
        }
      }
    });
    UiUtil.selectAllOnFocusGained(txtSearch);
    UiUtil.addKeyEvent(txtSearch, KeyEvent.VK_ESCAPE, new AbstractAction("FilteredTablePanel.requestTableFocus") {
      /** {@inheritDoc} */
      @Override
      public void actionPerformed(final ActionEvent e) {
        getJTable().requestFocusInWindow();
      }
    });

    txtSearch.setComponentPopupMenu(initializeSearchFieldPopupMenu());

    return txtSearch;
  }

  private void doSearch(final boolean addToSelection, final int fromIndex, final boolean forward,
                        final String searchText) {
    if (searchText.length() != 0) {
      final Point coordinate = tableModel.findNextItemCoordinate(fromIndex, forward, searchText);
      if (coordinate != null) {
        lastSearchResultCoordinate = coordinate;
        if (addToSelection) {
          tableModel.addSelectedIndex(coordinate.y);
        }
        else {
          tableModel.setSelectedIndex(coordinate.y);
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
      /** {@inheritDoc} */
      @Override
      public void actionPerformed(final ActionEvent e) {
        tableModel.setRegularExpressionSearch(boxRegexp.isSelected());
      }
    };
    action.putValue(Action.MNEMONIC_KEY, Messages.get(Messages.OK_MNEMONIC).charAt(0));

    final JPopupMenu popupMenu = new JPopupMenu();
    popupMenu.add(new AbstractAction(Messages.get(Messages.SETTINGS)) {
      /** {@inheritDoc} */
      @Override
      public void actionPerformed(final ActionEvent e) {
        UiUtil.showInDialog(UiUtil.getParentWindow(FilteredTablePanel.this), panel, true,
                Messages.get(Messages.SETTINGS), true, true, action);
      }
    });

    return popupMenu;
  }

  private void initializeTableHeader() {
    table.getTableHeader().addMouseListener(new MouseSortHandler());
    table.getTableHeader().setDefaultRenderer(new SortableHeaderRenderer(table.getTableHeader().getDefaultRenderer()));
    table.getTableHeader().addMouseListener(new MouseAdapter() {
      /** {@inheritDoc} */
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
    tableModel.addSortingListener(new EventAdapter() {
      /** {@inheritDoc} */
      @Override
      public void eventOccurred() {
        table.getTableHeader().repaint();
      }
    });
    tableModel.addSelectedIndexListener(new EventAdapter() {
      /** {@inheritDoc} */
      @Override
      public void eventOccurred() {
        if (scrollToSelectedItem && !tableModel.isSelectionEmpty()) {
          scrollToCoordinate(tableModel.getSelectedIndex(), table.getSelectedColumn());
        }
      }
    });
    tableModel.addRefreshStartedListener(new EventAdapter() {
      /** {@inheritDoc} */
      @Override
      public void eventOccurred() {
        UiUtil.setWaitCursor(true, FilteredTablePanel.this);
      }
    });
    tableModel.addRefreshDoneListener(new EventAdapter() {
      /** {@inheritDoc} */
      @Override
      public void eventOccurred() {
        UiUtil.setWaitCursor(false, FilteredTablePanel.this);
      }
    });
    final Enumeration<TableColumn> columns = tableModel.getColumnModel().getColumns();
    while (columns.hasMoreElements()) {
      final TableColumn column = columns.nextElement();
      final ColumnSearchModel model = tableModel.getFilterModel((C) column.getIdentifier());
      if (model != null) {
        model.addSearchStateListener(new EventAdapter() {
          /** {@inheritDoc} */
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
  }

  private void toggleColumnFilterPanel(final MouseEvent event) {
    final int index = tableModel.getColumnModel().getColumnIndexAtX(event.getX());

    toggleFilterPanel(event.getLocationOnScreen(), columnFilterPanels.get(index), table);
  }

  private static void toggleFilterPanel(final Point position, final ColumnSearchPanel columnFilterPanel,
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

  @SuppressWarnings({"unchecked"})
  private static <T, C> List<ColumnSearchPanel<C>> initializeFilterPanels(final FilteredTableModel<T, C> tableModel) {
    Util.rejectNullValue(tableModel, "tableModel");
    final List<ColumnSearchPanel<C>> filterPanels = new ArrayList<ColumnSearchPanel<C>>(tableModel.getColumnCount());
    final Enumeration<TableColumn> columns = tableModel.getColumnModel().getColumns();
    while (columns.hasMoreElements()) {
      final ColumnSearchModel<C> model = tableModel.getFilterModel((C) columns.nextElement().getIdentifier());
      filterPanels.add(new ColumnSearchPanel<C>(model, true, true));
    }

    return filterPanels;
  }

  private final class MouseSortHandler extends MouseAdapter {
    /** {@inheritDoc} */
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
        SortingDirective status = tableModel.getSortingDirective(columnIdentifier);
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
          case DESCENDING:
            status = SortingDirective.ASCENDING;
            break;
        }

        tableModel.setSortingDirective(columnIdentifier, status, e.isControlDown());
      }
    }
  }

  private final class SortableHeaderRenderer implements TableCellRenderer {
    private final TableCellRenderer tableCellRenderer;

    private SortableHeaderRenderer(final TableCellRenderer tableCellRenderer) {
      this.tableCellRenderer = tableCellRenderer;
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings({"unchecked"})
    public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
                                                   final boolean hasFocus, final int row, final int column) {
      final Component component = tableCellRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      if (component instanceof JLabel) {
        final JLabel label = (JLabel) component;
        final TableColumn tableColumn = table.getColumnModel().getColumn(column);
        label.setIcon(getHeaderRendererIcon((C) tableColumn.getIdentifier(), label.getFont().getSize() + 5));
      }

      return component;
    }

    private Icon getHeaderRendererIcon(final C columnIdentifier, final int iconSizePixels) {
      final SortingDirective directive = tableModel.getSortingDirective(columnIdentifier);
      if (directive == SortingDirective.UNSORTED) {
        return null;
      }

      return new Arrow(directive == SortingDirective.DESCENDING, iconSizePixels, tableModel.getSortingPriority(columnIdentifier));
    }
  }

  private static final class Arrow implements Icon {
    private static final double PRIORITY_SIZE_RATIO = 0.8;
    private static final double PRIORITY_SIZE_CONST = 2.0;
    private final boolean descending;
    private final int size;
    private final int priority;

    private Arrow(final boolean descending, final int size, final int priority) {
      this.descending = descending;
      this.size = size;
      this.priority = priority;
    }

    /** {@inheritDoc} */
    @Override
    public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
      final Color color = c == null ? Color.GRAY : c.getBackground();
      // In a compound sort, make each successive triangle 20% smaller than the previous one.
      final int dx = (int)(size/PRIORITY_SIZE_CONST * Math.pow(PRIORITY_SIZE_RATIO, priority));
      final int dy = descending ? dx : -dx;
      // Align icon (roughly) with font baseline.
      final int theY = y + 5 * size / 6 + (descending ? -dy : 0);
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

    /** {@inheritDoc} */
    @Override
    public int getIconWidth() {
      return size;
    }

    /** {@inheritDoc} */
    @Override
    public int getIconHeight() {
      return size;
    }
  }
}
