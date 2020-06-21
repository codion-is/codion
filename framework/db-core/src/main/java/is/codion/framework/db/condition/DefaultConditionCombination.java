/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.Conjunction;
import is.codion.framework.domain.entity.Attribute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Objects.requireNonNull;

final class DefaultConditionCombination extends AbstractCondition implements Condition.Combination {

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
  public Combination add(final Condition... conditions) {
    requireNonNull(conditions);
    for (final Condition condition : conditions){
      add(condition);
    }

    return this;
  }

  @Override
  public Combination add(final Condition condition) {
    if (condition != null && !(condition instanceof EmptyCondition)) {
      conditions.add(condition);
    }

    return this;
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
  public List<Object> getValues() {
    final List<Object> values = new ArrayList<>();
    for (int i = 0; i < conditions.size(); i++) {
      values.addAll(conditions.get(i).getValues());
    }

    return values;
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    final List<Attribute<?>> attributes = new ArrayList<>();
    for (int i = 0; i < conditions.size(); i++) {
      attributes.addAll(conditions.get(i).getAttributes());
    }

    return attributes;
  }
}
