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
   * Returns the condition string represented by this condition
   * @param property the underlying property
   * @return the condition string
   */
  String getConditionString(ColumnProperty<?> property);

  /**
   * @param caseSensitive false if this condition should not be case-sensitive
   * @return this condition
   */
  AttributeCondition setCaseSensitive(boolean caseSensitive);
}
