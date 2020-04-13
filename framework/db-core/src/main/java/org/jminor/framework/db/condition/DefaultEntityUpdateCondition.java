/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.condition;

import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;

final class DefaultEntityUpdateCondition extends DefaultEntityCondition implements EntityUpdateCondition {

  private static final long serialVersionUID = 1;

  private final Map<String, Object> propertyValues = new LinkedHashMap<>();

  DefaultEntityUpdateCondition(final String entityId) {
    super(entityId);
  }

  DefaultEntityUpdateCondition(final String entityId, final Condition condition) {
    super(entityId, condition);
  }

  @Override
  public EntityUpdateCondition set(final String propertyId, final Object value) {
    requireNonNull(propertyId, "propertyId");
    if (propertyValues.containsKey(propertyId)) {
      throw new IllegalArgumentException("Update condition already contains a value for property: " + propertyId);
    }
    propertyValues.put(propertyId, value);

    return this;
  }

  @Override
  public Map<String, Object> getPropertyValues() {
    return unmodifiableMap(propertyValues);
  }
}
