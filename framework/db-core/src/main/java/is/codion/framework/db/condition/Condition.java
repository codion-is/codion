/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.Conjunction;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityType;

import java.io.Serializable;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

/**
 * Specifies objects serving as where conditions in database queries
 */
public interface Condition extends Serializable {

  /**
   * @return the entity type
   */
  EntityType<?> getEntityType();

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
   * An interface encapsulating a combination of Condition objects,
   * that should be either AND'ed or OR'ed together in a query context
   */
  interface Combination extends Condition {

    /**
     * Adds new Condition objects to this combination, adding a {@link EmptyCondition} instance has no effect
     * @param conditions the Condition to add
     * @return this combination instance
     */
    Combination add(Condition... conditions);

    /**
     * Adds a new Condition object to this combination, adding null or a {@link EmptyCondition} instance has no effect
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

  /**
   * An empty condition, with no values or attributes
   */
  final class EmptyCondition extends AbstractCondition implements Condition {

    private static final long serialVersionUID = 1;

    private final EntityType<?> entityType;

    EmptyCondition(final EntityType<?> entityType) {
      this.entityType = requireNonNull(entityType);
    }

    @Override
    public EntityType<?> getEntityType() {
      return entityType;
    }

    @Override
    public List<Object> getValues() {
      return emptyList();
    }

    @Override
    public List<Attribute<?>> getAttributes() {
      return emptyList();
    }
  }
}
