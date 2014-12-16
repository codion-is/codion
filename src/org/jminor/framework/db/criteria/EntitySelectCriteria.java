/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.criteria;

/**
 * A class encapsulating select query parameters.
 */
public interface EntitySelectCriteria extends EntityCriteria {

  /**
   * @return the order by clause specified by this criteria
   */
  String getOrderByClause();

  /**
   * @param orderByClause the order by clause
   * @return this EntitySelectCriteria instance
   */
  EntitySelectCriteria setOrderByClause(final String orderByClause);

  /**
   * @return the limit to use for the given criteria
   */
  int getLimit();

  /**
   * @param limit the limit to use for this criteria
   * @return this EntityCriteriaInstance
   */
  EntitySelectCriteria setLimit(final int limit);

  /**
   * @return the offset to use for the given criteria
   */
  int getOffset();

  /**
   * @param offset the offset to use for this criteria
   * @return this EntityCriteriaInstance
   */
  EntitySelectCriteria setOffset(final int offset);

  /**
   * @return true if this select should lock the result for update
   */
  boolean isForUpdate();

  /**
   * Marks this criteria as a select for update query, this means the resulting records
   * will be locked by the given connection until unlocked by running another (non - select for update)
   * query on the same connection or performing an update
   * @param forUpdate if true then the results should be locked for update
   * @return this EntitySelectCriteria instance
   */
  EntitySelectCriteria setForUpdate(final boolean forUpdate);

  /**
   * @return the maximum number of records to fetch from the result
   */
  int getFetchCount();

  /**
   * Returns the number of levels of foreign key values to fetch, with 0 meaning no referenced entities
   * should be fetched.
   * @param foreignKeyPropertyID the foreign key property ID
   * @return the number of levels of foreign key values to fetch
   */
  int getForeignKeyFetchDepthLimit(final String foreignKeyPropertyID);

  /**
   * Limit the levels of foreign keys to fetch via the given foreign key property, default 1
   * @param fetchDepthLimit the foreign key fetch depth limit
   * @return this EntitySelectCriteria instance
   */
  EntitySelectCriteria setForeignKeyFetchDepthLimit(final String foreignKeyPropertyID, final int fetchDepthLimit);

  /**
   * Limit the levels of foreign keys to fetch, default 1
   * @param fetchDepthLimit the foreign key fetch depth limit
   * @return this EntitySelectCriteria instance
   */
  EntitySelectCriteria setForeignKeyFetchDepthLimit(final int fetchDepthLimit);
}
