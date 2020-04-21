/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.nextreports.model;

import org.jminor.common.db.reports.ReportWrapper;

/**
 * Factory for {@link ReportWrapper} based on NextReports.
 */
public final class NextReports {

  private NextReports() {}

  /**
   * Instantiates a new {@link ReportWrapper} based on NextReports.
   * @param reportPath the report path, relative to the central report path {@link ReportWrapper#REPORT_PATH}.
   * @param format the format
   * @return a report wrapper
   */
  public static NextReportWrapper nextReportsWrapper(final String reportPath, final String format) {
    return new DefaultNextReportWrapper(reportPath, format);
  }
}
