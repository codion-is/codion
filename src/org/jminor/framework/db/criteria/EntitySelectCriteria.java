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

  EntitySelectCriteria setForeignKeyFetchDepthLimit(final String foreignKeyPropertyID, final int fetchDepthLimit);

  EntitySelectCriteria setForeignKeyFetchDepthLimit(final int fetchDepthLimit);
}
