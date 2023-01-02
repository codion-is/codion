/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

/**
 * A condition with no values or attributes, as in, no condition
 */
final class DefaultCondition extends AbstractCondition {

  private static final long serialVersionUID = 1;

  DefaultCondition(EntityType entityType) {
    super(entityType);
  }

  @Override
  public List<?> values() {
    return emptyList();
  }

  @Override
  public List<Attribute<?>> attributes() {
    return emptyList();
  }

  @Override
  public String toString(EntityDefinition definition) {
    requireNonNull(definition);
    return "";
  }
}
