/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2016 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.model;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.model.AbstractForeignKeyConditionModel;
import is.codion.swing.framework.model.component.EntityComboBoxModel;

import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * A {@link is.codion.common.model.table.ColumnConditionModel} based on a {@link EntityComboBoxModel}.
 * For instances use the {@link #entityComboBoxConditionModel(ForeignKey, EntityComboBoxModel)} factory method.
 * @see #entityComboBoxConditionModel(ForeignKey, EntityComboBoxModel)
 */
public final class EntityComboBoxConditionModel extends AbstractForeignKeyConditionModel {

  private final EntityComboBoxModel entityComboBoxModel;

  private boolean updatingModel = false;

  private EntityComboBoxConditionModel(ForeignKey foreignKey, EntityComboBoxModel comboBoxModel) {
    super(foreignKey);
    this.entityComboBoxModel = Objects.requireNonNull(comboBoxModel, "comboBoxModel");
    if (entityComboBoxModel.cleared()) {
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
   * @return the {@link EntityComboBoxModel} used by this {@link EntityComboBoxConditionModel}
   */
  public EntityComboBoxModel comboBoxModel() {
    return entityComboBoxModel;
  }

  /**
   * Instantiates a {@link EntityComboBoxConditionModel} instance
   * @param foreignKey the foreign key
   * @param comboBoxModel a {@link EntityComboBoxModel}
   * @return a new {@link EntityComboBoxConditionModel} instance
   */
  public static EntityComboBoxConditionModel entityComboBoxConditionModel(ForeignKey foreignKey,
                                                                          EntityComboBoxModel comboBoxModel) {
    return new EntityComboBoxConditionModel(foreignKey, comboBoxModel);
  }

  private void bindComboBoxEvents() {
    entityComboBoxModel.addSelectionListener(new SelectedEntityListener());
    equalValues().addDataListener(new EqualValuesListener());
    entityComboBoxModel.refresher().addRefreshListener(() -> entityComboBoxModel.setSelectedItem(getEqualValue()));
  }

  private final class SelectedEntityListener implements Consumer<Entity> {

    @Override
    public void accept(Entity selectedEntity) {
      if (!updatingModel) {
        setEqualValue(selectedEntity);
      }
    }
  }

  private final class EqualValuesListener implements Consumer<Set<Entity>> {

    @Override
    public void accept(Set<Entity> equalValues) {
      updatingModel = true;
      try {
        entityComboBoxModel.setSelectedItem(equalValues.isEmpty() ? null : equalValues.iterator().next());
      }
      finally {
        updatingModel = false;
      }
    }
  }
}
