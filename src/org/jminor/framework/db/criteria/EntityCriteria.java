/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.criteria;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.framework.domain.Property;

/**
 * A class encapsulating query criteria parameters for querying a set of entities.
 */
public interface EntityCriteria extends Criteria<Property.ColumnProperty> {

  /**
   * @return the entity ID
   */
  String getEntityID();

  /**
   * @return the Criteria object
   */
  Criteria<Property.ColumnProperty> getCriteria();
}
