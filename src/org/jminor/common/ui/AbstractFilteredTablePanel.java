/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui;

import org.jminor.common.model.table.AbstractFilteredTableModel;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

/**
 * User: Björn Darri<br>
 * Date: 25.4.2010<br>
 * Time: 12:47:55<br>
 */
public abstract class AbstractFilteredTablePanel extends JPanel {

  private final AbstractFilteredTableModel model;

  /**
   * the JTable for showing the underlying entities
   */
  private final JTable entityTable;

  /**
   * the scroll pane used by the JTable instance
   */
  private final JScrollPane tableScrollPane;

  public AbstractFilteredTablePanel(final AbstractFilteredTableModel model) {
    if (model == null)
      throw new IllegalArgumentException("Table model must not be null");

    this.model = model;
    this.entityTable = initializeJTable();
    this.tableScrollPane = new JScrollPane(entityTable);
  }

  /**
   * @return the EntityTableModel used by this EntityTablePanel
   */
  public AbstractFilteredTableModel getTableModel() {
    return model;
  }

  /**
   * @return the JTable instance
   */
  public JTable getJTable() {
    return entityTable;
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
