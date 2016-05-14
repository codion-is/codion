/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.model.table.ColumnCriteriaModel;
import org.jminor.framework.domain.Property;

/**
 * A base interface for a column criteria based on a property.
 * @param <T> the type of {@link Property} this criteria model is based on
 */
public interface PropertyCriteriaModel<T extends Property.SearchableProperty> extends ColumnCriteriaModel<T> {

  /**
   * @return a criteria object based on this criteria model
   */
  Criteria<Property.ColumnProperty> getCriteria();

  /**
   * Returns a String representing the state of this criteria model. The result of this method changes if any criteria
   * state element is changed, such as the operator or upper/lower bounds.
   * Note that this string is not meant for "human consumption", but comes in handy when trying to determine if
   * the state of this criteria model has changed.
   * @return a String representing the state of this criteria model
   */
  @Override
  String toString();
}
