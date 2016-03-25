/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui;

import org.jminor.framework.domain.Entities;
import org.jminor.javafx.framework.model.EntityModel;

import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;

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
    addKeyEvents();
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

  private void addKeyEvents() {
    setOnKeyReleased(event -> {
      switch (event.getCode()) {
        case I:
          if (editView != null && event.isControlDown()) {
            editView.selectInputControl();
          }
          event.consume();
          break;
        case F5:
          tableView.getListModel().refresh();
          event.consume();
          break;
      }
    });
  }

  private void bindEvents() {
    detailViewTabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue != null) {
        ((EntityView) newValue.getContent()).initializePanel();
      }
    });
  }

  private void initializeUI() {
    editView.initializePanel();
    final BorderPane editPane = new BorderPane();
    editPane.setCenter(editView);
    editPane.setRight(editView.getButtonPanel());
    final BorderPane tableBottomPane = new BorderPane();
    tableBottomPane.setCenter(tableView.getToolPane());
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
