/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.condition;

import java.util.Map;

/**
 * A condition specifying a where clause along with properties and
 * their associated values for update.
 */
public interface EntityUpdateCondition extends EntityCondition {

  /**
   * Adds a property value to update
   * @param propertyId the propertyId
   * @param value the new value
   * @return this {@link EntityUpdateCondition} instance
   */
  EntityUpdateCondition set(final String propertyId, final Object value);

  /**
   * @return the new values mapped to their respective propertyIds
   */
  Map<String, Object> getPropertyValues();
}
