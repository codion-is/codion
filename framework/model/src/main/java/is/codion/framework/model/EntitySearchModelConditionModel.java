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
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * A {@link is.codion.common.model.table.ColumnConditionModel} implementation based on a {@link EntitySearchModel}.
 * For instances use the {@link #entitySearchModelConditionModel(ForeignKey, EntitySearchModel)} factory method.
 * @see #entitySearchModelConditionModel(ForeignKey, EntitySearchModel)
 */
public final class EntitySearchModelConditionModel extends AbstractForeignKeyConditionModel {

  private final EntitySearchModel entitySearchModel;

  private boolean updatingModel = false;

  private EntitySearchModelConditionModel(ForeignKey foreignKey, EntitySearchModel entitySearchModel) {
    super(foreignKey);
    this.entitySearchModel = requireNonNull(entitySearchModel, "entitySearchModel");
    bindSearchModelEvents();
  }

  /**
   * @return the {@link EntitySearchModel} used by this {@link EntitySearchModelConditionModel}
   */
  public EntitySearchModel entitySearchModel() {
    return entitySearchModel;
  }

  /**
   * Instantiates a new {@link EntitySearchModelConditionModel} instance.
   * @param foreignKey the foreign key
   * @param entitySearchModel a EntitySearchModel
   * @return a new {@link EntitySearchModelConditionModel} instance.
   */
  public static EntitySearchModelConditionModel entitySearchModelConditionModel(ForeignKey foreignKey,
                                                                                EntitySearchModel entitySearchModel) {
    return new EntitySearchModelConditionModel(foreignKey, entitySearchModel);
  }

  private void bindSearchModelEvents() {
    entitySearchModel.addListener(new SelectedEntitiesListener());
    equalValues().addDataListener(new EqualValuesListener());
  }

  private final class SelectedEntitiesListener implements Consumer<List<Entity>> {

    @Override
    public void accept(List<Entity> selectedEntities) {
      updatingModel = true;
      try {
        setEqualValues(null);//todo this is a hack, otherwise super.conditionChangedEvent doesn't get triggered
        setEqualValues(selectedEntities);
      }
      finally {
        updatingModel = false;
      }
    }
  }

  private final class EqualValuesListener implements Consumer<Set<Entity>> {

    @Override
    public void accept(Set<Entity> equalValues) {
      if (!updatingModel) {
        entitySearchModel.setEntities(new ArrayList<>(equalValues));
      }
    }
  }
}
