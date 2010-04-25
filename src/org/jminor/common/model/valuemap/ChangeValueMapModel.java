/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap;

import org.jminor.common.model.AbstractFilteredTableModel;
import org.jminor.common.model.Refreshable;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * User: Björn Darri
 * Date: 25.4.2010
 * Time: 13:10:14
 */
public abstract class ChangeValueMapModel<T, V> implements Refreshable {

  /**
   * The ChangeValueMapEditModel instance
   */
  private ChangeValueMapEditModel<T, V> editModel;

  /**
   * The table model
   */
  private AbstractFilteredTableModel<? extends ChangeValueMap<T, V>> tableModel;

  /**
   * @return the ChangeValueMapEditModel instance used by this EntityModel
   */
  public ChangeValueMapEditModel<T, V> getEditModel() {
    if (editModel == null) {
      editModel = initializeEditModel();
      bindEvents();
    }
    
    return editModel;
  }

  /**
   * @return the AbstractFilteredTableModel, null if none is specified
   */
  public AbstractFilteredTableModel<? extends ChangeValueMap<T, V>> getTableModel() {
    if (tableModel == null) {
      tableModel = initializeTableModel();
      bindEvents();
    }

    return tableModel;
  }

  private void bindEvents() {
    if (editModel == null || tableModel == null)
      return;
    if (!containsTableModel())
      return;

    getTableModel().eventSelectedIndexChanged().addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent event) {
        getEditModel().setValueMap(getTableModel().getSelectionModel().isSelectionEmpty() ? null : getTableModel().getSelectedItem());
      }
    });

    getTableModel().addTableModelListener(new TableModelListener() {
      public void tableChanged(final TableModelEvent event) {
        //if the selected record is being updated via the table model refresh the one in the model
        if (event.getType() == TableModelEvent.UPDATE && event.getFirstRow() == getTableModel().getSelectedIndex()) {
          getEditModel().setValueMap(null);
          getEditModel().setValueMap(getTableModel().getSelectedItem());
        }
      }
    });
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

  protected abstract AbstractFilteredTableModel<? extends ChangeValueMap<T, V>> initializeTableModel();
}
