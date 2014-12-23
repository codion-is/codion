/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.Refreshable;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import java.util.Collection;

/**
 * A search model based on foreign key properties, using one or more {@link Entity} instances
 * as search values.
 */
public interface ForeignKeySearchModel extends PropertySearchModel<Property.ForeignKeyProperty>, Refreshable {

  /**
   * @return the EntityComboBoxModel used by this PropertySearchModel, if any
   */
  EntityComboBoxModel getEntityComboBoxModel();

  /**
   * @return the EntityLookupModel used by this PropertySearchModel, if any
   */
  EntityLookupModel getEntityLookupModel();

  /**
   * @return the entities involved in the current search criteria,
   * an empty Collection if no search criteria is specified
   */
  Collection<Entity> getSearchEntities();
}
