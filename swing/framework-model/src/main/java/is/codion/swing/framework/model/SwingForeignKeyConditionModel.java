/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.model.DefaultForeignKeyConditionModel;

import java.util.Objects;

/**
 * A {@link is.codion.framework.model.ForeignKeyConditionModel} based around a {@link SwingEntityComboBoxModel}
 */
public final class SwingForeignKeyConditionModel extends DefaultForeignKeyConditionModel {

  private final SwingEntityComboBoxModel entityComboBoxModel;

  /**
   * Constructs a SwingForeignKeyConditionModel instance
   * @param foreignKey the foreign key
   * @param comboBoxModel a SwingEntityComboBoxModel
   */
  public SwingForeignKeyConditionModel(final ForeignKey foreignKey, final SwingEntityComboBoxModel comboBoxModel) {
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

  public SwingEntityComboBoxModel getEntityComboBoxModel() {
    return entityComboBoxModel;
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
