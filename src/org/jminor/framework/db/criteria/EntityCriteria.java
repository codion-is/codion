/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.criteria;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.framework.domain.Property;

import java.util.List;

/**
 * A class encapsulating query criteria parameters for querying a set of entities.
 */
public interface EntityCriteria {

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
   * @param includeWhereKeyword if true the returned string is prefixed with the WHERE keyword,
   * if false it is prefixed with the AND keyword
   * @return a where clause base on this criteria
   */
  String getWhereClause(final boolean includeWhereKeyword);

  /**
   * @return the values the underlying criteria is based on, if any, in the order
   * their respective properties are returned by <code>getValueProperties()</code>
   */
  List<Object> getValues();

  /**
   * @return the properties of the values the underlying criteria is based on, if any,
   * in the order their respective values are returned by <code>getValues()</code>
   */
  List<Property.ColumnProperty> getValueProperties();
}
