/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import org.jminor.common.model.table.AbstractFilteredTableModel;

/**
 * User: Björn Darri
 * Date: 25.4.2010
 * Time: 13:10:14
 */
public abstract class ChangeValueMapModel<T, V> implements Refreshable {

  /**
   * The EditModel instance
   */
  private ChangeValueMapEditModel<T, V> editModel;

  /**
   * The table model
   */
  private AbstractFilteredTableModel tableModel;

  /**
   * @return the EntityEditor instance used by this EntityModel
   */
  public ChangeValueMapEditModel<T, V> getEditModel() {
    if (editModel == null)
      editModel = initializeEditModel();
    
    return editModel;
  }

  /**
   * @return the EntityTableModel, null if none is specified
   */
  public AbstractFilteredTableModel getTableModel() {
    if (tableModel == null)
      tableModel = initializeTableModel();

    return tableModel;
  }

  public void refresh() {
    if (tableModel != null)
      tableModel.refresh();

    if (editModel != null)
      editModel.refresh();
  }

  /**
   * @return true if this EntityModel contains a EntityTableModel
   */
  public boolean containsTableModel() {
    return getTableModel() != null;
  }

  protected abstract ChangeValueMapEditModel<T, V> initializeEditModel();

  protected abstract AbstractFilteredTableModel initializeTableModel();
}
