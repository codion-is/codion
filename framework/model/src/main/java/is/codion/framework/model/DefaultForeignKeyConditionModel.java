/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.Operator;
import is.codion.common.model.table.DefaultColumnConditionModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.property.Property;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * A default ForeignKeyConditionModel implementation.
 */
public class DefaultForeignKeyConditionModel extends DefaultColumnConditionModel<ForeignKey, Entity>
        implements ForeignKeyConditionModel {

  private final EntitySearchModel entitySearchModel;

  private boolean updatingModel = false;

  /**
   * Constructs a DefaultForeignKeyConditionModel instance
   * @param foreignKey the foreign key
   */
  public DefaultForeignKeyConditionModel(final ForeignKey foreignKey) {
    this(foreignKey, null);
  }

  /**
   * Constructs a DefaultForeignKeyConditionModel instance
   * @param foreignKey the foreign key
   * @param entitySearchModel a EntitySearchModel
   */
  public DefaultForeignKeyConditionModel(final ForeignKey foreignKey, final EntitySearchModel entitySearchModel) {
    super(foreignKey, Entity.class, Arrays.asList(Operator.EQUAL, Operator.NOT_EQUAL), Property.WILDCARD_CHARACTER.get());
    this.entitySearchModel = entitySearchModel;
    if (entitySearchModel != null) {
      bindSearchModelEvents();
    }
  }

  @Override
  public final EntitySearchModel getEntitySearchModel() {
    return entitySearchModel;
  }

  @Override
  public void refresh() {/*Nothing to refresh in this default implementation*/}

  protected final boolean isUpdatingModel() {
    return updatingModel;
  }

  protected final void setUpdatingModel(final boolean updatingModel) {
    this.updatingModel = updatingModel;
  }

  private void bindSearchModelEvents() {
    entitySearchModel.addSelectedEntitiesListener(selectedEntities -> {
      try {
        setUpdatingModel(true);
        setEqualValues(null);//todo this is a hack, otherwise super.conditionChangedEvent doesn't get triggered
        setEqualValues(selectedEntities);
      }
      finally {
        setUpdatingModel(false);
      }
    });
    addEqualsValueListener(() -> {
      if (!isUpdatingModel()) {
        entitySearchModel.setSelectedEntities(new ArrayList<>(getEqualValues()));
      }
    });
  }
}
