/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.nextreports.model;

import org.jminor.common.db.reports.ReportResult;

import java.io.Serializable;

/**
 * A NextReports {@link ReportResult} implementation
 */
public final class NextReportsResultWrapper implements ReportResult<NextReportsResult>, Serializable {

  private static final long serialVersionUID = 1;

  private final NextReportsResult result;

  /**
   * Instantiates a new {@link NextReportsResultWrapper} instance based on the given result
   * @param result the report result
   */
  public NextReportsResultWrapper(final NextReportsResult result) {
    this.result = result;
  }

  @Override
  public NextReportsResult getResult() {
    return result;
  }
}
