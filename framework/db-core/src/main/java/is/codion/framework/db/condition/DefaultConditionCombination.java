/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.Conjunction;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

class DefaultConditionCombination extends AbstractCondition implements Condition.Combination {

  private static final long serialVersionUID = 1;

  private final ArrayList<Condition> conditions = new ArrayList<>();
  private final Conjunction conjunction;

  private EntityType<?> entityType;

  DefaultConditionCombination(final Conjunction conjunction) {
    this.conjunction = requireNonNull(conjunction, "conjunction");
  }

  DefaultConditionCombination(final Conjunction conjunction, final Condition condition) {
    this(conjunction);
    add(condition);
  }

  DefaultConditionCombination(final Conjunction conjunction, final Condition... conditions) {
    this(conjunction);
    for (final Condition condition : requireNonNull(conditions, "conditions")) {
      add(condition);
    }
  }

  @Override
  public final EntityType<?> getEntityType() {
    if (entityType == null) {
      throw new IllegalStateException("No condition added to combination");
    }

    return entityType;
  }

  @Override
  public final Combination add(final Condition... conditions) {
    requireNonNull(conditions);
    for (final Condition condition : conditions){
      add(condition);
    }

    return this;
  }

  @Override
  public final Combination add(final Condition condition) {
    requireNonNull(condition);
    if (entityType == null) {
      entityType = condition.getEntityType();
    }
    else if (!entityType.equals(condition.getEntityType())) {
      throw new IllegalArgumentException("EntityType " + entityType + " expected, got: " + condition.getEntityType());
    }
    if (!(condition instanceof EmptyCondition)) {
      conditions.add(condition);
    }

    return this;
  }

  @Override
  public final List<Condition> getConditions() {
    return conditions;
  }

  @Override
  public final Conjunction getConjunction() {
    return conjunction;
  }

  @Override
  public final List<Object> getValues() {
    final List<Object> values = new ArrayList<>();
    for (int i = 0; i < conditions.size(); i++) {
      values.addAll(conditions.get(i).getValues());
    }

    return values;
  }

  @Override
  public final List<Attribute<?>> getAttributes() {
    final List<Attribute<?>> attributes = new ArrayList<>();
    for (int i = 0; i < conditions.size(); i++) {
      attributes.addAll(conditions.get(i).getAttributes());
    }

    return attributes;
  }
}
