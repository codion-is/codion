/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.framework.domain.entity.EntityIdentity;

import java.io.Serializable;

/**
 * A class encapsulating query condition parameters for querying a set of entities.
 */
public interface EntityCondition extends Serializable {

  /**
   * @return the entityId
   */
  EntityIdentity getEntityId();

  /**
   * @return the underlying {@link Condition} object, may not be null
   */
  Condition getCondition();
}
