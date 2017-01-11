/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.reports;

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
