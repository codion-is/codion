/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.db.Operator;
import is.codion.framework.domain.entity.Attribute;

/**
 * A Condition based on a single {@link Attribute}.
 * @param <T> the attribute type
 */
public interface AttributeCondition<T> extends Condition {

  /**
   * @return the attribute
   */
  Attribute<T> getAttribute();

  /**
   * @return the condition operator
   */
  Operator getOperator();

  /**
   * @return true if this is a null condition, that is, checks for null
   */
  boolean isNullCondition();

  /**
   * @param caseSensitive false if this condition should not be case-sensitive
   * @return this condition
   */
  AttributeCondition<T> setCaseSensitive(boolean caseSensitive);

  /**
   * @return true if this condition is case sensitive, only applicable to conditions based on String attributes.
   */
  boolean isCaseSensitive();
}
