/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.Conjunction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Objects.requireNonNull;

final class DefaultConditionCombination implements Condition.Combination {

  private static final long serialVersionUID = 1;

  private final ArrayList<Condition> conditions = new ArrayList<>();
  private final Conjunction conjunction;

  DefaultConditionCombination(final Conjunction conjunction, final Collection<Condition> conditions) {
    this.conjunction = requireNonNull(conjunction, "conjunction");
    for (final Condition condition : requireNonNull(conditions, "conditions")) {
      add(condition);
    }
  }

  @Override
  public void add(final Condition condition) {
    if (condition != null && !(condition instanceof EmptyCondition)) {
      conditions.add(condition);
    }
  }

  @Override
  public List<Condition> getConditions() {
    return conditions;
  }

  @Override
  public Conjunction getConjunction() {
    return conjunction;
  }

  @Override
  public List getValues() {
    final List values = new ArrayList<>();
    for (int i = 0; i < conditions.size(); i++) {
      values.addAll(conditions.get(i).getValues());
    }

    return values;
  }

  @Override
  public List<String> getPropertyIds() {
    final List<String> propertyIds = new ArrayList<>();
    for (int i = 0; i < conditions.size(); i++) {
      propertyIds.addAll(conditions.get(i).getPropertyIds());
    }

    return propertyIds;
  }
}
