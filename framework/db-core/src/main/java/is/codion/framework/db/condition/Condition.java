/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.Conjunction;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;

import java.util.List;

/**
 * Specifies objects serving as where conditions in database queries
 * @see Conditions for factory and builder methods
 */
public interface Condition {

  /**
   * @return the entity type
   */
  EntityType<?> getEntityType();

  /**
   * @return a list of the values this condition is based on, in the order they appear
   * in the condition clause. An empty list is returned in case no values are specified.
   */
  List<?> getValues();

  /**
   * @return a list of the attributes this condition is based on, in the same
   * order as their respective values appear in the condition clause.
   * An empty list is returned in case no values are specified.
   */
  List<Attribute<?>> getAttributes();

  /**
   * Combines this condition with the given one, AND'ing together.
   * @param conditions the conditions to combine with this one
   * @return a condition combination
   */
  Combination and(Condition... conditions);

  /**
   * Combines this condition with the given one, OR'ing together.
   * @param conditions the conditions to combine with this one
   * @return a condition combination
   */
  Combination or(Condition... conditions);

  /**
   * Returns a where clause element representing this condition, without the WHERE keyword.
   * @param definition the entity definition
   * @return a where clause element
   */
  String getWhereClause(EntityDefinition definition);

  /**
   * @return a {@link SelectCondition} based on this condition
   */
  SelectCondition select();

  /**
   * @return a {@link UpdateCondition} based on this condition
   */
  UpdateCondition update();

  /**
   * An interface encapsulating a combination of Condition objects,
   * that should be either AND'ed or OR'ed together in a query context
   */
  interface Combination extends Condition {

    /**
     * Adds new Condition objects to this combination
     * @param conditions the Condition to add
     * @return this combination instance
     */
    Combination add(Condition... conditions);

    /**
     * Adds a new Condition object to this combination
     * @param condition the Condition to add
     * @return this combination instance
     */
    Combination add(Condition condition);

    /**
     * @return the Conditions comprising this Combination
     */
    List<Condition> getConditions();

    /**
     * @return the conjunction
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
