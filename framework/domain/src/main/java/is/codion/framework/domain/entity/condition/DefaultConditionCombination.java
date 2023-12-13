/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity.condition;

import is.codion.common.Conjunction;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.condition.Condition.Combination;

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

  DefaultConditionCombination(Conjunction conjunction, Collection<Condition> conditions) {
    super(entityType(conditions), columns(conditions), values(conditions));
    this.conjunction = requireNonNull(conjunction);
    this.conditions = unmodifiableList(new ArrayList<>(conditions));
  }

  @Override
  public Collection<Condition> conditions() {
    return conditions;
  }

  @Override
  public Conjunction conjunction() {
    return conjunction;
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

  @Override
  public String toString() {
    return "DefaultConditionCombination{" +
            "conditions=" + conditions +
            ", conjunction=" + conjunction + "}";
  }

  private static String toString(Conjunction conjunction) {
    switch (conjunction) {
      case AND:
        return " AND ";
      case OR:
        return " OR ";
      default:
        throw new IllegalArgumentException("Unknown conjunction: " + conjunction);
    }
  }

  private static EntityType entityType(Collection<Condition> conditions) {
    if (requireNonNull(conditions).isEmpty()) {
      throw new IllegalArgumentException("One or more conditions must be specified for a condition combination");
    }

    return conditions.iterator().next().entityType();
  }

  private static List<?> values(Collection<Condition> conditions) {
    return conditions.stream()
            .flatMap(condition -> condition.values().stream())
            .collect(toList());
  }

  private static List<Column<?>> columns(Collection<Condition> conditions) {
    return conditions.stream()
            .flatMap(condition -> condition.columns().stream())
            .collect(toList());
  }
}
