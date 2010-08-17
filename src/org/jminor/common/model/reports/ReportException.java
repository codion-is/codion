/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.reports;

/**
 * An exception occurring during report generation.
 */
public class ReportException extends Exception {
  public ReportException(final Throwable cause) {
    super(cause);
  }
}
