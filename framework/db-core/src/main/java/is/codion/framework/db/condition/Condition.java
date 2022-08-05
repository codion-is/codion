/*
 * Copyright (c) 2008 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.Conjunction;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;

import java.util.Collection;
import java.util.List;

/**
 * Specifies a query condition.
 * @see Conditions for factory and builder methods
 */
public interface Condition {

  /**
   * @return the entity type
   */
  EntityType getEntityType();

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
   * Returns a new Combination instance, combining this condition with the given one, AND'ing together.
   * @param conditions the conditions to combine with this one
   * @return a new condition combination
   */
  Combination and(Condition... conditions);

  /**
   * Returns a new Combination instance, combining this condition with the given one, OR'ing together.
   * @param conditions the conditions to combine with this one
   * @return a new condition combination
   */
  Combination or(Condition... conditions);

  /**
   * Returns a string representing this condition, e.g. "column = ?" or "col1 is not null and col2 in (?, ?)".
   * @param definition the entity definition
   * @return a condition string
   */
  String getConditionString(EntityDefinition definition);

  /**
   * @return a {@link SelectCondition.Builder} instance based on this condition
   */
  SelectCondition.Builder selectBuilder();

  /**
   * @return a {@link UpdateCondition.Builder} instance based on this condition
   */
  UpdateCondition.Builder updateBuilder();

  /**
   * An interface encapsulating a combination of Condition objects,
   * that should be either AND'ed or OR'ed together in a query context
   */
  interface Combination extends Condition {

    /**
     * @return the Conditions comprising this Combination
     */
    Collection<Condition> getConditions();

    /**
     * @return the conjunction
     */
    Conjunction getConjunction();
  }
}
