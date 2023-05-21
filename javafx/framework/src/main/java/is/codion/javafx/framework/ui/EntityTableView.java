/*
 * Copyright (c) 2015 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.javafx.framework.ui;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.i18n.Messages;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.exception.ValidationException;
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
import javafx.scene.control.Tooltip;
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

import static is.codion.common.NullOrEmpty.nullOrEmpty;

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
    super(listModel.sortedList());
    this.listModel = listModel;
    this.listModel.setSelectionModel(getSelectionModel());
    this.listModel.setColumnSortOrder(getSortOrder());
    this.filterText.setPromptText(FrameworkMessages.search());
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
    if (FXUiUtil.confirm(FrameworkMessages.confirmDeleteSelected())) {
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
  public final FXEntityListModel listModel() {
    return listModel;
  }

  /**
   * @return the {@link TextField} used to filter this table view
   */
  public final TextField filterTextField() {
    return filterText;
  }

  /**
   * @return the tool pane associated with this table view
   */
  public final Pane toolPane() {
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
    for (Property<?> property : listModel().entityDefinition().visibleProperties()) {
      getColumns().add(entityTableColumn(property));
    }
    listModel.setColumns(getColumns());
  }

  private <T extends Comparable<T>> EntityTableColumn<T> entityTableColumn(Property<?> property) {
    return new EntityTableColumn<>(listModel, (Property<T>) property, getCellValueFactory((Property<T>) property));
  }

  private <T extends Comparable<T>> Callback<TableColumn.CellDataFeatures<Entity, T>, ObservableValue<T>> getCellValueFactory(Property<T> property) {
    return row -> new ReadOnlyObjectWrapper<>(row.getValue().get(property.attribute()));
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
    Button button = new Button(FrameworkMessages.refresh());
    button.setTooltip(new Tooltip(FrameworkMessages.refreshTip()));
    button.setOnAction(event -> listModel.refresh());
    FXUiUtil.link(button.disableProperty(),
            listModel.conditionChangedObserver().reversedObserver());

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
    MenuItem copyCell = new MenuItem(FrameworkMessages.copyCell());
    FXUiUtil.link(copyCell.disableProperty(), listModel.selectionEmptyObserver());
    copyCell.setOnAction(event -> copyCell());
    MenuItem copyTable = new MenuItem(FrameworkMessages.copyTableWithHeader());
    copyTable.setOnAction(event -> copyTable());

    Menu copyMenu = new Menu(Messages.copy());
    copyMenu.getItems().add(copyCell);
    copyMenu.getItems().add(copyTable);

    return copyMenu;
  }

  private Menu createSearchMenu() {
    CheckMenuItem showConditionPane = new CheckMenuItem(FrameworkMessages.show());
    showConditionPane.selectedProperty().addListener((observable, oldValue, newValue) -> setConditionPaneVisible(newValue));
    CheckMenuItem advanced = new CheckMenuItem(Messages.advanced());
    advanced.selectedProperty().addListener((observable, oldValue, newValue) -> setConditionPaneAdvanced(newValue));
    MenuItem clearSearch = new MenuItem(Messages.clear());
    clearSearch.setOnAction(event -> listModel.conditionModel().clear());

    Menu searchMenu = new Menu(FrameworkMessages.search());
    searchMenu.getItems().add(showConditionPane);
    searchMenu.getItems().add(advanced);
    searchMenu.getItems().add(clearSearch);

    return searchMenu;
  }

  private Menu createUpdateSelectedItem() {
    Menu updateSelected = new Menu(FrameworkMessages.update());
    FXUiUtil.link(updateSelected.disableProperty(), listModel.selectionEmptyObserver());
    listModel().entityDefinition().updatableProperties().stream()
            .filter(this::includeUpdateSelectedProperty)
            .sorted(Property.propertyComparator())
            .forEach(property -> addUpdateSelectedMenuItem(updateSelected, property));

    return updateSelected;
  }

  private void addUpdateSelectedMenuItem(Menu updateSelected, Property<?> property) {
    String caption = property.caption() == null ? property.attribute().name() : property.caption();
    MenuItem updateProperty = new MenuItem(caption);
    updateProperty.setOnAction(actionEvent -> updateSelectedEntities(property));
    updateSelected.getItems().add(updateProperty);
  }

  private MenuItem createDeleteSelectionItem() {
    MenuItem delete = new MenuItem(FrameworkMessages.delete());
    delete.setOnAction(actionEvent -> deleteSelected());
    FXUiUtil.link(delete.disableProperty(), listModel.selectionEmptyObserver());

    return delete;
  }

  private MenuItem createRefreshItem() {
    MenuItem refresh = new MenuItem(FrameworkMessages.refresh());
    refresh.setOnAction(actionEvent -> listModel.refresh());

    return refresh;
  }

  private MenuItem createClearItem() {
    MenuItem refresh = new MenuItem(Messages.clear());
    refresh.setOnAction(actionEvent -> listModel.clear());

    return refresh;
  }

  private <T> void updateSelectedEntities(Property<T> property) {
    List<Entity> selectedEntities = Entity.copy(listModel.selectionModel().getSelectedItems());

    Collection<T> values = Entity.getDistinct(property.attribute(), selectedEntities);
    T defaultValue = values.size() == 1 ? values.iterator().next() : null;

    PropertyInputDialog<T> inputDialog = new PropertyInputDialog<>(property, defaultValue, listModel.connectionProvider());

    Platform.runLater(inputDialog.control()::requestFocus);
    Optional<PropertyInputDialog.InputResult<T>> inputResult = inputDialog.showAndWait();
    try {
      if (inputResult.isPresent() && inputResult.get().isInputAccepted()) {
        Entity.put(property.attribute(), inputResult.get().value(), selectedEntities);
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
    FXUiUtil.setClipboard(listModel.tableDataAsDelimitedString('\t'));
  }

  private void copyCell() {
    SelectionModel<Entity> selectionModel = getSelectionModel();
    if (!selectionModel.isEmpty()) {
      TablePosition<Entity, Object> pos = getSelectionModel().getSelectedCells().get(0);
      Entity item = listModel.get(pos.getRow());
      String value = item.toString(((EntityTableColumn<?>) pos.getTableColumn()).attribute());
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
    listModel.sortedList().comparatorProperty().bind(comparatorProperty());
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
    FXEntityListModel entityTableModel = listModel();

    return !entityTableModel.isReadOnly() && entityTableModel.isUpdateEnabled() &&
            entityTableModel.isBatchUpdateEnabled() &&
            !entityTableModel.entityDefinition().updatableProperties().isEmpty();
  }

  private boolean includeDeleteSelectedControl() {
    FXEntityListModel entityTableModel = listModel();

    return !entityTableModel.isReadOnly() && entityTableModel.isDeleteEnabled();
  }
}
