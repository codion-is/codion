/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.condition;

import org.jminor.common.Conjunction;
import org.jminor.common.db.ConditionType;

import java.io.Serializable;
import java.util.List;

/**
 * Specifies objects serving as where conditions in database queries
 */
public interface Condition extends Serializable {

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
  List<String> getPropertyIds();

  /**
   * A Condition based around a hard coded string
   */
  interface StringCondition extends Condition {

    /**
     * @return the condition string, with ? as substitution character
     */
    String getConditionString();
  }

  /**
   * A Condition based on a {@link org.jminor.framework.domain.Property}
   */
  interface PropertyCondition extends Condition {

    /**
     * @return the propertyId
     */
    String getPropertyId();

    /**
     * @return the condition type
     */
    ConditionType getConditionType();

    /**
     * @return true if this condition denotes a null condition, as in, where x is null
     */
    boolean isNullCondition();

    /**
     * @return true if this condition is case sensitive, only applicable to conditions based on string properties
     */
    boolean isCaseSensitive();
  }

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
     * @return the Conditions contained in this Set
     */
    List<Condition> getConditions();

    /**
     * @return the Set conjunction
     */
    Conjunction getConjunction();
  }

  /**
   * For providing dynamic Conditions
   */
  interface Provider {

    /**
     * @return the Condition
     */
    Condition getCondition();
  }
}
