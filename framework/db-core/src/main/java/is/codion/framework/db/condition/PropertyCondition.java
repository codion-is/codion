/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.db.Operator;
import is.codion.framework.domain.property.Attribute;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.Property;

/**
 * A Condition based on a {@link Property}
 */
public interface PropertyCondition extends Condition {

  /**
   * @return the propertyId
   */
  Attribute<?> getPropertyId();

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
