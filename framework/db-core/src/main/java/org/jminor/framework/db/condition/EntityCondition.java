/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.condition;

/**
 * A class encapsulating query condition parameters for querying a set of entities.
 */
public interface EntityCondition extends Condition {

  /**
   * @return the entity ID
   */
  String getEntityId();

  /**
   * @return the underlying {@link Condition} object, can be null
   */
  Condition getCondition();
}
