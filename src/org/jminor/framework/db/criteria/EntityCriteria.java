/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.criteria;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.model.Util;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.Property;

import java.io.Serializable;
import java.util.List;

/**
 * A class encapsulating query criteria parameters.
 */
public class EntityCriteria implements Serializable {

  private static final long serialVersionUID = 1;

  private final String entityID;
  private final Criteria<Property> criteria;

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
   * @see org.jminor.common.db.criteria.CriteriaSet
   * @see org.jminor.framework.db.criteria.PropertyCriteria
   * @see org.jminor.framework.db.criteria.EntityKeyCriteria
   */
  public EntityCriteria(final String entityID, final Criteria<Property> criteria) {
    Util.rejectNullValue(entityID);
    this.entityID = entityID;
    this.criteria = criteria;
  }

  /**
   * @return the values the underlying criteria is based on, if any
   */
  public List<Object> getValues() {
    return criteria == null ? null : criteria.getValues();
  }

  /**
   * @return the properties of the values the underlying criteria is based on, if any
   */
  public List<Property> getValueProperties() {
    return criteria == null ? null : criteria.getValueKeys();
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
   * @return a where condition based on this EntityCriteria
   */
  public String asString() {
    return EntityRepository.getTableName(getEntityID()) + " " + getWhereClause();
  }

  /**
   * @return the where clause
   */
  public String getWhereClause() {
    return getWhereClause(true);
  }

  /**
   * @param includeWhereKeyword if false AND is used instead of the WHERE keyword
   * @return a where clause base on this criteria
   */
  public String getWhereClause(final boolean includeWhereKeyword) {
    final String criteriaString = criteria == null ? "" : criteria.asString();

    return criteriaString.length() > 0 ? (includeWhereKeyword ? "where " : "and ") + criteriaString : "";
  }
}
