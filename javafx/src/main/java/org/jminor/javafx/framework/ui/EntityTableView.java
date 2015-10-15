/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui;

import org.jminor.common.model.Util;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;
import org.jminor.framework.i18n.FrameworkMessages;
import org.jminor.javafx.framework.model.ObservableEntityList;

import javafx.collections.transformation.FilteredList;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class EntityTableView extends TableView<Entity> {

  private final ObservableEntityList entityList;
  private final TextField filterText = new TextField();

  public EntityTableView(final ObservableEntityList entityList) {
    super(new FilteredList<Entity>(entityList));
    this.entityList = entityList;
    filterText.setPromptText(FrameworkMessages.get(FrameworkMessages.SEARCH));
    getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    initializeColumns();
    bindEvents();
  }

  public ObservableEntityList getEntityList() {
    return entityList;
  }

  public TextField getFilterTextField() {
    return filterText;
  }

  private void initializeColumns() {
    for (final Property property : Entities.getVisibleProperties(entityList.getEntityID())) {
      getColumns().add(new EntityTableColumn(property, entityList.getCellValueFactory(property)));
    }
  }

  private void bindEvents() {
    filterText.textProperty().addListener((observable, oldValue, newValue) -> {
      ((FilteredList<Entity>) getItems()).setPredicate(entity -> {
        if (Util.nullOrEmpty(newValue)) {
          return true;
        }
        for (final TableColumn column : getColumns()) {
          if (entity.getValueAsString(((EntityTableColumn) column).getProperty())
                  .toLowerCase().contains(newValue.toLowerCase())) {
            return true;
          }
        }

        return false;
      });
    });
  }
}
