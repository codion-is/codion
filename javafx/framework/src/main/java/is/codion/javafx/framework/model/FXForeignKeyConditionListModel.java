/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.javafx.framework.model;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.model.DefaultForeignKeyConditionModel;

import java.util.Collection;

/**
 * A foreign key condition model based on an {@link ObservableEntityList}
 */
public final class FXForeignKeyConditionListModel extends DefaultForeignKeyConditionModel {

  private final ObservableEntityList listModel;

  /**
   * Constructs a FXForeignKeyConditionListModel instance
   * @param foreignKey the foreign key
   * @param listModel the list model to use
   */
  public FXForeignKeyConditionListModel(ForeignKey foreignKey, ObservableEntityList listModel) {
    super(foreignKey);
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

  private void bindListModelEvents() {
    listModel.addSelectionChangedListener(() -> {
      if (!isUpdatingModel()) {
        setEqualValue(listModel.getSelectionModel().getSelectedItem());
      }
    });
    addEqualsValueListener(() -> {
      try {
        setUpdatingModel(true);
        Collection<Entity> equalsValues = getEqualValues();
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
      Collection<Entity> equalsValues = getEqualValues();
      if (!equalsValues.isEmpty()) {
        listModel.getSelectionModel().setSelectedItem(equalsValues.iterator().next());
      }
      else {
        listModel.getSelectionModel().clearSelection();
      }
    });
  }
}