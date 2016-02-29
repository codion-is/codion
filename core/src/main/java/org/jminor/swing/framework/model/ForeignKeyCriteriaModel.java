/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.model;

import org.jminor.common.model.Refreshable;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import java.util.Collection;

/**
 * A criteria model based on foreign key properties, using one or more {@link Entity} instances as criteria values.
 */
public interface ForeignKeyCriteriaModel extends PropertyCriteriaModel<Property.ForeignKeyProperty>, Refreshable {

  /**
   * @return the EntityComboBoxModel used by this ForeignKeyCriteriaModel, if any
   */
  EntityComboBoxModel getEntityComboBoxModel();

  /**
   * @return the EntityLookupModel used by this ForeignKeyCriteriaModel, if any
   */
  EntityLookupModel getEntityLookupModel();

  /**
   * @return the entities involved in the current criteria, an empty Collection if no criteria is specified
   */
  Collection<Entity> getCriteriaEntities();
}
