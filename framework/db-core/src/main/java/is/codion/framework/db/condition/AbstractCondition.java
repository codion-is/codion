/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.Conjunction;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

abstract class AbstractCondition implements Condition {

  private static final long serialVersionUID = 1;

  @Override
  public final Condition.Combination and(final Condition... conditions) {
    return new DefaultConditionCombination(Conjunction.AND, singletonList(this)).add(requireNonNull(conditions));
  }

  @Override
  public final Condition.Combination or(final Condition... conditions) {
    return new DefaultConditionCombination(Conjunction.OR, singletonList(this)).add(requireNonNull(conditions));
  }
}
