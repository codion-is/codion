/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import java.io.Serializable;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

final class DefaultConditionType implements ConditionType, Serializable {

  private static final long serialVersionUID = 1;

  private final EntityType entityType;
  private final String name;

  DefaultConditionType(EntityType entityType, String name) {
    this.entityType = requireNonNull(entityType);
    this.name = requireNonNull(name);
  }

  @Override
  public EntityType getEntityType() {
    return entityType;
  }

  @Override
  public String getName() {
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
    DefaultConditionType that = (DefaultConditionType) object;

    return entityType.equals(that.entityType) && name.equals(that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(entityType, name);
  }
}
