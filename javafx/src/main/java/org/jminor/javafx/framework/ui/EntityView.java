/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui;

import org.jminor.framework.domain.Entity;
import org.jminor.javafx.framework.model.EntityModel;

import javafx.collections.ListChangeListener;
import javafx.scene.layout.BorderPane;

import java.util.List;

public class EntityView extends BorderPane {

  private final EntityModel model;
  private final EntityEditView editView;
  private final EntityTableView tableView;
  private boolean initialized = false;

  public EntityView(final EntityModel model, final EntityEditView editView, final EntityTableView tableView) {
    this.model = model;
    this.editView = editView;
    this.tableView = tableView;
    bindEvents();
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
      }
    });
  }

  public final EntityView initializePanel() {
    if (!initialized) {
      initializeUI();
      initialized = true;
    }

    return this;
  }

  private void initializeUI() {
    editView.initializePanel();
    setTop(editView);
    setCenter(tableView);
  }
}
