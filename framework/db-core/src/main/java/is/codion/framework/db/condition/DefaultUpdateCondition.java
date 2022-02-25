/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityDefinition;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;

final class DefaultUpdateCondition extends AbstractCondition implements UpdateCondition {

  private static final long serialVersionUID = 1;

  private final Condition condition;
  private final Map<Attribute<?>, Object> propertyValues = new LinkedHashMap<>();

  DefaultUpdateCondition(Condition condition) {
    super(requireNonNull(condition, "condition").getEntityType());
    this.condition = condition;
  }

  private DefaultUpdateCondition(DefaultUpdateCondition updateCondition) {
    super(updateCondition.getEntityType());
    this.condition = updateCondition.condition;
    this.propertyValues.putAll(updateCondition.propertyValues);
  }

  @Override
  public Condition getCondition() {
    return condition;
  }

  @Override
  public List<?> getValues() {
    return condition.getValues();
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return condition.getAttributes();
  }

  @Override
  public String getConditionString(EntityDefinition definition) {
    return condition.getConditionString(definition);
  }

  @Override
  public <T> UpdateCondition set(Attribute<T> attribute, T value) {
    requireNonNull(attribute, "attribute");
    if (propertyValues.containsKey(attribute)) {
      throw new IllegalArgumentException("Update condition already contains a value for attribute: " + attribute);
    }
    DefaultUpdateCondition updateCondition = new DefaultUpdateCondition(this);
    updateCondition.propertyValues.put(attribute, value);

    return updateCondition;
  }

  @Override
  public Map<Attribute<?>, Object> getAttributeValues() {
    return unmodifiableMap(propertyValues);
  }
}
