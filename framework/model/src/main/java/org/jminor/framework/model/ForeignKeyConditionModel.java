/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.model.Refreshable;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import java.util.Collection;

/**
 * A condition model based on foreign key properties, using one or more {@link Entity} instances as condition values.
 */
public interface ForeignKeyConditionModel extends PropertyConditionModel<Property.ForeignKeyProperty>, Refreshable {

  /**
   * @return the EntityLookupModel used by this ForeignKeyConditionModel, if any
   */
  EntityLookupModel getEntityLookupModel();

  /**
   * @return the entities involved in the current condition, an empty Collection if no condition is specified
   */
  Collection<Entity> getConditionEntities();
}
