/*
 * Copyright (c) 2010 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.model.table.ColumnConditionModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.ForeignKey;

/**
 * A condition model based on a foreign key, using one or more {@link Entity} instances as condition values.
 */
public interface ForeignKeyConditionModel extends ColumnConditionModel<ForeignKey, Entity> {

  /**
   * @return the {@link EntitySearchModel} used by this {@link ForeignKeyConditionModel}, null if none is available
   */
  EntitySearchModel entitySearchModel();

  /**
   * Refreshes any data bound models in this condition model
   */
  void refresh();
}
