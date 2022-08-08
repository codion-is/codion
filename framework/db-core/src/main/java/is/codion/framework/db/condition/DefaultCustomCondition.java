/*
 * Copyright (c) 2019 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.ConditionType;
import is.codion.framework.domain.entity.EntityDefinition;

import java.util.List;
import java.util.Objects;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

final class DefaultCustomCondition extends AbstractCondition implements CustomCondition {

  private static final long serialVersionUID = 1;

  private final ConditionType conditionType;
  private final List<Attribute<?>> attributes;
  private final List<Object> values;

  DefaultCustomCondition(ConditionType conditionType, List<Attribute<?>> attributes,
                         List<Object> values) {
    super(requireNonNull(conditionType, "conditionType").entityType());
    this.conditionType = conditionType;
    this.attributes = unmodifiableList(requireNonNull(attributes, "attributes"));
    this.values = unmodifiableList(requireNonNull(values, "values"));
  }

  @Override
  public ConditionType conditionType() {
    return conditionType;
  }

  @Override
  public List<Attribute<?>> attributes() {
    return attributes;
  }

  @Override
  public List<?> values() {
    return values;
  }

  @Override
  public String toString(EntityDefinition definition) {
    return requireNonNull(definition).conditionProvider(conditionType).conditionString(attributes, values);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof DefaultCustomCondition)) {
      return false;
    }
    if (!super.equals(object)) {
      return false;
    }
    DefaultCustomCondition that = (DefaultCustomCondition) object;
    return conditionType.equals(that.conditionType) &&
            attributes.equals(that.attributes) &&
            values.equals(that.values);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), conditionType, attributes, values);
  }
}
