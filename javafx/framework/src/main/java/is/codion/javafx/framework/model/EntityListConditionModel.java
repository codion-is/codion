/*
 * Copyright (c) 2016 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.javafx.framework.model;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.model.AbstractForeignKeyConditionModel;

import java.util.Collection;

import static java.util.Objects.requireNonNull;

/**
 * A foreign key condition model based on an {@link ObservableEntityList}
 * For instances use the {@link #entityListConditionModel(ForeignKey, ObservableEntityList)} factory method.
 * @see #entityListConditionModel(ForeignKey, ObservableEntityList)
 */
public final class EntityListConditionModel extends AbstractForeignKeyConditionModel {

  private final ObservableEntityList listModel;

  private boolean updatingModel = false;

  private EntityListConditionModel(ForeignKey foreignKey, ObservableEntityList listModel) {
    super(foreignKey);
    this.listModel = requireNonNull(listModel, "listModel");
    listModel.refresh();
    bindListModelEvents();
  }

  /**
   * @return the {@link ObservableEntityList} this model is based on, null if none is available
   */
  public ObservableEntityList listModel() {
    return listModel;
  }

  /**
   * Refreshes the underlying list model.
   */
  public void refresh() {
    listModel.refresh();
  }

  /**
   * Instantiates a new {@link EntityListConditionModel} instance
   * @param foreignKey the foreign key
   * @param listModel the list model to use
   * @return a new {@link EntityListConditionModel} instance
   */
  public static EntityListConditionModel entityListConditionModel(ForeignKey foreignKey, ObservableEntityList listModel) {
    return new EntityListConditionModel(foreignKey, listModel);
  }

  private void bindListModelEvents() {
    listModel.addSelectionListener(() -> {
      if (!updatingModel) {
        setEqualValue(listModel.selectionModel().getSelectedItem());
      }
    });
    addEqualsValueListener(() -> {
      try {
        updatingModel = true;
        Collection<Entity> equalsValues = getEqualValues();
        if (!equalsValues.isEmpty()) {
          listModel.selectionModel().setSelectedItem(equalsValues.iterator().next());
        }
        else {
          listModel.selectionModel().clearSelection();
        }
      }
      finally {
        updatingModel = false;
      }
    });
    listModel.addRefreshListener(() -> listModel.selectionModelOptional().ifPresent(selectionModel -> {
      Collection<Entity> equalsValues = getEqualValues();
      if (!equalsValues.isEmpty()) {
        selectionModel.setSelectedItem(equalsValues.iterator().next());
      }
      else {
        selectionModel.clearSelection();
      }
    }));
  }
}
