/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.condition;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

final class DefaultCustomCondition implements CustomCondition {

  private static final long serialVersionUID = 1;

  private final String conditionId;
  private final ArrayList<String> propertyIds;
  private final ArrayList values;

  DefaultCustomCondition(final String conditionId, final List<String> propertyIds, final List values) {
    this.conditionId = requireNonNull(conditionId, "conditionId");
    this.propertyIds = new ArrayList<>(requireNonNull(propertyIds, "propertyIds"));
    this.values = new ArrayList(requireNonNull(values, "values"));
  }

  @Override
  public String getConditionId() {
    return conditionId;
  }

  @Override
  public List<String> getPropertyIds() {
    return propertyIds;
  }

  @Override
  public List getValues() {
    return values;
  }
}
