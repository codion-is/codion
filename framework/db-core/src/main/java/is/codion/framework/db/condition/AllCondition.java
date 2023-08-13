/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;

import java.io.Serializable;
import java.util.Objects;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

public final class AllCondition extends AbstractCondition implements Serializable {

  private static final long serialVersionUID = 1;

  AllCondition(EntityType entityType) {
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
    if (!(object instanceof AllCondition)) {
      return false;
    }
    AllCondition that = (AllCondition) object;
    return Objects.equals(entityType(), that.entityType());
  }

  @Override
  public int hashCode() {
    return Objects.hash(entityType());
  }
}
