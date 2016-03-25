/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.Util;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;
import org.jminor.framework.i18n.FrameworkMessages;
import org.jminor.javafx.framework.model.EntityListModel;

import javafx.application.Platform;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

import java.util.Collection;
import java.util.List;

public class EntityTableView extends TableView<Entity> {

  private final EntityListModel listModel;
  private final TextField filterText = new TextField();

  public EntityTableView(final EntityListModel listModel) {
    super(new SortedList<>(new FilteredList<>(listModel)));
    this.listModel = listModel;
    this.listModel.setSelectionModel(getSelectionModel());
    filterText.setPromptText(FrameworkMessages.get(FrameworkMessages.SEARCH));
    initializeColumns();
    addPopupMenu();
    addKeyEvents();
    bindEvents();
  }

  public final void deleteSelected() {
    if (FXUiUtil.confirm(FrameworkMessages.get(FrameworkMessages.CONFIRM_DELETE_SELECTED))) {
      try {
        listModel.deleteSelected();
      }
      catch (final DatabaseException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public final EntityListModel getListModel() {
    return listModel;
  }

  public final TextField getFilterTextField() {
    return filterText;
  }

  protected boolean includeUpdateSelectedProperty(final Property property) {
    return true;
  }

  private void initializeColumns() {
    for (final Property property : Entities.getVisibleProperties(listModel.getEntityID())) {
      getColumns().add(new EntityTableColumn(listModel, property, listModel.getCellValueFactory(property)));
    }
  }

  private void addPopupMenu() {
    final Menu updateSelected = createUpdateSelectedItem();
    final MenuItem delete = new MenuItem(FrameworkMessages.get(FrameworkMessages.DELETE));
    delete.setOnAction(actionEvent -> deleteSelected());
    final MenuItem refresh = new MenuItem(FrameworkMessages.get(FrameworkMessages.REFRESH));
    refresh.setOnAction(actionEvent -> listModel.refresh());

    setContextMenu(new ContextMenu(updateSelected, delete, refresh));
  }

  private Menu createUpdateSelectedItem() {
    final StateObserver disabled = getListModel().getSelectionEmptyObserver();
    final Menu updateSelected = new Menu(FrameworkMessages.get(FrameworkMessages.UPDATE_SELECTED));
    EntityUtil.getUpdatableProperties(getListModel().getEntityID()).stream().filter(
            this::includeUpdateSelectedProperty).forEach(property -> {
      final String caption = property.getCaption() == null ? property.getPropertyID() : property.getCaption();
      final MenuItem updateProperty = new MenuItem(caption);
      FXUiUtil.link(updateProperty.disableProperty(), disabled);
      updateProperty.setOnAction(actionEvent -> updateSelectedEntities(property));
      updateSelected.getItems().add(updateProperty);
    });

    return updateSelected;
  }

  private void updateSelectedEntities(final Property property) {
    final List<Entity> selectedEntities = EntityUtil.copyEntities(getListModel().getSelectionModel().getSelectedItems());

    final Collection<Object> values = EntityUtil.getDistinctValues(property.getPropertyID(), selectedEntities);
    final Object defaultValue = values.size() == 1 ? values.iterator().next() : null;

    final PropertyInputDialog inputDialog = new PropertyInputDialog(property, defaultValue, getListModel().getConnectionProvider());

    Platform.runLater(inputDialog.getControl()::requestFocus);
    final PropertyInputDialog.InputResult result = inputDialog.showAndWait().get();
    try {
      if (result.isInputAccepted()) {
        EntityUtil.put(property.getPropertyID(), result.getValue(), selectedEntities);
        getListModel().getEditModel().update(selectedEntities);
      }
    }
    catch (final DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  private void addKeyEvents() {
    setOnKeyReleased(event -> {
      if (event.getCode() == KeyCode.DELETE) {
        deleteSelected();
      }
    });
  }

  private SortedList<Entity> getSortedList() {
    return (SortedList<Entity>) getItems();
  }

  private FilteredList<Entity> getFilteredList() {
    return (FilteredList<Entity>) getSortedList().getSource();
  }

  private void bindEvents() {
    getSortedList().comparatorProperty().bind(comparatorProperty());
    filterText.textProperty().addListener((observable, oldValue, filterByValue) -> {
      getFilteredList().setPredicate(entity -> {
        if (Util.nullOrEmpty(filterByValue)) {
          return true;
        }
        for (final TableColumn<Entity, ?> column : getColumns()) {
          if (entity.getAsString(((EntityTableColumn) column).getProperty()).toLowerCase().contains(filterByValue.toLowerCase())) {
            return true;
          }
        }

        return false;
      });
    });
  }
}
