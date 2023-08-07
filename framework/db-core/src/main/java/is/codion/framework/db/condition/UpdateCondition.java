/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.framework.domain.entity.Attribute;

import java.util.Map;

/**
 * A condition specifying a where clause along with attributes and their associated values for update.
 */
public interface UpdateCondition extends Condition {

  /**
   * @return an unmodifiable view of the new values mapped to their respective attributes
   */
  Map<Attribute<?>, Object> attributeValues();

  /**
   * Builds an {@link UpdateCondition}.
   */
  interface Builder {

    /**
     * Adds a attribute value to update
     * @param attribute the attribute
     * @param value the new value
     * @param <T> the value type
     * @return this builder
     */
    <T> Builder set(Attribute<?> attribute, T value);

    /**
     * @return a new {@link UpdateCondition} instance based on this builder
     */
    UpdateCondition build();
  }
}
