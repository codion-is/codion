/*
 * Copyright (c) 2020 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.framework.domain.entity.Attribute;

import java.util.Map;

/**
 * A condition specifying a where clause along with properties and
 * their associated values for update.
 */
public interface UpdateCondition extends Condition {

  /**
   * @return the underlying condition
   */
  Condition getCondition();

  /**
   * Adds a attribute value to update
   * @param attribute the attribute
   * @param value the new value
   * @param <T> the value type
   * @return a new {@link UpdateCondition} instance with the given value to update
   */
  <T> UpdateCondition set(Attribute<T> attribute, T value);

  /**
   * @return an unmodifiable view of the new values mapped to their respective attributes
   */
  Map<Attribute<?>, Object> getAttributeValues();
}
