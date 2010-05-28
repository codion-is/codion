/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui;

import org.jminor.common.i18n.Messages;
import org.jminor.common.model.AbstractFilteredTableModel;
import org.jminor.common.model.DocumentAdapter;
import org.jminor.common.ui.control.Control;
import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.textfield.SearchFieldHint;

import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.table.TableColumn;
import java.awt.Color;
import java.awt.GridLayout;
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

  private final AbstractFilteredTableModel<T> model;

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
  private int searchResultIndex = -1;

  private final JTextField searchField;

  public AbstractFilteredTablePanel(final AbstractFilteredTableModel<T> model) {
    if (model == null)
      throw new IllegalArgumentException("Table model must not be null");

    this.model = model;
    this.table = initializeJTable();
    this.tableScrollPane = new JScrollPane(table);
    this.searchField = initializeSearchField();
  }

  /**
   * @return the TableModel used by this TablePanel
   */
  public AbstractFilteredTableModel<T> getTableModel() {
    return model;
  }

  /**
   * @return the JTable instance
   */
  public JTable getJTable() {
    return table;
  }

  public JTextField getSearchField() {
    return searchField;
  }

  public JScrollPane getTableScrollPane() {
    return tableScrollPane;
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
    final List<TableColumn> allColumns = Collections.list(getTableModel().getColumnModel().getColumns());
    allColumns.addAll(getTableModel().getHiddenColumns());
    Collections.sort(allColumns, new Comparator<TableColumn>() {
      public int compare(final TableColumn colOne, final TableColumn colTwo) {
        return Collator.getInstance().compare(colOne.getIdentifier().toString(), colTwo.getIdentifier().toString());
      }
    });

    final JPanel togglePanel = new JPanel(new GridLayout(Math.min(15, allColumns.size()), 0));
    final List<JCheckBox> buttonList = new ArrayList<JCheckBox>();
    for (final TableColumn column : allColumns) {
      final JCheckBox chkColumn = new JCheckBox(column.getHeaderValue().toString(), getTableModel().isColumnVisible(column.getIdentifier()));
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
        getTableModel().setColumnVisible(column.getIdentifier(), chkButton.isSelected());
      }
    }
  }

  protected JTextField initializeSearchField() {
    final JTextField txtSearch = new JTextField();
    txtSearch.setBackground((Color) UIManager.getLookAndFeel().getDefaults().get("TextField.inactiveBackground"));
    new SearchFieldHint(txtSearch);
    txtSearch.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      public void insertOrUpdate(final DocumentEvent e) {
        doSearch(false, searchResultIndex == -1 ? 0 : searchResultIndex, searchField.getText());
      }
    });
    txtSearch.addKeyListener(new KeyAdapter() {
      @Override
      public void keyReleased(final KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER)
          doSearch(e.isShiftDown(), searchResultIndex + 1, searchField.getText());
      }
    });

    return txtSearch;
  }

  private void doSearch(final boolean addToSelection, final int fromIndex,
                        final String searchText) {
    if (searchText.length() > 0) {
      final int viewIndex = getTableModel().findNextItemIndex(fromIndex, searchText);
      if (viewIndex >= 0) {
        searchResultIndex = viewIndex;
        if (addToSelection)
          getTableModel().addSelectedItemIndex(viewIndex);
        else
          getTableModel().setSelectedItemIndex(viewIndex);
      }
      else
        searchResultIndex = -1;
    }
    else
      searchResultIndex = -1;
  }

  /**
   * Initializes the JTable instance
   * @return the JTable instance
   */
  protected abstract JTable initializeJTable();
}
