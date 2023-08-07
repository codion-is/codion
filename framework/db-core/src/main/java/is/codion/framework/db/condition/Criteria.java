/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.Conjunction;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;

import java.util.Collection;
import java.util.List;

/**
 * Specifies a query criteria.
 * @see #all(EntityType)
 */
public interface Criteria {

  /**
   * @return the entity type
   */
  EntityType entityType();

  /**
   * @return a list of the values this criteria is based on, in the order they appear
   * in the condition clause. An empty list is returned in case no values are specified.
   */
  List<?> values();

  /**
   * @return a list of the attributes this criteria is based on, in the same
   * order as their respective values appear in the condition clause.
   * An empty list is returned in case no values are specified.
   */
  List<Attribute<?>> attributes();

  /**
   * Returns a string representing this condition, e.g. "column = ?" or "col1 is not null and col2 in (?, ?)".
   * @param definition the entity definition
   * @return a condition string
   */
  String toString(EntityDefinition definition);

  /**
   * An interface encapsulating a combination of Criteria instances,
   * that should be either AND'ed or OR'ed together in a query context
   */
  interface Combination extends Criteria {

    /**
     * @return the Conditions comprising this Combination
     */
    Collection<Criteria> criteria();

    /**
     * @return the conjunction
     */
    Conjunction conjunction();
  }

  /**
   * Creates an empty criteria specifying all entities of the given type
   * @param entityType the entity type
   * @return an empty criteria
   */
  static Criteria all(EntityType entityType) {
    return new AllCriteria(entityType);
  }
}
