/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

abstract class AbstractCriteria implements Criteria, Serializable {

  private final EntityType entityType;
  private final List<Attribute<?>> attributes;
  private final List<?> values;

  protected AbstractCriteria(EntityType entityType, List<Attribute<?>> attributes, List<?> values) {
    this.entityType = requireNonNull(entityType);
    this.attributes = unmodifiableList(new ArrayList<>(attributes));
    this.values = unmodifiableList(new ArrayList<>(values));
  }

  @Override
  public final EntityType entityType() {
    return entityType;
  }

  @Override
  public final List<Attribute<?>> attributes() {
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
}
