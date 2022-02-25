/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.ConditionType;
import is.codion.framework.domain.entity.EntityDefinition;

import java.util.List;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

final class DefaultCustomCondition extends AbstractCondition implements CustomCondition {

  private static final long serialVersionUID = 1;

  private final ConditionType conditionType;
  private final List<Attribute<?>> attributes;
  private final List<Object> values;

  DefaultCustomCondition(ConditionType conditionType, List<Attribute<?>> attributes,
                         List<Object> values) {
    super(requireNonNull(conditionType, "conditionType").getEntityType());
    this.conditionType = conditionType;
    this.attributes = unmodifiableList(requireNonNull(attributes, "attributes"));
    this.values = unmodifiableList(requireNonNull(values, "values"));
  }

  @Override
  public ConditionType getConditionType() {
    return conditionType;
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return attributes;
  }

  @Override
  public List<?> getValues() {
    return values;
  }

  @Override
  public String getConditionString(EntityDefinition definition) {
    return definition.getConditionProvider(conditionType).getConditionString(attributes, values);
  }
}
