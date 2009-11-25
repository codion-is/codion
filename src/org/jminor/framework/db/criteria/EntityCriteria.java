/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.criteria;

import org.jminor.common.db.Criteria;
import org.jminor.common.db.dbms.Database;
import org.jminor.framework.domain.EntityRepository;

import java.io.Serializable;

/**
 * A class encapsulating query criteria parameters
 */
public class EntityCriteria implements Serializable {

  private static final long serialVersionUID = 1;

  private final String entityID;
  private final Criteria criteria;

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
    if (entityID == null)
      throw new IllegalArgumentException("Can not instantiate EntityCriteria without entityID");
    this.entityID = entityID;
    this.criteria = criteria;
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
   * Returns a where condition based on this EntityCriteria
   * @param database the Database instance
   * @return a where condition based on this EntityCriteria
   */
  public String asString(final Database database) {
    return EntityRepository.getTableName(getEntityID()) + " " + getWhereClause(database);
  }

  /**
   * @param database the Database instance
   * @return the where clause
   */
  public String getWhereClause(final Database database) {
    return getWhereClause(database, true);
  }

  /**
   * @param database the Database instance
   * @param includeWhereKeyword if false AND is used instead of the WHERE keyword
   * @return a where clause base on this criteria
   */
  public String getWhereClause(final Database database, final boolean includeWhereKeyword) {
    final String criteriaString = criteria == null ? "" : criteria.asString(database);

    return criteriaString.length() > 0 ? (includeWhereKeyword ? "where " : "and ") + criteriaString : "";
  }
}
