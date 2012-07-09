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
   * Returns a WHERE clause based on this EntityCriteria, note that this
   * where clause contains the ? substitute character instead of the actual values
   * @return a where clause based on this EntityCriteria
   * @see #getValues()
   */
  String getWhereClause();

  /**
   * Returns a WHERE clause based on this EntityCriteria, note that this
   * where clause contains the ? substitute character instead of the actual values
   * @param includeWhereKeyword if true the returned string is prefixed with the WHERE keyword,
   * if false it is prefixed with the AND keyword
   * @return a where clause based on this EntityCriteria
   * @see #getValues()
   */
  String getWhereClause(final boolean includeWhereKeyword);

  /**
   * @return the values the underlying criteria is based on, if any, in the order
   * they appear in the resulting where clause
   * @see #getWhereClause()
   */
  List<Object> getValues();

  /**
   * @return the properties of the values the underlying criteria is based on, if any,
   * in the order they appear in the resulting where clause
   * @see #getWhereClause()
   */
  List<Property.ColumnProperty> getValueProperties();
}
