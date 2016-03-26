/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui;

import org.jminor.framework.domain.Entities;
import org.jminor.javafx.framework.model.EntityModel;

import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;

public class EntityView extends BorderPane {

  private final EntityModel model;
  private final EntityEditView editView;
  private final EntityTableView tableView;

  private EntityView masterView;

  private final TabPane detailViewTabPane = new TabPane();

  private boolean initialized = false;

  public EntityView(final EntityModel model, final EntityEditView editView, final EntityTableView tableView) {
    this.model = model;
    this.editView = editView;
    this.tableView = tableView;
    addKeyEvents();
    bindEvents();
  }

  public final EntityModel getModel() {
    return model;
  }

  public final void setMasterView(final EntityView masterView) {
    this.masterView = masterView;
  }

  public final EntityView initializePanel() {
    if (!initialized) {
      initializeUI();
      initialized = true;
    }

    return this;
  }

  public final void addDetailView(final EntityView detailView) {
    detailViewTabPane.getTabs().add(new Tab(Entities.getCaption(detailView.getModel().getEntityID()), detailView));
    detailView.setMasterView(this);
  }

  private void checkIfInitalized() {
    if (initialized) {
      throw new IllegalStateException("View has already been initialized");
    }
  }

  private void addKeyEvents() {
    setOnKeyReleased(event -> {
      switch (event.getCode()) {
        case T:
          if (tableView != null && event.isControlDown()) {
            tableView.requestFocus();
            event.consume();
          }
          break;
        case F:
          if (tableView != null && event.isControlDown()) {
            tableView.getFilterTextField().requestFocus();
            event.consume();
          }
          break;
        case S:
          if (tableView != null && event.isControlDown()) {
            tableView.setCriteriaPaneVisible(true);
            tableView.requestFocus();
            event.consume();
          }
        case I:
          if (editView != null && event.isControlDown()) {
            editView.selectInputControl();
            event.consume();
          }
          break;
        case F5:
          tableView.getListModel().refresh();
          event.consume();
          break;
        case DOWN:
        case UP:
        case LEFT:
        case RIGHT:
          if (event.isControlDown() && event.isAltDown()) {
            navigate(event);
            event.consume();
          }
          break;
      }
    });
  }

  private void navigate(final KeyEvent event) {
    switch (event.getCode()) {
      case DOWN:
        navigateDown();
        break;
      case UP:
        navigateUp();
        break;
      case LEFT:
        navigateLeft();
        break;
      case RIGHT:
        navigateRight();
        break;
    }
  }

  private void navigateRight() {

  }

  private void navigateLeft() {

  }

  private void navigateUp() {
    if (masterView != null) {
      masterView.requestInputFocus();
    }
  }

  private void requestInputFocus() {
    if (editView != null) {
      editView.requestInitialFocus();
    }
    else if (tableView != null) {
      tableView.requestFocus();
    }
    else {
      requestFocus();
    }
  }

  private void navigateDown() {
    if (!detailViewTabPane.getTabs().isEmpty()) {
      ((EntityView) detailViewTabPane.getSelectionModel().getSelectedItem().getContent()).requestInputFocus();
    }
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
