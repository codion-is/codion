/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.db.Operator;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.property.ColumnProperty;

/**
 * A Condition based on a single {@link Attribute}.
 */
public interface AttributeCondition extends Condition {

  /**
   * @return the attribute
   */
  Attribute<?> getAttribute();

  /**
   * @return the condition operator
   */
  Operator getOperator();

  /**
   * @return true if this is a null condition, that is, checks for null
   */
  boolean isNullCondition();

  /**
   * Returns the condition string represented by this condition
   * @param property the underlying property
   * @param <T> the attribute type
   * @return the condition string
   */
  <T> String getConditionString(ColumnProperty<T> property);

  /**
   * @param caseSensitive false if this condition should not be case-sensitive
   * @return this condition
   */
  AttributeCondition setCaseSensitive(boolean caseSensitive);

  /**
   * @return true if this condition is case sensitive, only applicable to conditions based on String attributes.
   */
  boolean isCaseSensitive();
}
