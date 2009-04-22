/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.criteria;

import org.jminor.common.db.ICriteria;

import java.io.Serializable;

/**
 * A class encapsulating query criteria parameters
 */
public class EntityCriteria implements Serializable {

  private final String entityID;
  private final ICriteria criteria;
  private final int fetchCount;
  private final String orderByClause;

  /**
   * Instantiates a new empty EntityCriteria.
   * Using an empty criteria means all underlying records should be selected
   * @param entityID the ID of the entity to select
   */
  public EntityCriteria(final String entityID) {
    this(entityID, null);
  }

  /**
   * Instantiates a new EntityCriteria
   * @param entityID the ID of the entity to select
   * @param criteria the ICriteria object
   * @see org.jminor.common.db.CriteriaSet
   * @see org.jminor.framework.db.criteria.PropertyCriteria
   * @see org.jminor.framework.db.criteria.EntityKeyCriteria
   */
  public EntityCriteria(final String entityID, final ICriteria criteria) {
    this(entityID, criteria, null);
  }

  /**
   * Instantiates a new EntityCriteria
   * @param entityID the ID of the entity to select
   * @param criteria the ICriteria object
   * @param orderByClause the 'order by' clause to use, i.e. "last_name, first_name desc"
   * @see org.jminor.common.db.CriteriaSet
   * @see org.jminor.framework.db.criteria.PropertyCriteria
   * @see org.jminor.framework.db.criteria.EntityKeyCriteria
   */
  public EntityCriteria(final String entityID, final ICriteria criteria, final String orderByClause) {
    this(entityID, criteria, orderByClause, -1);
  }

  /**
   * Instantiates a new EntityCriteria
   * @param entityID the ID of the entity to select
   * @param criteria the ICriteria object
   * @param fetchCount the maximum number of records to fetch from the result
   * @see org.jminor.common.db.CriteriaSet
   * @see org.jminor.framework.db.criteria.PropertyCriteria
   * @see org.jminor.framework.db.criteria.EntityKeyCriteria
   */
  public EntityCriteria(final String entityID, final ICriteria criteria, final int fetchCount) {
    this(entityID, criteria, null, fetchCount);
  }

  /**
   * Instantiates a new EntityCriteria
   * @param entityID the ID of the entity to select
   * @param criteria the ICriteria object
   * @param orderByClause the 'order by' clause to use, i.e. "last_name, first_name desc"
   * @param fetchCount the maximum number of records to fetch from the result
   * @see org.jminor.common.db.CriteriaSet
   * @see org.jminor.framework.db.criteria.PropertyCriteria
   * @see org.jminor.framework.db.criteria.EntityKeyCriteria
   */
  public EntityCriteria(final String entityID, final ICriteria criteria, final String orderByClause,
                        final int fetchCount) {
    this.entityID = entityID;
    this.criteria = criteria;
    this.fetchCount = fetchCount;
    this.orderByClause = orderByClause;
  }

  /**
   * @return the entity ID
   */
  public String getEntityID() {
    return entityID;
  }

  /**
   * @return the ICriteria object
   */
  public ICriteria getCriteria() {
    return criteria;
  }

  /**
   * @return the maximum number of records to fetch from the result
   */
  public int getFetchCount() {
    return fetchCount;
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
   * @return the where clause
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
