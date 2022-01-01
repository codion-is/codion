/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.report;

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
   * @param message the exception message
   * @param cause the root cause
   */
  public ReportException(final String message, final Throwable cause) {
    super(message, cause);
  }

  /**
   * Instantiates a new ReportException
   * @param cause the root cause
   */
  public ReportException(final Throwable cause) {
    super(cause);
  }
}
