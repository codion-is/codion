/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;
import org.jminor.javafx.framework.model.EntityModel;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;

import java.util.List;

public class EntityView extends BorderPane {

  private final EntityModel model;
  private final EntityEditView editView;
  private final EntityTableView tableView;

  private final TabPane detailViewTabPane = new TabPane();

  private boolean initialized = false;

  public EntityView(final EntityModel model, final EntityEditView editView, final EntityTableView tableView) {
    this.model = model;
    this.editView = editView;
    this.tableView = tableView;
    bindEvents();
  }

  public EntityModel getModel() {
    return model;
  }

  public final EntityView initializePanel() {
    if (!initialized) {
      initializeUI();
      initialized = true;
    }

    return this;
  }

  public void addDetailView(final EntityView detailView) {
    detailViewTabPane.getTabs().add(new Tab(Entities.getCaption(detailView.getModel().getEntityID()), detailView));
  }

  private void checkIfInitalized() {
    if (initialized) {
      throw new IllegalStateException("View has already been initialized");
    }
  }

  private void bindEvents() {
    tableView.getSelectionModel().getSelectedItems().addListener(new ListChangeListener<Entity>() {
      @Override
      public void onChanged(final Change<? extends Entity> change) {
        final List<Entity> selected = tableView.getSelectionModel().getSelectedItems();
        if (selected.isEmpty()) {
          model.getEditModel().setEntity(null);
        }
        else {
          model.getEditModel().setEntity(selected.get(0));
        }
        try {
          initializeDetailViews();
        }
        catch (final DatabaseException e) {
          throw new RuntimeException(e);
        }
      }
    });
    detailViewTabPane.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Tab>() {
      @Override
      public void changed(final ObservableValue<? extends Tab> observable, final Tab oldValue, final Tab newValue) {
        ((EntityView) newValue.getContent()).initializePanel();
      }
    });
  }

  private void initializeDetailViews() throws DatabaseException {
    if (!detailViewTabPane.getTabs().isEmpty()) {
      final EntityView selectedDetailView =
              (EntityView) detailViewTabPane.getSelectionModel().getSelectedItem().getContent();
      selectedDetailView.initialize(getModel().getEntityID(), tableView.getSelectionModel().getSelectedItems());
    }
  }

  private void initialize(final String masterEntityID, final List<Entity> foreignKeyEntities) throws DatabaseException {
    final List<Property.ForeignKeyProperty> foreignKeyProperties =
            Entities.getForeignKeyProperties(getModel().getEntityID(), masterEntityID);
    editView.getModel().setValue(foreignKeyProperties.get(0).getPropertyID(), foreignKeyEntities.get(0));
    tableView.getEntityList().filterBy(foreignKeyProperties.get(0), foreignKeyEntities);
  }

  private void initializeUI() {
    editView.initializePanel();
    final BorderPane editPane = new BorderPane();
    editPane.setCenter(editView);
    editPane.setRight(editView.getButtonPanel());
    final BorderPane tableBottomPane = new BorderPane();
    tableBottomPane.setLeft(tableView.getFilterTextField());
    final BorderPane tablePane = new BorderPane();
    tablePane.setCenter(tableView);
    tablePane.setBottom(tableBottomPane);
    if (detailViewTabPane.getTabs().isEmpty()) {
      setTop(editPane);
      setCenter(tablePane);
    }
    else {
      final BorderPane leftPane = new BorderPane();
      leftPane.setTop(editPane);
      leftPane.setCenter(tablePane);

      final SplitPane splitPane = new SplitPane(leftPane, detailViewTabPane);
      setCenter(splitPane);
    }
  }
}
