/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.model.table.DefaultColumnConditionModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.property.Property;

import java.util.ArrayList;

/**
 * A default ForeignKeyConditionModel implementation.
 */
public class DefaultForeignKeyConditionModel extends DefaultColumnConditionModel<Entity, ForeignKey, Entity>
        implements ForeignKeyConditionModel {

  private final EntityLookupModel entityLookupModel;

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
   * @param entityLookupModel a EntityLookupModel
   */
  public DefaultForeignKeyConditionModel(final ForeignKey foreignKey, final EntityLookupModel entityLookupModel) {
    super(foreignKey, Entity.class, Property.WILDCARD_CHARACTER.get());
    this.entityLookupModel = entityLookupModel;
    if (entityLookupModel != null) {
      bindLookupModelEvents();
    }
  }

  @Override
  public final EntityLookupModel getEntityLookupModel() {
    return entityLookupModel;
  }

  @Override
  public void refresh() {/*Nothing to refresh in this default implementation*/}

  protected final boolean isUpdatingModel() {
    return updatingModel;
  }

  protected final void setUpdatingModel(final boolean updatingModel) {
    this.updatingModel = updatingModel;
  }

  private void bindLookupModelEvents() {
    entityLookupModel.addSelectedEntitiesListener(selectedEntities -> {
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
        entityLookupModel.setSelectedEntities(new ArrayList<>(getEqualValues()));
      }
    });
  }
}
