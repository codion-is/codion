/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.common.db.reports;

/**
 * An exception occurring during report generation.
 */
public class ReportException extends Exception {

  /**
   * Instantiates a new ReportException
   * @param message the exception message
   */
  public ReportException(final String message) {
    super(message);
  }

  /**
   * Instantiates a new ReportException
   * @param cause the root cause
   */
  public ReportException(final Throwable cause) {
    super(cause);
  }
}
