/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.Refreshable;
import org.jminor.framework.domain.Property;

/**
 * A search model based on foreign key properties.
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
}
