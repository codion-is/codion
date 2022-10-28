/*
 * Copyright (c) 2008 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.Operator;
import is.codion.common.Text;
import is.codion.common.model.table.DefaultColumnConditionModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.ForeignKey;

import java.util.ArrayList;
import java.util.Arrays;

import static java.util.Objects.requireNonNull;

/**
 * A {@link is.codion.common.model.table.ColumnConditionModel} implementation based on a {@link EntitySearchModel}.
 * For instances use the {@link #entitySearchModelConditionModel(ForeignKey, EntitySearchModel)} factory method.
 * @see #entitySearchModelConditionModel(ForeignKey, EntitySearchModel)
 */
public final class EntitySearchModelConditionModel extends DefaultColumnConditionModel<ForeignKey, Entity> {

  private final EntitySearchModel entitySearchModel;

  private boolean updatingModel = false;

  private EntitySearchModelConditionModel(ForeignKey foreignKey, EntitySearchModel entitySearchModel) {
    super(foreignKey, Entity.class, Arrays.asList(Operator.EQUAL, Operator.NOT_EQUAL), Text.WILDCARD_CHARACTER.get());
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
    entitySearchModel.addSelectedEntitiesListener(selectedEntities -> {
      try {
        updatingModel = true;
        setEqualValues(null);//todo this is a hack, otherwise super.conditionChangedEvent doesn't get triggered
        setEqualValues(selectedEntities);
      }
      finally {
        updatingModel = false;
      }
    });
    addEqualsValueListener(() -> {
      if (!updatingModel) {
        entitySearchModel.setSelectedEntities(new ArrayList<>(getEqualValues()));
      }
    });
  }
}
