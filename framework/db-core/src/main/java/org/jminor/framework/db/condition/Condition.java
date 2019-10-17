/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.condition;

import org.jminor.framework.domain.Property;

import java.io.Serializable;
import java.util.List;

/**
 * Specifies objects serving as where conditions in database queries
 */
public interface Condition extends Serializable {

  /**
   * Returns a condition clause based on this Condition without the WHERE keyword,
   * note that this clause contains the ? substitution character instead of actual values.
   * Note that this method can return an empty string.
   * @return a where clause based on this Condition or an empty string if it does not represent a condition
   * @see #getValues()
   */
  String getWhereClause();

  /**
   * @return a list of the values this condition is based on, in the order they appear
   * in the condition clause. An empty list is returned in case no values are specified.
   */
  List getValues();

  /**
   * @return a list of the properties this condition is based on, in the same
   * order as their respective values appear in the condition clause.
   * An empty list is returned in case no values are specified.
   */
  List<Property.ColumnProperty> getProperties();

  /**
   * An interface encapsulating a set of PropertyCondition objects,
   * that should be either AND'ed or OR'ed together in a query context
   */
  interface Set extends Condition {

    /**
     * Adds a new Condition object to this set, adding a null condition has no effect
     * @param condition the Condition to add
     */
    void add(final Condition condition);

    /**
     * @return the number of conditions in this set
     */
    int getConditionCount();
  }

  /**
   * For providing dynamic Conditions
   * @param <T> the type used to describe the condition values
   */
  interface Provider {

    /**
     * @return the Condition
     */
    Condition getCondition();
  }
}
