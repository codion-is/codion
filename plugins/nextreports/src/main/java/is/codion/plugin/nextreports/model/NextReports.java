/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.nextreports.model;

import is.codion.common.db.report.Report;

/**
 * Factory for {@link Report} based on NextReports.
 */
public final class NextReports {

  private NextReports() {}

  /**
   * Instantiates a new {@link Report} based on NextReports.
   * @param reportPath the report path, relative to the central report path {@link Report#REPORT_PATH}.
   * @param format the format
   * @return a report wrapper
   */
  public static NextReport nextReport(String reportPath, String format) {
    return new DefaultNextReport(reportPath, format);
  }
}
