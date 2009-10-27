/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.criteria;

import org.jminor.common.db.Criteria;
import org.jminor.common.db.dbms.Dbms;
import org.jminor.common.model.SearchType;
import org.jminor.framework.domain.EntityRepository;

import java.io.Serializable;

/**
 * A class encapsulating query criteria parameters
 */
public class EntityCriteria implements Serializable {

  private static final long serialVersionUID = 1;

  private final String entityID;
  private final Criteria criteria;
  private final int fetchCount;
  private String orderByClause;

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
   * @param criteria the Criteria object
   * @see org.jminor.common.db.CriteriaSet
   * @see org.jminor.framework.db.criteria.PropertyCriteria
   * @see org.jminor.framework.db.criteria.EntityKeyCriteria
   */
  public EntityCriteria(final String entityID, final Criteria criteria) {
    this(entityID, criteria, null);
  }

  /**
   * Instantiates a new EntityCriteria
   * @param entityID the ID of the entity to select
   * @param criteria the Criteria object
   * @param orderByClause the 'order by' clause to use, i.e. "last_name, first_name desc"
   * @see org.jminor.common.db.CriteriaSet
   * @see org.jminor.framework.db.criteria.PropertyCriteria
   * @see org.jminor.framework.db.criteria.EntityKeyCriteria
   */
  public EntityCriteria(final String entityID, final Criteria criteria, final String orderByClause) {
    this(entityID, criteria, orderByClause, -1);
  }

  /**
   * Instantiates a new EntityCriteria
   * @param entityID the ID of the entity to select
   * @param criteria the Criteria object
   * @param fetchCount the maximum number of records to fetch from the result
   * @see org.jminor.common.db.CriteriaSet
   * @see org.jminor.framework.db.criteria.PropertyCriteria
   * @see org.jminor.framework.db.criteria.EntityKeyCriteria
   */
  public EntityCriteria(final String entityID, final Criteria criteria, final int fetchCount) {
    this(entityID, criteria, null, fetchCount);
  }

  /**
   * Instantiates a new EntityCriteria
   * @param entityID the ID of the entity to select
   * @param criteria the Criteria object
   * @param orderByClause the 'order by' clause to use, i.e. "last_name, first_name desc"
   * @param fetchCount the maximum number of records to fetch from the result
   * @see org.jminor.common.db.CriteriaSet
   * @see org.jminor.framework.db.criteria.PropertyCriteria
   * @see org.jminor.framework.db.criteria.EntityKeyCriteria
   */
  public EntityCriteria(final String entityID, final Criteria criteria, final String orderByClause,
                        final int fetchCount) {
    if (entityID == null)
      throw new IllegalArgumentException("Can not instantiate EntityCriteria without entityID");
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
   * @return the Criteria object
   */
  public Criteria getCriteria() {
    return criteria;
  }

  /**
   * @return the maximum number of records to fetch from the result
   */
  public int getFetchCount() {
    return fetchCount;
  }

  /**
   * Returns a where condition based on this EntityCriteria
   * @param database the Dbms instance
   * @return a where condition based on this EntityCriteria
   */
  public String asString(final Dbms database) {
    return getEntityID() + " " + getWhereClause(database);
  }

  /**
   * @param database the Dbms instance
   * @return the where clause
   */
  public String getWhereClause(final Dbms database) {
    return getWhereClause(database, true);
  }

  /**
   * @param database the Dbms instance
   * @param includeWhereKeyword if false AND is used instead of the WHERE keyword
   * @return a where clause base on this criteria
   */
  public String getWhereClause(final Dbms database, final boolean includeWhereKeyword) {
    final String criteriaString = criteria == null ? "" : criteria.asString(database);

    return criteriaString.length() > 0 ? (includeWhereKeyword ? "where " : "and ") + criteriaString : "";
  }

  public EntityCriteria setOrderByClause(final String orderByClause) {
    this.orderByClause = orderByClause;
    return this;
  }

  /**
   * @return the order by clause specified by this criteria
   */
  public String getOrderByClause() {
    return orderByClause;
  }

  public static EntityCriteria propertyCriteria(final String entityID, final String propertyID,
                                                final SearchType searchType, final Object... values) {
    return propertyCriteria(entityID, propertyID, searchType, -1, values);
  }

  public static EntityCriteria propertyCriteria(final String entityID, final String propertyID,
                                                final SearchType searchType, final int fetchCount,
                                                final Object... values) {
    return propertyCriteria(entityID, propertyID, searchType, null, fetchCount, values);
  }

  public static EntityCriteria propertyCriteria(final String entityID, final String propertyID,
                                                final SearchType searchType, final String orderByClause,
                                                final int fetchCount, final Object... values) {
    return new EntityCriteria(entityID, new PropertyCriteria(EntityRepository.getProperty(entityID, propertyID),
            searchType, values), orderByClause, fetchCount);
  }
}
