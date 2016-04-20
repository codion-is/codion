/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.Util;
import org.jminor.common.model.valuemap.exception.ValidationException;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;
import org.jminor.framework.i18n.FrameworkMessages;
import org.jminor.javafx.framework.model.FXEntityListModel;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.Collection;
import java.util.List;

public class EntityTableView extends TableView<Entity> {

  private final FXEntityListModel listModel;
  private final TextField filterText = new TextField();
  private final BorderPane toolPane = new BorderPane();

  public EntityTableView(final FXEntityListModel listModel) {
    super(listModel.getSortedList());
    this.listModel = listModel;
    this.listModel.setSelectionModel(getSelectionModel());
    ((MultipleSelectionModel<Entity>) this.listModel.getListSelectionModel()).setSelectionMode(SelectionMode.MULTIPLE);
    filterText.setPromptText(FrameworkMessages.get(FrameworkMessages.SEARCH));
    initializeColumns();
    initializeToolPane();
    setTableMenuButtonVisible(true);
    addPopupMenu();
    addKeyEvents();
    bindEvents();
  }

  public void setCriteriaPaneVisible(final boolean visible) {
    getColumns().forEach(column -> ((EntityTableColumn) column).setCriteriaViewVisible(visible));
  }

  public void setCriteriaPaneAdvanced(final boolean advanced) {
    getColumns().forEach(column -> ((EntityTableColumn) column).setCriteriaViewAdvanced(advanced));
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

  public final FXEntityListModel getListModel() {
    return listModel;
  }

  public final TextField getFilterTextField() {
    return filterText;
  }

  public final Pane getToolPane() {
    return toolPane;
  }

  protected boolean includeUpdateSelectedProperty(final Property property) {
    return true;
  }

  private void initializeColumns() {
    for (final Property property : Entities.getVisibleProperties(listModel.getEntityID())) {
      getColumns().add(new EntityTableColumn(listModel, property, listModel.getConnectionProvider(),
              listModel.getCellValueFactory(property)));
    }
  }

  private void initializeToolPane() {
    final StackPane filterPane = new StackPane(filterText);
    StackPane.setAlignment(filterText, Pos.CENTER);
    VBox.setVgrow(filterText, Priority.ALWAYS);
    toolPane.setLeft(filterPane);
    toolPane.setRight(createToolBar());
  }

  private ToolBar createToolBar() {
    final ToolBar toolBar = new ToolBar();
    toolBar.getItems().add(createRefreshButton());

    return toolBar;
  }

  private Button createRefreshButton() {
    final Button button = new Button(FrameworkMessages.get(FrameworkMessages.REFRESH));
    button.setOnAction(event -> listModel.refresh());
    FXUiUtil.link(button.disableProperty(),
            listModel.getCriteriaModel().getCriteriaStateObserver().getReversedObserver());

    return button;
  }

  private void addPopupMenu() {
    final Menu updateSelected = createUpdateSelectedItem();
    final MenuItem delete = createDeleteSelectionItem();
    final MenuItem refresh = createRefreshItem();

    final ContextMenu contextMenu = new ContextMenu();
    contextMenu.getItems().add(updateSelected);
    contextMenu.getItems().add(delete);
    contextMenu.getItems().add(new SeparatorMenuItem());
    contextMenu.getItems().add(createSearchMenu());
    contextMenu.getItems().add(new SeparatorMenuItem());
    contextMenu.getItems().add(refresh);

    setContextMenu(contextMenu);
  }

  private Menu createSearchMenu() {
    final CheckMenuItem showCriteriaPane = new CheckMenuItem(FrameworkMessages.get(FrameworkMessages.SHOW));
    showCriteriaPane.selectedProperty().addListener((observable, oldValue, newValue) -> {
      setCriteriaPaneVisible(newValue);
    });
    final CheckMenuItem advanced = new CheckMenuItem(FrameworkMessages.get(FrameworkMessages.ADVANCED));
    advanced.selectedProperty().addListener((observable, oldValue, newValue) -> {
      setCriteriaPaneAdvanced(newValue);
    });
    final MenuItem clear = new MenuItem(FrameworkMessages.get(FrameworkMessages.CLEAR));
    clear.setOnAction(event -> listModel.getCriteriaModel().clear());

    final Menu searchMenu = new Menu(FrameworkMessages.get(FrameworkMessages.SEARCH));
    searchMenu.getItems().add(showCriteriaPane);
    searchMenu.getItems().add(advanced);
    searchMenu.getItems().add(clear);

    return searchMenu;
  }

  private Menu createUpdateSelectedItem() {
    final Menu updateSelected = new Menu(FrameworkMessages.get(FrameworkMessages.UPDATE_SELECTED));
    FXUiUtil.link(updateSelected.disableProperty(), getListModel().getSelectionEmptyObserver());
    EntityUtil.getUpdatableProperties(getListModel().getEntityID()).stream().filter(
            this::includeUpdateSelectedProperty).forEach(property -> {
      final String caption = property.getCaption() == null ? property.getPropertyID() : property.getCaption();
      final MenuItem updateProperty = new MenuItem(caption);
      updateProperty.setOnAction(actionEvent -> updateSelectedEntities(property));
      updateSelected.getItems().add(updateProperty);
    });

    return updateSelected;
  }

  private MenuItem createDeleteSelectionItem() {
    final MenuItem delete = new MenuItem(FrameworkMessages.get(FrameworkMessages.DELETE));
    delete.setOnAction(actionEvent -> deleteSelected());
    FXUiUtil.link(delete.disableProperty(), getListModel().getSelectionEmptyObserver());

    return delete;
  }

  private MenuItem createRefreshItem() {
    final MenuItem refresh = new MenuItem(FrameworkMessages.get(FrameworkMessages.REFRESH));
    refresh.setOnAction(actionEvent -> listModel.refresh());

    return refresh;
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
    catch (ValidationException e) {
      //todo
    }
  }

  private void addKeyEvents() {
    setOnKeyReleased(event -> {
      switch (event.getCode()) {
        case DELETE:
          if (event.getTarget() == this && !getSelectionModel().isEmpty()) {
            deleteSelected();
            event.consume();
          }
          break;
        case F5:
          listModel.refresh();
          event.consume();
          break;
      }
    });
  }

  private void bindEvents() {
    listModel.getSortedList().comparatorProperty().bind(comparatorProperty());
    filterText.textProperty().addListener((observable, oldValue, filterByValue) -> {
      listModel.getFilteredList().setPredicate(entity -> {
        if (Util.nullOrEmpty(filterByValue)) {
          return true;
        }
        for (final TableColumn<Entity, ?> column : getColumns()) {
          if (column.isVisible() && entity.getAsString(((EntityTableColumn) column).getProperty()).toLowerCase()
                  .contains(filterByValue.toLowerCase())) {
            return true;
          }
        }

        return false;
      });
    });
  }
}
