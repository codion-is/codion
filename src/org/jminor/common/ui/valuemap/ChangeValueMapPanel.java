/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.valuemap;

import org.jminor.common.model.AbstractFilteredTableModel;
import org.jminor.common.model.valuemap.ChangeValueMapEditModel;
import org.jminor.common.model.valuemap.ChangeValueMapModel;
import org.jminor.common.ui.AbstractFilteredTablePanel;

import javax.swing.JPanel;

/**
 * A UI class which associates a ChangeValueMapEditPanel with a AbstractFilteredTablePanel.
 * User: Björn Darri<br>
 * Date: 25.4.2010<br>
 * Time: 13:42:51<br>
 */
public abstract class ChangeValueMapPanel extends JPanel {

  /**
   * The ChangeValueMapModel instance used by this ChangeValueMapPanel
   */
  private final ChangeValueMapModel model;

  /**
   * The ChangeValueMapEditPanel instance
   */
  private ChangeValueMapEditPanel editPanel;

  /**
   * The AbstractFilteredTablePanel instance used by this ChangeValueMapPanel
   */
  private AbstractFilteredTablePanel tablePanel;

  public ChangeValueMapPanel(final ChangeValueMapModel model) {
    if (model == null)
      throw new IllegalArgumentException("Model can not be null");

    this.model = model;
  }

  public ChangeValueMapModel getModel() {
    return model;
  }

  public ChangeValueMapEditPanel getEditPanel() {
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
   * Initializes the ChangeValueMapEditPanel, that is, the panel containing the UI controls for
   * editing the active value map, this method should return null if editing is not required
   * @param editModel the ChangeValueMapEditModel
   * @return the ChangeValueMapEditPanel panel
   */
  protected abstract ChangeValueMapEditPanel initializeEditPanel(final ChangeValueMapEditModel editModel);

  /**
   * Initializes the AbstractFilteredTablePanel instance using the AbstractFilteredTableModel instance
   * provided by the getTableModel() method in the underlying ChangeValueMapModel
   * @param tableModel the AbstractFilteredTableModel
   * @return the AbstractFilteredTablePanel
   */
  protected abstract AbstractFilteredTablePanel initializeTablePanel(final AbstractFilteredTableModel tableModel);
}
