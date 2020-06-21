/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

final class DefaultCustomCondition extends AbstractCondition implements CustomCondition {

  private static final long serialVersionUID = 1;

  private final String conditionId;
  private final List<Attribute<?>> attributes;
  private final List<Object> values;
  private final EntityType<?> entityType;

  DefaultCustomCondition(final EntityType<?> entityType, final String conditionId, final List<Attribute<?>> attributes, final List<Object> values) {
    this.entityType = requireNonNull(entityType, "entityType");
    this.conditionId = requireNonNull(conditionId, "conditionId");
    this.attributes = new ArrayList<>(requireNonNull(attributes, "attributes"));
    this.values = new ArrayList<>(requireNonNull(values, "values"));
  }

  @Override
  public EntityType<?> getEntityType() {
    return entityType;
  }

  @Override
  public String getConditionId() {
    return conditionId;
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return attributes;
  }

  @Override
  public List<Object> getValues() {
    return values;
  }
}
