/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

/**
 * A Condition based on a custom {@link is.codion.framework.domain.entity.ConditionProvider}
 * associated with {@link CustomCondition#getConditionId()}
 */
public interface CustomCondition extends Condition {

  /**
   * @return the condition id
   */
  String getConditionId();
}
