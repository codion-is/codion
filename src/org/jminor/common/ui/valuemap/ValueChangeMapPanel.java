/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.valuemap;

import org.jminor.common.model.AbstractFilteredTableModel;
import org.jminor.common.model.valuemap.ValueChangeMapEditModel;
import org.jminor.common.model.valuemap.ValueChangeMapModel;
import org.jminor.common.ui.AbstractFilteredTablePanel;

import javax.swing.JPanel;

/**
 * A UI class which associates a ValueChangeMapEditPanel with a AbstractFilteredTablePanel.<br>
 * User: Björn Darri<br>
 * Date: 25.4.2010<br>
 * Time: 13:42:51<br>
 */
public abstract class ValueChangeMapPanel extends JPanel {

  /**
   * The ValueChangeMapModel instance used by this ValueChangeMapPanel
   */
  private final ValueChangeMapModel model;

  /**
   * The ValueChangeMapEditPanel instance
   */
  private ValueChangeMapEditPanel editPanel;

  /**
   * The AbstractFilteredTablePanel instance used by this ValueChangeMapPanel
   */
  private AbstractFilteredTablePanel tablePanel;

  public ValueChangeMapPanel(final ValueChangeMapModel model) {
    if (model == null)
      throw new IllegalArgumentException("Model can not be null");

    this.model = model;
  }

  public ValueChangeMapModel getModel() {
    return model;
  }

  public ValueChangeMapEditPanel getEditPanel() {
    if (editPanel == null)
      editPanel = initializeEditPanel(getModel().getEditModel());

    return editPanel;
  }

  public AbstractFilteredTablePanel getTablePanel() {
    if (getModel().containsTableModel() && tablePanel == null)
      tablePanel = initializeTablePanel(getModel().getTableModel());

    return tablePanel;
  }

  /**
   * Initializes the ValueChangeMapEditPanel, that is, the panel containing the UI controls for
   * editing the active value map, this method should return null if editing is not required
   * @param editModel the ValueChangeMapEditModel
   * @return the ValueChangeMapEditPanel panel
   */
  protected abstract ValueChangeMapEditPanel initializeEditPanel(final ValueChangeMapEditModel editModel);

  /**
   * Initializes the AbstractFilteredTablePanel instance using the AbstractFilteredTableModel instance
   * provided by the getTableModel() method in the underlying ValueChangeMapModel
   * @param tableModel the AbstractFilteredTableModel
   * @return the AbstractFilteredTablePanel
   */
  protected abstract AbstractFilteredTablePanel initializeTablePanel(final AbstractFilteredTableModel tableModel);
}
