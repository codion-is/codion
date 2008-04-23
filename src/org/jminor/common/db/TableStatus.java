/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

import java.io.Serializable;

/**
 * A class encapsulating the status of the data in a table,
 * the record cound and time of last modification, in case the table
 * has the required stamp columns
 * @deprecated 
 */
public class TableStatus implements Serializable {

  private Integer recordCount;
  private Long lastChange;
  private boolean tableHasAuditColumns = true;

  /** Constructs a new TableStatus. */
  public TableStatus() {
    this(-1);
  }

  public TableStatus(final int recordCount) {
    this(recordCount, Long.MAX_VALUE);
  }

  public TableStatus(final int recordCount, final long lastChange) {
    this.recordCount = recordCount;
    this.lastChange = lastChange;
  }

  /** {@inheritDoc} */
  public boolean equals(final Object object) {
    return object instanceof TableStatus && equals((TableStatus) object);
  }

  public boolean equals(TableStatus s) {
    return this.recordCount.equals(s.getRecordCount())
      && this.lastChange.equals(s.getLastChange());
  }

  public boolean tableHasAuditColumns() {
    return this.tableHasAuditColumns;
  }

  /**
   * @param val Value to set for property 'tableHasAuditColumns'.
   */
  public void setTableHasAuditColumns(boolean val) {
    this.tableHasAuditColumns = val;
  }

  /** {@inheritDoc} */
  public String toString() {
    return "Record count: " + getRecordCount() + ", Last change: " + getLastChange();
  }

  /**
   * @return Value for property 'null'.
   */
  public boolean isNull() {
    return recordCount < 0 && lastChange == Long.MAX_VALUE;
  }

  public void setNull() {
    recordCount = -1;
    lastChange = Long.MAX_VALUE;
  }

  /**
   * @return Value for property 'lastChange'.
   */
  public Long getLastChange() {
    return lastChange;
  }

  /**
   * @param lastChange Value to set for property 'lastChange'.
   */
  public void setLastChange(Long lastChange) {
    this.lastChange = lastChange;
  }

  /**
   * @return Value for property 'recordCount'.
   */
  public Integer getRecordCount() {
    return recordCount;
  }

  /**
   * @param recordCount Value to set for property 'recordCount'.
   */
  public void setRecordCount(Integer recordCount) {
    this.recordCount = recordCount;
  }
}
