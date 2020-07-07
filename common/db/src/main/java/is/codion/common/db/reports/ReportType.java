/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.reports;

import java.sql.Connection;

/**
 * Identifies a report.
 * @param <T> the report type
 * @param <R> the report result type
 * @param <P> the report parameters type
 */
public interface ReportType<T, R, P> {

  /**
   * @return the report name
   */
  String getName();

  /**
   * Fills the given report.
   * @param connection the connection
   * @param report the report to fill
   * @param parameters the parameters
   * @return a report result
   * @throws ReportException in case of an exception
   */
  R fillReport(Connection connection, Report<T, R, P> report, P parameters) throws ReportException;
}
