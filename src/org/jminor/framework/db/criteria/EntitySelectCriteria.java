/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.criteria;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.db.dbms.Database;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.Property;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A class encapsulating select query parameters.
 */
public class EntitySelectCriteria extends EntityCriteria {

  private final int fetchCount;
  private final String orderByClause;
  private int currentFetchDepth = 0;
  private Map<String, Integer> foreignKeyFetchDepths;
  private int fetchDepth;

  /**
   * Instantiates a new EntityCriteria, which includes all the underlying entities
   * @param entityID the ID of the entity to select
   */
  public EntitySelectCriteria(final String entityID) {
    this(entityID, null);
  }

  /**
   * Instantiates a new EntityCriteria
   * @param entityID the ID of the entity to select
   * @param criteria the Criteria object
   * @see org.jminor.common.db.criteria.CriteriaSet
   * @see org.jminor.framework.db.criteria.PropertyCriteria
   * @see org.jminor.framework.db.criteria.EntityKeyCriteria
   */
  public EntitySelectCriteria(final String entityID, final Criteria criteria) {
    this(entityID, criteria, null);
  }

  /**
   * Instantiates a new EntityCriteria
   * @param entityID the ID of the entity to select
   * @param criteria the Criteria object
   * @param orderByClause the 'order by' clause to use, i.e. "last_name, first_name desc"
   * @see org.jminor.common.db.criteria.CriteriaSet
   * @see org.jminor.framework.db.criteria.PropertyCriteria
   * @see org.jminor.framework.db.criteria.EntityKeyCriteria
   */
  public EntitySelectCriteria(final String entityID, final Criteria criteria, final String orderByClause) {
    this(entityID, criteria, orderByClause, -1);
  }

  /**
   * Instantiates a new EntityCriteria
   * @param entityID the ID of the entity to select
   * @param criteria the Criteria object
   * @param fetchCount the maximum number of records to fetch from the result
   * @see org.jminor.common.db.criteria.CriteriaSet
   * @see org.jminor.framework.db.criteria.PropertyCriteria
   * @see org.jminor.framework.db.criteria.EntityKeyCriteria
   */
  public EntitySelectCriteria(final String entityID, final Criteria criteria, final int fetchCount) {
    this(entityID, criteria, null, fetchCount);
  }

  /**
   * Instantiates a new EntityCriteria
   * @param entityID the ID of the entity to select
   * @param criteria the Criteria object
   * @param orderByClause the 'order by' clause to use, i.e. "last_name, first_name desc"
   * @param fetchCount the maximum number of records to fetch from the result
   * @see org.jminor.common.db.criteria.CriteriaSet
   * @see org.jminor.framework.db.criteria.PropertyCriteria
   * @see org.jminor.framework.db.criteria.EntityKeyCriteria
   */
  public EntitySelectCriteria(final String entityID, final Criteria criteria, final String orderByClause,
                              final int fetchCount) {
    super(entityID, criteria);
    this.fetchCount = fetchCount;
    this.orderByClause = orderByClause;
    this.foreignKeyFetchDepths = initializeForeignKeyFetchDepths(entityID);
  }

  /**
   * Returns a where condition based on this EntityCriteria
   * @param database the Database instance
   * @param valueProvider responsible for providing the actual sql string values
   * @return a where condition based on this EntityCriteria
   */
  @Override
  public String asString(final Database database, final Criteria.ValueProvider valueProvider) {
    return EntityRepository.getSelectTableName(getEntityID()) + " " + getWhereClause(database, valueProvider);
  }

  /**
   * @return the maximum number of records to fetch from the result
   */
  public int getFetchCount() {
    return fetchCount;
  }

  /**
   * @return the order by clause specified by this criteria
   */
  public String getOrderByClause() {
    return orderByClause;
  }

  public int getCurrentFetchDepth() {
    return currentFetchDepth;
  }

  public EntitySelectCriteria setCurrentFetchDepth(final int currentFetchDepth) {
    this.currentFetchDepth = currentFetchDepth;
    return this;
  }

  public EntitySelectCriteria setFetchDepth(final String fkPropertyID, final int maxFetchDepth) {
    this.foreignKeyFetchDepths.put(fkPropertyID, maxFetchDepth);
    return this;
  }

  public int getFetchDepth(final String fkPropertyID) {
    if (foreignKeyFetchDepths.containsKey(fkPropertyID))
      return foreignKeyFetchDepths.get(fkPropertyID);

    return 0;
  }

  public int getFetchDepth() {
    return fetchDepth;
  }

  public EntitySelectCriteria setFetchDepth(final int fetchDepth) {
    this.fetchDepth = fetchDepth;
    return this;
  }

  private Map<String, Integer> initializeForeignKeyFetchDepths(final String entityID) {
    final Collection<Property.ForeignKeyProperty > properties = EntityRepository.getForeignKeyProperties(entityID);
    final Map<String, Integer> depths = new HashMap<String, Integer>(properties.size());
    for (final Property.ForeignKeyProperty property : properties)
      depths.put(property.getPropertyID(), property.getFetchDepth());

    return depths;
  }
}