/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.criteria;

import is.codion.framework.domain.entity.Column;
import is.codion.framework.domain.entity.EntityType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

/**
 * A base class for Criteria implementations.
 */
public abstract class AbstractCriteria implements Criteria, Serializable {

  private final EntityType entityType;
  private final List<Column<?>> attributes;
  private final List<?> values;

  protected AbstractCriteria(EntityType entityType, List<Column<?>> attributes, Collection<?> values) {
    this.entityType = requireNonNull(entityType);
    this.attributes = validateAttributes(unmodifiableList(new ArrayList<>(attributes)));
    this.values = unmodifiableList(new ArrayList<>(values));
  }

  @Override
  public final EntityType entityType() {
    return entityType;
  }

  @Override
  public final List<Column<?>> attributes() {
    return attributes;
  }

  @Override
  public final List<?> values() {
    return values;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof AbstractCriteria)) {
      return false;
    }
    AbstractCriteria that = (AbstractCriteria) object;
    return entityType.equals(that.entityType()) &&
            Objects.equals(attributes, that.attributes()) &&
            Objects.equals(values, that.values());
  }

  @Override
  public int hashCode() {
    return Objects.hash(entityType, attributes, values);
  }

  private List<Column<?>> validateAttributes(List<Column<?>> attributes) {
    for (Column<?> attribute : attributes) {
      if (!attribute.entityType().equals(entityType)) {
        throw new IllegalArgumentException("Criteria attribute entityType mismatch, " +
                entityType + " expected, got: " + attribute.entityType());
      }
    }

    return attributes;
  }
}
