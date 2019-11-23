/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.condition;

/**
 * A Condition based on a custom {@link org.jminor.framework.domain.Entity.ConditionProvider}
 * associated with {@link CustomCondition#getConditionId()}
 */
public interface CustomCondition extends Condition {

  /**
   * @return the condition id
   */
  String getConditionId();
}
