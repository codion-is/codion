/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.framework.domain.entity.Attribute;

import java.util.Map;

/**
 * A condition specifying a where clause along with properties and
 * their associated values for update.
 */
public interface EntityUpdateCondition extends EntityCondition {

  /**
   * Adds a attribute value to update
   * @param attribute the attribute
   * @param value the new value
   * @return this {@link EntityUpdateCondition} instance
   */
  <T> EntityUpdateCondition set(Attribute<T> attribute, T value);

  /**
   * @return the new values mapped to their respective attributes
   */
  Map<Attribute<?>, Object> getAttributeValues();
}
