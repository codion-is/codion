/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui;

import org.jminor.framework.domain.Entities;
import org.jminor.framework.model.EntityModel;
import org.jminor.javafx.framework.model.FXEntityListModel;

import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;

import java.util.ArrayList;
import java.util.List;

public class EntityView extends BorderPane implements ViewTreeNode {

  private final SplitPane splitPane = new SplitPane();

  public enum PanelState {
    DIALOG, EMBEDDED, HIDDEN
  }

  private final String caption;
  private final EntityModel model;
  private final EntityEditView editView;
  private final EntityTableView tableView;
  private final List<EntityView> detailViews = new ArrayList<>();

  private ViewTreeNode parentView;

  private final TabPane detailViewTabPane = new TabPane();

  private boolean initialized = false;
  private PanelState detailPanelState = PanelState.EMBEDDED;

  public EntityView(final EntityModel model) {
    this(model, (EntityEditView) null);
  }

  public EntityView(final EntityModel model, final EntityEditView editView) {
    this(model, editView, new EntityTableView((FXEntityListModel) model.getTableModel()));
  }

  public EntityView(final EntityModel model, final EntityTableView tableView) {
    this(model, null, tableView);
  }

  public EntityView(final EntityModel model, final EntityEditView editView, final EntityTableView tableView) {
    this(Entities.getCaption(model.getEntityID()), model, editView, tableView);
  }

  public EntityView(final String caption, final EntityModel model, final EntityEditView editView, final EntityTableView tableView) {
    this.caption = caption;
    this.model = model;
    this.editView = editView;
    this.tableView = tableView;
    addKeyEvents();
    bindEvents();
  }

  public final EntityModel getModel() {
    return model;
  }

  public final String getCaption() {
    return caption;
  }

  public final void setParentView(final ViewTreeNode parentView) {
    this.parentView = parentView;
  }

  @Override
  public final ViewTreeNode getParentView() {
    return parentView;
  }

  @Override
  public final ViewTreeNode getPreviousSiblingView() {
    if (getParentView() == null) {
      return null;
    }

    final List<? extends ViewTreeNode> siblings = getParentView().getChildViews();
    if (siblings.contains(this)) {
      final int index = siblings.indexOf(this);
      if (index == 0) {
        return siblings.get(siblings.size() - 1);
      }
      else {
        return siblings.get(index - 1);
      }
    }

    return null;
  }

  @Override
  public final ViewTreeNode getNextSiblingView() {
    if (getParentView() == null) {//no parent, no siblings
      return null;
    }
    final List<? extends ViewTreeNode> siblings = getParentView().getChildViews();
    if (siblings.contains(this)) {
      final int index = siblings.indexOf(this);
      if (index == siblings.size() - 1) {
        return siblings.get(0);
      }
      else {
        return siblings.get(index + 1);
      }
    }

    return null;
  }

  @Override
  public final List<? extends ViewTreeNode> getChildViews() {
    return detailViews;
  }

  public final EntityView initializePanel() {
    if (!initialized) {
      initializeUI();
      initialized = true;
    }

    return this;
  }

  public EntityEditView getEditView() {
    return editView;
  }

  public EntityTableView getTableView() {
    return tableView;
  }

  public final void addDetailView(final EntityView detailView) {
    detailViews.add(detailView);
    detailView.setParentView(this);
  }

  /**
   * Saves any user preferences for all entity panels and associated elements
   */
  public final void savePreferences() {
    detailViews.forEach(EntityView::savePreferences);
    getModel().savePreferences();
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
          break;
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

  private void navigateLeft() {
    final EntityView leftSibling = (EntityView) getPreviousSiblingView();
    if (leftSibling != null) {
      leftSibling.activateView();
    }
  }

  private void navigateRight() {
    final EntityView rightSibling = (EntityView) getNextSiblingView();
    if (rightSibling != null) {
      rightSibling.activateView();
    }
  }

  private void navigateUp() {
    if (parentView != null && parentView instanceof EntityView) {
      ((EntityView) parentView).activateView();
    }
  }

  private void navigateDown() {
    if (!detailViewTabPane.getTabs().isEmpty()) {
      ((EntityView) detailViewTabPane.getSelectionModel().getSelectedItem().getContent()).activateView();
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

  private void activateView() {
    initializePanel();
    final TabPane parent = FXUiUtil.getParentOfType(this, TabPane.class);
    if (parent != null) {
      parent.getTabs().stream().filter(tab -> tab.getContent().equals(this)).forEach(tab -> {
        parent.getSelectionModel().select(tab);
      });
    }
    requestInputFocus();
  }

  private void bindEvents() {
    detailViewTabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
      if (newValue != null) {
        ((EntityView) newValue.getContent()).initializePanel();
      }
    });
  }

  private void initializeUI() {
    final BorderPane editPane;
    if (editView != null) {
      editPane = new BorderPane();
      editView.initializePanel();
      editPane.setCenter(editView);
      editPane.setRight(editView.getButtonPanel());
    }
    else {
      editPane = null;
    }
    final BorderPane tableBottomPane = new BorderPane();
    tableBottomPane.setCenter(tableView.getToolPane());
    final BorderPane tablePane = new BorderPane();
    tablePane.setCenter(tableView);
    tablePane.setBottom(tableBottomPane);
    if (detailViews.isEmpty()) {
      if (editPane != null) {
        setTop(editPane);
      }
      setCenter(tablePane);
    }
    else {
      final BorderPane leftPane = new BorderPane();
      if (editPane != null) {
        leftPane.setTop(editPane);
      }
      leftPane.setCenter(tablePane);

      for (final EntityView detailView : detailViews) {
        detailViewTabPane.getTabs().add(new Tab(detailView.getCaption(), detailView));
      }
      splitPane.getItems().add(leftPane);
      setCenter(splitPane);
      setDetailPanelState(detailPanelState);
    }
  }

  private void setDetailPanelState(final PanelState state) {
    if (detailViewTabPane == null) {
      this.detailPanelState = state;
      return;
    }
    if (state != PanelState.HIDDEN) {
      getTabbedDetailPanel().initializePanel();
    }

    final EntityModel entityModel = getModel();
    if (state == PanelState.HIDDEN) {
      entityModel.removeLinkedDetailModel(getTabbedDetailPanel().model);
    }
    else {
      entityModel.addLinkedDetailModel(getTabbedDetailPanel().model);
    }
    detailPanelState = state;
    if (state.equals(PanelState.EMBEDDED)) {
      splitPane.getItems().add(detailViewTabPane);
    }
  }

  private EntityView getTabbedDetailPanel() {
    return (EntityView) detailViewTabPane.getSelectionModel().getSelectedItem().getContent();
  }
}
