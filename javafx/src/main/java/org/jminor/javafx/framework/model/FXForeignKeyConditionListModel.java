/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.model;

import org.jminor.framework.db.condition.EntityConditions;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;
import org.jminor.framework.model.DefaultForeignKeyConditionModel;

import java.util.Collection;

/**
 * A foreign key condition model based on an {@link ObservableEntityList}
 */
public final class FXForeignKeyConditionListModel extends DefaultForeignKeyConditionModel {

  private final ObservableEntityList listModel;

  /**
   * Constructs a FXForeignKeyConditionListModel instance
   * @param entityConditions the {@link EntityConditions} instance to use
   * @param property the property
   * @param listModel the list model to use
   */
  public FXForeignKeyConditionListModel(final EntityConditions entityConditions, final Property.ForeignKeyProperty property,
                                        final ObservableEntityList listModel) {
    super(entityConditions, property);
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

  /**
   * @return the {@link ObservableEntityList} this model is based on
   */
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
