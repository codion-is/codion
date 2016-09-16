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

/**
 * A Pane joining a {@link EntityEditView} and a {@link EntityTableView}.
 */
public class EntityView extends BorderPane implements ViewTreeNode {

  private final SplitPane splitPane = new SplitPane();

  /**
   * The possible panel states
   */
  public enum PanelState {
    /**
     * The panel is displayed in a dialog
     */
    DIALOG,
    /**
     * The panel is embedded in its parent panel
     */
    EMBEDDED,
    /**
     * The panel is hidden
     */
    HIDDEN
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

  /**
   * Instantiates a new {@link EntityView} with no {@link EntityEditView} and a default {@link EntityTableView}
   * @param model the {@link EntityModel} to base this view on
   */
  public EntityView(final EntityModel model) {
    this(model, (EntityEditView) null);
  }

  /**
   * Instantiates a new {@link EntityView} with the given {@link EntityEditView} and a default {@link EntityTableView}
   * @param model the {@link EntityModel} to base this view on
   * @param editView the editView
   */
  public EntityView(final EntityModel model, final EntityEditView editView) {
    this(model, editView, new EntityTableView((FXEntityListModel) model.getTableModel()));
  }

  /**
   * Instantiates a new {@link EntityView} with no {@link EntityEditView} and the given {@link EntityTableView}
   * @param model the {@link EntityModel} to base this view on
   * @param tableView the tableView
   */
  public EntityView(final EntityModel model, final EntityTableView tableView) {
    this(model, null, tableView);
  }

  /**
   * Instantiates a new {@link EntityView} with the given {@link EntityEditView} and {@link EntityTableView}
   * @param model the {@link EntityModel} to base this view on
   * @param editView the editView
   * @param tableView the tableView
   */
  public EntityView(final EntityModel model, final EntityEditView editView, final EntityTableView tableView) {
    this(Entities.getCaption(model.getEntityID()), model, editView, tableView);
  }

  /**
   * Instantiates a new {@link EntityView} with the given {@link EntityEditView} and {@link EntityTableView}
   * @param caption the view caption
   * @param model the {@link EntityModel} to base this view on
   * @param editView the editView
   * @param tableView the tableView
   */
  public EntityView(final String caption, final EntityModel model, final EntityEditView editView, final EntityTableView tableView) {
    this.caption = caption;
    this.model = model;
    this.editView = editView;
    this.tableView = tableView;
    addKeyEvents();
    bindEvents();
  }

  /**
   * @return the underlying {@link EntityModel}
   */
  public final EntityModel getModel() {
    return model;
  }

  /**
   * @return the view caption
   */
  public final String getCaption() {
    return caption;
  }

  /**
   * Sets the parent view of this {@link EntityView}
   * @param parentView the parent view
   */
  public final void setParentView(final ViewTreeNode parentView) {
    this.parentView = parentView;
  }

  /** {@inheritDoc} */
  @Override
  public final ViewTreeNode getParentView() {
    return parentView;
  }

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
  @Override
  public final List<? extends ViewTreeNode> getChildViews() {
    return detailViews;
  }

  /**
   * Initializes this {@link EntityView}
   * @return the initialized {@link EntityView}
   */
  public final EntityView initializePanel() {
    if (!initialized) {
      initializeUI();
      initialized = true;
    }

    return this;
  }

  /**
   * @return the {@link EntityEditView} or null if none exists
   */
  public EntityEditView getEditView() {
    return editView;
  }

  /**
   * @return the {@link EntityTableView}
   */
  public EntityTableView getTableView() {
    return tableView;
  }

  /**
   * Adds a detail {@link EntityView} to this {@link EntityView}
   * @param detailView the detail view to add
   */
  public final void addDetailView(final EntityView detailView) {
    checkIfInitalized();
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
            tableView.setConditionPaneVisible(true);
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
          if (tableView != null) {
            tableView.getListModel().refresh();
            event.consume();
          }
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
        default:
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
      default:
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
      parent.getTabs().stream().filter(tab -> tab.getContent().equals(this)).forEach(tab -> parent.getSelectionModel().select(tab));
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
