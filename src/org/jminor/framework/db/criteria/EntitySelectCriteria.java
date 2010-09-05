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
   * @return true if this select should lock to result for update
   */
  boolean isSelectForUpdate();

  /**
     * @return the maximum number of records to fetch from the result
     */
  int getFetchCount();

  int getForeignKeyFetchDepthLimit(final String foreignKeyPropertyID);

  EntitySelectCriteria setForeignKeyFetchDepthLimit(final String foreignKeyPropertyID, final int maxFetchDepth);

  EntitySelectCriteria setForeignKeyFetchDepthLimit(final int fetchDepth);

  EntitySelectCriteria setSelectForUpdate(final boolean selectForUpdate);
}
