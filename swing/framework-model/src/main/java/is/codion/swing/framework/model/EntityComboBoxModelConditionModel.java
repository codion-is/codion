/*
 * Copyright (c) 2016 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.model.AbstractForeignKeyConditionModel;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * A {@link is.codion.common.model.table.ColumnConditionModel} based on a {@link EntityComboBoxModel}.
 * For instances use the {@link #entityComboBoxModelConditionModel(ForeignKey, EntityComboBoxModel)} factory method.
 * @see #entityComboBoxModelConditionModel(ForeignKey, EntityComboBoxModel)
 */
public final class EntityComboBoxModelConditionModel extends AbstractForeignKeyConditionModel {

  private final EntityComboBoxModel entityComboBoxModel;

  private boolean updatingModel = false;

  private EntityComboBoxModelConditionModel(ForeignKey foreignKey, EntityComboBoxModel comboBoxModel) {
    super(foreignKey);
    this.entityComboBoxModel = Objects.requireNonNull(comboBoxModel, "comboBoxModel");
    if (entityComboBoxModel.isCleared()) {
      entityComboBoxModel.setSelectedItem(getEqualValue());
    }
    bindComboBoxEvents();
  }

  /**
   * Refreshes the underlying combo box model.
   */
  public void refresh() {
    entityComboBoxModel.refresh();
  }

  /**
   * @return the {@link EntityComboBoxModel} used by this {@link EntityComboBoxModelConditionModel}
   */
  public EntityComboBoxModel entityComboBoxModel() {
    return entityComboBoxModel;
  }

  /**
   * Instantiates a {@link EntityComboBoxModelConditionModel} instance
   * @param foreignKey the foreign key
   * @param comboBoxModel a {@link EntityComboBoxModel}
   * @return a new {@link EntityComboBoxModelConditionModel} instance
   */
  public static EntityComboBoxModelConditionModel entityComboBoxModelConditionModel(ForeignKey foreignKey,
                                                                                    EntityComboBoxModel comboBoxModel) {
    return new EntityComboBoxModelConditionModel(foreignKey, comboBoxModel);
  }

  private void bindComboBoxEvents() {
    entityComboBoxModel.addSelectionListener(new SelectedEntityListener());
    equalValues().value().addDataListener(new EqualValueListener());
    entityComboBoxModel.refresher().addRefreshListener(() -> entityComboBoxModel.setSelectedItem(getEqualValue()));
  }

  private final class SelectedEntityListener implements Consumer<Entity> {

    @Override
    public void accept(Entity selectedEntity) {
      updatingModel = true;
      try {
        setEqualValue(selectedEntity);
      }
      finally {
        updatingModel = false;
      }
    }
  }

  private final class EqualValueListener implements Consumer<Entity> {

    @Override
    public void accept(Entity equalValue) {
      if (!updatingModel) {
        entityComboBoxModel.setSelectedItem(equalValue);
      }
    }
  }
}
