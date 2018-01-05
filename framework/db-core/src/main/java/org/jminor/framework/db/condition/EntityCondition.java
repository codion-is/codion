/*
 * Chinook.Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.condition;

import org.jminor.common.db.condition.Condition;
import org.jminor.framework.domain.Property;

/**
 * A class encapsulating query condition parameters for querying a set of entities.
 */
public interface EntityCondition extends Condition<Property.ColumnProperty> {

  /**
   * @return the entity ID
   */
  String getEntityId();

  /**
   * @return the underlying {@link Condition} object, can be null
   */
  Condition<Property.ColumnProperty> getCondition();
}
