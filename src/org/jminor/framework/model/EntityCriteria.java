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
  private final boolean isRangeCriteria;
  private final int recordCount;

  private boolean tableHasAuditColumns = false;
  private String whereClause;

  public EntityCriteria(final String entityID) {
    this(entityID, null);
  }

  public EntityCriteria(final String entityID, final ICriteria criteria) {
    this(entityID, criteria, false);
  }

  public EntityCriteria(final String entityID, final ICriteria criteria,
                        final boolean isRangeCriteria) {
    this(entityID, criteria, isRangeCriteria, -1);
  }

  public EntityCriteria(final String entityID, final ICriteria criteria,
                        final int recordCount) {
    this(entityID, criteria, false, recordCount);
  }

  public EntityCriteria(final String entityID, final ICriteria criteria,
                        final boolean isRangeCriteria, final int recordCount) {
    this.entityID = entityID;
    this.criteria = criteria;
    this.isRangeCriteria = isRangeCriteria;
    this.recordCount = recordCount;
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
   * @return Value for property 'rangeCriteria'.
   */
  public boolean isRangeCriteria() {
    return isRangeCriteria;
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

  public boolean tableHasAuditColumns() {
    return tableHasAuditColumns;
  }

  /**
   * @param tableHasAuditColumns Value to set for property 'tableHasAuditColumns'.
   */
  public void setTableHasAuditColumns(final boolean tableHasAuditColumns) {
    this.tableHasAuditColumns = tableHasAuditColumns;
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

  public String getWhereClause(final boolean includeWhere) {
    if (whereClause != null)
      return whereClause;

    final String criteriaString = criteria == null ? "" : criteria.toString();
    if (criteriaString.length() > 0)
      whereClause = (includeWhere ? "where " : "and ") + criteriaString;
    else
      whereClause = "";

    return whereClause;
  }
}
