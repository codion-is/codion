/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.Conjunction;
import is.codion.framework.db.condition.Criteria.Combination;
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

final class DefaultCriteriaCombination extends AbstractCriteria implements Combination, Serializable {

  private static final long serialVersionUID = 1;

  private final List<Criteria> criteria;
  private final Conjunction conjunction;

  DefaultCriteriaCombination(Conjunction conjunction, List<Criteria> criteria) {
    super(entityType(criteria), unmodifiableList(criteria.stream()
            .flatMap(condition -> condition.attributes().stream())
            .collect(toList())), unmodifiableList(criteria.stream()
            .flatMap(condition -> condition.values().stream())
            .collect(toList())));
    this.conjunction = requireNonNull(conjunction);
    this.criteria = unmodifiableList(new ArrayList<>(criteria));
    for (int i = 1; i < this.criteria.size(); i++) {
      EntityType conditionEntityType = this.criteria.get(i).entityType();
      if (!conditionEntityType.equals(entityType())) {
        throw new IllegalArgumentException("EntityType " + entityType() + " expected, got: " + conditionEntityType);
      }
    }
  }

  @Override
  public Collection<Criteria> criteria() {
    return criteria;
  }

  @Override
  public Conjunction conjunction() {
    return conjunction;
  }

  @Override
  public String toString(EntityDefinition definition) {
    requireNonNull(definition);
    if (criteria.isEmpty()) {
      return "";
    }
    if (criteria.size() == 1) {
      return criteria.get(0).toString(definition);
    }

    return criteria.stream()
            .map(condition -> condition.toString(definition))
            .filter(string -> !string.isEmpty())
            .collect(joining(toString(conjunction), "(", ")"));
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof DefaultCriteriaCombination)) {
      return false;
    }
    if (!super.equals(object)) {
      return false;
    }
    DefaultCriteriaCombination that = (DefaultCriteriaCombination) object;
    return conjunction == that.conjunction &&
            criteria.equals(that.criteria);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), criteria, conjunction);
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

  private static EntityType entityType(List<Criteria> criteria) {
    if (requireNonNull(criteria).isEmpty()) {
      throw new IllegalArgumentException("One or more criteria must be specified for a criteria combination");
    }

    return criteria.get(0).entityType();
  }
}
