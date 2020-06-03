/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.framework.domain.entity.Attribute;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

final class DefaultCustomCondition implements CustomCondition {

  private static final long serialVersionUID = 1;

  private final String conditionId;
  private final ArrayList<Attribute<?>> attributes;
  private final ArrayList values;

  DefaultCustomCondition(final String conditionId, final List<Attribute<?>> attributes, final List values) {
    this.conditionId = requireNonNull(conditionId, "conditionId");
    this.attributes = new ArrayList<>(requireNonNull(attributes, "attributes"));
    this.values = new ArrayList(requireNonNull(values, "values"));
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
  public List getValues() {
    return values;
  }
}
