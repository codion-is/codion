/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.model;

import dev.codion.common.model.Refreshable;
import dev.codion.common.model.table.ColumnConditionModel;
import dev.codion.framework.domain.entity.Entity;
import dev.codion.framework.domain.property.ForeignKeyProperty;

import java.util.Collection;

/**
 * A condition model based on foreign key properties, using one or more {@link Entity} instances as condition values.
 */
public interface ForeignKeyConditionModel extends ColumnConditionModel<Entity, ForeignKeyProperty>, Refreshable {

  /**
   * @return the EntityLookupModel used by this ForeignKeyConditionModel, if any
   */
  EntityLookupModel getEntityLookupModel();

  /**
   * @return the entities involved in the current condition, an empty Collection if no condition is specified
   */
  Collection<Entity> getConditionEntities();
}
