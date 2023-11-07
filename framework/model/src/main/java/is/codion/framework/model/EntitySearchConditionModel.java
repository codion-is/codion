/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import java.util.Set;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * A {@link is.codion.common.model.table.ColumnConditionModel} implementation based on a {@link EntitySearchModel}.
 * For instances use the {@link #entitySearchConditionModel(ForeignKey, EntitySearchModel)} factory method.
 * @see #entitySearchConditionModel(ForeignKey, EntitySearchModel)
 */
public final class EntitySearchConditionModel extends AbstractForeignKeyConditionModel {

  private final EntitySearchModel entitySearchModel;

  private boolean updatingModel = false;

  private EntitySearchConditionModel(ForeignKey foreignKey, EntitySearchModel entitySearchModel) {
    super(foreignKey);
    this.entitySearchModel = requireNonNull(entitySearchModel, "entitySearchModel");
    bindSearchModelEvents();
  }

  /**
   * @return the {@link EntitySearchModel} used by this {@link EntitySearchConditionModel}
   */
  public EntitySearchModel searchModel() {
    return entitySearchModel;
  }

  /**
   * Instantiates a new {@link EntitySearchConditionModel} instance.
   * @param foreignKey the foreign key
   * @param entitySearchModel a EntitySearchModel
   * @return a new {@link EntitySearchConditionModel} instance.
   */
  public static EntitySearchConditionModel entitySearchConditionModel(ForeignKey foreignKey,
                                                                      EntitySearchModel entitySearchModel) {
    return new EntitySearchConditionModel(foreignKey, entitySearchModel);
  }

  private void bindSearchModelEvents() {
    entitySearchModel.entities().addDataListener(new EntitiesListener());
    equalValues().addDataListener(new EqualValuesListener());
  }

  private final class EntitiesListener implements Consumer<Set<Entity>> {

    @Override
    public void accept(Set<Entity> selectedEntities) {
      if (!updatingModel) {
        setEqualValues(selectedEntities);
      }
    }
  }

  private final class EqualValuesListener implements Consumer<Set<Entity>> {

    @Override
    public void accept(Set<Entity> equalValues) {
      updatingModel = true;
      try {
        entitySearchModel.entities().set(equalValues);
      }
      finally {
        updatingModel = false;
      }
    }
  }
}
