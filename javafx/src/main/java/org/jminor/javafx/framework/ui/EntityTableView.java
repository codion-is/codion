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

import javafx.beans.value.ObservableValue;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.util.Callback;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class EntityTableView extends TableView<Entity> {

  private final EntityListModel tableModel;
  private final TextField filterText = new TextField();

  public EntityTableView(final EntityListModel tableModel) {
    super(new FilteredList<>(tableModel));
    this.tableModel = tableModel;
    this.tableModel.setSelectionModel(getSelectionModel());
    filterText.setPromptText(FrameworkMessages.get(FrameworkMessages.SEARCH));
    initializeColumns();
    addPopupMenu();
    addKeyEvents();
    bindEvents();
  }

  public final void deleteSelected() {
    if (FXUiUtil.confirm(FrameworkMessages.get(FrameworkMessages.CONFIRM_DELETE_SELECTED))) {
      try {
        tableModel.deleteSelected();
      }
      catch (final DatabaseException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public final EntityListModel getTableModel() {
    return tableModel;
  }

  public final TextField getFilterTextField() {
    return filterText;
  }

  protected boolean includeUpdateSelectedProperty(final Property property) {
    return true;
  }

  private void initializeColumns() {
    for (final Property property : Entities.getVisibleProperties(tableModel.getEntityID())) {
      getColumns().add(new EntityTableColumn(property, tableModel.getCellValueFactory(property)));
    }
  }

  private void addPopupMenu() {
    final Menu updateSelected = createUpdateSelectedItem();
    final MenuItem delete = new MenuItem(FrameworkMessages.get(FrameworkMessages.DELETE));
    delete.setOnAction(actionEvent -> deleteSelected());
    final MenuItem refresh = new MenuItem(FrameworkMessages.get(FrameworkMessages.REFRESH));
    refresh.setOnAction(actionEvent -> {
      try {
        tableModel.refresh();
      }
      catch (final DatabaseException e) {
        throw new RuntimeException(e);
      }
    });

    setContextMenu(new ContextMenu(updateSelected, delete, refresh));
  }

  private Menu createUpdateSelectedItem() {
    final StateObserver disabled = getTableModel().getSelectionEmptyObserver();
    final Menu updateSelected = new Menu(FrameworkMessages.get(FrameworkMessages.UPDATE_SELECTED));
    EntityUtil.getUpdatableProperties(getTableModel().getEntityID()).stream().filter(
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
    final List<Entity> selectedEntities = EntityUtil.copyEntities(getTableModel().getSelectionModel().getSelectedItems());

    final Collection values = EntityUtil.getDistinctValues(property.getPropertyID(), selectedEntities);
    final Object defaultValue = values.size() == 1 ? values.iterator().next() : null;

    final PropertyInputDialog inputDialog = new PropertyInputDialog(property, defaultValue, getTableModel().getConnectionProvider());

    final Optional<Object> value = inputDialog.showAndWait();
    try {
      if (value.isPresent()) {
        EntityUtil.put(property.getPropertyID(), value.get(), selectedEntities);
        getTableModel().getEditModel().update(selectedEntities);
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

  public static final class EntityTableColumn extends TableColumn<Entity, Object> {

    private final Property property;

    public EntityTableColumn(final Property property,
                             final Callback<CellDataFeatures<Entity, Object>, ObservableValue<Object>> cellValueFactory) {
      super(property.getCaption());
      this.property = property;
      final int preferredWidth = property.getPreferredColumnWidth();
      if (preferredWidth > 0) {
        setPrefWidth(preferredWidth);
      }
      setCellValueFactory(cellValueFactory);
    }

    public Property getProperty() {
      return property;
    }
  }
}
