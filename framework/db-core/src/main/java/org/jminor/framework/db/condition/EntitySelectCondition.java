/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.condition;

import org.jminor.framework.domain.Entity;

/**
 * A class encapsulating select query parameters.
 */
public interface EntitySelectCondition extends EntityCondition {

  /**
   * @return the order by clause specified by this condition
   */
  String getOrderByClause();

  /**
   * Sets the OrderBy for this condition
   * @param orderBy the OrderBy to use when applying this condition
   * @return this EntitySelectCondition instance
   */
  EntitySelectCondition setOrderBy(final Entity.OrderBy orderBy);

  /**
   * @return the limit to use for the given condition
   */
  int getLimit();

  /**
   * @param limit the limit to use for this condition
   * @return this EntitySelectCondition instance
   */
  EntitySelectCondition setLimit(final int limit);

  /**
   * @return the offset to use for the given condition
   */
  int getOffset();

  /**
   * @param offset the offset to use for this condition
   * @return this EntitySelectCondition instance
   */
  EntitySelectCondition setOffset(final int offset);

  /**
   * @return true if this select should lock the result for update
   */
  boolean isForUpdate();

  /**
   * Marks this condition as a select for update query, this means the resulting records
   * will be locked by the given connection until unlocked by running another (non - select for update)
   * query on the same connection or performing an update
   * @param forUpdate if true then the results should be locked for update
   * @return this EntitySelectCondition instance
   */
  EntitySelectCondition setForUpdate(final boolean forUpdate);

  /**
   * @return the maximum number of records to fetch from the result
   */
  int getFetchCount();

  /**
   * Returns the number of levels of foreign key values to fetch, with 0 meaning no referenced entities
   * should be fetched.
   * @param foreignKeyPropertyId the foreign key property ID
   * @return the number of levels of foreign key values to fetch
   */
  int getForeignKeyFetchDepthLimit(final String foreignKeyPropertyId);

  /**
   * Limit the levels of foreign keys to fetch via the given foreign key property, default 1
   * @param foreignKeyPropertyId the property id
   * @param fetchDepthLimit the foreign key fetch depth limit
   * @return this EntitySelectCondition instance
   */
  EntitySelectCondition setForeignKeyFetchDepthLimit(final String foreignKeyPropertyId, final int fetchDepthLimit);

  /**
   * Limit the levels of foreign keys to fetch, default 1
   * @param fetchDepthLimit the foreign key fetch depth limit
   * @return this EntitySelectCondition instance
   */
  EntitySelectCondition setForeignKeyFetchDepthLimit(final int fetchDepthLimit);
}
