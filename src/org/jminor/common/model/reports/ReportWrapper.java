/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model.reports;

import java.sql.Connection;

/**
 * User: Bjorn Darri<br>
 * Date: 23.5.2010<br>
 */
public interface ReportWrapper<R> {

  /**
   * @return the name of the report
   */
  String getReportName();

  /**
   * Fills the report using the given database connection
   * @param connection the connection to use for the report generation
   * @return a filled report ready for display
   * @throws ReportException in case of an exception
   */
  ReportResult<R> fillReport(final Connection connection) throws ReportException;

  /**
   * Fills the report using the given data wrapper
   * @param dataWrapper the data provider to use for the report generation
   * @return a filled report ready for display
   * @throws ReportException in case of an exception
   */
  ReportResult<R> fillReport(final ReportDataWrapper dataWrapper) throws ReportException;
}
