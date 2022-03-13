/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.report;

import is.codion.common.Configuration;
import is.codion.common.properties.PropertyValue;

import java.sql.Connection;

/**
 * A wrapper for a report
 * @param <T> the report type
 * @param <R> the report result type
 * @param <P> the report parameters type
 */
public interface Report<T, R, P> {

  /**
   * The report path used for file based report generation.
   */
  PropertyValue<String> REPORT_PATH = Configuration.stringValue("codion.report.path")
          .build();

  /**
   * Specifies whether to cache reports when loaded from disk/network, this prevents "hot deploy" of reports.<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  PropertyValue<Boolean> CACHE_REPORTS = Configuration.booleanValue("codion.report.cacheReports")
          .defaultValue(true)
          .build();

  /**
   * Loads and fills the report using the given database connection
   * @param connection the connection to use for the report generation
   * @param parameters the report parameters, if any
   * @return a filled report ready for display
   * @throws ReportException in case of an exception
   */
  R fillReport(Connection connection, P parameters) throws ReportException;

  /**
   * Loads the report this report wrapper is based on.
   * @return a loaded report object
   * @throws ReportException in case of an exception
   */
  T loadReport() throws ReportException;

  /**
   * @return the value associated with {@link Report#REPORT_PATH}
   * @throws IllegalStateException in case it is not specified
   */
  static String getReportPath() {
    return REPORT_PATH.getOrThrow();
  }

  /**
   * Returns a full report path, combined from the report location specified by {@link #REPORT_PATH}
   * and the given report path.
   * @param reportPath the report path relative to {@link Report#REPORT_PATH}.
   * @return a full report path
   * @throws IllegalStateException in case {@link Report#REPORT_PATH} is not specified
   */
  static String getFullReportPath(String reportPath) {
    final String slash = "/";
    String reportLocation = getReportPath();
    StringBuilder builder = new StringBuilder(reportLocation);
    if (!reportLocation.endsWith(slash) && !reportPath.startsWith(slash)) {
      builder.append(slash);
    }

    return builder.append(reportPath).toString();
  }
}
