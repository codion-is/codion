/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui;

import org.jminor.common.Util;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.valuemap.exception.ValidationException;
import org.jminor.common.i18n.Messages;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Properties;
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
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * A {@link TableView} extension based on entities
 */
public class EntityTableView extends TableView<Entity> {

  private final FXEntityListModel listModel;
  private final TextField filterText = new TextField();
  private final BorderPane toolPane = new BorderPane();

  /**
   * Instantiates a new {@link EntityTableView}
   * @param listModel the list mode to base the table view on
   */
  public EntityTableView(final FXEntityListModel listModel) {
    super(listModel.getSortedList());
    this.listModel = listModel;
    this.listModel.setSelectionModel(getSelectionModel());
    this.filterText.setPromptText(FrameworkMessages.get(FrameworkMessages.SEARCH));
    initializeColumns();
    initializeToolPane();
    setTableMenuButtonVisible(true);
    addPopupMenu();
    bindEvents();
  }

  /**
   * Toggles the visibility of the property condition views
   * @param visible the toggle values
   */
  public final void setConditionPaneVisible(final boolean visible) {
    getColumns().forEach(column -> ((EntityTableColumn) column).setConditionViewVisible(visible));
  }

  /**
   * Toggles the advanced property condition view
   * @param advanced the toggle values
   */
  public final void setConditionPaneAdvanced(final boolean advanced) {
    getColumns().forEach(column -> ((EntityTableColumn) column).setConditionViewAdvanced(advanced));
  }

  /**
   * Deletes the selected entities after displaying a confirm dialog
   */
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

  /**
   * @return the underlying {@link FXEntityListModel}
   */
  public final FXEntityListModel getListModel() {
    return listModel;
  }

  /**
   * @return the {@link TextField} used to filter this table view
   */
  public final TextField getFilterTextField() {
    return filterText;
  }

  /**
   * @return the tool pane associated with this table view
   */
  public final Pane getToolPane() {
    return toolPane;
  }

  /**
   * Specifies whether or not a property should be included in the update selected menu
   * @param property the property
   * @return true if the user should be able to update the property value for multiple entities at a time
   */
  protected boolean includeUpdateSelectedProperty(final Property property) {
    return true;
  }

  private void initializeColumns() {
    for (final Property property : getListModel().getDomain().getVisibleProperties(listModel.getEntityId())) {
      getColumns().add(new EntityTableColumn(listModel, property, getCellValueFactory(property)));
    }
    listModel.setColumns(getColumns());
  }

  private Callback<TableColumn.CellDataFeatures<Entity, Object>, ObservableValue<Object>> getCellValueFactory(final Property property) {
    return row -> new ReadOnlyObjectWrapper<>(row.getValue().get(property.getPropertyId()));
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
    final ContextMenu contextMenu = new ContextMenu();
    contextMenu.getItems().add(createRefreshItem());
    contextMenu.getItems().add(createClearItem());
    contextMenu.getItems().add(new SeparatorMenuItem());
    boolean separatorRequired = false;
    if (includeUpdateSelectedControls()) {
      contextMenu.getItems().add(createUpdateSelectedItem());
      separatorRequired = true;
    }
    if (includeDeleteSelectedControl()) {
      contextMenu.getItems().add(createDeleteSelectionItem());
      separatorRequired = true;
    }
    if (separatorRequired) {
      contextMenu.getItems().add(new SeparatorMenuItem());
    }
    contextMenu.getItems().add(createSearchMenu());
    contextMenu.getItems().add(new SeparatorMenuItem());
    contextMenu.getItems().add(createCopyMenu());

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
    Properties.sort(getListModel().getDomain().getUpdatableProperties(listModel.getEntityId())).stream().filter(
            this::includeUpdateSelectedProperty).forEach(property -> {
      final String caption = property.getCaption() == null ? property.getPropertyId() : property.getCaption();
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
    final List<Entity> selectedEntities = Entities.copyEntities(listModel.getSelectionModel().getSelectedItems());

    final Collection<Object> values = Entities.getDistinctValues(property.getPropertyId(), selectedEntities);
    final Object defaultValue = values.size() == 1 ? values.iterator().next() : null;

    final PropertyInputDialog inputDialog = new PropertyInputDialog(property, defaultValue, listModel.getConnectionProvider());

    Platform.runLater(inputDialog.getControl()::requestFocus);
    final Optional<PropertyInputDialog.InputResult> inputResult = inputDialog.showAndWait();
    try {
      if (inputResult.isPresent() && inputResult.get().isInputAccepted()) {
        Entities.put(property.getPropertyId(), inputResult.get().getValue(), selectedEntities);
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

  private void onKeyRelease(final KeyEvent event) {
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
  }

  private void bindEvents() {
    listModel.getSortedList().comparatorProperty().bind(comparatorProperty());
    filterText.textProperty().addListener((observable, oldValue, newValue) -> {
      if (Util.nullOrEmpty(newValue)) {
        listModel.setFilterCondition(null);
      }
      else {
        listModel.setFilterCondition(item -> getColumns().stream().map(column -> column.getCellObservableValue(item).getValue())
                .anyMatch(value -> value != null && value.toString().toLowerCase().contains(newValue.toLowerCase())));
      }
    });
    setOnKeyReleased(this::onKeyRelease);
  }

  private boolean includeUpdateSelectedControls() {
    final FXEntityListModel entityTableModel = getListModel();

    return !entityTableModel.isReadOnly() && entityTableModel.isUpdateAllowed() &&
            entityTableModel.isBatchUpdateAllowed() && !entityTableModel.getDomain()
            .getUpdatableProperties(entityTableModel.getEntityId()).isEmpty();
  }

  private boolean includeDeleteSelectedControl() {
    final FXEntityListModel entityTableModel = getListModel();

    return !entityTableModel.isReadOnly() && entityTableModel.isDeleteAllowed();
  }
}
