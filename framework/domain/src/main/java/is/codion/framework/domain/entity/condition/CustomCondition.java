/*
 * Copyright (c) 2019 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.condition;

/**
 * A Condition based on a custom {@link ConditionProvider}
 * associated with {@link ConditionType}
 */
public interface CustomCondition extends Condition {

  /**
   * @return the condition type
   */
  ConditionType conditionType();
}
