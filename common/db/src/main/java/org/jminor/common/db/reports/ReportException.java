/*
 * Chinook.Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.reports;

/**
 * An exception occurring during report generation.
 */
public class ReportException extends Exception {
  /**
   * Instantiates a new ReportException
   * @param cause the root cause
   */
  public ReportException(final Throwable cause) {
    super(cause);
  }
}
