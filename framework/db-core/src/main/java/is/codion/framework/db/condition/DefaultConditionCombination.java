/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.Conjunction;
import is.codion.framework.db.condition.Condition.Combination;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

final class DefaultConditionCombination extends AbstractCondition implements Combination, Serializable {

  private static final long serialVersionUID = 1;

  private final List<Condition> conditions;
  private final Conjunction conjunction;

  DefaultConditionCombination(Conjunction conjunction, List<Condition> conditions) {
    super(entityType(conditions));
    this.conjunction = requireNonNull(conjunction);
    this.conditions = new ArrayList<>(conditions);
    for (int i = 1; i < this.conditions.size(); i++) {
      EntityType conditionEntityType = this.conditions.get(i).entityType();
      if (!conditionEntityType.equals(entityType())) {
        throw new IllegalArgumentException("EntityType " + entityType() + " expected, got: " + conditionEntityType);
      }
    }
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
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof DefaultConditionCombination)) {
      return false;
    }
    if (!super.equals(object)) {
      return false;
    }
    DefaultConditionCombination that = (DefaultConditionCombination) object;
    return conjunction == that.conjunction &&
            conditions.equals(that.conditions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), conditions, conjunction);
  }

  private static String toString(Conjunction conjunction) {
    switch (conjunction) {
      case AND:
        return " and ";
      case OR:
        return " or ";
      default:
        throw new IllegalArgumentException("Unknown conjunction: " + conjunction);
    }
  }

  private static EntityType entityType(List<Condition> conditions) {
    if (requireNonNull(conditions).isEmpty()) {
      throw new IllegalArgumentException("One or more conditions must be specified for a condition combination");
    }

    return conditions.get(0).entityType();
  }
}
