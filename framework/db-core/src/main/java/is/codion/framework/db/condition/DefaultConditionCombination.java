/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.Conjunction;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

final class DefaultConditionCombination implements Condition.Combination, Serializable {

  private static final long serialVersionUID = 1;

  private final ArrayList<Condition> conditions = new ArrayList<>();
  private final Conjunction conjunction;

  private EntityType entityType;

  DefaultConditionCombination(final Conjunction conjunction) {
    this.conjunction = requireNonNull(conjunction, "conjunction");
  }

  DefaultConditionCombination(final Conjunction conjunction, final Condition condition) {
    this(conjunction);
    add(condition);
  }

  @Override
  public EntityType getEntityType() {
    if (entityType == null) {
      throw new IllegalStateException("No condition added to combination");
    }

    return entityType;
  }

  @Override
  public Combination add(final Condition... conditions) {
    requireNonNull(conditions);
    for (final Condition condition : conditions) {
      add(condition);
    }

    return this;
  }

  @Override
  public Combination add(final Collection<Condition> conditions) {
    requireNonNull(conditions);
    for (final Condition condition : conditions) {
      add(condition);
    }

    return this;
  }

  @Override
  public Combination add(final Condition condition) {
    requireNonNull(condition);
    if (entityType == null) {
      entityType = condition.getEntityType();
    }
    else if (!entityType.equals(condition.getEntityType())) {
      throw new IllegalArgumentException("EntityType " + entityType + " expected, got: " + condition.getEntityType());
    }
    conditions.add(condition);

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
  public List<?> getValues() {
    final List<Object> values = new ArrayList<>();
    for (int i = 0; i < conditions.size(); i++) {
      values.addAll(conditions.get(i).getValues());
    }

    return unmodifiableList(values);
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    final List<Attribute<?>> attributes = new ArrayList<>();
    for (int i = 0; i < conditions.size(); i++) {
      attributes.addAll(conditions.get(i).getAttributes());
    }

    return unmodifiableList(attributes);
  }

  @Override
  public Condition.Combination and(final Condition... conditions) {
    return new DefaultConditionCombination(Conjunction.AND, this).add(conditions);
  }

  @Override
  public Condition.Combination or(final Condition... conditions) {
    return new DefaultConditionCombination(Conjunction.OR, this).add(conditions);
  }

  @Override
  public String getWhereClause(final EntityDefinition definition) {
    if (conditions.isEmpty()) {
      return "";
    }
    if (conditions.size() == 1) {
      return conditions.get(0).getWhereClause(definition);
    }

    return new StringBuilder("(").append(conditions.stream()
            .map(condition -> condition.getWhereClause(definition))
            .filter(string -> !string.isEmpty())
            .collect(joining(toString(conjunction)))).append(")").toString();
  }

  @Override
  public SelectCondition toSelectCondition() {
    return new DefaultSelectCondition(this);
  }

  @Override
  public UpdateCondition toUpdateCondition() {
    return new DefaultUpdateCondition(this);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + ": " + getEntityType();
  }

  private static String toString(final Conjunction conjunction) {
    switch (conjunction) {
      case AND: return " and ";
      case OR: return " or ";
      default: throw new IllegalArgumentException("Unknown conjunction: " + conjunction);
    }
  }
}
