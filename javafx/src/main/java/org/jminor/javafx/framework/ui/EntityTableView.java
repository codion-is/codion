/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui;

import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;
import org.jminor.javafx.framework.model.ObservableEntityList;

import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class EntityTableView extends TableView<Entity> {

  private final ObservableEntityList observableList;

  public EntityTableView(final ObservableEntityList observableList) {
    this.observableList = observableList;
    getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    initializeColumns();
    setItems(observableList);
  }

  public ObservableEntityList getTableData() {
    return observableList;
  }

  private void initializeColumns() {
    for (final Property property : Entities.getVisibleProperties(observableList.getEntityID())) {
      final TableColumn<Entity, Object> column = new TableColumn(property.getCaption());
      final int preferredWidth = property.getPreferredColumnWidth();
      if (preferredWidth > 0) {
        column.setPrefWidth(preferredWidth);
      }
      column.setCellValueFactory(observableList.getCellValueFactory(property));
      getColumns().add(column);
    }
  }
}
