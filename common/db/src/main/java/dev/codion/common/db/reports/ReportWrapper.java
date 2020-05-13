/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.common.db.reports;

import dev.codion.common.Configuration;
import dev.codion.common.value.PropertyValue;

import java.io.Serializable;
import java.sql.Connection;

import static dev.codion.common.Util.nullOrEmpty;

/**
 * A simple wrapper for a report
 * @param <T> the report type
 * @param <R> the report result type
 * @param <P> the report parameters type
 */
public interface ReportWrapper<T, R, P> extends Serializable {

  /**
   * The report path used for file based report generation.
   */
  PropertyValue<String> REPORT_PATH = Configuration.stringValue("codion.report.path", null);

  /**
   * Specifies whether to cache reports when loaded from disk/network, this prevents "hot deploy" of reports.<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  PropertyValue<Boolean> CACHE_REPORTS = Configuration.booleanValue("codion.report.cacheReports", true);

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
   * @return the value associated with {@link ReportWrapper#REPORT_PATH}
   * @throws IllegalArgumentException in case it is not specified
   */
  static String getReportPath() {
    final String path = REPORT_PATH.get();
    if (nullOrEmpty(path)) {
      throw new IllegalArgumentException(REPORT_PATH + " property is not specified");
    }

    return path;
  }

  /**
   * Returns a full report path, combined from the report location specified by {@link #REPORT_PATH}
   * and the given report path.
   * @param reportPath the report path relative to {@link ReportWrapper#REPORT_PATH}.
   * @return a full report path
   * @throws IllegalArgumentException in case {@link ReportWrapper#REPORT_PATH} is not specified
   */
  static String getFullReportPath(final String reportPath) {
    final String slash = "/";
    final String reportLocation = getReportPath();
    final StringBuilder builder = new StringBuilder(reportLocation);
    if (!reportLocation.endsWith(slash) && !reportPath.startsWith(slash)) {
      builder.append(slash);
    }

    return builder.append(reportPath).toString();
  }
}
