/*
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.report;

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
