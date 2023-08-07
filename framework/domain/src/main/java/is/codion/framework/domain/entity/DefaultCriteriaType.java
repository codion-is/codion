/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import java.io.Serializable;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

final class DefaultCriteriaType implements CriteriaType, Serializable {

  private static final long serialVersionUID = 1;

  private final EntityType entityType;
  private final String name;

  DefaultCriteriaType(EntityType entityType, String name) {
    this.entityType = requireNonNull(entityType);
    this.name = requireNonNull(name);
  }

  @Override
  public EntityType entityType() {
    return entityType;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null || getClass() != object.getClass()) {
      return false;
    }
    DefaultCriteriaType that = (DefaultCriteriaType) object;
    return entityType.equals(that.entityType) && name.equals(that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(entityType, name);
  }
}
