/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui;

import org.jminor.common.model.AbstractFilteredTableModel;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

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
   * Initializes the JTable instance
   * @return the JTable instance
   */
  protected abstract JTable initializeJTable();
}
