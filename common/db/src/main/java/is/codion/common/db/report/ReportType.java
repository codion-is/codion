/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.report;

import java.sql.Connection;

/**
 * Identifies a report.
 * A factory for {@link ReportType} instances.
 * @param <T> the report type
 * @param <R> the report result type
 * @param <P> the report parameters type
 */
public interface ReportType<T, R, P> {

  /**
   * @return the report name
   */
  String name();

  /**
   * Fills the given report.
   * @param report the report to fill
   * @param connection the connection to use
   * @param parameters the report parameters
   * @return a report result
   * @throws ReportException in case of an exception
   */
  R fillReport(Report<T, R, P> report, Connection connection, P parameters) throws ReportException;

  /**
   * Instantiates a new Report instance with the given name.
   * @param name the report name
   * @param <T> the report type
   * @param <R> the report result type
   * @param <P> the report parameters type
   * @return a report
   */
  static <T, R, P> ReportType<T, R, P> reportType(String name) {
    return new DefaultReportType<>(name);
  }
}
