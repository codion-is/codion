/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.Util;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;
import org.jminor.framework.i18n.FrameworkMessages;
import org.jminor.javafx.framework.model.ObservableEntityList;

import javafx.collections.transformation.FilteredList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

public class EntityTableView extends TableView<Entity> {

  private final ObservableEntityList entityList;
  private final TextField filterText = new TextField();

  public EntityTableView(final ObservableEntityList entityList) {
    super(new FilteredList<>(entityList));
    this.entityList = entityList;
    this.entityList.setSelectionModel(getSelectionModel());
    filterText.setPromptText(FrameworkMessages.get(FrameworkMessages.SEARCH));
    initializeColumns();
    addPopupMenu();
    addKeyEvents();
    bindEvents();
  }

  public final void deleteSelected() {
    if (EntityFXUtil.confirm(FrameworkMessages.get(FrameworkMessages.CONFIRM_DELETE_SELECTED))) {
      try {
        entityList.deleteSelected();
      }
      catch (final DatabaseException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public final ObservableEntityList getEntityList() {
    return entityList;
  }

  public final TextField getFilterTextField() {
    return filterText;
  }

  private void initializeColumns() {
    for (final Property property : Entities.getVisibleProperties(entityList.getEntityID())) {
      getColumns().add(new EntityTableColumn(property, entityList.getCellValueFactory(property)));
    }
  }

  private void addPopupMenu() {
    final MenuItem delete = new MenuItem(FrameworkMessages.get(FrameworkMessages.DELETE));
    delete.setOnAction(actionEvent -> deleteSelected());
    setContextMenu(new ContextMenu(delete));
  }

  private void addKeyEvents() {
    setOnKeyReleased(event -> {
      if (event.getCode() == KeyCode.DELETE) {
        deleteSelected();
      }
    });
  }

  private void bindEvents() {
    filterText.textProperty().addListener((observable, oldValue, newValue) -> {
      ((FilteredList<Entity>) getItems()).setPredicate(entity -> {
        if (Util.nullOrEmpty(newValue)) {
          return true;
        }
        for (final TableColumn<Entity, ?> column : getColumns()) {
          if (entity.getAsString(((EntityTableColumn) column).getProperty())
                  .toLowerCase().contains(newValue.toLowerCase())) {
            return true;
          }
        }

        return false;
      });
    });
  }
}
