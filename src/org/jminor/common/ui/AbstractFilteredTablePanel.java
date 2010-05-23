/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui;

import org.jminor.common.i18n.Messages;
import org.jminor.common.model.AbstractFilteredTableModel;
import org.jminor.common.ui.control.Control;
import org.jminor.common.ui.control.ControlFactory;

import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableColumn;
import java.awt.GridLayout;
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

  public AbstractFilteredTablePanel(final AbstractFilteredTableModel<T> model) {
    if (model == null)
      throw new IllegalArgumentException("Table model must not be null");

    this.model = model;
    this.table = initializeJTable();
    this.tableScrollPane = new JScrollPane(table);
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

  /**
   * Initializes the JTable instance
   * @return the JTable instance
   */
  protected abstract JTable initializeJTable();
}
