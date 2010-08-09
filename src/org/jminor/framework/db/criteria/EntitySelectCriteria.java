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

  int getCurrentFetchDepth();

  int getFetchDepth();

  int getFetchDepth(final String foreignKeyPropertyID);

  EntitySelectCriteria setCurrentFetchDepth(final int currentFetchDepth);

  EntitySelectCriteria setFetchDepth(final int maxFetchDepth);

  EntitySelectCriteria setFetchDepth(final String foreignKeyPropertyID, final int maxFetchDepth);

  EntitySelectCriteria setFetchDepthForAll(final int fetchDepth);

  EntitySelectCriteria setSelectForUpdate(final boolean selectForUpdate);
}