/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.reports;

/**
 * A factory for {@link ReportType} instances.
 */
public final class Reports {

  private Reports() {}

  /**
   * Instantiates a new Report instance with the given name.
   * @param name the report name
   * @param <T> the report type
   * @param <R> the report result type
   * @param <P> the report parameters type
   * @return a report
   */
  public static <T, R, P> ReportType<T, R, P> reportType(final String name) {
    return new DefaultReportType<>(name);
  }
}
