/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.swing.model;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.swing.model.table.ColumnSearchModel;
import org.jminor.framework.domain.Property;

/**
 * A base interface for searching a set of entities based on a property.
 */
public interface PropertySearchModel<T extends Property.SearchableProperty> extends ColumnSearchModel<T> {

  /**
   * @return a criteria object based on this search model
   */
  Criteria<Property.ColumnProperty> getCriteria();

  /**
   * Returns a String representing the state of this search model. The result of this method changes if any search
   * state element is changed, such as the search type or upper/lower bounds.
   * Note that this string is not meant for "human consumption", but comes in handy when trying to determine if
   * the state of this search model has changed.
   * @return a String representing the state of this search model
   */
  String toString();
}
