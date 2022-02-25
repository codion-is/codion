/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.Conjunction;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

final class DefaultConditionCombination implements Condition.Combination, Serializable {

  private static final long serialVersionUID = 1;

  private static final String CONDITIONS = "conditions";

  private final List<Condition> conditions;
  private final Conjunction conjunction;
  private final EntityType entityType;

  DefaultConditionCombination(Conjunction conjunction, Condition... conditions) {
    this(conjunction, Arrays.asList(requireNonNull(conditions, CONDITIONS)));
  }

  DefaultConditionCombination(Conjunction conjunction, Condition condition, Condition... conditions) {
    this(conjunction, combine(condition, conditions));
  }

  DefaultConditionCombination(Conjunction conjunction, Collection<Condition> conditions) {
    this(conjunction, new ArrayList<>(requireNonNull(conditions, CONDITIONS)));
  }

  DefaultConditionCombination(Conjunction conjunction, List<Condition> conditions) {
    this.conjunction = requireNonNull(conjunction, "conjunction");
    this.conditions = new ArrayList<>(requireNonNull(conditions, CONDITIONS));
    this.entityType = this.conditions.isEmpty() ? null : this.conditions.get(0).getEntityType();
    for (int i = 1; i < this.conditions.size(); i++) {
      EntityType conditionEntityType = this.conditions.get(i).getEntityType();
      if (!conditionEntityType.equals(this.entityType)) {
        throw new IllegalArgumentException("EntityType " + this.entityType + " expected, got: " + conditionEntityType);
      }
    }
  }

  @Override
  public EntityType getEntityType() {
    if (entityType == null) {
      throw new IllegalStateException("No condition added to combination");
    }

    return entityType;
  }

  @Override
  public Collection<Condition> getConditions() {
    return unmodifiableList(conditions);
  }

  @Override
  public Conjunction getConjunction() {
    return conjunction;
  }

  @Override
  public List<?> getValues() {
    List<Object> values = new ArrayList<>();
    for (int i = 0; i < conditions.size(); i++) {
      values.addAll(conditions.get(i).getValues());
    }

    return unmodifiableList(values);
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    List<Attribute<?>> attributes = new ArrayList<>();
    for (int i = 0; i < conditions.size(); i++) {
      attributes.addAll(conditions.get(i).getAttributes());
    }

    return unmodifiableList(attributes);
  }

  @Override
  public Condition.Combination and(Condition... conditions) {
    if (this.conditions.isEmpty()) {
      return new DefaultConditionCombination(Conjunction.AND, combine(null, conditions));
    }

    return new DefaultConditionCombination(Conjunction.AND, combine(this, conditions));
  }

  @Override
  public Condition.Combination or(Condition... conditions) {
    if (this.conditions.isEmpty()) {
      return new DefaultConditionCombination(Conjunction.OR, combine(null, conditions));
    }

    return new DefaultConditionCombination(Conjunction.OR, combine(this, conditions));
  }

  @Override
  public String getConditionString(EntityDefinition definition) {
    if (conditions.isEmpty()) {
      return "";
    }
    if (conditions.size() == 1) {
      return conditions.get(0).getConditionString(definition);
    }

    return conditions.stream()
            .map(condition -> condition.getConditionString(definition))
            .filter(string -> !string.isEmpty())
            .collect(joining(toString(conjunction), "(", ")"));
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

  private static List<Condition> combine(Condition condition, Condition... conditions) {
    List<Condition> list = new ArrayList<>(requireNonNull(conditions, CONDITIONS).length + (condition != null ? 1 : 0));
    if (condition != null) {
      list.add(condition);
    }
    list.addAll(Arrays.asList(conditions));

    return list;
  }

  private static String toString(Conjunction conjunction) {
    switch (conjunction) {
      case AND: return " and ";
      case OR: return " or ";
      default: throw new IllegalArgumentException("Unknown conjunction: " + conjunction);
    }
  }
}
