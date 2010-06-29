/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui;

import org.jminor.common.i18n.Messages;
import org.jminor.common.model.DocumentAdapter;
import org.jminor.common.model.FilteredTableModel;
import org.jminor.common.model.Util;
import org.jminor.common.ui.control.Control;
import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.textfield.SearchFieldHint;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.table.TableColumn;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
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
    Util.rejectNullValue(tableModel);
    this.tableModel = tableModel;
    this.table = initializeJTable();
    this.tableScrollPane = new JScrollPane(table);
    this.searchField = initializeSearchField();
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
  public JTable getJTable() {
    return table;
  }

  /**
   * @return the text field used to enter a search critieria
   * @see #initializeSearchField()
   */
  public JTextField getSearchField() {
    return searchField;
  }

  public JScrollPane getTableScrollPane() {
    return tableScrollPane;
  }

  public void scrollToCoordinate(final int row, final int column) {
    table.scrollRectToVisible(table.getCellRect(row, column, true));
  }

  /**
   * @return a control for showing the column selection dialog
   */
  public Control getSelectColumnsControl() {
    return ControlFactory.methodControl(this, "selectTableColumns",
            Messages.get(Messages.SELECT_COLUMNS) + "...", null,
            Messages.get(Messages.SELECT_COLUMNS));
  }

  /**
   * Shows a dialog for selecting which columns to show/hide
   */
  public void selectTableColumns() {
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

  protected JTextField initializeSearchField() {
    final JTextField txtSearch = new JTextField();
    txtSearch.setBackground((Color) UIManager.getLookAndFeel().getDefaults().get("TextField.inactiveBackground"));
    SearchFieldHint.enable(txtSearch);
    txtSearch.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      public void insertOrUpdate(final DocumentEvent e) {
        doSearch(false, lastSearchResultIndex.y == -1 ? 0 : lastSearchResultIndex.y, true, searchField.getText());
      }
    });
    txtSearch.addKeyListener(new KeyAdapter() {
      @Override
      public void keyReleased(final KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_DOWN) {
          doSearch(e.isShiftDown(), lastSearchResultIndex.y + 1, true, searchField.getText());
        }
        else if (e.getKeyCode() == KeyEvent.VK_UP) {
          doSearch(e.isShiftDown(), lastSearchResultIndex.y - 1, false, searchField.getText());
        }
      }
    });

    txtSearch.setComponentPopupMenu(initializeSearchPopupMenu());

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

  /**
   * Initializes the JTable instance
   * @return the JTable instance
   */
  protected abstract JTable initializeJTable();

  private JPopupMenu initializeSearchPopupMenu() {
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
}
