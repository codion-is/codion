/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.reports;

import java.io.Serializable;
import java.sql.Connection;

/**
 * @param <T> the report type
 * @param <R> the report result type
 * @param <P> the report parameters type
 */
public interface Report<T, R, P> extends Serializable {

  /**
   * @return the report name
   */
  String getName();

  /**
   * Fills the given report.
   * @param connection the connection
   * @param reportWrapper the report to fill
   * @param parameters the parameters
   * @return a report result
   * @throws ReportException in case of an exception
   */
  R fillReport(Connection connection, ReportWrapper<T, R, P> reportWrapper, P parameters) throws ReportException;
}
