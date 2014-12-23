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

  /**
   * Returns a WHERE clause based on this EntityCriteria, note that this
   * where clause contains the ? substitute character instead of the actual values
   * @param includeWhereKeyword if true the returned string is prefixed with the WHERE keyword,
   * if false it is prefixed with the AND keyword
   * @return a where clause based on this EntityCriteria
   * @see #getValues()
   */
  String getWhereClause(final boolean includeWhereKeyword);
}
