/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.criteria;

import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;

import java.io.Serializable;
import java.util.Objects;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

public final class AllCriteria extends AbstractCriteria implements Serializable {

  private static final long serialVersionUID = 1;

  AllCriteria(EntityType entityType) {
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
    if (!(object instanceof AllCriteria)) {
      return false;
    }
    AllCriteria that = (AllCriteria) object;
    return Objects.equals(entityType(), that.entityType());
  }

  @Override
  public int hashCode() {
    return Objects.hash(entityType());
  }
}
