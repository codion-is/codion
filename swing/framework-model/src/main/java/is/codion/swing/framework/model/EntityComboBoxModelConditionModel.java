/*
 * Copyright (c) 2016 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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
 * A {@link is.codion.common.model.table.ColumnConditionModel} based on a {@link EntityComboBoxModel}.
 * For instances use the {@link #entityComboBoxModelConditionModel(ForeignKey, EntityComboBoxModel)} factory method.
 * @see #entityComboBoxModelConditionModel(ForeignKey, EntityComboBoxModel)
 */
public final class EntityComboBoxModelConditionModel extends DefaultColumnConditionModel<Entity, ForeignKey, Entity> {

  private final EntityComboBoxModel entityComboBoxModel;

  private boolean updatingModel = false;

  private EntityComboBoxModelConditionModel(ForeignKey foreignKey, EntityComboBoxModel comboBoxModel) {
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
