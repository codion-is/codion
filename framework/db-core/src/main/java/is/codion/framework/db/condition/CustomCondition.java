/*
 * Copyright (c) 2019 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.framework.domain.entity.ConditionType;

/**
 * A Condition based on a custom {@link is.codion.framework.domain.entity.ConditionProvider}
 * associated with {@link ConditionType}
 */
public interface CustomCondition extends Condition {

  /**
   * @return the condition type
   */
  ConditionType conditionType();
}
