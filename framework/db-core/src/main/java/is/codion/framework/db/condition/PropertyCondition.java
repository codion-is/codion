/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.db.condition;

import dev.codion.common.db.Operator;
import dev.codion.framework.domain.property.ColumnProperty;
import dev.codion.framework.domain.property.Property;

/**
 * A Condition based on a {@link Property}
 */
public interface PropertyCondition extends Condition {

  /**
   * @return the propertyId
   */
  String getPropertyId();

  /**
   * @return the condition operator
   */
  Operator getOperator();

  /**
   * Returns the condition string represented by this condition
   * @param property the underlying property
   * @return the condition string
   */
  String getConditionString(ColumnProperty property);

  /**
   * @param caseSensitive false if this condition should not be case-sensitive
   * @return this condition
   */
  PropertyCondition setCaseSensitive(boolean caseSensitive);
}
