/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.ForeignKey;

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
    entitySearchModel.addSelectedEntitiesListener(new SelectedEntitiesListener());
    equalValueSet().addDataListener(new EqualValuesListener());
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
        entitySearchModel.setSelectedEntities(new ArrayList<>(equalValues));
      }
    }
  }
}
