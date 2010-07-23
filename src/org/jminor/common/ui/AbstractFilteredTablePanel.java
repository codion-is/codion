/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui;

import org.jminor.common.i18n.Messages;
import org.jminor.common.model.DocumentAdapter;
import org.jminor.common.model.FilteredTableModel;
import org.jminor.common.model.SortingDirective;
import org.jminor.common.model.Util;
import org.jminor.common.ui.control.Control;
import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.textfield.SearchFieldHint;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * User: Björn Darri<br>
 * Date: 25.4.2010<br>
 * Time: 12:47:55<br>
 */
public abstract class AbstractFilteredTablePanel<T> extends JPanel {

  private static final Point NULL_POINT = new Point(-1, -1);

  /**
   * The table model
   */
  private final FilteredTableModel<T> tableModel;

  /**
   * the JTable for showing the underlying entities
   */
  private final JTable table;

  /**
   * the scroll pane used by the JTable instance
   */
  private final JScrollPane tableScrollPane;

  /**
   * Represents the index of the last search result
   */
  private Point lastSearchResultIndex = NULL_POINT;

  /**
   * The text field used for entering the search criteria
   */
  private final JTextField searchField;

  public AbstractFilteredTablePanel(final FilteredTableModel<T> tableModel) {
    Util.rejectNullValue(tableModel, "tableModel");
    this.tableModel = tableModel;
    this.table = initializeJTable();
    this.tableScrollPane = new JScrollPane(table);
    this.searchField = initializeSearchField();
    setupTableHeader();
    bindEvents();
  }

  /**
   * @return the TableModel used by this TablePanel
   */
  public FilteredTableModel<T> getTableModel() {
    return tableModel;
  }

  /**
   * @return the JTable instance
   */
  public final JTable getJTable() {
    return table;
  }

  /**
   * @return the text field used to enter a search critieria
   * @see #initializeSearchField()
   */
  public final JTextField getSearchField() {
    return searchField;
  }

  public final JScrollPane getTableScrollPane() {
    return tableScrollPane;
  }

  public final void scrollToCoordinate(final int row, final int column) {
    table.scrollRectToVisible(table.getCellRect(row, column, true));
  }

  /**
   * @return a control for showing the column selection dialog
   */
  public final Control getSelectColumnsControl() {
    return ControlFactory.methodControl(this, "selectTableColumns",
            Messages.get(Messages.SELECT_COLUMNS) + "...", null,
            Messages.get(Messages.SELECT_COLUMNS));
  }

  /**
   * Shows a dialog for selecting which columns to show/hide
   */
  public final void selectTableColumns() {
    final List<TableColumn> allColumns = Collections.list(tableModel.getColumnModel().getColumns());
    allColumns.addAll(tableModel.getHiddenColumns());
    Collections.sort(allColumns, new Comparator<TableColumn>() {
      public int compare(final TableColumn o1, final TableColumn o2) {
        return Collator.getInstance().compare(o1.getIdentifier().toString(), o2.getIdentifier().toString());
      }
    });

    final JPanel togglePanel = new JPanel(new GridLayout(Math.min(15, allColumns.size()), 0));
    final List<JCheckBox> buttonList = new ArrayList<JCheckBox>();
    for (final TableColumn column : allColumns) {
      final JCheckBox chkColumn = new JCheckBox(column.getHeaderValue().toString(), tableModel.isColumnVisible(column.getIdentifier()));
      buttonList.add(chkColumn);
      togglePanel.add(chkColumn);
    }
    final JScrollPane scroller = new JScrollPane(togglePanel);
    final int result = JOptionPane.showOptionDialog(this, scroller,
            Messages.get(Messages.SELECT_COLUMNS), JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE, null, null, null);
    if (result == JOptionPane.OK_OPTION) {
      for (final JCheckBox chkButton : buttonList) {
        final TableColumn column = allColumns.get(buttonList.indexOf(chkButton));
        tableModel.setColumnVisible(column.getIdentifier(), chkButton.isSelected());
      }
    }
  }

