/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.condition;

import org.jminor.common.db.ConditionType;
import org.jminor.framework.domain.property.ColumnProperty;
import org.jminor.framework.domain.property.Property;

/**
 * A Condition based on a {@link Property}
 */
public interface PropertyCondition extends Condition {

  /**
   * @return the propertyId
   */
  String getPropertyId();

  /**
   * @return the condition type
   */
  ConditionType getConditionType();

  /**
   * @param property the underlying property
   * @return the condition string
   */
  String getConditionString(final ColumnProperty property);

  /**
   * @param caseSensitive false if this condition should not be case-sensitive
   * @return this condition
   */
  PropertyCondition setCaseSensitive(final boolean caseSensitive);
}
