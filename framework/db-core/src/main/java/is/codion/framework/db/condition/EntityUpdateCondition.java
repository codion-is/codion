/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.framework.domain.property.Attribute;

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
  EntityUpdateCondition set(Attribute<?> propertyId, Object value);

  /**
   * @return the new values mapped to their respective propertyIds
   */
  Map<Attribute<?>, Object> getPropertyValues();
}
