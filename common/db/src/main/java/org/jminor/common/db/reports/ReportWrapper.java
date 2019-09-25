/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.reports;

import java.sql.Connection;

/**
 * A simple wrapper for a report
 * @param <R> the type of the report being wrapped.
 * @param <D> the type of the report datasource
 */
public interface ReportWrapper<R, D> {

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
   * Fills the report using the data source wrapped by the given data wrapper
   * @param dataWrapper the data provider to use for the report generation
   * @return a filled report ready for display
   * @throws ReportException in case of an exception
   */
  ReportResult<R> fillReport(final ReportDataWrapper<D> dataWrapper) throws ReportException;
}
