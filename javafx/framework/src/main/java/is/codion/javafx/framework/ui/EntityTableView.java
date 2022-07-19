/*
 * Copyright (c) 2015 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.javafx.framework.ui;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.i18n.Messages;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.domain.property.Properties;
import is.codion.framework.domain.property.Property;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.javafx.framework.model.FXEntityListModel;

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

import static is.codion.common.Util.nullOrEmpty;

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
  public EntityTableView(FXEntityListModel listModel) {
    super(listModel.getSortedList());
    this.listModel = listModel;
    this.listModel.setSelectionModel(getSelectionModel());
    this.listModel.setColumnSortOrder(getSortOrder());
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
  public final void setConditionPaneVisible(boolean visible) {
    getColumns().forEach(column -> ((EntityTableColumn<?>) column).setConditionViewVisible(visible));
  }

  /**
   * Toggles the advanced property condition view
   * @param advanced the toggle values
   */
  public final void setConditionPaneAdvanced(boolean advanced) {
    getColumns().forEach(column -> ((EntityTableColumn<?>) column).setConditionViewAdvanced(advanced));
  }

  /**
   * Deletes the selected entities after displaying a confirm dialog
   */
  public final void deleteSelected() {
    if (FXUiUtil.confirm(FrameworkMessages.get(FrameworkMessages.CONFIRM_DELETE_SELECTED))) {
      try {
        listModel.deleteSelected();
      }
      catch (DatabaseException e) {
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
   * Specifies whether a property should be included in the update selected menu
   * @param property the property
   * @param <T> the value type
   * @return true if the user should be able to update the property value for multiple entities at a time
   */
  protected <T> boolean includeUpdateSelectedProperty(Property<T> property) {
    return true;
  }

  private void initializeColumns() {
    for (Property<?> property : getListModel().getEntityDefinition().getVisibleProperties()) {
      getColumns().add(entityTableColumn(property));
    }
    listModel.setColumns(getColumns());
  }

  private <T extends Comparable<T>> EntityTableColumn<T> entityTableColumn(Property<?> property) {
    return new EntityTableColumn<>(listModel, (Property<T>) property, getCellValueFactory((Property<T>) property));
  }

  private <T extends Comparable<T>> Callback<TableColumn.CellDataFeatures<Entity, T>, ObservableValue<T>> getCellValueFactory(Property<T> property) {
    return row -> new ReadOnlyObjectWrapper<>(row.getValue().get(property.getAttribute()));
  }

  private void initializeToolPane() {
    StackPane filterPane = new StackPane(filterText);
    StackPane.setAlignment(filterText, Pos.CENTER);
    VBox.setVgrow(filterText, Priority.ALWAYS);
    toolPane.setLeft(filterPane);
    toolPane.setRight(createToolBar());
  }

  private ToolBar createToolBar() {
    ToolBar toolBar = new ToolBar();
    toolBar.getItems().add(createRefreshButton());

    return toolBar;
  }

  private Button createRefreshButton() {
    Button button = new Button(FrameworkMessages.get(FrameworkMessages.REFRESH));
    button.setOnAction(event -> listModel.refresh());
    FXUiUtil.link(button.disableProperty(),
            listModel.getTableConditionModel().getConditionChangedObserver().getReversedObserver());

    return button;
  }

  private void addPopupMenu() {
    ContextMenu contextMenu = new ContextMenu();
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
    MenuItem copyCell = new MenuItem(FrameworkMessages.get(FrameworkMessages.COPY_CELL));
    FXUiUtil.link(copyCell.disableProperty(), listModel.getSelectionEmptyObserver());
    copyCell.setOnAction(event -> copyCell());
    MenuItem copyTable = new MenuItem(FrameworkMessages.get(FrameworkMessages.COPY_TABLE_WITH_HEADER));
    copyTable.setOnAction(event -> copyTable());

    Menu copyMenu = new Menu(Messages.get(Messages.COPY));
    copyMenu.getItems().add(copyCell);
    copyMenu.getItems().add(copyTable);

    return copyMenu;
  }

  private Menu createSearchMenu() {
    CheckMenuItem showConditionPane = new CheckMenuItem(FrameworkMessages.get(FrameworkMessages.SHOW));
    showConditionPane.selectedProperty().addListener((observable, oldValue, newValue) -> setConditionPaneVisible(newValue));
    CheckMenuItem advanced = new CheckMenuItem(FrameworkMessages.get(FrameworkMessages.ADVANCED));
    advanced.selectedProperty().addListener((observable, oldValue, newValue) -> setConditionPaneAdvanced(newValue));
    MenuItem clearSearch = new MenuItem(FrameworkMessages.get(FrameworkMessages.CLEAR));
    clearSearch.setOnAction(event -> listModel.getTableConditionModel().clearConditions());

    Menu searchMenu = new Menu(FrameworkMessages.get(FrameworkMessages.SEARCH));
    searchMenu.getItems().add(showConditionPane);
    searchMenu.getItems().add(advanced);
    searchMenu.getItems().add(clearSearch);

    return searchMenu;
  }

  private Menu createUpdateSelectedItem() {
    Menu updateSelected = new Menu(FrameworkMessages.get(FrameworkMessages.UPDATE));
    FXUiUtil.link(updateSelected.disableProperty(), listModel.getSelectionEmptyObserver());
    Properties.sort(getListModel().getEntityDefinition().getUpdatableProperties()).stream()
            .filter(this::includeUpdateSelectedProperty)
            .forEach(property -> addUpdateSelectedMenuItem(updateSelected, property));

    return updateSelected;
  }

  private void addUpdateSelectedMenuItem(Menu updateSelected, Property<?> property) {
    String caption = property.getCaption() == null ? property.getAttribute().getName() : property.getCaption();
    MenuItem updateProperty = new MenuItem(caption);
    updateProperty.setOnAction(actionEvent -> updateSelectedEntities(property));
    updateSelected.getItems().add(updateProperty);
  }

  private MenuItem createDeleteSelectionItem() {
    MenuItem delete = new MenuItem(FrameworkMessages.get(FrameworkMessages.DELETE));
    delete.setOnAction(actionEvent -> deleteSelected());
    FXUiUtil.link(delete.disableProperty(), listModel.getSelectionEmptyObserver());

    return delete;
  }

  private MenuItem createRefreshItem() {
    MenuItem refresh = new MenuItem(FrameworkMessages.get(FrameworkMessages.REFRESH));
    refresh.setOnAction(actionEvent -> listModel.refresh());

    return refresh;
  }

  private MenuItem createClearItem() {
    MenuItem refresh = new MenuItem(FrameworkMessages.get(FrameworkMessages.CLEAR));
    refresh.setOnAction(actionEvent -> listModel.clear());

    return refresh;
  }

  private <T> void updateSelectedEntities(Property<T> property) {
    List<Entity> selectedEntities = Entity.deepCopy(listModel.getSelectionModel().getSelectedItems());

    Collection<T> values = Entity.getDistinct(property.getAttribute(), selectedEntities);
    T defaultValue = values.size() == 1 ? values.iterator().next() : null;

    PropertyInputDialog<T> inputDialog = new PropertyInputDialog<>(property, defaultValue, listModel.getConnectionProvider());

    Platform.runLater(inputDialog.getControl()::requestFocus);
    Optional<PropertyInputDialog.InputResult<T>> inputResult = inputDialog.showAndWait();
    try {
      if (inputResult.isPresent() && inputResult.get().isInputAccepted()) {
        Entity.put(property.getAttribute(), inputResult.get().getValue(), selectedEntities);
        listModel.update(selectedEntities);
      }
    }
    catch (ValidationException e) {
      FXUiUtil.showExceptionDialog(e);
    }
    catch (DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  private void copyTable() {
    FXUiUtil.setClipboard(listModel.getTableDataAsDelimitedString('\t'));
  }

  private void copyCell() {
    SelectionModel<Entity> selectionModel = getSelectionModel();
    if (!selectionModel.isEmpty()) {
      TablePosition<Entity, Object> pos = getSelectionModel().getSelectedCells().get(0);
      Entity item = listModel.get(pos.getRow());
      String value = item.toString(((EntityTableColumn<?>) pos.getTableColumn()).getAttribute());
      FXUiUtil.setClipboard(value);
    }
  }

  private void onKeyRelease(KeyEvent event) {
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
      if (nullOrEmpty(newValue)) {
        listModel.setIncludeCondition(null);
      }
      else {
        listModel.setIncludeCondition(item -> getColumns().stream()
                .map(column -> column.getCellObservableValue(item).getValue())
                .anyMatch(value -> value != null && value.toString().toLowerCase().contains(newValue.toLowerCase())));
      }
    });
    setOnKeyReleased(this::onKeyRelease);
  }

  private boolean includeUpdateSelectedControls() {
    FXEntityListModel entityTableModel = getListModel();

    return !entityTableModel.isReadOnly() && entityTableModel.isUpdateEnabled() &&
            entityTableModel.isBatchUpdateEnabled() &&
            !entityTableModel.getEntityDefinition().getUpdatableProperties().isEmpty();
  }

  private boolean includeDeleteSelectedControl() {
    FXEntityListModel entityTableModel = getListModel();

    return !entityTableModel.isReadOnly() && entityTableModel.isDeleteEnabled();
  }
}
