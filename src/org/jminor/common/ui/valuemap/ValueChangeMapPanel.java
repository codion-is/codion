/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.valuemap;

import org.jminor.common.model.AbstractFilteredTableModel;
import org.jminor.common.model.valuemap.ValueChangeMap;
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
public abstract class ValueChangeMapPanel<K, V> extends JPanel {

  /**
   * The ValueChangeMapModel instance used by this ValueChangeMapPanel
   */
  private final ValueChangeMapModel<K, V> model;

  /**
   * The ValueChangeMapEditPanel instance
   */
  private ValueChangeMapEditPanel<K, V> editPanel;

  /**
   * The AbstractFilteredTablePanel instance used by this ValueChangeMapPanel
   */
  private AbstractFilteredTablePanel<? extends ValueChangeMap<K, V>> tablePanel;

  public ValueChangeMapPanel(final ValueChangeMapModel<K, V> model) {
    if (model == null)
      throw new IllegalArgumentException("Model can not be null");

    this.model = model;
  }

  public ValueChangeMapModel<K, V> getModel() {
    return model;
  }

  /**
   * @return true if this panel contains a edit panel.
   */
  public boolean containsEditPanel() {
    return getEditPanel() != null;
  }

  public ValueChangeMapEditPanel<K, V> getEditPanel() {
    if (editPanel == null)
      editPanel = initializeEditPanel(getModel().getEditModel());

    return editPanel;
  }

  /**
   * @return true if this panel contains a table panel.
   */
  public boolean containsTablePanel() {
    return getTablePanel() != null;
  }

  public AbstractFilteredTablePanel<? extends ValueChangeMap<K, V>> getTablePanel() {
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
  protected abstract ValueChangeMapEditPanel<K, V> initializeEditPanel(final ValueChangeMapEditModel<K, V> editModel);

  /**
   * Initializes the AbstractFilteredTablePanel instance using the AbstractFilteredTableModel instance
   * provided by the getTableModel() method in the underlying ValueChangeMapModel
   * @param tableModel the AbstractFilteredTableModel
   * @return the AbstractFilteredTablePanel
   */
  protected abstract AbstractFilteredTablePanel<? extends ValueChangeMap<K, V>> initializeTablePanel(
          final AbstractFilteredTableModel<? extends ValueChangeMap<K, V>> tableModel);
}
