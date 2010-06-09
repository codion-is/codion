/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap;

import org.jminor.common.model.AbstractFilteredTableModel;
import org.jminor.common.model.Util;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * A base model class for associating a ValueChangeMapEditModel with a AbstractFilterTableModel,
 * so that the value map selected in the table model is set in the edit model.<br>
 * User: Björn Darri<br>
 * Date: 25.4.2010<br>
 * Time: 13:10:14<br>
 */
public abstract class ValueChangeMapModel<K, V> {

  /**
   * The ValueChangeMapEditModel instance
   */
  private ValueChangeMapEditModel<K, V> editModel;

  /**
   * The table model
   */
  private AbstractFilteredTableModel<? extends ValueChangeMap<K, V>> tableModel;

  /**
   * True if this EntityModel should contain a table model
   */
  private final boolean includeTableModel;

  private final String mapTypeID;

  public ValueChangeMapModel(final String mapTypeID) {
    this(mapTypeID, true);
  }

  public ValueChangeMapModel(final String mapTypeID, final boolean includeTableModel) {
    Util.rejectNullValue(mapTypeID);
    this.mapTypeID = mapTypeID;
    this.includeTableModel = includeTableModel;
  }

  public String getMapTypeID() {
    return mapTypeID;
  }

  /**
   * @return the ValueChangeMapEditModel instance used by this ValueChangeMapModel
   */
  public ValueChangeMapEditModel<K, V> getEditModel() {
    if (editModel == null) {
      editModel = initializeEditModel();
      if (!editModel.getValueMap().getMapTypeID().equals(getMapTypeID())) {
        throw new RuntimeException("Expected edit model based on " + getMapTypeID() +
                ", got: " + editModel.getValueMap().getMapTypeID());
      }
      bindEvents();
    }

    return editModel;
  }

  /**
   * @return the AbstractFilteredTableModel, null if none is specified
   */
  public AbstractFilteredTableModel<? extends ValueChangeMap<K, V>> getTableModel() {
    if (includeTableModel && tableModel == null) {
      tableModel = initializeTableModel();
      if (!tableModel.getMapTypeID().equals(getMapTypeID())) {
        throw new RuntimeException("Expected table model based on " + getMapTypeID() +
                ", got: " + tableModel.getMapTypeID());
      }
      bindEvents();
    }

    return tableModel;
  }

  private void bindEvents() {
    if (editModel == null || tableModel == null) {
      return;
    }
    if (!containsTableModel()) {
      return;
    }

    getTableModel().eventSelectedIndexChanged().addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent event) {
        getEditModel().setValueMap(getTableModel().getSelectionModel().isSelectionEmpty() ? null : getTableModel().getSelectedItem());
      }
    });

    getTableModel().addTableModelListener(new TableModelListener() {
      public void tableChanged(final TableModelEvent event) {
        //if the selected record is being updated via the table model refresh the one in the edit model
        if (event.getType() == TableModelEvent.UPDATE && event.getFirstRow() == getTableModel().getSelectedIndex()) {
          getEditModel().setValueMap(null);
          getEditModel().setValueMap(getTableModel().getSelectedItem());
        }
      }
    });
  }

  /**
   * @return true if this ValueChangeMapModel contains a TableModel
   */
  public boolean containsTableModel() {
    return getTableModel() != null;
  }

  protected abstract ValueChangeMapEditModel<K, V> initializeEditModel();

  protected abstract AbstractFilteredTableModel<? extends ValueChangeMap<K, V>> initializeTableModel();
}
