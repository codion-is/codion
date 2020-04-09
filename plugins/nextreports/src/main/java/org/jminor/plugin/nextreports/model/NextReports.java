/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.nextreports.model;

import org.jminor.common.db.reports.ReportWrapper;

import java.util.Map;

/**
 * Factory for {@link ReportWrapper} based on NextReports.
 */
public final class NextReports {

  /**
   * Instantiates a new {@link ReportsWrapper} based on NextReports.
   * @param reportPath the path to the report
   * @param reportParameters the report parameters
   * @param format the format
   * @return a report wrapper
   */
  public static ReportWrapper<NextReportsResult, Void> NextReportsWrapper(final String reportPath, final Map<String, Object> reportParameters, final String format) {
    return new NextReportsWrapper(reportPath, reportParameters, format);
  }
}
