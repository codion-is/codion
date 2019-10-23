/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.condition;

import org.jminor.framework.domain.Domain;

import java.io.Serializable;

/**
 * A class encapsulating query condition parameters for querying a set of entities.
 */
public interface EntityCondition extends Serializable {

  /**
   * @return the entity ID
   */
  String getEntityId();

  /**
   * @param domain the underlying domain model
   * @return the underlying {@link Condition} object, may not be null
   */
  Condition getCondition(final Domain domain);

  /**
   * Returns a condition clause based on this Condition without the WHERE keyword,
   * note that this clause contains the ? substitution character instead of actual values.
   * Note that this method can return an empty string.
   * @param domain the underlying domain model
   * @return a where clause based on this EntityCondition or an empty string if it does not represent a condition
   */
  String getWhereClause(final Domain domain);
}
