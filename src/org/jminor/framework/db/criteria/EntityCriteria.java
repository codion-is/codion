/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.criteria;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.framework.domain.Property;

import java.io.Serializable;
import java.util.List;

/**
 * A class encapsulating query criteria parameters.
 */
public interface EntityCriteria extends Serializable {

  /**
   * @return the entity ID
   */
  String getEntityID();

  /**
   * @return the Criteria object
   */
  Criteria<Property.ColumnProperty> getCriteria();

  /**
   * @return the where clause
   */
  String getWhereClause();

  /**
   * @param includeWhereKeyword if false AND is used instead of the WHERE keyword
   * @return a where clause base on this criteria
   */
  String getWhereClause(final boolean includeWhereKeyword);

  /**
   * @return the values the underlying criteria is based on, if any
   */
  List<Object> getValues();

  /**
   * @return the properties of the values the underlying criteria is based on, if any
   */
  List<Property.ColumnProperty> getValueProperties();
}
