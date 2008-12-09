/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.db.ICriteria;

import java.io.Serializable;

/**
 * A class encapsulating query criteria parameters
 */
public class EntityCriteria implements Serializable {

  private final String entityID;
  private final ICriteria criteria;
  private final int recordCount;
  private final String orderByClause;

  public EntityCriteria(final String entityID) {
    this(entityID, null);
  }

  public EntityCriteria(final String entityID, final ICriteria criteria) {
    this(entityID, criteria, null);
  }

  public EntityCriteria(final String entityID, final ICriteria criteria, final String orderByClause) {
    this(entityID, criteria, orderByClause, -1);
  }

  public EntityCriteria(final String entityID, final ICriteria criteria, final int recordCount) {
    this(entityID, criteria, null, recordCount);
  }

  public EntityCriteria(final String entityID, final ICriteria criteria, final String orderByClause,
                        final int recordCount) {
    this.entityID = entityID;
    this.criteria = criteria;
    this.recordCount = recordCount;
    this.orderByClause = orderByClause;
  }

  /**
   * @return Value for property 'entityID'.
   */
  public String getEntityID() {
    return entityID;
  }

  public ICriteria getCriteria() {
    return criteria;
  }

  /**
   * @return the number of records to be returned
   */
  public int getRecordCount() {
    return recordCount;
  }

  /**
   * @return true if the criteria is an instance of EntityKeyCriteria and the entityID
   * matches the entityID of said primary keys
   */
  public boolean isKeyCriteria() {
    return criteria instanceof EntityKeyCriteria && ((EntityKeyCriteria) criteria).getEntityID().equals(entityID);
  }

  /** {@inheritDoc} */
  public String toString() {
    return getEntityID() + " " + getWhereClause();
  }

  /**
   * @return Value for property 'whereClause'.
   */
  public String getWhereClause() {
    return getWhereClause(true);
  }

  /**
   * @param includeWhereKeyword if false AND is used instaed of the WHERE keyword
   * @return a where clause base on this criteria
   */
  public String getWhereClause(final boolean includeWhereKeyword) {
    final String criteriaString = criteria == null ? "" : criteria.toString();
    if (criteriaString.length() > 0)
      return (includeWhereKeyword ? "where " : "and ") + criteriaString;
    else
      return "";
  }

  /**
   * @return the order by clause specified by this criteria
   */
  public String getOrderByClause() {
    return orderByClause;
  }
}
