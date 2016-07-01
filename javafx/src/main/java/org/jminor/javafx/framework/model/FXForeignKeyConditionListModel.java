/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.model;

import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;
import org.jminor.framework.model.DefaultForeignKeyConditionModel;

import java.util.Collection;

public final class FXForeignKeyConditionListModel extends DefaultForeignKeyConditionModel {

  private final ObservableEntityList listModel;

  /**
   * Constructs a FXForeignKeyConditionListModel instance
   * @param property the property
   * @param listModel the list model to use
   */
  public FXForeignKeyConditionListModel(final Property.ForeignKeyProperty property, final ObservableEntityList listModel) {
    super(property);
    this.listModel = listModel;
    if (listModel != null) {
      listModel.refresh();
    }
//        if (listModel.isCleared()) {
//          listModel.setSelectedItem((Entity) getUpperBound());
//        }
    if (listModel != null) {
      bindListModelEvents();
    }
  }

  public ObservableEntityList getListModel() {
    return listModel;
  }

  /** {@inheritDoc} */
  @Override
  public final void refresh() {
    listModel.refresh();
  }

  /** {@inheritDoc} */
  @Override
  public final void clear() {
    listModel.clear();
  }

  private void bindListModelEvents() {
    listModel.addSelectionChangedListener(() -> {
      if (!isUpdatingModel()) {
        setUpperBound(listModel.getSelectionModel().getSelectedItem());
      }
    });
    addUpperBoundListener(() -> {
      try {
        setUpdatingModel(true);
        final Object upper = getUpperBound();
        if (upper instanceof Collection && !((Collection) upper).isEmpty()) {
          listModel.getSelectionModel().setSelectedItem(((Collection<Entity>) upper).iterator().next());
        }
        else {
          listModel.getSelectionModel().setSelectedItem((Entity) upper);
        }
      }
      finally {
        setUpdatingModel(false);
      }
    });
    listModel.addRefreshListener(() -> {
      final Object upper = getUpperBound();
      if (upper instanceof Collection && !((Collection) upper).isEmpty()) {
        listModel.getSelectionModel().setSelectedItem(((Collection<Entity>) upper).iterator().next());
      }
      else {
        listModel.getSelectionModel().setSelectedItem((Entity) upper);
      }
    });
  }
}
