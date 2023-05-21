/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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
import java.util.Objects;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

final class DefaultConditionCombination implements Condition.Combination, Serializable {

  private static final long serialVersionUID = 1;

  private static final String CONDITIONS = "conditions";

  private final List<Condition> conditions;
  private final Conjunction conjunction;
  private final EntityType entityType;

  DefaultConditionCombination(Conjunction conjunction, Condition condition, Condition... conditions) {
    this(conjunction, combine(condition, conditions));
  }

  DefaultConditionCombination(Conjunction conjunction, Collection<Condition> conditions) {
    this(conjunction, new ArrayList<>(requireNonNull(conditions, CONDITIONS)));
  }

  DefaultConditionCombination(Conjunction conjunction, List<Condition> conditions) {
    this.conjunction = requireNonNull(conjunction, "conjunction");
    if (requireNonNull(conditions, CONDITIONS).isEmpty()) {
      throw new IllegalArgumentException("One or more conditions must be specified for a condition combination");
    }
    this.conditions = new ArrayList<>(conditions);
    this.entityType = conditions.get(0).entityType();
    for (int i = 1; i < this.conditions.size(); i++) {
      EntityType conditionEntityType = this.conditions.get(i).entityType();
      if (!conditionEntityType.equals(this.entityType)) {
        throw new IllegalArgumentException("EntityType " + this.entityType + " expected, got: " + conditionEntityType);
      }
    }
  }

  @Override
  public EntityType entityType() {
    return entityType;
  }

  @Override
  public Collection<Condition> conditions() {
    return unmodifiableList(conditions);
  }

  @Override
  public Conjunction conjunction() {
    return conjunction;
  }

  @Override
  public List<?> values() {
    return unmodifiableList(conditions.stream()
            .flatMap(condition -> condition.values().stream())
            .collect(toList()));
  }

  @Override
  public List<Attribute<?>> attributes() {
    return unmodifiableList(conditions.stream()
            .flatMap(condition -> condition.attributes().stream())
            .collect(toList()));
  }

  @Override
  public Condition.Combination and(Condition... conditions) {
    return new DefaultConditionCombination(Conjunction.AND, combine(this, conditions));
  }

  @Override
  public Condition.Combination or(Condition... conditions) {
    return new DefaultConditionCombination(Conjunction.OR, combine(this, conditions));
  }

  @Override
  public String toString(EntityDefinition definition) {
    requireNonNull(definition);
    if (conditions.isEmpty()) {
      return "";
    }
    if (conditions.size() == 1) {
      return conditions.get(0).toString(definition);
    }

    return conditions.stream()
            .map(condition -> condition.toString(definition))
            .filter(string -> !string.isEmpty())
            .collect(joining(toString(conjunction), "(", ")"));
  }

  @Override
  public SelectCondition.Builder selectBuilder() {
    return new DefaultSelectCondition.DefaultBuilder(this);
  }

  @Override
  public UpdateCondition.Builder updateBuilder() {
    return new DefaultUpdateCondition.DefaultBuilder(this);
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + ": " + entityType();
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof DefaultConditionCombination)) {
      return false;
    }
    DefaultConditionCombination that = (DefaultConditionCombination) object;
    return conjunction == that.conjunction &&
            entityType.equals(that.entityType) &&
            conditions.equals(that.conditions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(conditions, conjunction, entityType);
  }

  private static List<Condition> combine(Condition condition, Condition... conditions) {
    requireNonNull(condition);
    List<Condition> list = new ArrayList<>(requireNonNull(conditions, CONDITIONS).length + 1);
    list.add(condition);
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
