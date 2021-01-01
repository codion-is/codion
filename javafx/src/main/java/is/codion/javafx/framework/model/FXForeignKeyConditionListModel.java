/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.javafx.framework.model;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.model.DefaultForeignKeyConditionModel;

import java.util.Collection;

/**
 * A foreign key condition model based on an {@link ObservableEntityList}
 */
public final class FXForeignKeyConditionListModel extends DefaultForeignKeyConditionModel {

  private final ObservableEntityList listModel;

  /**
   * Constructs a FXForeignKeyConditionListModel instance
   * @param property the property
   * @param listModel the list model to use
   */
  public FXForeignKeyConditionListModel(final ForeignKeyProperty property, final ObservableEntityList listModel) {
    super(property);
    this.listModel = listModel;
    if (listModel != null) {
      listModel.refresh();
    }
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

  @Override
  public void refresh() {
    listModel.refresh();
  }

  @Override
  public void clear() {
    listModel.clear();
  }

  private void bindListModelEvents() {
    listModel.addSelectionChangedListener(() -> {
      if (!isUpdatingModel()) {
        setEqualValue(listModel.getSelectionModel().getSelectedItem());
      }
    });
    addEqualsValueListener(() -> {
      try {
        setUpdatingModel(true);
        final Collection<Entity> equalsValues = getEqualValues();
        if (!equalsValues.isEmpty()) {
          listModel.getSelectionModel().setSelectedItem(equalsValues.iterator().next());
        }
        else {
          listModel.getSelectionModel().clearSelection();
        }
      }
      finally {
        setUpdatingModel(false);
      }
    });
    listModel.addRefreshListener(() -> {
      final Collection<Entity> equalsValues = getEqualValues();
      if (!equalsValues.isEmpty()) {
        listModel.getSelectionModel().setSelectedItem(equalsValues.iterator().next());
      }
      else {
        listModel.getSelectionModel().clearSelection();
      }
    });
  }
}
