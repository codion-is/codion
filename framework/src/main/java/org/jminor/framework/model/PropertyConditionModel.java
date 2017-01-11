/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.db.condition.Condition;
import org.jminor.common.model.table.ColumnConditionModel;
import org.jminor.framework.domain.Property;

/**
 * A base interface for a column condition based on a property.
 * @param <T> the type of {@link Property} this condition model is based on
 */
public interface PropertyConditionModel<T extends Property> extends ColumnConditionModel<T>, Condition.Provider<Property.ColumnProperty> {

  /**
   * @return a condition object based on this condition model
   */
  Condition<Property.ColumnProperty> getCondition();

  /**
   * Returns a String representing the state of this condition model. The result of this method changes if any condition
   * state element is changed, such as the operator or upper/lower bounds.
   * Note that this string is not meant for "human consumption", but comes in handy when trying to determine if
   * the state of this condition model has changed.
   * @return a String representing the state of this condition model
   */
  @Override
  String toString();
}
