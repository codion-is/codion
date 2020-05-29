/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.framework.domain.property.Attribute;

import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;

final class DefaultEntityUpdateCondition extends DefaultEntityCondition implements EntityUpdateCondition {

  private static final long serialVersionUID = 1;

  private final Map<Attribute<?>, Object> propertyValues = new LinkedHashMap<>();

  DefaultEntityUpdateCondition(final String entityId) {
    super(entityId);
  }

  DefaultEntityUpdateCondition(final String entityId, final Condition condition) {
    super(entityId, condition);
  }

  @Override
  public EntityUpdateCondition set(final Attribute<?> attribute, final Object value) {
    requireNonNull(attribute, "attribute");
    if (propertyValues.containsKey(attribute)) {
      throw new IllegalArgumentException("Update condition already contains a value for attribute: " + attribute);
    }
    propertyValues.put(attribute, value);

    return this;
  }

  @Override
  public Map<Attribute<?>, Object> getAttributeValues() {
    return unmodifiableMap(propertyValues);
  }
}
