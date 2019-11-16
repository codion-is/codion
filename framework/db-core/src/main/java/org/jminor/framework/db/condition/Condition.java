/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.condition;

import org.jminor.common.Conjunction;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.property.Property;

import java.io.Serializable;
import java.util.List;

import static java.util.Collections.emptyList;

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
   * Returns a condition string which can be used in a WHERE clause,
   * containing the ? substitution character in place of any values
   * @param definition the underlying entity definition
   * @return a where clause based on this condition
   */
  String getConditionString(final Entity.Definition definition);

  /**
   * Expands this condition, if required
   * @param definition the underlying entity definition
   * @return an expanded Condition, or this condition if expansion is not required
   */
  default Condition expand(final Entity.Definition definition) {
    return this;
  }

  /**
   * A Condition based on a custom {@link org.jminor.framework.domain.Entity.ConditionProvider}
   * associated with {@link CustomCondition#getConditionId()}
   */
  interface CustomCondition extends Condition {

    /**
     * @return the condition id
     */
    String getConditionId();
  }

  /**
   * A Condition based on a {@link Property}
   */
  interface PropertyCondition extends Condition {

    /**
     * @return the propertyId
     */
    String getPropertyId();
  }

  /**
   * An interface encapsulating a set of PropertyCondition objects,
   * that should be either AND'ed or OR'ed together in a query context
   */
  interface Set extends Condition {

    /**
     * Adds a new Condition object to this set, adding null or a {@link EmptyCondition} instance has no effect
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

  /**
   * An empty condition, with no values or propertyIds
   */
  final class EmptyCondition implements Condition {

    private static final long serialVersionUID = 1;

    @Override
    public List getValues() {
      return emptyList();
    }

    @Override
    public List<String> getPropertyIds() {
      return emptyList();
    }

    @Override
    public String getConditionString(final Entity.Definition definition) {
      return "";
    }
  }
}
