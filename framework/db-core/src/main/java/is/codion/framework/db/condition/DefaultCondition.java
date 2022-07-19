/*
 * Copyright (c) 2021 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;

import java.util.List;

import static java.util.Collections.emptyList;

/**
 * A condition with no values or attributes, as in, no condition
 */
final class DefaultCondition extends AbstractCondition {

  private static final long serialVersionUID = 1;

  DefaultCondition(EntityType entityType) {
    super(entityType);
  }

  @Override
  public List<?> getValues() {
    return emptyList();
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return emptyList();
  }

  @Override
  public String getConditionString(EntityDefinition definition) {
    return "";
  }
}
