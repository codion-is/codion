/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui;

import org.jminor.framework.domain.Entity;

import javafx.collections.ListChangeListener;
import javafx.scene.layout.BorderPane;

import java.util.List;

public class EntityView extends BorderPane {

  private final EntityEditView editView;
  private final EntityTableView tableView;
  private boolean initialized = false;

  public EntityView(final EntityEditView editView, final EntityTableView tableView) {
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
          editView.setEntity(null);
        }
        else {
          editView.setEntity(selected.get(0));
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
