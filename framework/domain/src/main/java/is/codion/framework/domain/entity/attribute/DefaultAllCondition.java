/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.attribute;

import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;

import java.io.Serializable;
import java.util.Objects;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

final class DefaultAllCondition extends AbstractCondition implements Condition.All, Serializable {

  private static final long serialVersionUID = 1;

  DefaultAllCondition(EntityType entityType) {
    super(entityType, emptyList(), emptyList());
  }

  @Override
  public String toString(EntityDefinition definition) {
    requireNonNull(definition);
    return "";
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof All)) {
      return false;
    }
    All that = (All) object;
    return Objects.equals(entityType(), that.entityType());
  }

  @Override
  public int hashCode() {
    return entityType().hashCode();
  }
}
