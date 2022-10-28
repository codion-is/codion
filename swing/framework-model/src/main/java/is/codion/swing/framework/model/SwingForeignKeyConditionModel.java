/*
 * Copyright (c) 2016 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model;

import is.codion.common.Operator;
import is.codion.common.Text;
import is.codion.common.model.table.DefaultColumnConditionModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.ForeignKey;

import java.util.Arrays;
import java.util.Objects;

/**
 * A {@link is.codion.common.model.table.ColumnConditionModel} based on a {@link SwingEntityComboBoxModel}.
 * For instances use the {@link #swingForeignKeyConditionModel(ForeignKey, SwingEntityComboBoxModel)} factory method.
 * @see #swingForeignKeyConditionModel(ForeignKey, SwingEntityComboBoxModel)
 */
public final class SwingForeignKeyConditionModel extends DefaultColumnConditionModel<ForeignKey, Entity> {

  private final SwingEntityComboBoxModel entityComboBoxModel;

  private boolean updatingModel = false;

  private SwingForeignKeyConditionModel(ForeignKey foreignKey, SwingEntityComboBoxModel comboBoxModel) {
    super(foreignKey, Entity.class, Arrays.asList(Operator.EQUAL, Operator.NOT_EQUAL), Text.WILDCARD_CHARACTER.get());
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
   * @return the {@link SwingEntityComboBoxModel} used by this {@link SwingForeignKeyConditionModel}
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
      if (!updatingModel) {
        setEqualValue(selected);
      }
    });
    addEqualsValueListener(() -> {
      try {
        updatingModel = true;
        entityComboBoxModel.setSelectedItem(getEqualValue());
      }
      finally {
        updatingModel = false;
      }
    });
    entityComboBoxModel.addRefreshListener(() -> entityComboBoxModel.setSelectedItem(getEqualValue()));
  }
}
