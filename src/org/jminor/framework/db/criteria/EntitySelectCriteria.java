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
     * @return the maximum number of records to fetch from the result
     */
  int getFetchCount();

  EntitySelectCriteria setSelectForUpdate(final boolean selectForUpdate);

  boolean isSelectForUpdate();

  EntitySelectCriteria setFetchDepthForAll(final int fetchDepth);

  int getCurrentFetchDepth();

  EntitySelectCriteria setCurrentFetchDepth(final int currentFetchDepth);

  int getFetchDepth();

  int getFetchDepth(final String foreignKeyPropertyID);

  EntitySelectCriteria setFetchDepth(final int maxFetchDepth);

  EntitySelectCriteria setFetchDepth(final String foreignKeyPropertyID, final int maxFetchDepth);
}