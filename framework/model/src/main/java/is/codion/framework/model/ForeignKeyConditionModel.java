/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.model.Refreshable;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.ForeignKey;

/**
 * A condition model based on a foreign key, using one or more {@link Entity} instances as condition values.
 */
public interface ForeignKeyConditionModel extends ColumnConditionModel<Entity, ForeignKey, Entity>, Refreshable {

  /**
   * @return the EntityLookupModel used by this ForeignKeyConditionModel, if any
   */
  EntityLookupModel getEntityLookupModel();
}