  /**
   * Initializes the JTable instance
   * @return the JTable instance
   */
  protected abstract JTable initializeJTable();

  private JTextField initializeSearchField() {
    final JTextField txtSearch = new JTextField();
    txtSearch.setBackground((Color) UIManager.getLookAndFeel().getDefaults().get("TextField.inactiveBackground"));
    txtSearch.setBorder(BorderFactory.createLineBorder(txtSearch.getForeground(), 1));
    txtSearch.setColumns(8);
    SearchFieldHint.enable(txtSearch);
    txtSearch.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      public void insertOrUpdate(final DocumentEvent e) {
        doSearch(false, lastSearchResultIndex.y == -1 ? 0 : lastSearchResultIndex.y, true, txtSearch.getText());
      }
    });
    txtSearch.addKeyListener(new KeyAdapter() {
      @Override
      public void keyReleased(final KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_DOWN) {
          doSearch(e.isShiftDown(), lastSearchResultIndex.y + 1, true, txtSearch.getText());
        }
        else if (e.getKeyCode() == KeyEvent.VK_UP) {
          doSearch(e.isShiftDown(), lastSearchResultIndex.y - 1, false, txtSearch.getText());
        }
        else if (txtSearch.getParent() != null) {
          txtSearch.getParent().dispatchEvent(e);
        }
      }
    });
    UiUtil.selectAllOnFocusGained(txtSearch);
    UiUtil.addKeyEvent(txtSearch, KeyEvent.VK_ESCAPE, new AbstractAction("requestJTableFocus") {
      public void actionPerformed(ActionEvent e) {
        getJTable().requestFocusInWindow();
      }
    });

    txtSearch.setComponentPopupMenu(initializeSearchFieldPopupMenu());

    return txtSearch;
  }

  private void doSearch(final boolean addToSelection, final int fromIndex, final boolean forward,
                        final String searchText) {
    if (searchText.length() > 0) {
      final Point viewIndex = tableModel.findNextItemCoordinate(fromIndex, forward, searchText);
      if (viewIndex != null) {
        lastSearchResultIndex = viewIndex;
        if (addToSelection) {
          tableModel.addSelectedItemIndex(viewIndex.y);
        }
        else {
          tableModel.setSelectedItemIndex(viewIndex.y);
          table.setColumnSelectionInterval(viewIndex.x, viewIndex.x);
        }
        scrollToCoordinate(viewIndex.y, viewIndex.x);
      }
      else {
        lastSearchResultIndex = NULL_POINT;
      }
    }
    else {
      lastSearchResultIndex = NULL_POINT;
    }
  }

  private JPopupMenu initializeSearchFieldPopupMenu() {
    final JPopupMenu popupMenu = new JPopupMenu();
    popupMenu.add(new AbstractAction(Messages.get(Messages.SETTINGS)) {
      public void actionPerformed(ActionEvent e) {
        final JPanel panel = new JPanel(new GridLayout(1,1,5,5));
        final JCheckBox boxRegexp =
                new JCheckBox(Messages.get(Messages.REGULAR_EXPRESSION_SEARCH), tableModel.isRegularExpressionSearch());
        panel.add(boxRegexp);
        final AbstractAction action = new AbstractAction(Messages.get(Messages.OK)) {
          public void actionPerformed(final ActionEvent evt) {
            tableModel.setRegularExpressionSearch(boxRegexp.isSelected());
          }
        };
        action.putValue(Action.MNEMONIC_KEY, Messages.get(Messages.OK_MNEMONIC).charAt(0));
        UiUtil.showInDialog(UiUtil.getParentWindow(AbstractFilteredTablePanel.this), panel, true,
                Messages.get(Messages.SETTINGS), true, true, action);
      }
    });

    return popupMenu;
  }

  private void setupTableHeader() {
    table.getTableHeader().addMouseListener(new MouseSortHandler());
    table.getTableHeader().setDefaultRenderer(new SortableHeaderRenderer(table.getTableHeader().getDefaultRenderer()));
  }

  private void bindEvents() {
    this.tableModel.eventSortingDone().addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        getJTable().getTableHeader().repaint();
      }
    });
  }

  private final class MouseSortHandler extends MouseAdapter {
    @Override
    public void mouseClicked(MouseEvent e) {
      if (e.getButton() != MouseEvent.BUTTON1) {
        return;
      }

      JTableHeader h = (JTableHeader) e.getSource();
      TableColumnModel columnModel = h.getColumnModel();
      int viewColumn = columnModel.getColumnIndexAtX(e.getX());
      int column;
      try {
        column = columnModel.getColumn(viewColumn).getModelIndex();
      }
      catch (ArrayIndexOutOfBoundsException ex) {
        return;
      }
      if (column != -1) {
        SortingDirective status = tableModel.getSortingDirective(column);
        if (!e.isControlDown()) {
          tableModel.clearSortingState();
        }
        // Cycle the sorting states through {NOT_SORTED, ASCENDING, DESCENDING} or
        // {NOT_SORTED, DESCENDING, ASCENDING} depending on whether shift is pressed.
        if (e.isShiftDown()) {
          switch (status) {
            case UNSORTED:
              status = SortingDirective.DESCENDING;
              break;
            case ASCENDING:
              status = SortingDirective.UNSORTED;
              break;
            case DESCENDING:
              status = SortingDirective.ASCENDING;
              break;
          }
        }
        else {
          switch (status) {
            case UNSORTED:
              status = SortingDirective.ASCENDING;
              break;
            case ASCENDING:
              status = SortingDirective.DESCENDING;
              break;
            case DESCENDING:
              status = SortingDirective.UNSORTED;
              break;
          }
        }
        tableModel.setSortingDirective(column, status);
      }
    }
  }

  private static final class Arrow implements Icon {
    private boolean descending;
    private int size;
    private int priority;

    private Arrow(boolean descending, int size, int priority) {
      this.descending = descending;
      this.size = size;
      this.priority = priority;
    }

    public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
      Color color = c == null ? Color.GRAY : c.getBackground();
      // In a compound sort, make each succesive triangle 20%
      // smaller than the previous one.
      int dx = (int)(size/2*Math.pow(0.8, priority));
      int dy = descending ? dx : -dx;
      // Align icon (roughly) with font baseline.
      int theY = y + 5*size/6 + (descending ? -dy : 0);
      int shift = descending ? 1 : -1;
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
      } else {
        g.setColor(color.brighter().brighter());
      }
      g.drawLine(dx, 0, 0, 0);

      g.setColor(color);
      g.translate(-x, -theY);
    }

    public int getIconWidth() {
      return size;
    }

    public int getIconHeight() {
      return size;
    }
  }

  private final class SortableHeaderRenderer implements TableCellRenderer {
    private TableCellRenderer tableCellRenderer;

    private SortableHeaderRenderer(TableCellRenderer tableCellRenderer) {
      this.tableCellRenderer = tableCellRenderer;
    }

    public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
                                                   final boolean hasFocus, final int row, final int column) {
      final Component component = tableCellRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      if (component instanceof JLabel) {
        final JLabel label = (JLabel) component;
        label.setHorizontalTextPosition(JLabel.LEFT);
        final int modelColumn = table.convertColumnIndexToModel(column);
        label.setIcon(getHeaderRendererIcon(modelColumn, label.getFont().getSize()+4));
      }

      return component;
    }

    protected Icon getHeaderRendererIcon(final int column, final int size) {
      SortingDirective directive = tableModel.getSortingDirective(column);
      if (directive == SortingDirective.UNSORTED) {
        return null;
      }

      return new Arrow(directive == SortingDirective.DESCENDING, size, tableModel.getSortPriority(column));
    }
  }
}
