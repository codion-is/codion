/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.Conjunction;
import is.codion.framework.domain.property.Attribute;

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
  List<Object> getValues();

  /**
   * @return a list of the attributes this condition is based on, in the same
   * order as their respective values appear in the condition clause.
   * An empty list is returned in case no values are specified.
   */
  List<Attribute<?>> getAttributes();

  /**
   * An interface encapsulating a combination of Condition objects,
   * that should be either AND'ed or OR'ed together in a query context
   */
  interface Combination extends Condition {

    /**
     * Adds a new Condition object to this set, adding null or a {@link EmptyCondition} instance has no effect
     * @param condition the Condition to add
     */
    void add(Condition condition);

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
   * An empty condition, with no values or attributes
   */
  final class EmptyCondition implements Condition {

    private static final long serialVersionUID = 1;

    @Override
    public List getValues() {
      return emptyList();
    }

    @Override
    public List<Attribute<?>> getAttributes() {
      return emptyList();
    }
  }
}
