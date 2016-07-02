/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui;

import org.jminor.common.Util;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.valuemap.exception.ValidationException;
import org.jminor.common.i18n.Messages;
import org.jminor.common.model.FilterCondition;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;
import org.jminor.framework.i18n.FrameworkMessages;
import org.jminor.javafx.framework.model.FXEntityListModel;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionModel;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

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
    this.filterText.setPromptText(FrameworkMessages.get(FrameworkMessages.SEARCH));
    initializeColumns();
    initializeToolPane();
    setTableMenuButtonVisible(true);
    addPopupMenu();
    addKeyEvents();
    bindEvents();
  }

  public final void setConditionPaneVisible(final boolean visible) {
    getColumns().forEach(column -> ((EntityTableColumn) column).setConditionViewVisible(visible));
  }

  public final void setConditionPaneAdvanced(final boolean advanced) {
    getColumns().forEach(column -> ((EntityTableColumn) column).setConditionViewAdvanced(advanced));
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
              getCellValueFactory(property)));
    }
    listModel.setColumns(getColumns());
  }

  private Callback<TableColumn.CellDataFeatures<Entity, Object>, ObservableValue<Object>> getCellValueFactory(final Property property) {
    return row -> new ReadOnlyObjectWrapper<>(row.getValue().get(property.getPropertyID()));
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
            listModel.getConditionModel().getConditionStateObserver().getReversedObserver());

    return button;
  }

  private void addPopupMenu() {
    final MenuItem refresh = createRefreshItem();
    final MenuItem clear = createClearItem();
    final Menu updateSelected = createUpdateSelectedItem();
    final MenuItem delete = createDeleteSelectionItem();
    final Menu search = createSearchMenu();
    final Menu copy = createCopyMenu();

    final ContextMenu contextMenu = new ContextMenu();
    contextMenu.getItems().add(refresh);
    contextMenu.getItems().add(clear);
    contextMenu.getItems().add(new SeparatorMenuItem());
    contextMenu.getItems().add(updateSelected);
    contextMenu.getItems().add(delete);
    contextMenu.getItems().add(new SeparatorMenuItem());
    contextMenu.getItems().add(search);
    contextMenu.getItems().add(new SeparatorMenuItem());
    contextMenu.getItems().add(copy);

    setContextMenu(contextMenu);
  }

  private Menu createCopyMenu() {
    final MenuItem copyCell = new MenuItem(FrameworkMessages.get(FrameworkMessages.COPY_CELL));
    FXUiUtil.link(copyCell.disableProperty(), listModel.getSelectionEmptyObserver());
    copyCell.setOnAction(event -> copyCell());
    final MenuItem copyTable = new MenuItem(FrameworkMessages.get(FrameworkMessages.COPY_TABLE_WITH_HEADER));
    copyTable.setOnAction(event -> copyTable());

    final Menu copyMenu = new Menu(Messages.get(Messages.COPY));
    copyMenu.getItems().add(copyCell);
    copyMenu.getItems().add(copyTable);

    return copyMenu;
  }

  private Menu createSearchMenu() {
    final CheckMenuItem showConditionPane = new CheckMenuItem(FrameworkMessages.get(FrameworkMessages.SHOW));
    showConditionPane.selectedProperty().addListener((observable, oldValue, newValue) -> setConditionPaneVisible(newValue));
    final CheckMenuItem advanced = new CheckMenuItem(FrameworkMessages.get(FrameworkMessages.ADVANCED));
    advanced.selectedProperty().addListener((observable, oldValue, newValue) -> setConditionPaneAdvanced(newValue));
    final MenuItem clear = new MenuItem(FrameworkMessages.get(FrameworkMessages.CLEAR));
    clear.setOnAction(event -> listModel.getConditionModel().clear());

    final Menu searchMenu = new Menu(FrameworkMessages.get(FrameworkMessages.SEARCH));
    searchMenu.getItems().add(showConditionPane);
    searchMenu.getItems().add(advanced);
    searchMenu.getItems().add(clear);

    return searchMenu;
  }

  private Menu createUpdateSelectedItem() {
    final Menu updateSelected = new Menu(FrameworkMessages.get(FrameworkMessages.UPDATE_SELECTED));
    FXUiUtil.link(updateSelected.disableProperty(), listModel.getSelectionEmptyObserver());
    EntityUtil.getUpdatableProperties(listModel.getEntityID()).stream().filter(
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
    FXUiUtil.link(delete.disableProperty(), listModel.getSelectionEmptyObserver());

    return delete;
  }

  private MenuItem createRefreshItem() {
    final MenuItem refresh = new MenuItem(FrameworkMessages.get(FrameworkMessages.REFRESH));
    refresh.setOnAction(actionEvent -> listModel.refresh());

    return refresh;
  }

  private MenuItem createClearItem() {
    final MenuItem refresh = new MenuItem(FrameworkMessages.get(FrameworkMessages.CLEAR));
    refresh.setOnAction(actionEvent -> listModel.clear());

    return refresh;
  }

  private void updateSelectedEntities(final Property property) {
    final List<Entity> selectedEntities = EntityUtil.copyEntities(listModel.getSelectionModel().getSelectedItems());

    final Collection<Object> values = EntityUtil.getDistinctValues(property.getPropertyID(), selectedEntities);
    final Object defaultValue = values.size() == 1 ? values.iterator().next() : null;

    final PropertyInputDialog inputDialog = new PropertyInputDialog(property, defaultValue, listModel.getConnectionProvider());

    Platform.runLater(inputDialog.getControl()::requestFocus);
    final PropertyInputDialog.InputResult result = inputDialog.showAndWait().get();
    try {
      if (result.isInputAccepted()) {
        EntityUtil.put(property.getPropertyID(), result.getValue(), selectedEntities);
        listModel.update(selectedEntities);
      }
    }
    catch (final ValidationException e) {
      FXUiUtil.showExceptionDialog(e);
    }
    catch (final DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  private void copyTable() {
    FXUiUtil.setClipboard(listModel.getTableDataAsDelimitedString('\t'));
  }

  private void copyCell() {
    final SelectionModel<Entity> selectionModel = getSelectionModel();
    if (!selectionModel.isEmpty()) {
      final TablePosition<Entity, Object> pos = getSelectionModel().getSelectedCells().get(0);
      final Entity item = listModel.get(pos.getRow());
      final String value = item.getAsString(((EntityTableColumn) pos.getTableColumn()).getProperty());
      FXUiUtil.setClipboard(value);
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
        default:
          break;
      }
    });
  }

  private void bindEvents() {
    listModel.getSortedList().comparatorProperty().bind(comparatorProperty());
    filterText.textProperty().addListener((observable, oldValue, newValue) -> {
      if (Util.nullOrEmpty(newValue)) {
        listModel.setFilterCondition(new FilterCondition.AcceptAllCondition());
      }
      else {
        listModel.setFilterCondition(item -> {
          boolean found = false;
          for (final TableColumn<Entity, ?> column : getColumns()) {
            final Object value = column.getCellObservableValue(item).getValue();
            if (value != null && value.toString().toLowerCase().contains(newValue.toLowerCase())) {
              found = true;
              break;
            }
          }

          return found;
        });
      }
    });
  }
}
