/*
 * Copyright (c) 2016 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.model.DefaultForeignKeyConditionModel;
import is.codion.framework.model.ForeignKeyConditionModel;

import java.util.Objects;

/**
 * A {@link is.codion.framework.model.ForeignKeyConditionModel} based around a {@link SwingEntityComboBoxModel}.
 * For instances use the {@link #swingForeignKeyConditionModel(ForeignKey, SwingEntityComboBoxModel)} factory method.
 * @see #swingForeignKeyConditionModel(ForeignKey, SwingEntityComboBoxModel)
 */
public final class SwingForeignKeyConditionModel extends DefaultForeignKeyConditionModel {

  private final SwingEntityComboBoxModel entityComboBoxModel;

  private SwingForeignKeyConditionModel(ForeignKey foreignKey, SwingEntityComboBoxModel comboBoxModel) {
    super(foreignKey);
    this.entityComboBoxModel = Objects.requireNonNull(comboBoxModel, "comboBoxModel");
    if (entityComboBoxModel.isCleared()) {
      entityComboBoxModel.setSelectedItem(getEqualValue());
    }
    bindComboBoxEvents();
  }

  @Override
  public void refresh() {
    entityComboBoxModel.refresh();
  }

  /**
   * @return the {@link SwingEntityComboBoxModel} used by this {@link ForeignKeyConditionModel}
   */
  public SwingEntityComboBoxModel entityComboBoxModel() {
    return entityComboBoxModel;
  }

  /**
   * Instantiates a {@link SwingForeignKeyConditionModel} instance
   * @param foreignKey the foreign key
   * @param comboBoxModel a SwingEntityComboBoxModel
   * @return a new {@link SwingForeignKeyConditionModel} instance
   */
  public static SwingForeignKeyConditionModel swingForeignKeyConditionModel(ForeignKey foreignKey, SwingEntityComboBoxModel comboBoxModel) {
    return new SwingForeignKeyConditionModel(foreignKey, comboBoxModel);
  }

  private void bindComboBoxEvents() {
    entityComboBoxModel.addSelectionListener(selected -> {
      if (!isUpdatingModel()) {
        setEqualValue(selected);
      }
    });
    addEqualsValueListener(() -> {
      try {
        setUpdatingModel(true);
        entityComboBoxModel.setSelectedItem(getEqualValue());
      }
      finally {
        setUpdatingModel(false);
      }
    });
    entityComboBoxModel.addRefreshListener(() -> entityComboBoxModel.setSelectedItem(getEqualValue()));
  }
}
